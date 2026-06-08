package com.cmenu.ui.wasd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * WASD navigation session.
 * Manages the WASD navigation state for a single player.
 */
public class WASDSession {

    private final JavaPlugin plugin;
    private final Player player;
    private boolean enabled;
    private TextDisplay[] textDisplays;
    private double[] originalScales;
    private List<Double> configScales;

    public WASDSession(JavaPlugin plugin, Player player, boolean enabled) {
        this.plugin = plugin;
        this.player = player;
        this.enabled = enabled;
    }

    /** Sets the text display entities. */
    public void setTextDisplays(TextDisplay[] displays, List<Double> scales) {
        this.textDisplays = displays;
        this.configScales = scales;
        this.originalScales = new double[displays.length];
        for (int i = 0; i < displays.length; i++) {
            originalScales[i] = scales.get(i);
        }
    }

    /** Called when the selected item changes. */
    public void onSelectionChanged(Player player, int oldIndex, int newIndex) {
        if (textDisplays == null) return;

        // Restore the previous selection's scale
        if (oldIndex >= 0 && oldIndex < textDisplays.length && textDisplays[oldIndex] != null) {
            double oldScale = configScales != null && oldIndex < configScales.size()
                    ? configScales.get(oldIndex) : 1.0;
            setDisplayScale(textDisplays[oldIndex], oldScale);
        }

        // Enlarge the new selection
        if (newIndex >= 0 && newIndex < textDisplays.length && textDisplays[newIndex] != null) {
            double newScale = configScales != null && newIndex < configScales.size()
                    ? configScales.get(newIndex) + 0.5 : 1.5;
            setDisplayScale(textDisplays[newIndex], newScale);
        }

        // Play the selection sound
        playSelectionSound(player);
    }

    /** Sets the scale of a display entity. */
    private void setDisplayScale(TextDisplay display, double scale) {
        if (display == null || !display.isValid()) return;

        try {
            display.setTransformation(new Transformation(
                    new Vector3f(-1, 0, 0),
                    new AxisAngle4f(),
                    new Vector3f((float) scale, (float) scale, (float) scale),
                    new AxisAngle4f()
            ));
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /** Plays the selection sound. */
    private void playSelectionSound(Player player) {
        WASDConfig config = WASDNavigationManager.getInstance().getConfig();
        if (config.isSoundEnabled()) {
            try {
                player.playSound(player.getLocation(),
                        config.getSelectionSound(),
                        config.getSoundVolume(),
                        config.getSoundPitch());
            } catch (Exception e) {
                // Ignore sound errors
            }
        }
    }

    /** Executes a command. */
    public void onExecuteCommand(Player player, String command, int index) {
        if (command == null || command.isEmpty()) return;

        WASDConfig config = WASDNavigationManager.getInstance().getConfig();

        if (config.isDebugMode()) {
            plugin.getLogger().info("[WASDNavigation] Player " + player.getName() + " executing command: " + command);
        }

        // Parse command type and execute
        String processedCmd = command;

        if (processedCmd.toLowerCase().startsWith("[console]")) {
            String cmd = processedCmd.replaceAll("\\[console\\]", "").trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        } else if (processedCmd.toLowerCase().startsWith("[op]")) {
            String cmd = processedCmd.replaceAll("\\[op\\]", "").trim();
            if (player.isOp()) {
                player.performCommand(cmd);
            } else {
                try {
                    player.setOp(true);
                    player.performCommand(cmd);
                } finally {
                    player.setOp(false);
                }
            }
        } else if (processedCmd.toLowerCase().startsWith("[player]")) {
            String cmd = processedCmd.replaceAll("\\[player\\]", "").trim();
            player.performCommand(cmd);
        } else if (processedCmd.toLowerCase().startsWith("[server]")) {
            String server = processedCmd.replaceAll("\\[server\\]", "").trim();
            player.sendPluginMessage(plugin, "BungeeCord",
                    ("Connect " + server).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } else if (processedCmd.toLowerCase().startsWith("[link]")) {
            String link = processedCmd.replaceAll("\\[link\\]", "").trim();
            player.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent(
                    ChatColor.GREEN + "Click to open link: " + link
            ));
        } else {
            player.performCommand(processedCmd);
        }
    }

    /** Checks whether the session is enabled. */
    public boolean isEnabled() { return enabled; }

    /** Sets the enabled state. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Returns the player. */
    public Player getPlayer() { return player; }
}