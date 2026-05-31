package com.cmenu.ui;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.Section;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

public class CursorMenuPlaceholder extends PlaceholderExpansion {

    private final CursorMenuPlugin plugin;

    public CursorMenuPlaceholder(CursorMenuPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new MenuListener(plugin), plugin);
    }

    @Override
    public String getIdentifier() {
        return "cursormenu";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        if (!(offlinePlayer instanceof Player player)) {
            return "";
        }

        // 1. 检测当前使用的摄像机菜单
        if (params.equals("current_menu")) {
            return getCurrentMenu(player);
        }

        // 2. 检测当前点击的菜单选项
        if (params.equals("selected_option")) {
            return getSelectedOption(player);
        }

        // 3. 检测当前显示的物品ID
        if (params.equals("display_item_id")) {
            return getDisplayItemId(player);
        }

        // 4. 检测当前菜单所处世界
        if (params.equals("menu_world")) {
            return getMenuWorld(player);
        }

        // 5. 检测当前菜单文字按钮坐标 (x/y/z)
        if (params.startsWith("button_")) {
            String coord = params.split("_")[1];
            return getButtonCoordinate(player, coord);
        }

        // 6. 检测当前点击的菜单项 key
        if (params.equals("clicked_option")) {
            return getClickedOption(player);
        }
        // 7. 检测当前玩家是否处于攻击或破坏方块
        if (params.equals("is_attacking_or_breaking")) {
            return isAttackingOrBreaking(player) ? "true" : "false";
        }
        // 8. 检测当前玩家移动位置选择hovered_option
        if (params.equals("hovered_option")) {
            return getHoveredOption(player);
        }

        // 9. 获取用户输入数据
        if (params.startsWith("input_")) {
            String field = params.substring(6); // 获取input_后的字段名
            return getUserInput(player, field);
        }

        // 10. 获取密码可见性状态
        if (params.equals("password_visibility")) {
            return getPasswordVisibility(player);
        }

        return null;
    }

    private boolean isAttackingOrBreaking(Player player) {
        return player.hasMetadata("cursor_is_attacking_or_breaking");
    }

    private String getCurrentMenu(Player player) {
        return plugin.getCurrentPlayerMenu(player);
    }

    private String getSelectedOption(Player player) {
        MenuLayout selected = plugin.getSelectedLayout(player);
        return selected != null ? selected.name : "";
    }

    private String getDisplayItemId(Player player) {
        return plugin.getItemDisplayManager().getPlayerActiveItemId(player);
    }

    private String getMenuWorld(Player player) {
        String menuKey = plugin.getCurrentPlayerMenu(player);
        if (menuKey == null) return "";

        Section section = CursorMenuPlugin.sectionManager.get(menuKey);
        return section != null ? section.world : "";
    }

    private String getClickedOption(Player player) {
        MenuLayout selected = plugin.getSelectedLayout(player);
        return selected != null ? selected.key : "";
    }

    private String getButtonCoordinate(Player player, String coord) {
        MenuLayout selected = plugin.getSelectedLayout(player);
        if (selected == null) return "";

        switch (coord) {
            case "x": return String.valueOf(selected.x);
            case "y": return String.valueOf(selected.y);
            case "z": return String.valueOf(selected.z);
            default: return "";
        }
    }

    private String getUserInput(Player player, String field) {
        // 首先尝试从插件实例获取（新方式）
        Map<String, String> newUserData = plugin.getPlayerInputData(player);
        if (newUserData != null && newUserData.containsKey(field)) {
            // 对于密码字段，根据密码可见性设置决定返回星号还是明文
            if ("password".equals(field) || "confirm_password".equals(field)) {
                String value = newUserData.get(field);
                if (value == null) return ""; // 添加空值检查

                boolean isVisible = plugin.getPasswordVisibility(player);
                if (!isVisible) { // 修正逻辑：isVisible为false时显示星号
                    return "*".repeat(Math.max(0, value.length())); // 防止负数
                } else {
                    return value;
                }
            }
            return newUserData.get(field);
        }

        // 如果新方式没有数据，尝试从Commands类获取用户输入数据（旧方式）
        Map<UUID, Map<String, String>> userInputData = Commands.getUserInputData();
        Map<String, String> userData = userInputData.get(player.getUniqueId());
        if (userData != null && userData.containsKey(field)) {
            // 对于密码字段，根据密码可见性设置决定返回星号还是明文
            if ("password".equals(field) || "confirm_password".equals(field)) {
                String value = userData.get(field);
                if (value == null) return ""; // 添加空值检查

                boolean isVisible = plugin.getPasswordVisibility(player);
                if (!isVisible) { // 修正逻辑：isVisible为false时显示星号
                    return "*".repeat(Math.max(0, value.length())); // 防止负数
                } else {
                    return value;
                }
            }
            return userData.get(field);
        }

        // 对于非密码字段，如果没有任何数据，返回空字符串
        // 对于密码字段，即使没有数据也应该返回占位符，以便显示输入框
        if ("password".equals(field) || "confirm_password".equals(field)) {
            boolean isVisible = plugin.getPasswordVisibility(player);
            if (!isVisible) {
                return "*".repeat(1); // 显示一个星号作为占位符
            } else {
                return ""; // 显示空内容
            }
        }

        return "";
    }

    private String getPasswordVisibility(Player player) {
        boolean isVisible = plugin.getPasswordVisibility(player);
        // 根据密码可见性状态从语言配置中获取整个按钮文本
        String languageKey = isVisible ? "password.visibility.button.shown" : "password.visibility.button.hidden";
        String defaultValue = isVisible ? "[密码:隐藏]" : "[密码:显示]";
        String buttonText = plugin.getLangMessage(languageKey, defaultValue);
        
        // 解析文本中的 PAPI 变量
        if (CursorMenuPlugin.hasPAPI) {
            buttonText = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, buttonText);
        }
        
        return buttonText;
    }

    private String getHoveredOption(Player player) {
        String menuKey = plugin.getCurrentPlayerMenu(player);
        if (menuKey == null || menuKey.isEmpty()) return "";

        Section section = CursorMenuPlugin.sectionManager.get(menuKey);
        if (section == null) return "";

        World world = Bukkit.getWorld(section.world);
        if (world == null) return "";

        ArmorStand cursor = CursorMenuPlugin.playerCursors.get(player);
        Pig camera = CursorMenuPlugin.playerSit.get(player);
        if (cursor == null || camera == null) return "";


        Location cursorLoc = cursor.getLocation();
        Location cameraLoc = camera.getLocation();

        Vector dir = cameraLoc.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Vector up = dir.getCrossProduct(right).multiply(-1);

        MenuLayout closest = null;
        double minDistance = Double.MAX_VALUE;

        for (MenuLayout layout : section.layouts.values()) {
            Vector offset = dir.clone().multiply(layout.z)
                    .add(right.clone().multiply(layout.x))
                    .add(up.clone().multiply(layout.y));
            Vector layoutVec = cameraLoc.clone().add(offset).toVector();

            double distance = cursorLoc.toVector().distance(layoutVec);
            if (distance < minDistance) {
                minDistance = distance;
                closest = layout;
            }
        }

        return closest != null && minDistance < 1.1 ? closest.key : "";
    }
}