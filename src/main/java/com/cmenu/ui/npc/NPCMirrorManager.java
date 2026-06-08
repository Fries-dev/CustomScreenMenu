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
 * NPC mirror manager.
 * Responsible for creating and managing player mirror NPCs in 3D menus.
 *
 * This is a standalone module that does not modify the CMP core code.
 * It integrates with the menu system via hooks.
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
                plugin.getLogger().info("[NPCMirror] FancyNpcs plugin found, NPC mirror feature enabled");
            } catch (Exception e) {
                plugin.getLogger().warning("[NPCMirror] Failed to load FancyNpcs: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("[NPCMirror] FancyNpcs plugin not found, NPC mirror feature disabled");
        }

        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            try {
                skinsRestorer = SkinsRestorerProvider.get();
                skinsRestorerEnabled = true;
                plugin.getLogger().info("[NPCMirror] SkinsRestorer plugin found, skin sync feature enabled");
            } catch (Exception e) {
                plugin.getLogger().warning("[NPCMirror] Failed to load SkinsRestorer: " + e.getMessage());
            }
        } else {
            plugin.getLogger().info("[NPCMirror] SkinsRestorer plugin not found, default skin will be used");
        }
    }

    public boolean isEnabled() {
        return config.isEnabled() && fancyNpcsEnabled;
    }

    /**
     * Checks whether NPC is enabled for the specified menu.
     * @param menuKey the menu key
     * @return whether enabled
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
            player.sendMessage("§a[NPCMirror] NPC mirror creation enabled");
        } else {
            disabledPlayers.add(playerId);
            player.sendMessage("§c[NPCMirror] NPC mirror creation disabled");
        }
        config.saveDisabledPlayers(disabledPlayers);
    }

    public boolean toggleNPCForPlayer(Player player) {
        boolean newState = isNPCCreationEnabled(player);
        setNPCCreationEnabled(player, !newState);
        return !newState;
    }

    /**
     * Creates a mirror NPC for the player.
     * @param player the player
     * @param baseLocation the base NPC location
     * @param yaw the NPC facing yaw
     * @param pitch the NPC facing pitch
     * @param menuKey the menu key (used to check whether NPC is enabled)
     * @return whether creation succeeded
     */
    public boolean createMirrorNPC(Player player, Location baseLocation, float yaw, float pitch, String menuKey) {
        if (!isEnabled() || !isNPCCreationEnabled(player)) return false;

        if (menuKey != null && !isMenuEnabled(menuKey)) {
            if (config.isDebugMode()) {
                plugin.getLogger().info("[NPCMirror] Menu " + menuKey + " does not have NPC enabled, skipping creation");
            }
            return false;
        }

        removeMirrorNPC(player);

        try {
            NpcManager npcManager = fancyNpcsPlugin.getNpcManager();
            if (npcManager == null) return false;

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

            // Apply name display
            updateNpcName(player, npc);

            playerNPCs.put(player.getUniqueId(), npc);

            if (config.isDebugMode()) {
                plugin.getLogger().info("[NPCMirror] Mirror NPC created for player " + player.getName());
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] Error creating NPC: " + e.getMessage());
            return false;
        }
    }

    /** Creates a mirror NPC using the default config location (without menu check). */
    public boolean createMirrorNPC(Player player, Location playerLocation) {
        return createMirrorNPC(player, playerLocation, config.getDefaultYaw(), config.getDefaultPitch(), null);
    }

    /** Creates a mirror NPC using the default config location (with menu check). */
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
        if (!skinsRestorerEnabled) return;

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
            plugin.getLogger().info("[NPCMirror] SkinsRestorer API unavailable, skipping skin setup");
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] Failed to retrieve player skin: " + e.getMessage());
        }
    }

    private void copyPlayerEquipment(Player player, NpcData npcData) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        setEquipmentIfPresent(npcData, NpcEquipmentSlot.HEAD,     inv.getHelmet());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.CHEST,    inv.getChestplate());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.LEGS,     inv.getLeggings());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.FEET,     inv.getBoots());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.MAINHAND, inv.getItemInMainHand());
        setEquipmentIfPresent(npcData, NpcEquipmentSlot.OFFHAND,  inv.getItemInOffHand());
    }

    private void applyNpcName(Player player, NpcData npcData) {
        if (!config.isShowNpcName()) return;
    }

    /** Updates the NPC's name display. */
    public void updateNpcName(Player player, Npc npc) {
        if (!config.isShowNpcName()) return;

        String formattedName = config.getFormattedNpcName(player.getName(), player.getDisplayName());

        // Apply PlaceholderAPI variables
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            formattedName = PlaceholderAPI.setPlaceholders(player, formattedName);
        }

        try {
            npc.getData().setDisplayName(formattedName);
            npc.updateForAll();

            if (config.isDebugMode()) {
                plugin.getLogger().info("[NPCMirror] Set NPC name: " + formattedName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NPCMirror] Error setting NPC name: " + e.getMessage());
        }
    }

    private void setEquipmentIfPresent(NpcData npcData, NpcEquipmentSlot slot, ItemStack item) {
        if (item != null && item.getType() != org.bukkit.Material.AIR) {
            npcData.addEquipment(slot, item.clone());
        }
    }

    /** Removes the player's mirror NPC. */
    public void removeMirrorNPC(Player player) {
        UUID playerId = player.getUniqueId();
        Npc npc = playerNPCs.remove(playerId);

        if (npc != null) {
            try {
                npc.removeForAll();
            } catch (Exception e) {
                plugin.getLogger().warning("[NPCMirror] Error removing NPC: " + e.getMessage());
            }
        }
    }

    /** Rotates the player's NPC. */
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
            plugin.getLogger().warning("[NPCMirror] Error rotating NPC: " + e.getMessage());
        }
    }

    /** Updates the NPC's location. */
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
            plugin.getLogger().warning("[NPCMirror] Error updating NPC location: " + e.getMessage());
        }
    }

    /** Checks whether the player has an NPC. */
    public boolean hasNPC(Player player) {
        return playerNPCs.containsKey(player.getUniqueId());
    }

    /** Returns the player's NPC. */
    public Npc getPlayerNPC(Player player) {
        return playerNPCs.get(player.getUniqueId());
    }

    /** Cleans up all NPCs. */
    public void cleanup() {
        for (Npc npc : playerNPCs.values()) {
            try {
                npc.removeForAll();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
        playerNPCs.clear();
    }

    /** Returns the configuration. */
    public NPCConfig getConfig() { return config; }

    /** Reloads the configuration. */
    public void reloadConfig() {
        config.reload();
        disabledPlayers.clear();
        disabledPlayers.addAll(config.loadDisabledPlayers());
    }

    /** Returns the FancyNpcs plugin instance. */
    public FancyNpcsPlugin getFancyNpcsPlugin() { return fancyNpcsPlugin; }

    /** Returns the NpcManager. */
    public NpcManager getNpcManager() {
        return fancyNpcsPlugin != null ? fancyNpcsPlugin.getNpcManager() : null;
    }
}