package com.cmenu.ui.npc;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NPC mirror command handler.
 * Handles /cursormenu npc related commands.
 */
public class NPCCommandHandler implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final NPCMirrorHook npcHook;

    public NPCCommandHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.npcHook = NPCMirrorHook.getInstance(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "[NPCMirror] This command can only be executed by a player");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "toggle":  return handleToggle(player);
            case "enable":  return handleEnable(player, true);
            case "disable": return handleEnable(player, false);
            case "status":  return handleStatus(player);
            case "reload":  return handleReload(player);
            case "rotate":  return handleRotate(player, args);
            case "help":
                sendHelp(player);
                return true;
            default:
                player.sendMessage(ChatColor.RED + "[NPCMirror] Unknown command: " + subCommand);
                sendHelp(player);
                return true;
        }
    }

    private boolean handleToggle(Player player) {
        if (!player.hasPermission("cursormenu.npc.toggle")) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to run this command");
            return true;
        }

        boolean newState = npcHook.toggleNPCForPlayer(player);
        player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC mirror creation " + (newState ? "enabled" : "disabled"));
        return true;
    }

    private boolean handleEnable(Player player, boolean enable) {
        if (!player.hasPermission("cursormenu.npc.toggle")) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to run this command");
            return true;
        }

        npcHook.setNPCEnabledForPlayer(player, enable);
        player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC mirror creation " + (enable ? "enabled" : "disabled"));
        return true;
    }

    private boolean handleStatus(Player player) {
        if (!player.hasPermission("cursormenu.npc.status")) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to run this command");
            return true;
        }

        player.sendMessage(ChatColor.YELLOW + "===== NPC Mirror System Status =====");
        player.sendMessage(ChatColor.GRAY + "System enabled: " + ChatColor.WHITE + npcHook.isEnabled());
        player.sendMessage(ChatColor.GRAY + "Your NPC creation: " + ChatColor.WHITE +
                (npcHook.isNPCEnabledForPlayer(player) ? "Enabled" : "Disabled"));
        player.sendMessage(ChatColor.GRAY + "Has NPC: " + ChatColor.WHITE +
                (npcHook.getNPCManager().hasNPC(player) ? "Yes" : "No"));
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("cursormenu.npc.reload")) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to run this command");
            return true;
        }

        npcHook.reload();
        player.sendMessage(ChatColor.GREEN + "[NPCMirror] Configuration reloaded");
        return true;
    }

    private boolean handleRotate(Player player, String[] args) {
        if (!player.hasPermission("cursormenu.npc.rotate")) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] You do not have permission to run this command");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] Usage: /cursormenu npc rotate <angle>");
            return true;
        }

        try {
            float angle = Float.parseFloat(args[1]);
            npcHook.rotateNPC(player, angle);
            player.sendMessage(ChatColor.GREEN + "[NPCMirror] NPC rotated by " + angle + " degrees");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "[NPCMirror] Invalid angle value: " + args[1]);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "===== NPC Mirror System Help =====");
        player.sendMessage(ChatColor.GRAY + "/cursormenu npc toggle "  + ChatColor.WHITE + "- Toggle NPC creation state");
        player.sendMessage(ChatColor.GRAY + "/cursormenu npc enable "  + ChatColor.WHITE + "- Enable NPC creation");
        player.sendMessage(ChatColor.GRAY + "/cursormenu npc disable " + ChatColor.WHITE + "- Disable NPC creation");
        player.sendMessage(ChatColor.GRAY + "/cursormenu npc status "  + ChatColor.WHITE + "- View NPC status");
        player.sendMessage(ChatColor.GRAY + "/cursormenu npc reload "  + ChatColor.WHITE + "- Reload NPC configuration");
        player.sendMessage(ChatColor.GRAY + "/cursormenu npc rotate <angle> " + ChatColor.WHITE + "- Rotate NPC");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("toggle", "enable", "disable", "status", "reload", "rotate", "help"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("rotate")) {
            completions.addAll(Arrays.asList("45", "90", "180", "-45", "-90", "-180"));
        }

        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));

        return completions;
    }
}