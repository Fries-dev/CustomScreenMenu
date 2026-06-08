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
 * WASD navigation PlaceholderAPI expansion.
 *
 * Supported variables:
 * %cmp_wasd_menu%     - The name of the WASD menu the player is currently in (if WASD is active)
 * %cmp_wasd_enabled%  - Whether WASD navigation is enabled for the player (true/false)
 * %cmp_wasd_index%    - The index currently selected by the player
 * %cmp_<menu>_xyz%    - The player's coordinates in the specified menu (e.g. %cmp_test_xyz%)
 * %cmp_<menu>_x%      - The player's X coordinate in the specified menu
 * %cmp_<menu>_y%      - The player's Y coordinate in the specified menu
 * %cmp_<menu>_z%      - The player's Z coordinate in the specified menu
 */
public class WASDExpansion extends PlaceholderExpansion {

    private final WASDNavigationManager navigationManager;
    private final Map<UUID, String> playerCurrentMenus = new ConcurrentHashMap<>();
    private final Map<UUID, Location> playerCurrentLocations = new ConcurrentHashMap<>();

    public WASDExpansion(WASDNavigationManager navigationManager) {
        this.navigationManager = navigationManager;
    }

    @Override
    public @NotNull String getIdentifier() { return "cmp"; }

    @Override
    public @NotNull String getAuthor() { return "CustomScreenMenu"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null || !offlinePlayer.isOnline()) return "";

        Player player = offlinePlayer.getPlayer();
        if (player == null) return "";

        UUID playerId = player.getUniqueId();
        String lowerParams = params.toLowerCase();

        // %cmp_wasd_menu% - current menu name
        if (lowerParams.equals("wasd_menu")) {
            String menuKey = playerCurrentMenus.get(playerId);
            return menuKey != null ? menuKey : "";
        }

        // %cmp_wasd_enabled% - whether WASD is enabled
        if (lowerParams.equals("wasd_enabled")) {
            WASDSession session = navigationManager.getPlayerSession(playerId);
            return session != null && session.isEnabled() ? "true" : "false";
        }

        // %cmp_wasd_index% - currently selected index
        if (lowerParams.equals("wasd_index")) {
            int index = navigationManager.getSelectedIndex(player);
            return String.valueOf(index);
        }

        // %cmp_<menu>_xyz% - coordinates in the specified menu
        if (lowerParams.endsWith("_xyz")) {
            String menuKey = params.substring(0, params.length() - 4);
            return getMenuCoordinate(playerId, menuKey, "xyz");
        }

        // %cmp_<menu>_x% - X coordinate in the specified menu
        if (lowerParams.endsWith("_x")) {
            String menuKey = params.substring(0, params.length() - 2);
            return getMenuCoordinate(playerId, menuKey, "x");
        }

        // %cmp_<menu>_y% - Y coordinate in the specified menu
        if (lowerParams.endsWith("_y")) {
            String menuKey = params.substring(0, params.length() - 2);
            return getMenuCoordinate(playerId, menuKey, "y");
        }

        // %cmp_<menu>_z% - Z coordinate in the specified menu
        if (lowerParams.endsWith("_z")) {
            String menuKey = params.substring(0, params.length() - 2);
            return getMenuCoordinate(playerId, menuKey, "z");
        }

        // %cmp_wasd_x% - X coordinate in the current menu
        if (lowerParams.equals("wasd_x")) return getCurrentCoordinate(playerId, "x");

        // %cmp_wasd_y% - Y coordinate in the current menu
        if (lowerParams.equals("wasd_y")) return getCurrentCoordinate(playerId, "y");

        // %cmp_wasd_z% - Z coordinate in the current menu
        if (lowerParams.equals("wasd_z")) return getCurrentCoordinate(playerId, "z");

        // %cmp_wasd_location% - full coordinates of the current menu
        if (lowerParams.equals("wasd_location")) return getCurrentLocationString(playerId);

        return null;
    }

    /** Returns the coordinate of the specified menu for the player. */
    private String getMenuCoordinate(UUID playerId, String menuKey, String type) {
        String currentMenu = playerCurrentMenus.get(playerId);
        if (currentMenu == null || !currentMenu.equalsIgnoreCase(menuKey)) return "";

        Location loc = playerCurrentLocations.get(playerId);
        if (loc == null) return "";

        return formatCoordinate(loc, type);
    }

    /** Returns the coordinate of the current menu for the player. */
    private String getCurrentCoordinate(UUID playerId, String type) {
        Location loc = playerCurrentLocations.get(playerId);
        if (loc == null) return "";
        return formatCoordinate(loc, type);
    }

    /** Returns the full coordinate string for the current menu. */
    private String getCurrentLocationString(UUID playerId) {
        String menuKey = playerCurrentMenus.get(playerId);
        Location loc = playerCurrentLocations.get(playerId);

        if (menuKey == null || loc == null) return "";

        return String.format("%s,%.2f,%.2f,%.2f", menuKey, loc.getX(), loc.getY(), loc.getZ());
    }

    /** Formats a coordinate value. */
    private String formatCoordinate(Location loc, String type) {
        switch (type.toLowerCase()) {
            case "x":   return String.format("%.2f", loc.getX());
            case "y":   return String.format("%.2f", loc.getY());
            case "z":   return String.format("%.2f", loc.getZ());
            case "xyz": return String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
            default:    return "";
        }
    }

    /** Updates the player's current menu information. */
    public void updatePlayerMenu(UUID playerId, String menuKey, Location location) {
        playerCurrentMenus.put(playerId, menuKey);
        if (location != null) {
            playerCurrentLocations.put(playerId, location.clone());
        }
    }

    /** Clears the player's menu information. */
    public void clearPlayerMenu(UUID playerId) {
        playerCurrentMenus.remove(playerId);
        playerCurrentLocations.remove(playerId);
    }

    /** Returns the player's current menu. */
    public String getPlayerCurrentMenu(UUID playerId) {
        return playerCurrentMenus.get(playerId);
    }

    /** Returns the player's current location. */
    public Location getPlayerCurrentLocation(UUID playerId) {
        return playerCurrentLocations.get(playerId);
    }
}