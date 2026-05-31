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

                    // 查找最近的文本显示
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

                        // 检测光标是否在文本附近
                        String layoutKey = menuKey + ":" + layout.key;

                        // 查找对应的TextDisplay实体
                        TextDisplay display = null;
                        for (TextDisplay d : cursorLoc.getWorld().getEntitiesByClass(TextDisplay.class)) {
                            if (layoutKey.equals(d.getName())) {
                                display = d;
                                break;
                            }
                        }

                        if (display != null && TextDisplayHitBox.isInside(display, cursorLoc)) {
                            minDistance = 0; // 只要命中就取这一个
                            closestDisplay = display;
                            break; // 找到后立即跳出循环
                        }
                    }

                    // 处理悬停放大效果
                    TextDisplay currentlyEnlarged = enlargedTextDisplays.get(player.getUniqueId());

                    if (closestDisplay != null) {
                        // 如果有新的最近显示项，放大它
                        if (currentlyEnlarged != closestDisplay) {
                            // 重置之前放大的显示项
                            if (currentlyEnlarged != null) {
                                resetTextDisplaySize(currentlyEnlarged);
                                enlargedTextDisplays.remove(player.getUniqueId());
                            }

                            // 放大新的显示项
                            enlargeTextDisplay(closestDisplay);
                            enlargedTextDisplays.put(player.getUniqueId(), closestDisplay);
                        }
                    } else if (currentlyEnlarged != null) {
                        // 如果没有最近的显示项，但有之前放大的项，则重置它
                        resetTextDisplaySize(currentlyEnlarged);
                        enlargedTextDisplays.remove(player.getUniqueId());
                    }
                }
            }
        };
        hoverDetectionTask.runTaskTimer(plugin, 0L, 1L); // 每tick检查一次
    }

    private void enlargeTextDisplay(TextDisplay textDisplay) {
        // 首先确保重置任何现有的变换
        resetTextDisplaySize(textDisplay);

        var transformation = textDisplay.getTransformation();
        Vector3f currentScale = transformation.getScale();

        // 保存原始尺寸（仅当尚未保存时）
        if (!originalScales.containsKey(textDisplay.getUniqueId())) {
            originalScales.put(textDisplay.getUniqueId(), new Vector3f(currentScale));
        }

        // 获取放大倍数
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
        // 停止检测任务
        if (hoverDetectionTask != null) {
            hoverDetectionTask.cancel();
            hoverDetectionTask = null;
        }

        // 重置所有放大的文本显示
        for (TextDisplay display : enlargedTextDisplays.values()) {
            resetTextDisplaySize(display);
        }
        enlargedTextDisplays.clear();
        originalScales.clear();
    }
}