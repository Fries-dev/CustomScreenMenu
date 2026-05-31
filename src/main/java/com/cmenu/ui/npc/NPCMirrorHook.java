package com.cmenu.ui.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * NPC镜像钩子类
 * 提供与CMP菜单系统的集成接口
 * 
 * 使用方法：
 * 1. 在菜单打开时调用 onMenuOpen()
 * 2. 在菜单关闭时调用 onMenuClose()
 * 3. 在菜单切换时调用 onMenuSwitch()
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

    /**
     * 初始化钩子
     */
    public void initialize() {
        if (!initialized) {
            plugin.getLogger().info("[NPCMirrorHook] NPC镜像系统未启用，跳过初始化");
            return;
        }
        
        plugin.getLogger().info("[NPCMirrorHook] NPC镜像钩子已初始化");
    }

    /**
     * 当玩家打开菜单时调用
     * 创建玩家镜像NPC
     * 
     * @param player 玩家
     * @param menuLocation 菜单位置（相机位置）
     * @param menuYaw 菜单朝向yaw
     * @param menuPitch 菜单朝向pitch
     * @param menuKey 菜单键名（用于检查是否启用NPC）
     */
    public void onMenuOpen(Player player, Location menuLocation, float menuYaw, float menuPitch, String menuKey) {
        if (!initialized || !npcManager.isEnabled()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }

                float npcYaw = menuYaw + 180;
                npcManager.createMirrorNPC(player, menuLocation, npcYaw, menuPitch, menuKey);
            }
        }.runTaskLater(plugin, 5L);
    }

    /**
     * 当玩家打开菜单时调用（不带菜单键）
     */
    public void onMenuOpen(Player player, Location menuLocation, float menuYaw, float menuPitch) {
        onMenuOpen(player, menuLocation, menuYaw, menuPitch, null);
    }

    /**
     * 当玩家打开菜单时调用（使用默认朝向）
     */
    public void onMenuOpen(Player player, Location menuLocation) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), null);
    }

    /**
     * 当玩家打开菜单时调用（使用默认朝向，带菜单键）
     */
    public void onMenuOpen(Player player, Location menuLocation, String menuKey) {
        onMenuOpen(player, menuLocation, menuLocation.getYaw(), menuLocation.getPitch(), menuKey);
    }

    /**
     * 当玩家关闭菜单时调用
     * 移除玩家镜像NPC
     * 
     * @param player 玩家
     */
    public void onMenuClose(Player player) {
        if (!initialized) {
            return;
        }

        npcManager.removeMirrorNPC(player);
    }

    /**
     * 当玩家切换菜单时调用
     * 更新NPC位置或重建NPC
     * 
     * @param player 玩家
     * @param newMenuLocation 新菜单位置
     * @param newMenuYaw 新菜单朝向yaw
     * @param newMenuPitch 新菜单朝向pitch
     * @param menuKey 新菜单键名
     */
    public void onMenuSwitch(Player player, Location newMenuLocation, float newMenuYaw, float newMenuPitch, String menuKey) {
        if (!initialized || !npcManager.isEnabled()) {
            return;
        }

        npcManager.removeMirrorNPC(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }

                float npcYaw = newMenuYaw + 180;
                npcManager.createMirrorNPC(player, newMenuLocation, npcYaw, newMenuPitch, menuKey);
            }
        }.runTaskLater(plugin, 3L);
    }

    /**
     * 当玩家切换菜单时调用（不带菜单键）
     */
    public void onMenuSwitch(Player player, Location newMenuLocation, float newMenuYaw, float newMenuPitch) {
        onMenuSwitch(player, newMenuLocation, newMenuYaw, newMenuPitch, null);
    }

    /**
     * 当玩家切换菜单时调用（使用默认朝向）
     */
    public void onMenuSwitch(Player player, Location newMenuLocation) {
        onMenuSwitch(player, newMenuLocation, newMenuLocation.getYaw(), newMenuLocation.getPitch(), null);
    }

    /**
     * 当玩家切换菜单时调用（使用默认朝向，带菜单键）
     */
    public void onMenuSwitch(Player player, Location newMenuLocation, String menuKey) {
        onMenuSwitch(player, newMenuLocation, newMenuLocation.getYaw(), newMenuLocation.getPitch(), menuKey);
    }

    /**
     * 当玩家位置更新时调用（用于更新NPC位置）
     * 
     * @param player 玩家
     * @param newLocation 新位置
     */
    public void onPlayerLocationUpdate(Player player, Location newLocation) {
        if (!initialized || !npcManager.isEnabled()) {
            return;
        }

        if (npcManager.hasNPC(player)) {
            // NPC位置通常固定，不需要频繁更新
            // 如果需要跟随玩家，可以在这里更新
        }
    }

    /**
     * 旋转玩家的NPC
     * 
     * @param player 玩家
     * @param yawOffset yaw偏移量
     */
    public void rotateNPC(Player player, float yawOffset) {
        if (!initialized) {
            return;
        }

        npcManager.rotateNPC(player, yawOffset);
    }

    /**
     * 切换玩家的NPC创建状态
     * 
     * @param player 玩家
     * @return 新状态（true=启用，false=禁用）
     */
    public boolean toggleNPCForPlayer(Player player) {
        return npcManager.toggleNPCForPlayer(player);
    }

    /**
     * 设置玩家的NPC创建状态
     * 
     * @param player 玩家
     * @param enabled 是否启用
     */
    public void setNPCEnabledForPlayer(Player player, boolean enabled) {
        npcManager.setNPCCreationEnabled(player, enabled);
    }

    /**
     * 检查NPC系统是否启用
     */
    public boolean isEnabled() {
        return initialized && npcManager.isEnabled();
    }

    /**
     * 检查玩家是否启用了NPC
     */
    public boolean isNPCEnabledForPlayer(Player player) {
        return npcManager.isNPCCreationEnabled(player);
    }

    /**
     * 获取NPC管理器
     */
    public NPCMirrorManager getNPCManager() {
        return npcManager;
    }

    /**
     * 清理所有资源
     */
    public void cleanup() {
        if (npcManager != null) {
            npcManager.cleanup();
        }
    }

    /**
     * 重载配置
     */
    public void reload() {
        if (npcManager != null) {
            npcManager.reloadConfig();
        }
    }
}
