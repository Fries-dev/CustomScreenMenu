package com.cmenu.ui;

import com.cmenu.ui.CursorMenuPlugin;
import com.cmenu.ui.section.SectionManager;
import com.cmenu.ui.section.Section;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CreatureSpawnListener implements Listener {
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!CursorMenuPlugin.creatureSpawnLimitEnabled) {
            return;
        }
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        Location spawnLocation = event.getLocation();
        SectionManager sectionManager = CursorMenuPlugin.sectionManager;
        for (Section section : sectionManager.getAll().values()) {
            Location menuLocation = new Location(
                    spawnLocation.getWorld(),
                    section.cameraX,
                    section.cameraY,
                    section.cameraZ
            );
            if (!spawnLocation.getWorld().getName().equals(section.world)) {
                continue;
            }
            if (menuLocation.distance(spawnLocation) <= CursorMenuPlugin.creatureSpawnLimitRadius) {
                event.setCancelled(true);
                return;
            }
        }
    }
}