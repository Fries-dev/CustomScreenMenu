package com.cmenu.ui;

import com.cmenu.ui.CursorMenuPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

import static com.cmenu.ui.CursorMenuPlugin.*;

public class MenuListener implements Listener {

    private final CursorMenuPlugin plugin;

    public MenuListener(CursorMenuPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (joinRunBool) {
            foliaLib.scheduling().entitySpecificScheduler(event.getPlayer()).runDelayed(task -> {
                plugin.setupCursor(event.getPlayer(), joinRunSection);
            }, null, 15 + (20 * runDelay));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.stopCursor(event.getPlayer(), true);
    }

    @EventHandler
    public void onVehicleLeave(VehicleExitEvent event) {
        if (event.getExited() instanceof Player) {
            Player player = (Player) event.getExited();
            if (playerSit.containsKey(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCommandCancel(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!playerSit.containsKey(player)) return;

        String message = event.getMessage().toLowerCase().trim();
        String command = message.startsWith("/") ? message.substring(1) : message;

        if (command.startsWith("cmenu") || command.startsWith("cursormenu")) {
            return;
        }

        for (String allowed : allowedCommands) {
            if (command.startsWith(allowed)) {
                return;
            }
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "&c[CursorMenu] ");
        String blocked = plugin.getConfig().getString("messages.command_blocked", "&7This command is not available in menu mode.");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + blocked));
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // Check if the player is in a specific login/register menu
        String currentMenu = plugin.getCurrentPlayerMenu(player);
        if (currentMenu == null || (!currentMenu.equals("Login Menu") && !currentMenu.equals("Register Menu"))) {
            return; // Not in a login/register menu, use normal chat functionality
        }

        // Cancel the chat event so the message is not sent to other players
        event.setCancelled(true);

        // Get the field the player is currently supposed to be filling in
        String inputField = plugin.getCurrentPlayerInputField(player);
        if (inputField == null || inputField.isEmpty()) {
            // No input field specified, ignore the input
            return;
        }

        // Get the content entered by the player
        String inputValue = event.getMessage();

        // Save the entered data
        plugin.setPlayerInputData(player, inputField, inputValue);

        // Send a confirmation message to the player
        player.sendMessage(ChatColor.GREEN + "[CursorMenu] " + inputField + " has been set to: " + inputValue);

        // Refresh the menu on the main thread to update the display
        foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
            plugin.setupCursor(player, currentMenu);
        }, null);
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            player.sendMessage(
                    ChatColor.translateAlternateColorCodes('&',
                            "&a[CustomScreenMenu] &fThis version is open source and does not receive author support and assistance\n" +
                                    "&fPlugin Version: &e" + plugin.getDescription().getVersion() + "\n" +
                                    "&fAuthor: &eNobi Nobita\nQQ: 3357153117\n" +
                                    "&fDiscord: https://discord.gg/YpZNACup"
                    )
            );
        }
    }
}