package com.cmenu.ui.wasd;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WASD导航PAPI变量扩展
 * 
 * 支持的变量：
 * %cmp_wasd_menu% - 当前玩家所在的WASD菜单名称（如果启用了WASD）
 * %cmp_wasd_enabled% - 当前玩家是否启用了WASD导航（true/false）
 * %cmp_wasd_index% - 当前玩家选中的索引
 * %cmp_<menu>_xyz% - 当前玩家在指定菜单中的坐标（如 %cmp_test_xyz%）
 * %cmp_<menu>_x% - 当前玩家在指定菜单中的X坐标
 * %cmp_<menu>_y% - 当前玩家在指定菜单中的Y坐标
 * %cmp_<menu>_z% - 当前玩家在指定菜单中的Z坐标
 */
public class WASDExpansion extends PlaceholderExpansion {

    private final WASDNavigationManager navigationManager;
    private final Map<UUID, String> playerCurrentMenus = new ConcurrentHashMap<>();
    private final Map<UUID, Location> playerCurrentLocations = new ConcurrentHashMap<>();

    public WASDExpansion(WASDNavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cmp";
    }

    @Override
    public @NotNull String getAuthor() {
        return "CustomScreenMenu";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) {
            return "";
        }

        Player player = offlinePlayer.getPlayer();
        if (player == null) {
            return "";
        }

        UUID playerId = player.getUniqueId();
        String lowerParams = params.toLowerCase();

        // %cmp_wasd_menu% - 当前菜单名称
        if (lowerParams.equals("wasd_menu")) {
            String menuKey = playerCurrentMenus.get(playerId);
            return menuKey != null ? menuKey : "";
        }

        // %cmp_wasd_enabled% - 是否启用WASD
        if (lowerParams.equals("wasd_enabled")) {
            WASDSession session = navigationManager.getPlayerSession(playerId);
            return session != null && session.isEnabled() ? "true" : "false";
        }

        // %cmp_wasd_index% - 当前选中索引
        if (lowerParams.equals("wasd_index")) {
            int index = navigationManager.getSelectedIndex(player);
            return String.valueOf(index);
        }

        // %cmp_<menu>_xyz% - 指定菜单的坐标
        if (lowerParams.endsWith("_xyz")) {
            String menuKey = params.substring(0, params.length() - 4);
            return getMenuCoordinate(playerId, menuKey, "xyz");
        }

        // %cmp_<menu>_x% - 指定菜单的X坐标
        if (lowerParams.endsWith("_x")) {
            String menuKey = params.substring(0, params.length() - 2);
            return getMenuCoordinate(playerId, menuKey, "x");
        }

        // %cmp_<menu>_y% - 指定菜单的Y坐标
        if (lowerParams.endsWith("_y")) {
            String menuKey = params.substring(0, params.length() - 2);
            return getMenuCoordinate(playerId, menuKey, "y");
        }

        // %cmp_<menu>_z% - 指定菜单的Z坐标
        if (lowerParams.endsWith("_z")) {
            String menuKey = params.substring(0, params.length() - 2);
            return getMenuCoordinate(playerId, menuKey, "z");
        }

        // %cmp_wasd_x% - 当前菜单的X坐标
        if (lowerParams.equals("wasd_x")) {
            return getCurrentCoordinate(playerId, "x");
        }

        // %cmp_wasd_y% - 当前菜单的Y坐标
        if (lowerParams.equals("wasd_y")) {
            return getCurrentCoordinate(playerId, "y");
        }

        // %cmp_wasd_z% - 当前菜单的Z坐标
        if (lowerParams.equals("wasd_z")) {
            return getCurrentCoordinate(playerId, "z");
        }

        // %cmp_wasd_location% - 当前菜单的完整坐标
        if (lowerParams.equals("wasd_location")) {
            return getCurrentLocationString(playerId);
        }

        return null;
    }

    /**
     * 获取指定菜单的坐标
     */
    private String getMenuCoordinate(UUID playerId, String menuKey, String type) {
        String currentMenu = playerCurrentMenus.get(playerId);
        if (currentMenu == null || !currentMenu.equalsIgnoreCase(menuKey)) {
            return "";
        }

        Location loc = playerCurrentLocations.get(playerId);
        if (loc == null) {
            return "";
        }

        return formatCoordinate(loc, type);
    }

    /**
     * 获取当前菜单的坐标
     */
    private String getCurrentCoordinate(UUID playerId, String type) {
        Location loc = playerCurrentLocations.get(playerId);
        if (loc == null) {
            return "";
        }

        return formatCoordinate(loc, type);
    }

    /**
     * 获取当前菜单的完整坐标字符串
     */
    private String getCurrentLocationString(UUID playerId) {
        String menuKey = playerCurrentMenus.get(playerId);
        Location loc = playerCurrentLocations.get(playerId);
        
        if (menuKey == null || loc == null) {
            return "";
        }

        return String.format("%s,%.2f,%.2f,%.2f", 
            menuKey, loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * 格式化坐标
     */
    private String formatCoordinate(Location loc, String type) {
        switch (type.toLowerCase()) {
            case "x":
                return String.format("%.2f", loc.getX());
            case "y":
                return String.format("%.2f", loc.getY());
            case "z":
                return String.format("%.2f", loc.getZ());
            case "xyz":
                return String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
            default:
                return "";
        }
    }

    /**
     * 更新玩家的当前菜单信息
     */
    public void updatePlayerMenu(UUID playerId, String menuKey, Location location) {
        playerCurrentMenus.put(playerId, menuKey);
        if (location != null) {
            playerCurrentLocations.put(playerId, location.clone());
        }
    }

    /**
     * 清除玩家的菜单信息
     */
    public void clearPlayerMenu(UUID playerId) {
        playerCurrentMenus.remove(playerId);
        playerCurrentLocations.remove(playerId);
    }

    /**
     * 获取玩家的当前菜单
     */
    public String getPlayerCurrentMenu(UUID playerId) {
        return playerCurrentMenus.get(playerId);
    }

    /**
     * 获取玩家的当前位置
     */
    public Location getPlayerCurrentLocation(UUID playerId) {
        return playerCurrentLocations.get(playerId);
    }
}
