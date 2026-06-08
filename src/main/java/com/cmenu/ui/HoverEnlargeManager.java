package com.cmenu.ui;

import com.cmenu.ui.CursorMenuPlugin;
import com.cmenu.ui.TextDisplayHitBox;
import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.Section;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HoverEnlargeManager {
    private final CursorMenuPlugin plugin;
    private final Map<UUID, TextDisplay> enlargedTextDisplays = new HashMap<>();
    private final Map<UUID, Vector3f> originalScales = new HashMap<>();
    private BukkitRunnable hoverDetectionTask;

    public HoverEnlargeManager(CursorMenuPlugin plugin) {
        this.plugin = plugin;
        startHoverDetection();
    }

    private void startHoverDetection() {
        hoverDetectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!plugin.playerCursors.containsKey(player)) continue;

                    Location cursorLoc = plugin.cursorExactLocations.get(player);
                    if (cursorLoc == null) continue;

                    String menuKey = plugin.getCurrentPlayerMenu(player);
                    if (menuKey == null) continue;

                    Section section = plugin.sectionManager.get(menuKey);
                    if (section == null) continue;

                    TextDisplay closestDisplay = null;
                    double minDistance = Double.MAX_VALUE;

                    // Find the nearest text display
                    for (MenuLayout layout : section.layouts.values()) {
                        if (!layout.isHoverEnlargeEnabled()) continue;

                        Location cameraLoc = new Location(
                                cursorLoc.getWorld(),
                                section.cameraX,
                                section.cameraY,
                                section.cameraZ,
                                section.yaw,
                                section.pitch
                        );

                        Vector dir = cameraLoc.getDirection().normalize();
                        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                        Vector up = dir.getCrossProduct(right).multiply(-1);

                        Vector offset = dir.multiply(layout.z)
                                .add(right.multiply(layout.x))
                                .add(up.multiply(layout.y));

                        Location buttonLoc = cameraLoc.clone().add(offset);

                        // Detect whether the cursor is near the text
                        String layoutKey = menuKey + ":" + layout.key;

                        // Find the corresponding TextDisplay entity
                        TextDisplay display = null;
                        for (TextDisplay d : cursorLoc.getWorld().getEntitiesByClass(TextDisplay.class)) {
                            if (layoutKey.equals(d.getName())) {
                                display = d;
                                break;
                            }
                        }

                        if (display != null && TextDisplayHitBox.isInside(display, cursorLoc)) {
                            minDistance = 0; // Take this one as soon as it hits
                            closestDisplay = display;
                            break; // Break immediately once found
                        }
                    }

                    // Handle hover-enlarge effect
                    TextDisplay currentlyEnlarged = enlargedTextDisplays.get(player.getUniqueId());

                    if (closestDisplay != null) {
                        // If there is a new closest display, enlarge it
                        if (currentlyEnlarged != closestDisplay) {
                            // Reset the previously enlarged display
                            if (currentlyEnlarged != null) {
                                resetTextDisplaySize(currentlyEnlarged);
                                enlargedTextDisplays.remove(player.getUniqueId());
                            }

                            // Enlarge the new display
                            enlargeTextDisplay(closestDisplay);
                            enlargedTextDisplays.put(player.getUniqueId(), closestDisplay);
                        }
                    } else if (currentlyEnlarged != null) {
                        // No closest display found, but a previously enlarged one exists — reset it
                        resetTextDisplaySize(currentlyEnlarged);
                        enlargedTextDisplays.remove(player.getUniqueId());
                    }
                }
            }
        };
        hoverDetectionTask.runTaskTimer(plugin, 0L, 1L); // Check every tick
    }

    private void enlargeTextDisplay(TextDisplay textDisplay) {
        // Ensure any existing transformation is reset first
        resetTextDisplaySize(textDisplay);

        var transformation = textDisplay.getTransformation();
        Vector3f currentScale = transformation.getScale();

        // Save the original scale (only if not already saved)
        if (!originalScales.containsKey(textDisplay.getUniqueId())) {
            originalScales.put(textDisplay.getUniqueId(), new Vector3f(currentScale));
        }

        // Get the enlarge scale factor
        String name = textDisplay.getName();
        MenuLayout layout = plugin.sectionManager.getLayout(name);
        if (layout != null && layout.isHoverEnlargeEnabled()) {
            float scaleValue = (float) layout.getHoverEnlargeScale();
            currentScale.mul(scaleValue);
            textDisplay.setTransformation(transformation);
        }
    }

    private void resetTextDisplaySize(TextDisplay textDisplay) {
        Vector3f originalScale = originalScales.remove(textDisplay.getUniqueId());
        if (originalScale != null) {
            var transformation = textDisplay.getTransformation();
            transformation.getScale().set(originalScale);
            textDisplay.setTransformation(transformation);
        }
    }

    public void cleanupPlayer(Player player) {
        TextDisplay display = enlargedTextDisplays.remove(player.getUniqueId());
        if (display != null) {
            resetTextDisplaySize(display);
        }
    }

    public void cleanup() {
        // Stop the detection task
        if (hoverDetectionTask != null) {
            hoverDetectionTask.cancel();
            hoverDetectionTask = null;
        }

        // Reset all enlarged text displays
        for (TextDisplay display : enlargedTextDisplays.values()) {
            resetTextDisplaySize(display);
        }
        enlargedTextDisplays.clear();
        originalScales.clear();
    }
}