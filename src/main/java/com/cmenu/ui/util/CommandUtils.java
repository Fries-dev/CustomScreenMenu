package com.cmenu.ui.util;

import com.cmenu.ui.CursorMenuPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static void executeCommand(Player player, String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) {
            return;
        }

        String processedCmd = cmd;
        if (CursorMenuPlugin.hasPAPI) {
            processedCmd = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, cmd);
        }

        String lowerCmd = processedCmd.toLowerCase();

        if (lowerCmd.startsWith("[console]")) {
            String finalCmd = processedCmd.replaceAll("\\[console\\]", "").trim();
            if (!finalCmd.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
            }
        } else if (lowerCmd.startsWith("[op]")) {
            String finalCmd = processedCmd.replaceAll("\\[op\\]", "").trim();
            if (!finalCmd.isEmpty()) {
                executeWithTempOp(player, finalCmd);
            }
        } else if (lowerCmd.startsWith("[player]")) {
            String finalCmd = processedCmd.replaceAll("\\[player\\]", "").trim();
            if (!finalCmd.isEmpty()) {
                player.performCommand(finalCmd);
            }
        } else if (lowerCmd.startsWith("[server]")) {
            String serverName = processedCmd.replaceAll("\\[server\\]", "").trim();
            if (!serverName.isEmpty()) {
                connectToServer(player, serverName);
            }
        } else if (lowerCmd.startsWith("[message]")) {
            String message = processedCmd.replaceAll("\\[message\\]", "").trim();
            if (!message.isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            }
        } else if (lowerCmd.startsWith("[actionbar]")) {
            String message = processedCmd.replaceAll("\\[actionbar\\]", "").trim();
            if (!message.isEmpty()) {
                sendActionBar(player, ChatColor.translateAlternateColorCodes('&', message));
            }
        } else {
            String finalCmd = processedCmd.trim();
            if (!finalCmd.isEmpty()) {
                player.performCommand(finalCmd);
            }
        }
    }

    public static void executeWithTempOp(Player player, String command) {
        if (player == null || command == null || command.trim().isEmpty()) {
            return;
        }

        boolean wasOp = player.isOp();
        try {
            if (!wasOp) {
                player.setOp(true);
            }
            player.performCommand(command.trim());
        } catch (Exception e) {
            CursorMenuPlugin.plugin.getLogger().warning("执行临时OP命令时出错: " + e.getMessage());
        } finally {
            if (!wasOp) {
                player.setOp(false);
            }
        }
    }

    public static void connectToServer(Player player, String serverName) {
        if (player == null || serverName == null || serverName.trim().isEmpty()) {
            return;
        }

        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);

            out.writeUTF("Connect");
            out.writeUTF(serverName.trim());

            player.sendPluginMessage(CursorMenuPlugin.plugin, "BungeeCord", b.toByteArray());
        } catch (Exception e) {
            player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("system.server_transfer_failed", "&c[CursorMenu] 跨服传送失败: %error%").replace("%error%", e.getMessage()));
            CursorMenuPlugin.plugin.getLogger().warning("跨服传送失败: " + e.getMessage());
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        try {
            player.spigot().sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
            );
        } catch (Exception e) {
            CursorMenuPlugin.plugin.getLogger().warning("发送ActionBar消息失败: " + e.getMessage());
        }
    }
}
