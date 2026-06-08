package com.cmenu.ui;

import com.cmenu.ui.section.Section;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class PlayerLocationOverride {

    private static boolean enabled = false;

    private PlayerLocationOverride() {}

    /**
     * Dynamically reads use-player-location from config.yml.
     * Called once per reload by CursorMenuPlugin.loadConfig().
     */
    public static void reload(boolean cfgValue) {
        enabled = cfgValue;
    }

    /**
     * If an override is needed, replaces the section's camera coordinates
     * with the player's current position and orientation.
     * Returns true if overridden; false if the original values are kept.
     */
    public static boolean apply(Player player, Section section) {
        if (!enabled) {
            return false;           // Keep original settings
        }
        Location loc = player.getLocation();
        section.world      = loc.getWorld().getName();
        section.cameraX    = loc.getX();
        section.cameraY    = loc.getY();
        section.cameraZ    = loc.getZ();
        section.yaw        = loc.getYaw();
        section.pitch      = loc.getPitch();
        return true;
    }
}