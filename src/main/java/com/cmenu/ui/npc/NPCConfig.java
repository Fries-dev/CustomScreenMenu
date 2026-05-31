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
 * NPC镜像系统配置类
 * 管理NPC相关的所有配置项
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
        header.add("NPC镜像系统配置文件");
        header.add("CustomScreenMenu NPC Mirror Module");
        header.add("===========================================");
        header.add("");
        header.add("enabled: 是否启用NPC镜像功能");
        header.add("debug-mode: 调试模式，输出详细日志");
        header.add("");
        header.add("offset: NPC相对于玩家位置的偏移量");
        header.add("default-yaw/pitch: NPC默认朝向");
        header.add("");
        header.add("auto-rotate: 自动旋转设置");
        header.add("sync: 同步设置（装备、皮肤）");
        header.add("disabled-worlds: 禁用NPC的世界列表");
        header.add("disabled-players: 禁用NPC的玩家UUID列表");
        header.add("");
        header.add("menu-settings: 菜单关联设置");
        header.add("  enable-for-all-menus: 是否对所有菜单启用NPC（true=全部启用）");
        header.add("  enabled-menus: 启用NPC的菜单列表（仅当enable-for-all-menus为false时生效）");
        header.add("  use-whitelist-mode: 是否使用白名单模式（true=仅列表中的菜单启用，false=列表中的菜单禁用）");
        header.add("");
        header.add("name-display: NPC名称显示设置");
        header.add("  show: 是否显示NPC头顶名称");
        header.add("  format: 名称格式，支持颜色代码和占位符");
        header.add("         %player% - 玩家名称");
        header.add("         %displayname% - 玩家显示名称");
        header.add("         & 颜色代码，如 &e 表示黄色");
        header.add("         支持PlaceholderAPI变量，如 %player_level%, %player_money%");
        header.add("  always-visible: 名称是否始终可见（不被方块遮挡）");
        header.add("");
        header.add("示例配置:");
        header.add("  如果只想在特定菜单启用NPC:");
        header.add("    enable-for-all-menus: false");
        header.add("    use-whitelist-mode: true");
        header.add("    enabled-menus: [\"main_menu\", \"lobby\", \"settings\"]");
        header.add("");
        header.add("  如果想禁用某些菜单的NPC:");
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
            plugin.getLogger().severe("[NPCConfig] 无法保存配置文件: " + e.getMessage());
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
                plugin.getLogger().warning("[NPCConfig] 无效的UUID格式: " + uuidString);
            }
        }
        
        return result;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("enabled", enabled);
        save();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public float getDefaultYaw() {
        return defaultYaw;
    }

    public float getDefaultPitch() {
        return defaultPitch;
    }

    public boolean isAutoRotateEnabled() {
        return autoRotateEnabled;
    }

    public float getAutoRotateSpeed() {
        return autoRotateSpeed;
    }

    public boolean isSyncEquipment() {
        return syncEquipment;
    }

    public boolean isSyncSkin() {
        return syncSkin;
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    /**
     * 检查指定菜单是否启用NPC
     * @param menuKey 菜单键名
     * @return 是否启用NPC
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
     * 获取启用NPC的菜单列表
     */
    public List<String> getEnabledMenus() {
        return new ArrayList<>(enabledMenus);
    }

    /**
     * 添加菜单到启用列表
     */
    public void addEnabledMenu(String menuKey) {
        if (!enabledMenus.contains(menuKey)) {
            enabledMenus.add(menuKey);
            config.set("menu-settings.enabled-menus", enabledMenus);
            save();
        }
    }

    /**
     * 从启用列表移除菜单
     */
    public void removeEnabledMenu(String menuKey) {
        enabledMenus.remove(menuKey);
        config.set("menu-settings.enabled-menus", enabledMenus);
        save();
    }

    /**
     * 设置是否对所有菜单启用
     */
    public void setEnableForAllMenus(boolean enable) {
        this.enableForAllMenus = enable;
        config.set("menu-settings.enable-for-all-menus", enable);
        save();
    }

    /**
     * 设置是否使用白名单模式
     */
    public void setUseWhitelistMode(boolean useWhitelist) {
        this.useWhitelistMode = useWhitelist;
        config.set("menu-settings.use-whitelist-mode", useWhitelist);
        save();
    }

    public boolean isShowNpcName() {
        return showNpcName;
    }

    public String getNpcNameFormat() {
        return npcNameFormat;
    }

    public boolean isShowNpcNameAlways() {
        return showNpcNameAlways;
    }

    /**
     * 获取格式化后的NPC名称
     * @param playerName 玩家名称
     * @param displayName 玩家显示名称
     * @return 格式化后的名称
     */
    public String getFormattedNpcName(String playerName, String displayName) {
        if (!showNpcName) {
            return "";
        }
        
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
