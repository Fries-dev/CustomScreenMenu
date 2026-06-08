package com.cmenu.ui.wasd;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * WASD navigation module entry point.
 * Provides a unified interface for module initialization and management.
 *
 * Integration guide:
 * 1. Call WASDModule.initialize(plugin) in the plugin's onEnable().
 * 2. Call WASDModule.shutdown() in the plugin's onDisable().
 * 3. Call WASDModule.onMenuOpen(player, menuKey, ...) when a menu opens.
 * 4. Call WASDModule.onMenuClose(player) when a menu closes.
 * 5. Call WASDModule.reload() when configuration is reloaded.
 */
public class WASDModule {

    private static WASDModule instance;
    private final JavaPlugin plugin;
    private final WASDNavigationManager navigationManager;
    private final WASDNavigationHook navigationHook;
    private boolean enabled = false;

    private WASDModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.navigationManager = WASDNavigationManager.getInstance(plugin);
        this.navigationHook = WASDNavigationHook.getInstance(plugin);
        this.enabled = navigationManager.getConfig().isEnabled();
    }

    /**
     * Initializes the WASD module. Should be called in the plugin's onEnable().
     */
    public static synchronized void initialize(JavaPlugin plugin) {
        if (instance != null) {
            plugin.getLogger().warning("[WASDModule] Module already initialized, skipping duplicate initialization");
            return;
        }

        instance = new WASDModule(plugin);
        instance.navigationHook.initialize();

        if (instance.enabled) {
            plugin.getLogger().info("[WASDModule] WASD navigation module enabled");
        } else {
            plugin.getLogger().info("[WASDModule] WASD navigation module not enabled (disabled in config)");
        }
    }

    /**
     * Shuts down the WASD module. Should be called in the plugin's onDisable().
     */
    public static synchronized void shutdown() {
        if (instance == null) return;

        instance.navigationHook.cleanup();
        instance.navigationManager.shutdown();
        instance = null;
    }

    /** Returns the module instance. */
    public static WASDModule getInstance() { return instance; }

    /** Checks whether the module is enabled. */
    public static boolean isModuleEnabled() {
        return instance != null && instance.enabled;
    }

    /** Called when a menu opens. */
    public static void onMenuOpen(Player player, String menuKey, Location[] textLocations,
                                  List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (instance != null && instance.enabled) {
            instance.navigationHook.onMenuOpen(player, menuKey, textLocations, commands, textDisplays, scales);
        }
    }

    /** Called when a menu closes. */
    public static void onMenuClose(Player player) {
        if (instance != null) {
            instance.navigationHook.onMenuClose(player);
        }
    }

    /** Called when a menu switches. */
    public static void onMenuSwitch(Player player, String newMenuKey, Location[] textLocations,
                                    List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (instance != null && instance.enabled) {
            instance.navigationHook.onMenuSwitch(player, newMenuKey, textLocations, commands, textDisplays, scales);
        }
    }

    /** Returns the currently selected index for the player. */
    public static int getSelectedIndex(Player player) {
        return instance != null ? instance.navigationManager.getSelectedIndex(player) : -1;
    }

    /** Returns the menu the player is currently in. */
    public static String getPlayerCurrentMenu(Player player) {
        return instance != null ? instance.navigationManager.getPlayerCurrentMenu(player.getUniqueId()) : null;
    }

    /** Returns the player's current location. */
    public static Location getPlayerCurrentLocation(Player player) {
        return instance != null ? instance.navigationManager.getPlayerCurrentLocation(player.getUniqueId()) : null;
    }

    /** Toggles the WASD navigation state for the player. */
    public static boolean toggleWASDForPlayer(Player player) {
        return instance != null && instance.navigationHook.toggleWASDForPlayer(player);
    }

    /** Checks whether WASD navigation is enabled for the specified menu. */
    public static boolean isMenuEnabled(String menuKey) {
        return instance != null && instance.navigationHook.isMenuEnabled(menuKey);
    }

    /** Checks whether the player can use WASD navigation. */
    public static boolean canPlayerUseWASD(Player player, String menuKey) {
        return instance != null && instance.navigationHook.canPlayerUseWASD(player, menuKey);
    }

    /** Reloads the module configuration. */
    public static void reload() {
        if (instance != null) {
            instance.navigationHook.reload();
            instance.enabled = instance.navigationManager.getConfig().isEnabled();
        }
    }

    /** Returns the navigation manager. */
    public WASDNavigationManager getNavigationManager() { return navigationManager; }

    /** Returns the navigation hook. */
    public WASDNavigationHook getNavigationHook() { return navigationHook; }

    /** Checks whether the module is enabled. */
    public boolean isEnabled() { return enabled; }
}