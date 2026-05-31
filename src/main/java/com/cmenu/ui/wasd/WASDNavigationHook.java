package com.cmenu.ui.wasd;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * WASD导航钩子类
 * 提供与CMP菜单系统的集成接口
 */
public class WASDNavigationHook {

    private static WASDNavigationHook instance;
    private final JavaPlugin plugin;
    private final WASDNavigationManager navigationManager;
    private boolean initialized = false;

    private WASDNavigationHook(JavaPlugin plugin) {
        this.plugin = plugin;
        this.navigationManager = WASDNavigationManager.getInstance(plugin);
    }

    public static synchronized WASDNavigationHook getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new WASDNavigationHook(plugin);
        }
        return instance;
    }

    public static WASDNavigationHook getInstance() {
        return instance;
    }

    /**
     * 初始化钩子
     */
    public void initialize() {
        if (!navigationManager.getConfig().isEnabled()) {
            plugin.getLogger().info("[WASDHook] WASD导航系统未启用，跳过初始化");
            return;
        }

        navigationManager.initialize();
        initialized = true;
        plugin.getLogger().info("[WASDHook] WASD导航钩子已初始化");
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (navigationManager != null) {
            navigationManager.shutdown();
        }
    }

    /**
     * 当玩家打开菜单时调用
     */
    public void onMenuOpen(Player player, String menuKey, Location[] textLocations, 
                          List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (!initialized || !navigationManager.getConfig().isMenuEnabled(menuKey)) {
            return;
        }

        if (navigationManager.getConfig().isPlayerDisabled(player.getName())) {
            return;
        }

        WASDSession session = new WASDSession(plugin, player, true);
        session.setTextDisplays(textDisplays, scales);
        
        navigationManager.startSession(player, menuKey, textLocations, commands, 0);
        
        if (navigationManager.getConfig().isDebugMode()) {
            plugin.getLogger().info("[WASDHook] 为玩家 " + player.getName() + " 启用WASD导航，菜单: " + menuKey);
        }
    }

    /**
     * 当玩家关闭菜单时调用
     */
    public void onMenuClose(Player player) {
        if (!initialized) {
            return;
        }

        navigationManager.stopSession(player);
    }

    /**
     * 当菜单切换时调用
     */
    public void onMenuSwitch(Player player, String newMenuKey, Location[] textLocations,
                            List<String> commands, TextDisplay[] textDisplays, List<Double> scales) {
        if (!initialized) {
            return;
        }

        navigationManager.stopSession(player);

        if (navigationManager.getConfig().isMenuEnabled(newMenuKey) && 
            !navigationManager.getConfig().isPlayerDisabled(player.getName())) {
            
            WASDSession session = new WASDSession(plugin, player, true);
            session.setTextDisplays(textDisplays, scales);
            navigationManager.startSession(player, newMenuKey, textLocations, commands, 0);
        }
    }

    /**
     * 获取玩家当前选中的索引
     */
    public int getSelectedIndex(Player player) {
        return navigationManager.getSelectedIndex(player);
    }

    /**
     * 切换玩家的WASD导航状态
     */
    public boolean toggleWASDForPlayer(Player player) {
        return navigationManager.getConfig().togglePlayer(player.getName());
    }

    /**
     * 检查WASD导航是否启用
     */
    public boolean isEnabled() {
        return initialized && navigationManager.getConfig().isEnabled();
    }

    /**
     * 检查指定菜单是否启用WASD导航
     */
    public boolean isMenuEnabled(String menuKey) {
        return navigationManager.getConfig().isMenuEnabled(menuKey);
    }

    /**
     * 检查玩家是否可以使用WASD导航
     */
    public boolean canPlayerUseWASD(Player player, String menuKey) {
        if (!isEnabled() || !isMenuEnabled(menuKey)) {
            return false;
        }
        return !navigationManager.getConfig().isPlayerDisabled(player.getName());
    }

    /**
     * 获取导航管理器
     */
    public WASDNavigationManager getNavigationManager() {
        return navigationManager;
    }

    /**
     * 重载配置
     */
    public void reload() {
        if (navigationManager != null) {
            navigationManager.reloadConfig();
        }
    }
}
