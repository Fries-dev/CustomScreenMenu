package com.cmenu.ui;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public class AdminIpValidator {

    private final JavaPlugin plugin;

    private static boolean adminIpCheckEnabled = false;
    private static List<String> adminIpWhitelist = null;

    private static final ThreadLocal<Boolean> isTemporaryOpOperation =
            ThreadLocal.withInitial(() -> false);

    public AdminIpValidator(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        adminIpCheckEnabled = plugin.getConfig().getBoolean(
                "ip-binding.admin-ip-whitelist.enabled", false);

        adminIpWhitelist = plugin.getConfig().getStringList(
                "ip-binding.admin-ip-whitelist.ips");

        plugin.getLogger().info(
                "Admin IP whitelist configuration loaded - Enabled: "
                        + adminIpCheckEnabled
                        + ", IP Count: "
                        + adminIpWhitelist.size());
    }

    public boolean canAdminLogin(Player player) {

        if (!adminIpCheckEnabled) {
            return true;
        }

        if (isInTemporaryOpOperation()) {
            return true;
        }

        boolean isRealOp = player.isOp()
                && (player.hasPermission("bukkit.command.op")
                || player.hasPermission("*"));

        if (!isRealOp) {
            return true;
        }

        String playerIp = getPlayerIp(player);

        plugin.getLogger().info(
                "Checking login IP for OP player "
                        + player.getName()
                        + ": "
                        + playerIp);

        if (adminIpWhitelist == null || adminIpWhitelist.isEmpty()) {
            plugin.getLogger().warning(
                    "Admin IP whitelist is empty, allowing OP player "
                            + player.getName()
                            + " to log in");

            return true;
        }

        for (String allowedIp : adminIpWhitelist) {

            allowedIp = allowedIp.trim();

            if (allowedIp.isEmpty()) {
                continue;
            }

            if (allowedIp.equals(playerIp)) {
                plugin.getLogger().info(
                        "OP player "
                                + player.getName()
                                + "'s IP "
                                + playerIp
                                + " is in the whitelist, login allowed");

                return true;
            }

            if (allowedIp.contains("/")) {
                if (matchesCidr(playerIp, allowedIp)) {
                    plugin.getLogger().info(
                            "OP player "
                                    + player.getName()
                                    + "'s IP "
                                    + playerIp
                                    + " matches CIDR "
                                    + allowedIp
                                    + ", login allowed");

                    return true;
                }
            }
        }

        plugin.getLogger().warning(
                "OP player "
                        + player.getName()
                        + " attempted to log in from IP "
                        + playerIp
                        + ", but the IP is not in the admin whitelist");

        return false;
    }

    private String getPlayerIp(Player player) {
        return player.getAddress() != null
                ? player.getAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private boolean matchesCidr(String ip, String cidr) {

        try {

            String[] parts = cidr.split("/");

            if (parts.length != 2) {
                return false;
            }

            String networkAddress = parts[0].trim();
            int prefixLength = Integer.parseInt(parts[1].trim());

            byte[] ipBytes = ipToBytes(ip);
            byte[] networkBytes = ipToBytes(networkAddress);

            if (ipBytes == null
                    || networkBytes == null
                    || ipBytes.length != networkBytes.length) {
                return false;
            }

            int totalBits = ipBytes.length * 8;

            if (prefixLength < 0 || prefixLength > totalBits) {
                return false;
            }

            for (int i = 0; i < ipBytes.length; i++) {

                int ipBit = (ipBytes[i] < 0 ? ipBytes[i] + 256 : ipBytes[i]);
                int networkBit = (networkBytes[i] < 0 ? networkBytes[i] + 256 : networkBytes[i]);

                int bitsInByte = Math.min(8, prefixLength - (i * 8));

                if (bitsInByte <= 0) {
                    break;
                }

                int mask = (1 << bitsInByte) - 1;

                int ipPrefix = ipBit & mask;
                int networkPrefix = networkBit & mask;

                if (ipPrefix != networkPrefix) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {

            plugin.getLogger().warning(
                    "CIDR format parsing error: "
                            + cidr
                            + ", Error: "
                            + e.getMessage());

            return false;
        }
    }

    private byte[] ipToBytes(String ip) {

        if (ip == null || ip.isEmpty()) {
            return null;
        }

        try {

            if (ip.contains(".") && !ip.contains(":")) {

                String[] octets = ip.split("\\.");

                if (octets.length != 4) {
                    return null;
                }

                byte[] bytes = new byte[4];

                for (int i = 0; i < 4; i++) {

                    int octet = Integer.parseInt(octets[i].trim());

                    if (octet < 0 || octet > 255) {
                        return null;
                    }

                    bytes[i] = (byte) octet;
                }

                return bytes;
            }
            else if (ip.contains(":") || ip.contains("::")) {
                return null;
            }

        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    public boolean isAdminIpCheckEnabled() {
        return adminIpCheckEnabled;
    }

    public List<String> getAdminIpWhitelist() {
        return adminIpWhitelist;
    }

    public static void beginTemporaryOpOperation() {
        isTemporaryOpOperation.set(true);
    }

    public static void endTemporaryOpOperation() {
        isTemporaryOpOperation.remove();
    }

    private static boolean isInTemporaryOpOperation() {
        return Boolean.TRUE.equals(isTemporaryOpOperation.get());
    }
}