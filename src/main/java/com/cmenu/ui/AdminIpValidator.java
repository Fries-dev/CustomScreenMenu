package com.cmenu.ui;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminIpValidator {
    private final JavaPlugin plugin;
    private static boolean adminIpCheckEnabled = false;
    private static List<String> adminIpWhitelist = null;
    
    // 用于跟踪当前是否在临时OP操作中，避免触发管理员IP检查
    private static final ThreadLocal<Boolean> isTemporaryOpOperation = ThreadLocal.withInitial(() -> false);

    public AdminIpValidator(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * 加载管理员IP白名单配置
     */
    public void loadConfig() {
        adminIpCheckEnabled = plugin.getConfig().getBoolean("ip-binding.admin-ip-whitelist.enabled", false);
        adminIpWhitelist = plugin.getConfig().getStringList("ip-binding.admin-ip-whitelist.ips");
        plugin.getLogger().info("管理员IP白名单配置已加载 - 启用: " + adminIpCheckEnabled + ", IP数: " + adminIpWhitelist.size());
    }

    /**
     * 检查玩家是否可以登录（针对OP玩家的IP检查）
     * @param player 玩家对象
     * @return true表示可以登录，false表示IP不允许登录
     */
    public boolean canAdminLogin(Player player) {
        // 如果功能未启用，直接允许登录
        if (!adminIpCheckEnabled) {
            return true;
        }

        // 如果是临时OP操作，直接允许登录
        if (isInTemporaryOpOperation()) {
            return true;
        }

        // 检查玩家是否真正具有OP权限（不是临时设置的OP）
        // 1. 首先检查isOp()状态
        // 2. 然后检查是否具有真正的OP权限（bukkit.command.op或*）
        boolean isRealOp = player.isOp() && (player.hasPermission("bukkit.command.op") || player.hasPermission("*"));
        
        // 只有真正的OP玩家才需要进行IP检查
        if (!isRealOp) {
            return true;
        }

        String playerIp = getPlayerIp(player);
        plugin.getLogger().info("正在检查OP玩家 " + player.getName() + " 的登录IP: " + playerIp);

        // 如果白名单为空，允许登录（防止误配置导致OP无法登录）
        if (adminIpWhitelist == null || adminIpWhitelist.isEmpty()) {
            plugin.getLogger().warning("管理员IP白名单为空，允许OP玩家 " + player.getName() + " 登录");
            return true;
        }

        // 检查玩家IP是否在白名单中
        for (String allowedIp : adminIpWhitelist) {
            allowedIp = allowedIp.trim();
            if (allowedIp.isEmpty()) {
                continue;
            }

            // 精确匹配
            if (allowedIp.equals(playerIp)) {
                plugin.getLogger().info("OP玩家 " + player.getName() + " 的IP " + playerIp + " 在白名单中，允许登录");
                return true;
            }

            // CIDR格式匹配
            if (allowedIp.contains("/")) {
                if (matchesCidr(playerIp, allowedIp)) {
                    plugin.getLogger().info("OP玩家 " + player.getName() + " 的IP " + playerIp + " 匹配CIDR " + allowedIp + "，允许登录");
                    return true;
                }
            }
        }

        // IP不在白名单中
        plugin.getLogger().warning("OP玩家 " + player.getName() + " 尝试使用IP " + playerIp + " 登录，但该IP不在管理员IP白名单中");
        return false;
    }

    /**
     * 获取玩家的IP地址
     * @param player 玩家对象
     * @return 玩家的IP地址
     */
    private String getPlayerIp(Player player) {
        return player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
    }

    /**
     * 检查IP是否匹配CIDR格式
     * @param ip IP地址
     * @param cidr CIDR格式的IP范围
     * @return true表示匹配，false表示不匹配
     */
    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }

            String networkAddress = parts[0].trim();
            int prefixLength = Integer.parseInt(parts[1].trim());

            // 转换IP和子网掩码为字节数组
            byte[] ipBytes = ipToBytes(ip);
            byte[] networkBytes = ipToBytes(networkAddress);

            if (ipBytes == null || networkBytes == null || ipBytes.length != networkBytes.length) {
                return false;
            }

            // 计算子网掩码
            int totalBits = ipBytes.length * 8;
            if (prefixLength < 0 || prefixLength > totalBits) {
                return false;
            }

            // 检查IP是否在子网范围内
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
            plugin.getLogger().warning("CIDR格式解析错误: " + cidr + ", 错误信息: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将IP地址转换为字节数组
     * @param ip IP地址
     * @return 字节数组
     */
    private byte[] ipToBytes(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }

        try {
            // IPv4
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
            // IPv6
            else if (ip.contains(":") || ip.contains("::")) {
                return null; // IPv6支持可根据需要添加
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return null;
    }

    /**
     * 检查管理员IP检查功能是否启用
     * @return true表示启用，false表示未启用
     */
    public boolean isAdminIpCheckEnabled() {
        return adminIpCheckEnabled;
    }

    /**
     * 获取管理员IP白名单
     * @return IP白名单列表
     */
    public List<String> getAdminIpWhitelist() {
        return adminIpWhitelist;
    }
    
    /**
     * 标记开始临时OP操作
     */
    public static void beginTemporaryOpOperation() {
        isTemporaryOpOperation.set(true);
    }
    
    /**
     * 标记结束临时OP操作
     */
    public static void endTemporaryOpOperation() {
        isTemporaryOpOperation.remove();
    }
    
    /**
     * 检查当前是否在临时OP操作中
     * @return true表示正在进行临时OP操作，false表示不是
     */
    private static boolean isInTemporaryOpOperation() {
        return Boolean.TRUE.equals(isTemporaryOpOperation.get());
    }
}
