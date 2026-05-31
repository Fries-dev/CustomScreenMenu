package com.cmenu.ui.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * NPC模块入口类
 * 提供统一的模块初始化和管理接口
 * 
 * 集成说明：
 * 1. 在插件 onEnable() 中调用 NPCModule.initialize(plugin)
 * 2. 在插件 onDisable() 中调用 NPCModule.shutdown()
 * 3. 在菜单打开时调用 NPCModule.onMenuOpen(player, location)
 * 4. 在菜单关闭时调用 NPCModule.onMenuClose(player)
 * 5. 在配置重载时调用 NPCModule.reload()
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
     * 初始化NPC模块
     * 应在插件 onEnable() 中调用
     */
    public static synchronized void initialize(JavaPlugin plugin) {
        if (instance != null) {
            plugin.getLogger().warning("[NPCModule] 模块已经初始化，跳过重复初始化");
            return;
        }

        instance = new NPCModule(plugin);
        instance.mirrorHook.initialize();

        if (instance.enabled) {
            plugin.getLogger().info("[NPCModule] NPC镜像模块已启用");
        } else {
            plugin.getLogger().info("[NPCModule] NPC镜像模块未启用（缺少 FancyNpcs 插件或配置已禁用）");
        }
    }

    /**
     * 关闭NPC模块
     * 应在插件 onDisable() 中调用
     */
    public static synchronized void shutdown() {
        if (instance == null) {
            return;
        }

        instance.mirrorHook.cleanup();
        instance.mirrorManager.cleanup();
        instance = null;
    }

    /**
     * 获取模块实例
     */
    public static NPCModule getInstance() {
        return instance;
    }

    /**
     * 检查模块是否启用
     */
    public static boolean isModuleEnabled() {
        return instance != null && instance.enabled;
    }

    /**
     * 当菜单打开时调用
     */
    public static void onMenuOpen(Player player, Location menuLocation, float yaw, float pitch, String menuKey) {
        if (instance != null && instance.enabled) {
            instance.mirrorHook.onMenuOpen(player, menuLocation, yaw, pitch, menuKey);
        }
    }

    /**
     * 当菜单打开时调用（不带菜单键）
     */
    public static void onMenuOpen(Player player, Location menuLocation, float yaw, float pitch) {
        onMenuOpen(player, menuLocation, yaw, pitch, null);
    }

    /**
     * 当菜单打开时调用（使用默认朝向）
     */
    public static void onMenuOpen(Player player, Location menuLocation) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), null);
    }

    /**
     * 当菜单打开时调用（使用默认朝向，带菜单键）
     */
    public static void onMenuOpen(Player player, Location menuLocation, String menuKey) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), menuKey);
    }

    /**
     * 当菜单关闭时调用
     */
    public static void onMenuClose(Player player) {
        if (instance != null) {
            instance.mirrorHook.onMenuClose(player);
        }
    }

    /**
     * 当菜单切换时调用
     */
    public static void onMenuSwitch(Player player, Location newLocation, float yaw, float pitch, String menuKey) {
        if (instance != null && instance.enabled) {
            instance.mirrorHook.onMenuSwitch(player, newLocation, yaw, pitch, menuKey);
        }
    }

    /**
     * 当菜单切换时调用（不带菜单键）
     */
    public static void onMenuSwitch(Player player, Location newLocation, float yaw, float pitch) {
        onMenuSwitch(player, newLocation, yaw, pitch, null);
    }

    /**
     * 重载模块配置
     */
    public static void reload() {
        if (instance != null) {
            instance.mirrorHook.reload();
            instance.enabled = instance.mirrorManager.isEnabled();
        }
    }

    /**
     * 获取镜像管理器
     */
    public NPCMirrorManager getMirrorManager() {
        return mirrorManager;
    }

    /**
     * 获取镜像钩子
     */
    public NPCMirrorHook getMirrorHook() {
        return mirrorHook;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
}
