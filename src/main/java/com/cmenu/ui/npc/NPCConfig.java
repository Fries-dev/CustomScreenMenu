package com.cmenu.ui.npc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * NPC mirror system configuration class.
 * Manages all configuration options related to NPCs.
 */
public class NPCConfig {

    private final JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private boolean enabled = true;
    private boolean debugMode = false;

    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double offsetZ = 0.0;

    private float defaultYaw = 180.0f;
    private float defaultPitch = 0.0f;

    private boolean autoRotateEnabled = false;
    private float autoRotateSpeed = 1.0f;

    private boolean syncEquipment = true;
    private boolean syncSkin = true;

    private List<String> disabledWorlds = new ArrayList<>();

    private boolean enableForAllMenus = true;
    private List<String> enabledMenus = new ArrayList<>();
    private boolean useWhitelistMode = false;

    private boolean showNpcName = true;
    private String npcNameFormat = "&e%player%";
    private boolean showNpcNameAlways = true;

    public NPCConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "npc_config.yml");

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

        config.set("offset.x", 0.0);
        config.set("offset.y", 0.0);
        config.set("offset.z", 0.0);

        config.set("default-yaw", 180.0);
        config.set("default-pitch", 0.0);

        config.set("auto-rotate.enabled", false);
        config.set("auto-rotate.speed", 1.0);

        config.set("sync.equipment", true);
        config.set("sync.skin", true);

        config.set("disabled-worlds", new ArrayList<>());
        config.set("disabled-players", new ArrayList<>());

        config.set("menu-settings.enable-for-all-menus", true);
        config.set("menu-settings.enabled-menus", new ArrayList<>());
        config.set("menu-settings.use-whitelist-mode", false);

        config.set("name-display.show", true);
        config.set("name-display.format", "&e%player%");
        config.set("name-display.always-visible", true);

        List<String> header = new ArrayList<>();
        header.add("===========================================");
        header.add("NPC Mirror System Configuration File");
        header.add("CustomScreenMenu NPC Mirror Module");
        header.add("===========================================");
        header.add("");
        header.add("enabled: Whether to enable the NPC mirror feature");
        header.add("debug-mode: Debug mode, outputs detailed logs");
        header.add("");
        header.add("offset: NPC position offset relative to the player");
        header.add("default-yaw/pitch: Default NPC facing direction");
        header.add("");
        header.add("auto-rotate: Auto-rotate settings");
        header.add("sync: Sync settings (equipment, skin)");
        header.add("disabled-worlds: List of worlds where NPCs are disabled");
        header.add("disabled-players: List of player UUIDs for whom NPCs are disabled");
        header.add("");
        header.add("menu-settings: Menu association settings");
        header.add("  enable-for-all-menus: Whether to enable NPC for all menus (true = enable all)");
        header.add("  enabled-menus: List of menus with NPC enabled (only applies when enable-for-all-menus is false)");
        header.add("  use-whitelist-mode: Whether to use whitelist mode (true = only listed menus enabled, false = listed menus disabled)");
        header.add("");
        header.add("name-display: NPC name display settings");
        header.add("  show: Whether to display the NPC's name above its head");
        header.add("  format: Name format, supports color codes and placeholders");
        header.add("         %player% - player name");
        header.add("         %displayname% - player display name");
        header.add("         & color codes, e.g. &e for yellow");
        header.add("         Supports PlaceholderAPI variables, e.g. %player_level%, %player_money%");
        header.add("  always-visible: Whether the name is always visible (not hidden behind blocks)");
        header.add("");
        header.add("Example configuration:");
        header.add("  To enable NPC only for specific menus:");
        header.add("    enable-for-all-menus: false");
        header.add("    use-whitelist-mode: true");
        header.add("    enabled-menus: [\"main_menu\", \"lobby\", \"settings\"]");
        header.add("");
        header.add("  To disable NPC for certain menus:");
        header.add("    enable-for-all-menus: true");
        header.add("    use-whitelist-mode: false");
        header.add("    enabled-menus: [\"login\", \"register\"]");
        config.options().setHeader(header);

        save();
    }

    private void readConfig() {
        enabled = config.getBoolean("enabled", true);
        debugMode = config.getBoolean("debug-mode", false);

        offsetX = config.getDouble("offset.x", 0.0);
        offsetY = config.getDouble("offset.y", 0.0);
        offsetZ = config.getDouble("offset.z", 0.0);

        defaultYaw = (float) config.getDouble("default-yaw", 180.0);
        defaultPitch = (float) config.getDouble("default-pitch", 0.0);

        autoRotateEnabled = config.getBoolean("auto-rotate.enabled", false);
        autoRotateSpeed = (float) config.getDouble("auto-rotate.speed", 1.0);

        syncEquipment = config.getBoolean("sync.equipment", true);
        syncSkin = config.getBoolean("sync.skin", true);

        disabledWorlds = config.getStringList("disabled-worlds");

        enableForAllMenus = config.getBoolean("menu-settings.enable-for-all-menus", true);
        enabledMenus = config.getStringList("menu-settings.enabled-menus");
        useWhitelistMode = config.getBoolean("menu-settings.use-whitelist-mode", false);

        showNpcName = config.getBoolean("name-display.show", true);
        npcNameFormat = config.getString("name-display.format", "&e%player%");
        showNpcNameAlways = config.getBoolean("name-display.always-visible", true);
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[NPCConfig] Failed to save configuration file: " + e.getMessage());
        }
    }

    public void saveDisabledPlayers(Set<UUID> disabledPlayers) {
        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : disabledPlayers) {
            uuidStrings.add(uuid.toString());
        }
        config.set("disabled-players", uuidStrings);
        save();
    }

    public Set<UUID> loadDisabledPlayers() {
        Set<UUID> result = new HashSet<>();
        List<String> uuidStrings = config.getStringList("disabled-players");

        for (String uuidString : uuidStrings) {
            try {
                result.add(UUID.fromString(uuidString));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[NPCConfig] Invalid UUID format: " + uuidString);
            }
        }

        return result;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("enabled", enabled);
        save();
    }

    public boolean isDebugMode() { return debugMode; }
    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
    public double getOffsetZ() { return offsetZ; }
    public float getDefaultYaw() { return defaultYaw; }
    public float getDefaultPitch() { return defaultPitch; }
    public boolean isAutoRotateEnabled() { return autoRotateEnabled; }
    public float getAutoRotateSpeed() { return autoRotateSpeed; }
    public boolean isSyncEquipment() { return syncEquipment; }
    public boolean isSyncSkin() { return syncSkin; }
    public List<String> getDisabledWorlds() { return disabledWorlds; }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    /**
     * Checks whether NPC is enabled for the specified menu.
     * @param menuKey the menu key
     * @return whether NPC is enabled
     */
    public boolean isMenuEnabled(String menuKey) {
        if (!enabled) return false;

        if (useWhitelistMode) {
            return enabledMenus.contains(menuKey);
        } else {
            return !enabledMenus.contains(menuKey);
        }
    }

    /** Returns the list of menus that have NPC enabled. */
    public List<String> getEnabledMenus() {
        return new ArrayList<>(enabledMenus);
    }

    /** Adds a menu to the enabled list. */
    public void addEnabledMenu(String menuKey) {
        if (!enabledMenus.contains(menuKey)) {
            enabledMenus.add(menuKey);
            config.set("menu-settings.enabled-menus", enabledMenus);
            save();
        }
    }

    /** Removes a menu from the enabled list. */
    public void removeEnabledMenu(String menuKey) {
        enabledMenus.remove(menuKey);
        config.set("menu-settings.enabled-menus", enabledMenus);
        save();
    }

    /** Sets whether NPC is enabled for all menus. */
    public void setEnableForAllMenus(boolean enable) {
        this.enableForAllMenus = enable;
        config.set("menu-settings.enable-for-all-menus", enable);
        save();
    }

    /** Sets whether whitelist mode is used. */
    public void setUseWhitelistMode(boolean useWhitelist) {
        this.useWhitelistMode = useWhitelist;
        config.set("menu-settings.use-whitelist-mode", useWhitelist);
        save();
    }

    public boolean isShowNpcName() { return showNpcName; }
    public String getNpcNameFormat() { return npcNameFormat; }
    public boolean isShowNpcNameAlways() { return showNpcNameAlways; }

    /**
     * Returns the formatted NPC name.
     * @param playerName the player's name
     * @param displayName the player's display name
     * @return the formatted name string
     */
    public String getFormattedNpcName(String playerName, String displayName) {
        if (!showNpcName) return "";

        String formatted = npcNameFormat
                .replace("%player%", playerName != null ? playerName : "")
                .replace("%displayname%", displayName != null ? displayName : playerName != null ? playerName : "");

        return formatted.replace('&', '§');
    }

    public void setShowNpcName(boolean show) {
        this.showNpcName = show;
        config.set("name-display.show", show);
        save();
    }

    public void setNpcNameFormat(String format) {
        this.npcNameFormat = format;
        config.set("name-display.format", format);
        save();
    }

    public void setShowNpcNameAlways(boolean always) {
        this.showNpcNameAlways = always;
        config.set("name-display.always-visible", always);
        save();
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}