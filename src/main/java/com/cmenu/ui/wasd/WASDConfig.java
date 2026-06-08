package com.cmenu.ui.wasd;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WASD navigation system configuration class.
 */
public class WASDConfig {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private boolean enabled = true;
    private boolean debugMode = false;

    private double horizontalThreshold = 0.5;
    private double dotProductThreshold = 0.5;
    private long selectionCooldown = 500;

    private boolean soundEnabled = true;
    private String selectionSound = "minecraft:entity.experience_orb.pickup";
    private float soundVolume = 1.0f;
    private float soundPitch = 1.0f;

    private boolean enableForAllMenus = true;
    private List<String> enabledMenus = new ArrayList<>();
    private boolean useWhitelistMode = false;

    private Set<String> disabledPlayers = new HashSet<>();

    public WASDConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "wasd_config.yml");

        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        readConfig();
    }

    private void createDefaultConfig() {
        config = new YamlConfiguration();

        config.set("enabled", true);
        config.set("debug-mode", false);

        config.set("navigation.horizontal-threshold", 0.5);
        config.set("navigation.dot-product-threshold", 0.5);
        config.set("navigation.selection-cooldown", 500);

        config.set("sound.enabled", true);
        config.set("sound.selection-sound", "minecraft:entity.experience_orb.pickup");
        config.set("sound.volume", 1.0);
        config.set("sound.pitch", 1.0);

        config.set("menu-settings.enable-for-all-menus", true);
        config.set("menu-settings.enabled-menus", new ArrayList<>());
        config.set("menu-settings.use-whitelist-mode", false);

        config.set("disabled-players", new ArrayList<>());

        List<String> header = new ArrayList<>();
        header.add("===========================================");
        header.add("WASD Navigation System Configuration File");
        header.add("CustomScreenMenu WASD Navigation Module");
        header.add("===========================================");
        header.add("");
        header.add("enabled: Whether to enable the WASD navigation feature");
        header.add("debug-mode: Debug mode, outputs detailed logs");
        header.add("");
        header.add("navigation: Navigation settings");
        header.add("  horizontal-threshold: Horizontal direction selection threshold");
        header.add("  dot-product-threshold: Left/right direction dot product threshold");
        header.add("  selection-cooldown: Selection cooldown time (milliseconds)");
        header.add("");
        header.add("sound: Sound effect settings");
        header.add("  enabled: Whether to enable sound effects");
        header.add("  selection-sound: Selection sound effect");
        header.add("  volume/pitch: Volume and pitch");
        header.add("");
        header.add("menu-settings: Menu association settings");
        header.add("  enable-for-all-menus: Whether to enable WASD navigation for all menus");
        header.add("  enabled-menus: List of menus with WASD navigation enabled");
        header.add("  use-whitelist-mode: Whether to use whitelist mode");
        config.options().setHeader(header);

        save();
    }

    private void readConfig() {
        enabled = config.getBoolean("enabled", true);
        debugMode = config.getBoolean("debug-mode", false);

        horizontalThreshold = config.getDouble("navigation.horizontal-threshold", 0.5);
        dotProductThreshold = config.getDouble("navigation.dot-product-threshold", 0.5);
        selectionCooldown = config.getLong("navigation.selection-cooldown", 500);

        soundEnabled = config.getBoolean("sound.enabled", true);
        selectionSound = config.getString("sound.selection-sound", "minecraft:entity.experience_orb.pickup");
        soundVolume = (float) config.getDouble("sound.volume", 1.0);
        soundPitch = (float) config.getDouble("sound.pitch", 1.0);

        enableForAllMenus = config.getBoolean("menu-settings.enable-for-all-menus", true);
        enabledMenus = config.getStringList("menu-settings.enabled-menus");
        useWhitelistMode = config.getBoolean("menu-settings.use-whitelist-mode", false);

        disabledPlayers = new HashSet<>(config.getStringList("disabled-players"));
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[WASDConfig] Failed to save configuration file: " + e.getMessage());
        }
    }

    /**
     * Checks whether WASD navigation is enabled for the specified menu.
     */
    public boolean isMenuEnabled(String menuKey) {
        if (!enabled) return false;

        if (useWhitelistMode) {
            return enabledMenus.contains(menuKey);
        } else {
            return !enabledMenus.contains(menuKey);
        }
    }

    /**
     * Checks whether WASD navigation is disabled for the player.
     */
    public boolean isPlayerDisabled(String playerName) {
        return disabledPlayers.contains(playerName.toLowerCase());
    }

    /**
     * Toggles the WASD navigation state for the player.
     */
    public boolean togglePlayer(String playerName) {
        String lowerName = playerName.toLowerCase();
        if (disabledPlayers.contains(lowerName)) {
            disabledPlayers.remove(lowerName);
            saveDisabledPlayers();
            return true;
        } else {
            disabledPlayers.add(lowerName);
            saveDisabledPlayers();
            return false;
        }
    }

    private void saveDisabledPlayers() {
        config.set("disabled-players", new ArrayList<>(disabledPlayers));
        save();
    }

    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isDebugMode() { return debugMode; }
    public double getHorizontalThreshold() { return horizontalThreshold; }
    public double getDotProductThreshold() { return dotProductThreshold; }
    public long getSelectionCooldown() { return selectionCooldown; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public String getSelectionSound() { return selectionSound; }
    public float getSoundVolume() { return soundVolume; }
    public float getSoundPitch() { return soundPitch; }
    public boolean isEnableForAllMenus() { return enableForAllMenus; }
    public List<String> getEnabledMenus() { return new ArrayList<>(enabledMenus); }
    public boolean isUseWhitelistMode() { return useWhitelistMode; }

    // Setters
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("enabled", enabled);
        save();
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
        config.set("debug-mode", debugMode);
        save();
    }
}