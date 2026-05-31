package com.cmenu.ui.npc;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.NpcManager;
import de.oliver.fancynpcs.api.utils.NpcEquipmentSlot;
import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC镜像管理器
 * 负责在3D菜单中创建和管理玩家镜像NPC
 * 
 * 这是一个独立的模块，不修改CMP核心代码
 * 通过钩子方式集成到菜单系统中
 */
public class NPCMirrorManager {

    private static NPCMirrorManager instance;
    private final JavaPlugin plugin;
    
    private FancyNpcsPlugin fancyNpcsPlugin;
    private SkinsRestorer skinsRestorer;
    private boolean fancyNpcsEnabled = false;
    private boolean skinsRestorerEnabled = false;
    
    private final Map<UUID, Npc> playerNPCs = new ConcurrentHashMap<>();
    private final Set<UUID> disabledPlayers = new HashSet<>();
    
    private NPCConfig config;

    private NPCMirrorManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = new NPCConfig(plugin);
        initializeDependencies();
    }

    public static synchronized NPCMirrorManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new NPCMirrorManager(plugin);
        }
        return instance;
    }

    public static NPCMirrorManager getInstance() {
        return instance;
    }

    private void initializeDependencies() {
        if (Bukkit.getPluginManager().isPluginEnabled("FancyNpcs")) {
            try {
                fancyNpcsPlugin = (FancyNpcsPlugin) Bukkit.getPluginManager().getPlugin("FancyNpcs");
                fancyNpcsEnabled = true;
                plugin.getLogger().info("[NPCMirror] FancyNpcs 插件已找到，NPC镜像功能已启用");
            } catch (Exception e) {
                plugin.getLogger().warning("[NPCMirror] 无法加载 FancyNpcs: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("[NPCMirror] FancyNpcs 插件未找到，NPC镜像功能已禁用");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            try {
                skinsRestorer = SkinsRestorerProvider.get();
                skinsRestorerEnabled = true;
                plugin.getLogger().info("[NPCMirror] SkinsRestorer 插件已找到，皮肤同步功能已启用");
            } catch (Exception e) {
                plugin.getLogger().warning("[NPCMirror] 无法加载 SkinsRestorer: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("[NPCMirror] SkinsRestorer 插件未找到，将使用默认皮肤");
        }
    }

    public boolean isEnabled() {
        return config.isEnabled() && fancyNpcsEnabled;
    }

    /**
     * 检查指定菜单是否启用NPC
     * @param menuKey 菜单键名
     * @return 是否启用
     */
    public boolean isMenuEnabled(String menuKey) {
        return config.isMenuEnabled(menuKey);
    }

    public boolean isNPCCreationEnabled(Player player) {
        return !disabledPlayers.contains(player.getUniqueId());
    }

    public void setNPCCreationEnabled(Player player, boolean enabled) {
        UUID playerId = player.getUniqueId();
        if (enabled) {
            disabledPlayers.remove(playerId);
            player.sendMessage("§a[NPCMirror] 已启用NPC镜像创建");
        } else {
            disabledPlayers.add(playerId);
            player.sendMessage("§c[NPCMirror] 已禁用NPC镜像创建");
        }
        config.saveDisabledPlayers(disabledPlayers);
    }

    public boolean toggleNPCForPlayer(Player player) {
        boolean newState = isNPCCreationEnabled(player);
        setNPCCreationEnabled(player, !newState);
        return !newState;
    }

    /**
     * 为玩家创建镜像NPC
     * @param player 玩家
     * @param baseLocation NPC基准位置
     * @param yaw NPC朝向yaw
     * @param pitch NPC朝向pitch
     * @param menuKey 菜单键名（用于检查是否启用NPC）
     * @return 是否创建成功
     */
    public boolean createMirrorNPC(Player player, Location baseLocation, float yaw, float pitch, String menuKey) {
        if (!isEnabled() || !isNPCCreationEnabled(player)) {
            return false;
        }

        if (menuKey != null && !isMenuEnabled(menuKey)) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[NPCMirror] 菜单 " + menuKey + " 未启用NPC，跳过创建");
            }
            return false;
        }

        removeMirrorNPC(player);

        try {
            NpcManager npcManager = fancyNpcsPlugin.getNpcManager();
            if (npcManager == null) {
                return false;
            }

            Location npcLocation = calculateNPCLocation(baseLocation, yaw, pitch);

            NpcData npcData = new NpcData(
                "CMP_MIRROR_" + player.getName(),
                player.getUniqueId(),
                npcLocation
            );

            applyPlayerSkin(player, npcData);
            copyPlayerEquipment(player, npcData);
            applyNpcName(player, npcData);

            Npc npc = fancyNpcsPlugin.getNpcAdapter().apply(npcData);
            npc.setSaveToFile(false);
            npc.create();
            npc.spawnForAll();

            // 应用名称显示
            updateNpcName(player, npc);

            playerNPCs.put(player.getUniqueId(), npc);

            if (config.isDebugMode()) {
                plugin.getLogger().info("[NPCMirror] 已为玩家 " + player.getName() + " 创建镜像NPC");
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] 创建NPC时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 为玩家创建镜像NPC（使用默认配置位置，不带菜单检查）
     */
    public boolean createMirrorNPC(Player player, Location playerLocation) {
        return createMirrorNPC(player, playerLocation, config.getDefaultYaw(), config.getDefaultPitch(), null);
    }

    /**
     * 为玩家创建镜像NPC（使用默认配置位置，带菜单检查）
     */
    public boolean createMirrorNPC(Player player, Location playerLocation, String menuKey) {
        return createMirrorNPC(player, playerLocation, config.getDefaultYaw(), config.getDefaultPitch(), menuKey);
    }

    private Location calculateNPCLocation(Location baseLocation, float yaw, float pitch) {
        Location npcLocation = baseLocation.clone();
        npcLocation.add(config.getOffsetX(), config.getOffsetY(), config.getOffsetZ());
        npcLocation.setYaw(yaw);
        npcLocation.setPitch(pitch);
        return npcLocation;
    }

    private void applyPlayerSkin(Player player, NpcData npcData) {
        if (!skinsRestorerEnabled) {
            return;
        }

        try {
            PlayerStorage playerStorage = skinsRestorer.getPlayerStorage();
            Optional<SkinProperty> skin = playerStorage.getSkinForPlayer(
                player.getUniqueId(), 
                player.getName()
            );
            
            if (skin.isPresent()) {
                npcData.setSkin(player.getName());
            }
        } catch (IllegalStateException e) {
            plugin.getLogger().info("[NPCMirror] SkinsRestorer API不可用，跳过皮肤设置");
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] 无法获取玩家皮肤: " + e.getMessage());
        }
    }

    private void copyPlayerEquipment(Player player, NpcData npcData) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        setEquipmentIfPresent(npcData, NpcEquipmentSlot.HEAD, inv.getHelmet());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.CHEST, inv.getChestplate());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.LEGS, inv.getLeggings());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.FEET, inv.getBoots());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.MAINHAND, inv.getItemInMainHand());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.OFFHAND, inv.getItemInOffHand());
    }

    private void applyNpcName(Player player, NpcData npcData) {
        if (!config.isShowNpcName()) {
            return;
        }
    }

    /**
     * 更新NPC的名称显示
     */
    public void updateNpcName(Player player, Npc npc) {
        if (!config.isShowNpcName()) {
            return;
        }

        String formattedName = config.getFormattedNpcName(player.getName(), player.getDisplayName());
        
        // 应用PlaceholderAPI变量
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedName = PlaceholderAPI.setPlaceholders(player, formattedName);
        }
        
        try {
            npc.getData().setDisplayName(formattedName);
            npc.updateForAll();
            
            if (config.isDebugMode()) {
                plugin.getLogger().info("[NPCMirror] 设置NPC名称: " + formattedName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] 设置NPC名称时出错: " + e.getMessage());
        }
    }

    private void setEquipmentIfPresent(NpcData npcData, NpcEquipmentSlot slot, ItemStack item) {
        if (item != null && item.getType() != org.bukkit.Material.AIR) {
            npcData.addEquipment(slot, item.clone());
        }
    }

    /**
     * 移除玩家的镜像NPC
     */
    public void removeMirrorNPC(Player player) {
        UUID playerId = player.getUniqueId();
        Npc npc = playerNPCs.remove(playerId);

        if (npc != null) {
            try {
                npc.removeForAll();
            } catch (Exception e) {
                plugin.getLogger().warning("[NPCMirror] 移除NPC时出错: " + e.getMessage());
            }
        }
    }

    /**
     * 旋转玩家的NPC
     */
    public void rotateNPC(Player player, float yawOffset) {
        Npc npc = playerNPCs.get(player.getUniqueId());
        if (npc == null) return;

        try {
            Location location = npc.getData().getLocation();
            float newYaw = (location.getYaw() + yawOffset + 360) % 360;
            location.setYaw(newYaw);
            npc.getData().setLocation(location);
            npc.updateForAll();
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] 旋转NPC时出错: " + e.getMessage());
        }
    }

    /**
     * 更新NPC位置
     */
    public void updateNPCLocation(Player player, Location newLocation) {
        Npc npc = playerNPCs.get(player.getUniqueId());
        if (npc == null) return;

        try {
            Location npcLoc = calculateNPCLocation(
                newLocation, 
                newLocation.getYaw(), 
                newLocation.getPitch()
            );
            npc.getData().setLocation(npcLoc);
            npc.updateForAll();
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] 更新NPC位置时出错: " + e.getMessage());
        }
    }

    /**
     * 检查玩家是否有NPC
     */
    public boolean hasNPC(Player player) {
        return playerNPCs.containsKey(player.getUniqueId());
    }

    /**
     * 获取玩家的NPC
     */
    public Npc getPlayerNPC(Player player) {
        return playerNPCs.get(player.getUniqueId());
    }

    /**
     * 清理所有NPC
     */
    public void cleanup() {
        for (Npc npc : playerNPCs.values()) {
            try {
                npc.removeForAll();
            } catch (Exception e) {
                // 忽略清理错误
            }
        }
        playerNPCs.clear();
    }

    /**
     * 获取配置
     */
    public NPCConfig getConfig() {
        return config;
    }

    /**
     * 重载配置
     */
    public void reloadConfig() {
        config.reload();
        disabledPlayers.clear();
        disabledPlayers.addAll(config.loadDisabledPlayers());
    }

    /**
     * 获取FancyNpcs插件实例
     */
    public FancyNpcsPlugin getFancyNpcsPlugin() {
        return fancyNpcsPlugin;
    }

    /**
     * 获取NpcManager
     */
    public NpcManager getNpcManager() {
        if (fancyNpcsPlugin != null) {
            return fancyNpcsPlugin.getNpcManager();
        }
        return null;
    }
}
