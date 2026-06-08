package com.cmenu.ui.wasd;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * WASD navigation hook class.
 * Provides an integration interface with the CMP menu system.
 */
public class WASDNavigationHook {

    private static WASDNavigationHook instance;
    private final JavaPlugin plugin;
    private final WASDNavigationManager navigationManager;
    private boolean initialized = false;

    private WASDNavigationHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.navigationManager = WASDNavigationManager.getInstance(plugin);
    }

    public static synchronized WASDNavigationHook getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new WASDNavigationHook(plugin);
        }
        return instance;
    }

    public static WASDNavigationHook getInstance() { return instance; }

    /** Initializes the hook. */
    public void initialize() {
        if (!navigationManager.getConfig().isEnabled()) {
            plugin.getLogger().info("[WASDHook] WASD navigation system is not enabled, skipping initialization");
            return;
        }

        navigationManager.initialize();
        initialized = true;
        plugin.getLogger().info("[WASDHook] WASD navigation hook initialized");
    }

    /** Cleans up resources. */
    public void cleanup() {
        if (navigationManager != null) {
            navigationManager.shutdown();
        }
    }

    /** Called when a player opens a menu. */
    public void onMenuOpen(Player player, String menuKey, Location[] textLocations,
                           List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (!initialized || !navigationManager.getConfig().isMenuEnabled(menuKey)) return;

        if (navigationManager.getConfig().isPlayerDisabled(player.getName())) return;

        WASDSession session = new WASDSession(plugin, player, true);
        session.setTextDisplays(textDisplays, scales);

        navigationManager.startSession(player, menuKey, textLocations, commands, 0);

        if (navigationManager.getConfig().isDebugMode()) {
            plugin.getLogger().info("[WASDHook] WASD navigation enabled for player " + player.getName() + ", menu: " + menuKey);
        }
    }

    /** Called when a player closes a menu. */
    public void onMenuClose(Player player) {
        if (!initialized) return;
        navigationManager.stopSession(player);
    }

    /** Called when a menu switches. */
    public void onMenuSwitch(Player player, String newMenuKey, Location[] textLocations,
                             List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (!initialized) return;

        navigationManager.stopSession(player);

        if (navigationManager.getConfig().isMenuEnabled(newMenuKey) &&
                !navigationManager.getConfig().isPlayerDisabled(player.getName())) {

            WASDSession session = new WASDSession(plugin, player, true);
            session.setTextDisplays(textDisplays, scales);
            navigationManager.startSession(player, newMenuKey, textLocations, commands, 0);
        }
    }

    /** Returns the currently selected index for the player. */
    public int getSelectedIndex(Player player) {
        return navigationManager.getSelectedIndex(player);
    }

    /** Toggles the WASD navigation state for the player. */
    public boolean toggleWASDForPlayer(Player player) {
        return navigationManager.getConfig().togglePlayer(player.getName());
    }

    /** Checks whether WASD navigation is enabled. */
    public boolean isEnabled() {
        return initialized && navigationManager.getConfig().isEnabled();
    }

    /** Checks whether WASD navigation is enabled for the specified menu. */
    public boolean isMenuEnabled(String menuKey) {
        return navigationManager.getConfig().isMenuEnabled(menuKey);
    }

    /** Checks whether the player can use WASD navigation. */
    public boolean canPlayerUseWASD(Player player, String menuKey) {
        if (!isEnabled() || !isMenuEnabled(menuKey)) return false;
        return !navigationManager.getConfig().isPlayerDisabled(player.getName());
    }

    /** Returns the navigation manager. */
    public WASDNavigationManager getNavigationManager() { return navigationManager; }

    /** Reloads the configuration. */
    public void reload() {
        if (navigationManager != null) {
            navigationManager.reloadConfig();
        }
    }
}