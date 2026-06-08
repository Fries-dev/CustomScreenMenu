package com.cmenu.ui.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * NPC mirror hook class.
 * Provides an integration interface with the CMP menu system.
 *
 * Usage:
 * 1. Call onMenuOpen() when a menu is opened.
 * 2. Call onMenuClose() when a menu is closed.
 * 3. Call onMenuSwitch() when switching between menus.
 */
public class NPCMirrorHook {

    private static NPCMirrorHook instance;
    private final JavaPlugin plugin;
    private final NPCMirrorManager npcManager;

    private boolean initialized = false;

    private NPCMirrorHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.npcManager = NPCMirrorManager.getInstance(plugin);
        this.initialized = npcManager.isEnabled();
    }

    public static synchronized NPCMirrorHook getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new NPCMirrorHook(plugin);
        }
        return instance;
    }

    public static NPCMirrorHook getInstance() {
        return instance;
    }

    /** Initializes the hook. */
    public void initialize() {
        if (!initialized) {
            plugin.getLogger().info("[NPCMirrorHook] NPC mirror system is not enabled, skipping initialization");
            return;
        }

        plugin.getLogger().info("[NPCMirrorHook] NPC mirror hook initialized");
    }

    /**
     * Called when a player opens a menu. Creates the player's mirror NPC.
     *
     * @param player the player
     * @param menuLocation the menu location (camera position)
     * @param menuYaw the menu facing yaw
     * @param menuPitch the menu facing pitch
     * @param menuKey the menu key (used to check whether NPC is enabled)
     */
    public void onMenuOpen(Player player, Location menuLocation, float menuYaw, float menuPitch, String menuKey) {
        if (!initialized || !npcManager.isEnabled()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                float npcYaw = menuYaw + 180;
                npcManager.createMirrorNPC(player, menuLocation, npcYaw, menuPitch, menuKey);
            }
        }.runTaskLater(plugin, 5L);
    }

    /** Called when a player opens a menu (without menu key). */
    public void onMenuOpen(Player player, Location menuLocation, float menuYaw, float menuPitch) {
        onMenuOpen(player, menuLocation, menuYaw, menuPitch, null);
    }

    /** Called when a player opens a menu (using default facing). */
    public void onMenuOpen(Player player, Location menuLocation) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), null);
    }

    /** Called when a player opens a menu (using default facing, with menu key). */
    public void onMenuOpen(Player player, Location menuLocation, String menuKey) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), menuKey);
    }

    /**
     * Called when a player closes a menu. Removes the player's mirror NPC.
     *
     * @param player the player
     */
    public void onMenuClose(Player player) {
        if (!initialized) return;
        npcManager.removeMirrorNPC(player);
    }

    /**
     * Called when a player switches menus. Updates or rebuilds the NPC.
     *
     * @param player the player
     * @param newMenuLocation the new menu location
     * @param newMenuYaw the new menu facing yaw
     * @param newMenuPitch the new menu facing pitch
     * @param menuKey the new menu key
     */
    public void onMenuSwitch(Player player, Location newMenuLocation, float newMenuYaw, float newMenuPitch, String menuKey) {
        if (!initialized || !npcManager.isEnabled()) return;

        npcManager.removeMirrorNPC(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                float npcYaw = newMenuYaw + 180;
                npcManager.createMirrorNPC(player, newMenuLocation, npcYaw, newMenuPitch, menuKey);
            }
        }.runTaskLater(plugin, 3L);
    }

    /** Called when a player switches menus (without menu key). */
    public void onMenuSwitch(Player player, Location newMenuLocation, float newMenuYaw, float newMenuPitch) {
        onMenuSwitch(player, newMenuLocation, newMenuYaw, newMenuPitch, null);
    }

    /** Called when a player switches menus (using default facing). */
    public void onMenuSwitch(Player player, Location newMenuLocation) {
        onMenuSwitch(player, newMenuLocation, newMenuLocation.getYaw(), newMenuLocation.getPitch(), null);
    }

    /** Called when a player switches menus (using default facing, with menu key). */
    public void onMenuSwitch(Player player, Location newMenuLocation, String menuKey) {
        onMenuSwitch(player, newMenuLocation, newMenuLocation.getYaw(), newMenuLocation.getPitch(), menuKey);
    }

    /**
     * Called when a player's location updates (for updating NPC position).
     *
     * @param player the player
     * @param newLocation the new location
     */
    public void onPlayerLocationUpdate(Player player, Location newLocation) {
        if (!initialized || !npcManager.isEnabled()) return;

        if (npcManager.hasNPC(player)) {
            // NPC position is typically fixed; update here if follow-player behavior is needed
        }
    }

    /**
     * Rotates the player's NPC.
     *
     * @param player the player
     * @param yawOffset the yaw offset
     */
    public void rotateNPC(Player player, float yawOffset) {
        if (!initialized) return;
        npcManager.rotateNPC(player, yawOffset);
    }

    /**
     * Toggles the NPC creation state for the player.
     *
     * @param player the player
     * @return the new state (true = enabled, false = disabled)
     */
    public boolean toggleNPCForPlayer(Player player) {
        return npcManager.toggleNPCForPlayer(player);
    }

    /**
     * Sets the NPC creation state for the player.
     *
     * @param player the player
     * @param enabled whether to enable
     */
    public void setNPCEnabledForPlayer(Player player, boolean enabled) {
        npcManager.setNPCCreationEnabled(player, enabled);
    }

    /** Checks whether the NPC system is enabled. */
    public boolean isEnabled() {
        return initialized && npcManager.isEnabled();
    }

    /** Checks whether NPC is enabled for the player. */
    public boolean isNPCEnabledForPlayer(Player player) {
        return npcManager.isNPCCreationEnabled(player);
    }

    /** Returns the NPC manager. */
    public NPCMirrorManager getNPCManager() {
        return npcManager;
    }

    /** Cleans up all resources. */
    public void cleanup() {
        if (npcManager != null) {
            npcManager.cleanup();
        }
    }

    /** Reloads the configuration. */
    public void reload() {
        if (npcManager != null) {
            npcManager.reloadConfig();
        }
    }
}