package com.cmenu.ui.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NPC module entry point.
 * Provides a unified interface for module initialization and management.
 *
 * Integration guide:
 * 1. Call NPCModule.initialize(plugin) in the plugin's onEnable().
 * 2. Call NPCModule.shutdown() in the plugin's onDisable().
 * 3. Call NPCModule.onMenuOpen(player, location) when a menu opens.
 * 4. Call NPCModule.onMenuClose(player) when a menu closes.
 * 5. Call NPCModule.reload() when configuration is reloaded.
 */
public class NPCModule {

    private static NPCModule instance;
    private final JavaPlugin plugin;
    private final NPCMirrorManager mirrorManager;
    private final NPCMirrorHook mirrorHook;
    private boolean enabled = false;

    private NPCModule(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mirrorManager = NPCMirrorManager.getInstance(plugin);
        this.mirrorHook = NPCMirrorHook.getInstance(plugin);
        this.enabled = mirrorManager.isEnabled();
    }

    /**
     * Initializes the NPC module. Should be called in the plugin's onEnable().
     */
    public static synchronized void initialize(JavaPlugin plugin) {
        if (instance != null) {
            plugin.getLogger().warning("[NPCModule] Module already initialized, skipping duplicate initialization");
            return;
        }

        instance = new NPCModule(plugin);
        instance.mirrorHook.initialize();

        if (instance.enabled) {
            plugin.getLogger().info("[NPCModule] NPC mirror module enabled");
        } else {
            plugin.getLogger().info("[NPCModule] NPC mirror module not enabled (FancyNpcs plugin missing or disabled in config)");
        }
    }

    /**
     * Shuts down the NPC module. Should be called in the plugin's onDisable().
     */
    public static synchronized void shutdown() {
        if (instance == null) return;
        instance.mirrorHook.cleanup();
        instance.mirrorManager.cleanup();
        instance = null;
    }

    /** Returns the module instance. */
    public static NPCModule getInstance() { return instance; }

    /** Checks whether the module is enabled. */
    public static boolean isModuleEnabled() {
        return instance != null && instance.enabled;
    }

    /** Called when a menu opens. */
    public static void onMenuOpen(Player player, Location menuLocation, float yaw, float pitch, String menuKey) {
        if (instance != null && instance.enabled) {
            instance.mirrorHook.onMenuOpen(player, menuLocation, yaw, pitch, menuKey);
        }
    }

    /** Called when a menu opens (without menu key). */
    public static void onMenuOpen(Player player, Location menuLocation, float yaw, float pitch) {
        onMenuOpen(player, menuLocation, yaw, pitch, null);
    }

    /** Called when a menu opens (using default facing). */
    public static void onMenuOpen(Player player, Location menuLocation) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), null);
    }

    /** Called when a menu opens (using default facing, with menu key). */
    public static void onMenuOpen(Player player, Location menuLocation, String menuKey) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), menuKey);
    }

    /** Called when a menu closes. */
    public static void onMenuClose(Player player) {
        if (instance != null) {
            instance.mirrorHook.onMenuClose(player);
        }
    }

    /** Called when a menu switches. */
    public static void onMenuSwitch(Player player, Location newLocation, float yaw, float pitch, String menuKey) {
        if (instance != null && instance.enabled) {
            instance.mirrorHook.onMenuSwitch(player, newLocation, yaw, pitch, menuKey);
        }
    }

    /** Called when a menu switches (without menu key). */
    public static void onMenuSwitch(Player player, Location newLocation, float yaw, float pitch) {
        onMenuSwitch(player, newLocation, yaw, pitch, null);
    }

    /** Reloads the module configuration. */
    public static void reload() {
        if (instance != null) {
            instance.mirrorHook.reload();
            instance.enabled = instance.mirrorManager.isEnabled();
        }
    }

    /** Returns the mirror manager. */
    public NPCMirrorManager getMirrorManager() { return mirrorManager; }

    /** Returns the mirror hook. */
    public NPCMirrorHook getMirrorHook() { return mirrorHook; }

    /** Checks whether the module is enabled. */
    public boolean isEnabled() { return enabled; }
}