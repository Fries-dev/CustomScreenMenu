package com.cmenu.ui.wasd;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * WASD导航模块入口类
 * 提供统一的模块初始化和管理接口
 * 
 * 集成说明：
 * 1. 在插件 onEnable() 中调用 WASDModule.initialize(plugin)
 * 2. 在插件 onDisable() 中调用 WASDModule.shutdown()
 * 3. 在菜单打开时调用 WASDModule.onMenuOpen(player, menuKey, ...)
 * 4. 在菜单关闭时调用 WASDModule.onMenuClose(player)
 * 5. 在配置重载时调用 WASDModule.reload()
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
     * 初始化WASD模块
     * 应在插件 onEnable() 中调用
     */
    public static synchronized void initialize(JavaPlugin plugin) {
        if (instance != null) {
            plugin.getLogger().warning("[WASDModule] 模块已经初始化，跳过重复初始化");
            return;
        }

        instance = new WASDModule(plugin);
        instance.navigationHook.initialize();

        if (instance.enabled) {
            plugin.getLogger().info("[WASDModule] WASD导航模块已启用");
        } else {
            plugin.getLogger().info("[WASDModule] WASD导航模块未启用（配置已禁用）");
        }
    }

    /**
     * 关闭WASD模块
     * 应在插件 onDisable() 中调用
     */
    public static synchronized void shutdown() {
        if (instance == null) {
            return;
        }

        instance.navigationHook.cleanup();
        instance.navigationManager.shutdown();
        instance = null;
    }

    /**
     * 获取模块实例
     */
    public static WASDModule getInstance() {
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
    public static void onMenuOpen(Player player, String menuKey, Location[] textLocations,
                                 List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (instance != null && instance.enabled) {
            instance.navigationHook.onMenuOpen(player, menuKey, textLocations, commands, textDisplays, scales);
        }
    }

    /**
     * 当菜单关闭时调用
     */
    public static void onMenuClose(Player player) {
        if (instance != null) {
            instance.navigationHook.onMenuClose(player);
        }
    }

    /**
     * 当菜单切换时调用
     */
    public static void onMenuSwitch(Player player, String newMenuKey, Location[] textLocations,
                                   List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (instance != null && instance.enabled) {
            instance.navigationHook.onMenuSwitch(player, newMenuKey, textLocations, commands, textDisplays, scales);
        }
    }

    /**
     * 获取玩家当前选中的索引
     */
    public static int getSelectedIndex(Player player) {
        if (instance != null) {
            return instance.navigationManager.getSelectedIndex(player);
        }
        return -1;
    }

    /**
     * 获取玩家当前所在的菜单
     */
    public static String getPlayerCurrentMenu(Player player) {
        if (instance != null) {
            return instance.navigationManager.getPlayerCurrentMenu(player.getUniqueId());
        }
        return null;
    }

    /**
     * 获取玩家当前的位置
     */
    public static Location getPlayerCurrentLocation(Player player) {
        if (instance != null) {
            return instance.navigationManager.getPlayerCurrentLocation(player.getUniqueId());
        }
        return null;
    }

    /**
     * 切换玩家的WASD导航状态
     */
    public static boolean toggleWASDForPlayer(Player player) {
        if (instance != null) {
            return instance.navigationHook.toggleWASDForPlayer(player);
        }
        return false;
    }

    /**
     * 检查指定菜单是否启用WASD导航
     */
    public static boolean isMenuEnabled(String menuKey) {
        return instance != null && instance.navigationHook.isMenuEnabled(menuKey);
    }

    /**
     * 检查玩家是否可以使用WASD导航
     */
    public static boolean canPlayerUseWASD(Player player, String menuKey) {
        return instance != null && instance.navigationHook.canPlayerUseWASD(player, menuKey);
    }

    /**
     * 重载模块配置
     */
    public static void reload() {
        if (instance != null) {
            instance.navigationHook.reload();
            instance.enabled = instance.navigationManager.getConfig().isEnabled();
        }
    }

    /**
     * 获取导航管理器
     */
    public WASDNavigationManager getNavigationManager() {
        return navigationManager;
    }

    /**
     * 获取导航钩子
     */
    public WASDNavigationHook getNavigationHook() {
        return navigationHook;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }
}
