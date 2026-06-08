package com.cmenu.ui.scheduler;

import com.cmenu.ui.CursorMenuPlugin;
import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.Section;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk loader manager.
 * Responsible for managing chunk loading and unloading related to menus.
 */
public class ChunkLoaderManager {

    private final CursorMenuPlugin plugin;
    private BukkitTask chunkLoaderTask;

    // Stores chunks that must remain loaded (world name -> set of chunk keys)
    private final Map<String, Set<Long>> forcedLoadedChunks = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> persistentChunks = new ConcurrentHashMap<>();

    private boolean isTaskRunning = false;

    public ChunkLoaderManager(CursorMenuPlugin plugin) {
        this.plugin = plugin;
    }

    /** Starts the chunk loader task. */
    public void startChunkLoaderTask() {
        // Stop any existing task
        stopChunkLoaderTask();

        // Initialize force-loaded chunks
        updatePersistentChunks();

        // Verify chunk state every 30 seconds to prevent accidental unloading
        chunkLoaderTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updatePersistentChunks, 0L, 600L);
        isTaskRunning = true;

        if (plugin.getConfig().getBoolean("Debug", false)) {
            plugin.getLogger().info("Chunk loader task started");
        }
    }

    /** Stops the chunk loader task. */
    public void stopChunkLoaderTask() {
        // Cancel the task
        if (chunkLoaderTask != null) {
            chunkLoaderTask.cancel();
            chunkLoaderTask = null;
        }

        // Release all force-loaded chunks
        for (Map.Entry<String, Set<Long>> entry : forcedLoadedChunks.entrySet()) {
            String worldName = entry.getKey();
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            for (long chunkKey : entry.getValue()) {
                int x = (int) (chunkKey >> 32);
                int z = (int) (chunkKey & 0xFFFFFFFFL);

                Chunk chunk = world.getChunkAt(x, z);
                if (chunk != null) {
                    chunk.setForceLoaded(false);
                }
            }
        }
        forcedLoadedChunks.clear();
        persistentChunks.clear();

        isTaskRunning = false;

        if (plugin.getConfig().getBoolean("Debug", false)) {
            plugin.getLogger().info("Chunk loader task stopped");
        }
    }

    /** Updates the list of chunks that must remain loaded. */
    public void updatePersistentChunks() {
        // 1. Record all currently force-loaded chunks (for cleanup later)
        Map<String, Set<Long>> oldChunks = new HashMap<>();
        for (Map.Entry<String, Set<Long>> entry : forcedLoadedChunks.entrySet()) {
            oldChunks.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        // 2. Compute the new set of chunks that need loading (built fresh, not modifying the old set)
        Map<String, Set<Long>> newChunks = new HashMap<>();

        // Gather all menu sections
        Map<String, Section> allSections = plugin.sectionManager.getAll();
        for (Section section : allSections.values()) {
            World world = Bukkit.getWorld(section.world);
            if (world == null) continue;

            // Compute the chunk containing the menu camera position
            int chunkX = (int) Math.floor(section.cameraX / 16);
            int chunkZ = (int) Math.floor(section.cameraZ / 16);
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            newChunks.computeIfAbsent(section.world, k -> new HashSet<>()).add(chunkKey);

            // Process chunks for other elements in the menu
            if (section.layouts != null) {
                for (MenuLayout layout : section.layouts.values()) {
                    int layoutChunkX = (int) Math.floor((section.cameraX + layout.x) / 16);
                    int layoutChunkZ = (int) Math.floor((section.cameraZ + layout.z) / 16);
                    long layoutChunkKey = ((long) layoutChunkX << 32) | (layoutChunkZ & 0xFFFFFFFFL);
                    newChunks.get(section.world).add(layoutChunkKey);
                }
            }
        }

        // 3. Mark newly added chunks as force-loaded
        for (Map.Entry<String, Set<Long>> entry : newChunks.entrySet()) {
            String worldName = entry.getKey();
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Set<Long> chunkKeys = entry.getValue();
            for (long chunkKey : chunkKeys) {
                int x = (int) (chunkKey >> 32);
                int z = (int) (chunkKey & 0xFFFFFFFFL);

                // Version-compatible loading logic
                if (!world.isChunkLoaded(x, z)) {
                    world.loadChunk(x, z, true); // Load the chunk
                    Chunk chunk = world.getChunkAt(x, z);
                    if (chunk != null) {
                        chunk.setForceLoaded(true); // Mark as force-loaded
                    }
                } else {
                    Chunk chunk = world.getChunkAt(x, z);
                    chunk.setForceLoaded(true);
                }

                // Record in the force-loaded list
                forcedLoadedChunks.computeIfAbsent(worldName, k -> new HashSet<>()).add(chunkKey);
            }
        }

        // 4. Separately remove chunks that are no longer needed (iterated outside the new-set loop)
        for (Map.Entry<String, Set<Long>> entry : oldChunks.entrySet()) {
            String worldName = entry.getKey();
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Set<Long> oldChunkKeys = entry.getValue();
            // Find chunks not present in the new set (to be released)
            for (long chunkKey : oldChunkKeys) {
                Set<Long> newChunkKeys = newChunks.getOrDefault(worldName, Collections.emptySet());
                if (!newChunkKeys.contains(chunkKey)) {
                    // Release force-loaded flag
                    int x = (int) (chunkKey >> 32);
                    int z = (int) (chunkKey & 0xFFFFFFFFL);
                    Chunk chunk = world.getChunkAt(x, z);
                    if (chunk != null) {
                        chunk.setForceLoaded(false);
                    }
                    // Remove from force-loaded list
                    forcedLoadedChunks.getOrDefault(worldName, new HashSet<>()).remove(chunkKey);
                }
            }
        }

        // Remove empty world entries
        forcedLoadedChunks.entrySet().removeIf(e -> e.getValue().isEmpty());

        if (plugin.getConfig().getBoolean("Debug", false)) {
            plugin.getLogger().info("Force-loaded chunks updated: " + forcedLoadedChunks.size() + " world(s), " +
                    forcedLoadedChunks.values().stream().mapToInt(Set::size).sum() + " chunk(s)");
        }
    }

    /** Returns the total number of currently force-loaded chunks. */
    public int getLoadedChunkCount() {
        return forcedLoadedChunks.values().stream().mapToInt(Set::size).sum();
    }

    /** Returns the number of worlds with force-loaded chunks. */
    public int getLoadedWorldCount() {
        return forcedLoadedChunks.size();
    }

    /** Checks whether the task is currently running. */
    public boolean isTaskRunning() {
        return isTaskRunning;
    }
}