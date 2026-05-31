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
 * WASD导航系统配置类
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
        header.add("WASD导航系统配置文件");
        header.add("CustomScreenMenu WASD Navigation Module");
        header.add("===========================================");
        header.add("");
        header.add("enabled: 是否启用WASD导航功能");
        header.add("debug-mode: 调试模式，输出详细日志");
        header.add("");
        header.add("navigation: 导航设置");
        header.add("  horizontal-threshold: 水平方向选择阈值");
        header.add("  dot-product-threshold: 左右方向点积阈值");
        header.add("  selection-cooldown: 选择冷却时间(毫秒)");
        header.add("");
        header.add("sound: 音效设置");
        header.add("  enabled: 是否启用音效");
        header.add("  selection-sound: 选中音效");
        header.add("  volume/pitch: 音量和音调");
        header.add("");
        header.add("menu-settings: 菜单关联设置");
        header.add("  enable-for-all-menus: 是否对所有菜单启用WASD导航");
        header.add("  enabled-menus: 启用WASD导航的菜单列表");
        header.add("  use-whitelist-mode: 是否使用白名单模式");
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
            plugin.getLogger().severe("[WASDConfig] 无法保存配置文件: " + e.getMessage());
        }
    }

    /**
     * 检查指定菜单是否启用WASD导航
     */
    public boolean isMenuEnabled(String menuKey) {
        if (!enabled) {
            return false;
        }
        
        if (enableForAllMenus) {
            if (useWhitelistMode) {
                return enabledMenus.contains(menuKey);
            } else {
                return !enabledMenus.contains(menuKey);
            }
        } else {
            if (useWhitelistMode) {
                return enabledMenus.contains(menuKey);
            } else {
                return !enabledMenus.contains(menuKey);
            }
        }
    }

    /**
     * 检查玩家是否禁用了WASD导航
     */
    public boolean isPlayerDisabled(String playerName) {
        return disabledPlayers.contains(playerName.toLowerCase());
    }

    /**
     * 切换玩家的WASD导航状态
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
