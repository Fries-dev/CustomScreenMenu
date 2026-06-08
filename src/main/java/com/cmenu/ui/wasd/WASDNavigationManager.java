package com.cmenu.ui.wasd;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WASD navigation manager.
 * Handles WASD keyboard navigation and Space-to-confirm functionality inside menus.
 *
 * This is a standalone module that does not modify the CMP core code.
 * It coexists with the existing cursor control system without interference.
 */
public class WASDNavigationManager {

    private static WASDNavigationManager instance;
    private final JavaPlugin plugin;

    private WASDConfig config;
    private PacketListenerAbstract packetListener;
    private boolean registered = false;

    private WASDExpansion papiExpansion;

    private final Map<UUID, WASDSession> playerSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Location[]> playerTextLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> playerSelectedIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerSelectionCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerTextCommands = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerCurrentMenus = new ConcurrentHashMap<>();

    private static final long SELECTION_COOLDOWN_MS = 500;

    private WASDNavigationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = new WASDConfig(plugin);
    }

    public static synchronized WASDNavigationManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new WASDNavigationManager(plugin);
        }
        return instance;
    }

    public static WASDNavigationManager getInstance() { return instance; }

    /** Initializes the WASD navigation module. */
    public void initialize() {
        if (registered) return;

        registerPacketListener();
        registerPAPIExpansion();
        registered = true;
        plugin.getLogger().info("[WASDNavigation] WASD navigation module initialized");
    }

    /** Registers the PlaceholderAPI expansion. */
    private void registerPAPIExpansion() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                papiExpansion = new WASDExpansion(this);
                if (papiExpansion.register()) {
                    plugin.getLogger().info("[WASDNavigation] PAPI variable expansion registered");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[WASDNavigation] Failed to register PAPI expansion: " + e.getMessage());
            }
        }
    }

    /** Checks whether the module has been registered. */
    public boolean isRegistered() { return registered; }

    /** Registers the packet listener. */
    private void registerPacketListener() {
        packetListener = new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getUser() == null || event.getUser().getName() == null) return;

                Player player = Bukkit.getPlayer(event.getUser().getName());
                if (player == null || !player.isOnline()) return;

                UUID playerId = player.getUniqueId();

                if (!isWASDEnabledForPlayer(playerId)) return;

                if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
                    handlePlayerInput(player, event);
                }
            }
        };

        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    /** Handles player input. */
    private void handlePlayerInput(Player player, PacketReceiveEvent event) {
        UUID playerId = player.getUniqueId();

        if (!playerSessions.containsKey(playerId)) return;

        WrapperPlayClientPlayerInput inputPacket = new WrapperPlayClientPlayerInput(event);

        float sideways = 0.0f;
        float forward = 0.0f;

        if (inputPacket.isForward())  forward  += 1.0f;
        if (inputPacket.isBackward()) forward  -= 1.0f;
        if (inputPacket.isLeft())     sideways += 1.0f;
        if (inputPacket.isRight())    sideways -= 1.0f;

        if (inputPacket.isJump()) {
            executeSelectedCommand(player);
        }

        if (forward != 0 || sideways != 0) {
            handleNavigationInput(player, forward, sideways);
        }
    }

    /** Handles navigation input. */
    private void handleNavigationInput(Player player, float forward, float sideways) {
        UUID playerId = player.getUniqueId();

        if (isOnCooldown(playerId)) return;

        int currentIndex = playerSelectedIndex.getOrDefault(playerId, 0);
        int newIndex = -1;

        if (forward > 0)       newIndex = findPrevText(playerId, currentIndex);
        else if (forward < 0)  newIndex = findNextText(playerId, currentIndex);
        else if (sideways > 0) newIndex = findLeftText(player, playerId, currentIndex);
        else if (sideways < 0) newIndex = findRightText(player, playerId, currentIndex);

        if (newIndex != -1 && newIndex != currentIndex) {
            setSelectedIndex(player, newIndex);
            startCooldown(playerId);
        }
    }

    /** Finds the text above the current selection. */
    private int findPrevText(UUID playerId, int currentIndex) {
        Location[] locations = playerTextLocations.get(playerId);
        if (locations == null || currentIndex < 0 || currentIndex >= locations.length) return -1;

        Location currentLoc = locations[currentIndex];
        if (currentLoc == null) return -1;

        double currentY = currentLoc.getY();
        double currentX = currentLoc.getX();
        double currentZ = currentLoc.getZ();

        int bestIndex = -1;
        double smallestYDiff = Double.MAX_VALUE;

        for (int i = 0; i < locations.length; i++) {
            if (i == currentIndex || locations[i] == null) continue;

            Location targetLoc = locations[i];
            if (Math.abs(targetLoc.getX() - currentX) > config.getHorizontalThreshold() ||
                    Math.abs(targetLoc.getZ() - currentZ) > config.getHorizontalThreshold()) continue;

            double yDiff = targetLoc.getY() - currentY;
            if (yDiff > 0 && yDiff < smallestYDiff) {
                smallestYDiff = yDiff;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /** Finds the text below the current selection. */
    private int findNextText(UUID playerId, int currentIndex) {
        Location[] locations = playerTextLocations.get(playerId);
        if (locations == null || currentIndex < 0 || currentIndex >= locations.length) return -1;

        Location currentLoc = locations[currentIndex];
        if (currentLoc == null) return -1;

        double currentY = currentLoc.getY();
        double currentX = currentLoc.getX();
        double currentZ = currentLoc.getZ();

        int bestIndex = -1;
        double smallestYDiff = Double.MAX_VALUE;

        for (int i = 0; i < locations.length; i++) {
            if (i == currentIndex || locations[i] == null) continue;

            Location targetLoc = locations[i];
            if (Math.abs(targetLoc.getX() - currentX) > config.getHorizontalThreshold() ||
                    Math.abs(targetLoc.getZ() - currentZ) > config.getHorizontalThreshold()) continue;

            double yDiff = currentY - targetLoc.getY();
            if (yDiff > 0 && yDiff < smallestYDiff) {
                smallestYDiff = yDiff;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /** Finds the text to the left of the current selection. */
    private int findLeftText(Player player, UUID playerId, int currentIndex) {
        Location[] locations = playerTextLocations.get(playerId);
        if (locations == null || currentIndex < 0 || currentIndex >= locations.length) return -1;

        Location currentLoc = locations[currentIndex];
        if (currentLoc == null) return -1;

        org.bukkit.util.Vector playerDirection = player.getLocation().getDirection().setY(0).normalize();
        org.bukkit.util.Vector rightVector = playerDirection.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();
        org.bukkit.util.Vector leftVector = rightVector.clone().multiply(-1);

        int bestIndex = -1;
        double bestDotProduct = -1;

        for (int i = 0; i < locations.length; i++) {
            if (i == currentIndex || locations[i] == null) continue;

            org.bukkit.util.Vector offset = locations[i].toVector().subtract(currentLoc.toVector()).normalize();
            double dotProduct = offset.dot(leftVector);
            if (dotProduct > config.getDotProductThreshold() && dotProduct > bestDotProduct) {
                bestDotProduct = dotProduct;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /** Finds the text to the right of the current selection. */
    private int findRightText(Player player, UUID playerId, int currentIndex) {
        Location[] locations = playerTextLocations.get(playerId);
        if (locations == null || currentIndex < 0 || currentIndex >= locations.length) return -1;

        Location currentLoc = locations[currentIndex];
        if (currentLoc == null) return -1;

        org.bukkit.util.Vector playerDirection = player.getLocation().getDirection().setY(0).normalize();
        org.bukkit.util.Vector rightVector = playerDirection.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();

        int bestIndex = -1;
        double bestDotProduct = -1;

        for (int i = 0; i < locations.length; i++) {
            if (i == currentIndex || locations[i] == null) continue;

            org.bukkit.util.Vector offset = locations[i].toVector().subtract(currentLoc.toVector()).normalize();
            double dotProduct = offset.dot(rightVector);
            if (dotProduct > config.getDotProductThreshold() && dotProduct > bestDotProduct) {
                bestDotProduct = dotProduct;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    /** Sets the selected index. */
    private void setSelectedIndex(Player player, int index) {
        UUID playerId = player.getUniqueId();
        int oldIndex = playerSelectedIndex.getOrDefault(playerId, 0);
        playerSelectedIndex.put(playerId, index);

        WASDSession session = playerSessions.get(playerId);
        if (session != null) {
            session.onSelectionChanged(player, oldIndex, index);
        }

        // Update the location in PAPI variables
        if (papiExpansion != null) {
            Location[] locations = playerTextLocations.get(playerId);
            if (locations != null && index >= 0 && index < locations.length && locations[index] != null) {
                String menuKey = playerCurrentMenus.get(playerId);
                if (menuKey != null) {
                    papiExpansion.updatePlayerMenu(playerId, menuKey, locations[index]);
                }
            }
        }

        if (config.isDebugMode()) {
            plugin.getLogger().info("[WASDNavigation] Player " + player.getName() + " selected index: " + index);
        }
    }

    /** Executes the currently selected command. */
    private void executeSelectedCommand(Player player) {
        UUID playerId = player.getUniqueId();
        int selectedIndex = playerSelectedIndex.getOrDefault(playerId, -1);

        if (selectedIndex < 0) return;

        List<String> commands = playerTextCommands.get(playerId);
        if (commands == null || selectedIndex >= commands.size()) return;

        String command = commands.get(selectedIndex);
        if (command != null && !command.isEmpty()) {
            WASDSession session = playerSessions.get(playerId);
            if (session != null) {
                session.onExecuteCommand(player, command, selectedIndex);
            }
        }
    }

    /** Checks whether the player is on cooldown. */
    private boolean isOnCooldown(UUID playerId) {
        return playerSelectionCooldown.getOrDefault(playerId, false);
    }

    /** Starts a selection cooldown. */
    private void startCooldown(UUID playerId) {
        playerSelectionCooldown.put(playerId, true);
        new BukkitRunnable() {
            @Override
            public void run() {
                playerSelectionCooldown.put(playerId, false);
            }
        }.runTaskLater(plugin, SELECTION_COOLDOWN_MS / 50);
    }

    /** Checks whether WASD navigation is enabled for the player. */
    private boolean isWASDEnabledForPlayer(UUID playerId) {
        WASDSession session = playerSessions.get(playerId);
        return session != null && session.isEnabled();
    }

    /** Returns the player's WASD session. */
    public WASDSession getPlayerSession(UUID playerId) {
        return playerSessions.get(playerId);
    }

    /** Returns the player's current menu. */
    public String getPlayerCurrentMenu(UUID playerId) {
        return playerCurrentMenus.get(playerId);
    }

    /** Returns the player's current location. */
    public Location getPlayerCurrentLocation(UUID playerId) {
        String menuKey = playerCurrentMenus.get(playerId);
        if (menuKey == null) return null;

        int index = playerSelectedIndex.getOrDefault(playerId, -1);
        if (index < 0) return null;

        Location[] locations = playerTextLocations.get(playerId);
        if (locations == null || index >= locations.length) return null;

        return locations[index];
    }

    /** Starts a WASD navigation session for the player. */
    public void startSession(Player player, Location[] textLocations, List<String> commands, int initialIndex) {
        startSession(player, null, textLocations, commands, initialIndex);
    }

    /** Starts a WASD navigation session for the player (with menu name). */
    public void startSession(Player player, String menuKey, Location[] textLocations, List<String> commands, int initialIndex) {
        UUID playerId = player.getUniqueId();

        WASDSession session = new WASDSession(plugin, player, true);
        playerSessions.put(playerId, session);
        playerTextLocations.put(playerId, textLocations);
        playerTextCommands.put(playerId, commands);
        playerSelectedIndex.put(playerId, initialIndex);
        playerSelectionCooldown.put(playerId, false);

        // Update current menu information
        if (menuKey != null) {
            playerCurrentMenus.put(playerId, menuKey);

            // Update PAPI variables
            if (papiExpansion != null && textLocations != null && textLocations.length > 0) {
                papiExpansion.updatePlayerMenu(playerId, menuKey, textLocations[initialIndex >= 0 && initialIndex < textLocations.length ? initialIndex : 0]);
            }
        }

        if (config.isDebugMode()) {
            plugin.getLogger().info("[WASDNavigation] Started WASD navigation session for player " + player.getName() + ", menu: " + (menuKey != null ? menuKey : "unknown"));
        }
    }

    /** Updates the player's text locations. */
    public void updateTextLocations(Player player, Location[] textLocations, List<String> commands) {
        UUID playerId = player.getUniqueId();
        playerTextLocations.put(playerId, textLocations);
        playerTextCommands.put(playerId, commands);
    }

    /** Stops the player's WASD navigation session. */
    public void stopSession(Player player) {
        UUID playerId = player.getUniqueId();
        playerSessions.remove(playerId);
        playerTextLocations.remove(playerId);
        playerSelectedIndex.remove(playerId);
        playerSelectionCooldown.remove(playerId);
        playerTextCommands.remove(playerId);
        playerCurrentMenus.remove(playerId);

        // Clear PAPI variables
        if (papiExpansion != null) {
            papiExpansion.clearPlayerMenu(playerId);
        }

        if (config.isDebugMode()) {
            plugin.getLogger().info("[WASDNavigation] Stopped WASD navigation session for player " + player.getName());
        }
    }

    /** Returns the currently selected index for the player. */
    public int getSelectedIndex(Player player) {
        return playerSelectedIndex.getOrDefault(player.getUniqueId(), -1);
    }

    /** Returns the configuration. */
    public WASDConfig getConfig() { return config; }

    /** Reloads the configuration. */
    public void reloadConfig() { config.reload(); }

    /** Shuts down the module. */
    public void shutdown() {
        if (packetListener != null && registered) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }

        // Unregister PAPI expansion
        if (papiExpansion != null) {
            try {
                papiExpansion.unregister();
            } catch (Exception e) {
                // Ignore errors
            }
        }

        playerSessions.clear();
        playerTextLocations.clear();
        playerSelectedIndex.clear();
        playerSelectionCooldown.clear();
        playerTextCommands.clear();
        playerCurrentMenus.clear();

        registered = false;
    }
}