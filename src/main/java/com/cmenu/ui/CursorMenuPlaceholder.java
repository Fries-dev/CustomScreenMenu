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
        if (params.equals("current_menu")) {
            return getCurrentMenu(player);
        }
        if (params.equals("selected_option")) {
            return getSelectedOption(player);
        }
        if (params.equals("display_item_id")) {
            return getDisplayItemId(player);
        }
        if (params.equals("menu_world")) {
            return getMenuWorld(player);
        }
        if (params.startsWith("button_")) {
            String coord = params.split("_")[1];
            return getButtonCoordinate(player, coord);
        }
        if (params.equals("clicked_option")) {
            return getClickedOption(player);
        }
        if (params.equals("is_attacking_or_breaking")) {
            return isAttackingOrBreaking(player) ? "true" : "false";
        }
        if (params.equals("hovered_option")) {
            return getHoveredOption(player);
        }
        if (params.startsWith("input_")) {
            String field = params.substring(6);
            return getUserInput(player, field);
        }
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
        Map<String, String> newUserData = plugin.getPlayerInputData(player);
        if (newUserData != null && newUserData.containsKey(field)) {
            if ("password".equals(field) || "confirm_password".equals(field)) {
                String value = newUserData.get(field);
                if (value == null) return "";
                boolean isVisible = plugin.getPasswordVisibility(player);
                if (!isVisible) {
                    return "*".repeat(Math.max(0, value.length()));
                } else {
                    return value;
                }
            }
            return newUserData.get(field);
        }
        Map<UUID, Map<String, String>> userInputData = Commands.getUserInputData();
        Map<String, String> userData = userInputData.get(player.getUniqueId());
        if (userData != null && userData.containsKey(field)) {
            if ("password".equals(field) || "confirm_password".equals(field)) {
                String value = userData.get(field);
                if (value == null) return "";
                boolean isVisible = plugin.getPasswordVisibility(player);
                if (!isVisible) {
                    return "*".repeat(Math.max(0, value.length()));
                } else {
                    return value;
                }
            }
            return userData.get(field);
        }
        if ("password".equals(field) || "confirm_password".equals(field)) {
            boolean isVisible = plugin.getPasswordVisibility(player);
            if (!isVisible) {
                return "*".repeat(1);
            } else {
                return "";
            }
        }
        return "";
    }
    private String getPasswordVisibility(Player player) {
        boolean isVisible = plugin.getPasswordVisibility(player);
        String languageKey = isVisible ? "password.visibility.button.shown" : "password.visibility.button.hidden";
        String defaultValue = isVisible ? "[Password: Hide]" : "[Password: Show]";
        String buttonText = plugin.getLangMessage(languageKey, defaultValue);
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