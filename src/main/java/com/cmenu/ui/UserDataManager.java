package com.cmenu.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserDataManager {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    private final Map<UUID, Integer> loginAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lockedPlayers = new ConcurrentHashMap<>();

    private static final long LOCKOUT_DURATION = 5 * 60 * 1000;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int BCRYPT_ROUNDS = 12;

    public UserDataManager(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public boolean canRegister(Player player) {
        String playerIp = getPlayerIp(player);
        boolean ipBindingEnabled = CursorMenuPlugin.plugin.isIpBindingEnabled();
        boolean usernameExists = databaseManager.isUsernameExists(player.getName());

        if (ipBindingEnabled && databaseManager.isIpAlreadyRegistered(playerIp)) {
            if (!usernameExists) {
                DatabaseManager.UserInfo existingUser = databaseManager.getUserInfoByIp(playerIp);
                if (existingUser != null && !existingUser.getUsername().equalsIgnoreCase(player.getName())) {
                    return false;
                }
            }
        }
        return true;
    }

    public RegistrationResult registerUser(Player player, String password) {
        String playerIp = getPlayerIp(player);
        
        // IP黑名单检查
        if (CursorMenuPlugin.plugin.isIpBlacklistEnabled() && 
            CursorMenuPlugin.plugin.isIpInBlacklist(playerIp)) {
            plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试注册，但IP在黑名单中");
            return new RegistrationResult(false, "ip_blacklisted");
        }
        
        // IP白名单检查（如果启用白名单且IP不在白名单中，则拒绝注册）
        if (CursorMenuPlugin.plugin.isIpWhitelistEnabled() && 
            !CursorMenuPlugin.plugin.isIpInWhitelist(playerIp)) {
            plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试注册，但IP不在白名单中");
            return new RegistrationResult(false, "ip_not_in_whitelist");
        }
        
        if (password == null || password.isEmpty()) {
            return new RegistrationResult(false, "empty_password");
        }

        if (password.length() < 6 || password.length() > 12) {
            return new RegistrationResult(false, "invalid_password_length");
        }

        if (!isValidPassword(password)) {
            return new RegistrationResult(false, "weak_password");
        }

        boolean ipBindingEnabled = CursorMenuPlugin.plugin.isIpBindingEnabled();

        if (databaseManager.isUsernameExists(player.getName())) {
            return new RegistrationResult(false, "user_exists");
        }

        if (ipBindingEnabled && databaseManager.isIpAlreadyRegistered(playerIp)) {
            DatabaseManager.UserInfo existingUser = databaseManager.getUserInfoByIp(playerIp);
            if (existingUser != null && !existingUser.getUsername().equalsIgnoreCase(player.getName())) {
                return new RegistrationResult(false, "ip_already_registered");
            }
        }

        String hashedPassword = hashPassword(password);
        if (databaseManager.registerUser(player.getName(), hashedPassword, playerIp)) {
            plugin.getLogger().info("玩家 " + player.getName() + " 已注册，IP: " + playerIp);
            return new RegistrationResult(true, "success");
        }

        return new RegistrationResult(false, "failed");
    }

    public AuthenticationResult authenticateUser(Player player, String password) {
        UUID playerId = player.getUniqueId();
        String playerIp = getPlayerIp(player);
        
        // IP黑名单检查
        if (CursorMenuPlugin.plugin.isIpBlacklistEnabled() && 
            CursorMenuPlugin.plugin.isIpInBlacklist(playerIp)) {
            plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试登录，但IP在黑名单中");
            return new AuthenticationResult(false, "ip_blacklisted");
        }
        
        // 管理员IP白名单检查（仅对OP玩家生效）
        // 检查玩家是否真正具有OP权限（不是临时设置的OP）
        boolean hasRealOpPermission = player.isOp() && (player.hasPermission("bukkit.command.op") || player.hasPermission("*"));
        if (CursorMenuPlugin.plugin.getAdminIpValidator() != null && hasRealOpPermission && 
            !CursorMenuPlugin.plugin.getAdminIpValidator().canAdminLogin(player)) {
            plugin.getLogger().warning("OP玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试登录，但IP不在管理员白名单中");
            return new AuthenticationResult(false, "admin_ip_not_allowed");
        }
        
        // IP白名单检查（如果启用白名单且IP不在白名单中，则拒绝登录）
        if (CursorMenuPlugin.plugin.isIpWhitelistEnabled() && 
            !CursorMenuPlugin.plugin.isIpInWhitelist(playerIp)) {
            plugin.getLogger().warning("玩家 " + player.getName() + " (IP: " + playerIp + ") 尝试登录，但IP不在白名单中");
            return new AuthenticationResult(false, "ip_not_in_whitelist");
        }
        
        if (isLockedOut(playerId)) {
            long remainingTime = (lockedPlayers.get(playerId) - System.currentTimeMillis()) / 1000;
            return new AuthenticationResult(false, "locked_out", remainingTime);
        }

        if (password == null || password.isEmpty()) {
            incrementLoginAttempts(playerId);
            return new AuthenticationResult(false, "empty_password");
        }

        if (!databaseManager.isUsernameExists(player.getName())) {
            return new AuthenticationResult(false, "user_not_found");
        }

        String storedHash = databaseManager.getStoredPasswordHash(player.getName());
        if (storedHash == null) {
            return new AuthenticationResult(false, "user_not_found");
        }
        
        boolean authenticated = verifyPassword(password, storedHash);

        if (authenticated) {
            DatabaseManager.UserInfo userInfo = databaseManager.getUserInfo(player.getName());

            if (userInfo != null && CursorMenuPlugin.plugin.isIpBindingEnabled()) {
                if (userInfo.isSameIp(playerIp)) {
                    databaseManager.updateLoginInfo(player.getName(), playerIp, true);
                    resetLoginAttempts(playerId);
                    return new AuthenticationResult(true, "success");
                } else {
                    if (CursorMenuPlugin.plugin.isIpBindingStrict()) {
                        databaseManager.updateLoginInfo(player.getName(), playerIp, false);
                        incrementLoginAttempts(playerId);
                        String lastIp = userInfo.getLastLoginIp() != null ? userInfo.getLastLoginIp() : userInfo.getRegisterIp();
                        return new AuthenticationResult(false, "ip_mismatch", lastIp);
                    } else {
                        databaseManager.updateLoginInfo(player.getName(), playerIp, true);
                        resetLoginAttempts(playerId);
                        player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("ip_warning", "&e[警告] 您的IP与注册时不同！"));
                        return new AuthenticationResult(true, "success_with_ip_warning");
                    }
                }
            } else {
                databaseManager.updateLoginInfo(player.getName(), playerIp, true);
                resetLoginAttempts(playerId);
                return new AuthenticationResult(true, "success");
            }
        } else {
            databaseManager.updateLoginInfo(player.getName(), playerIp, false);
            incrementLoginAttempts(playerId);
            return new AuthenticationResult(false, "wrong_password");
        }
    }

    private void incrementLoginAttempts(UUID playerId) {
        int attempts = loginAttempts.getOrDefault(playerId, 0) + 1;
        loginAttempts.put(playerId, attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            lockedPlayers.put(playerId, System.currentTimeMillis() + LOCKOUT_DURATION);
            plugin.getLogger().warning("玩家 " + playerId + " 因登录尝试次数过多被锁定 " + (LOCKOUT_DURATION / 1000) + " 秒");
        }

        if (databaseManager != null) {
            databaseManager.updateLoginAttempts(Bukkit.getPlayer(playerId) != null ?
                Bukkit.getPlayer(playerId).getName() : playerId.toString(), attempts);
        }
    }

    private void resetLoginAttempts(UUID playerId) {
        loginAttempts.remove(playerId);
        lockedPlayers.remove(playerId);

        if (databaseManager != null) {
            String playerName = Bukkit.getPlayer(playerId) != null ?
                Bukkit.getPlayer(playerId).getName() : playerId.toString();
            databaseManager.updateLoginAttempts(playerName, 0);
        }
    }

    private boolean isLockedOut(UUID playerId) {
        Long lockoutEnd = lockedPlayers.get(playerId);
        if (lockoutEnd == null) return false;

        if (System.currentTimeMillis() > lockoutEnd) {
            lockedPlayers.remove(playerId);
            return false;
        }
        return true;
    }

    public int getRemainingAttempts(UUID playerId) {
        int attempts = loginAttempts.getOrDefault(playerId, 0);
        return Math.max(0, MAX_LOGIN_ATTEMPTS - attempts);
    }

    public boolean isUserRegistered(String username) {
        return databaseManager.isUserRegistered(username);
    }

    public String hashPasswordForTest(String password) {
        return hashPassword(password);
    }

    private String hashPassword(String password) {
        if (password == null) {
            plugin.getLogger().warning("尝试哈希空密码");
            return "";
        }

        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
        } catch (Exception e) {
            plugin.getLogger().severe("无法创建密码哈希: " + e.getMessage());
            return "";
        }
    }

    private boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null || hash.isEmpty()) {
            return false;
        }
        
        if (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(password, hash);
            } catch (Exception e) {
                plugin.getLogger().warning("密码验证失败: " + e.getMessage());
                return false;
            }
        } else {
            return verifyLegacyPassword(password, hash);
        }
    }

    private boolean verifyLegacyPassword(String password, String hash) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            plugin.getLogger().severe("无法验证旧密码格式: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) return false;

        // 使用正则表达式检查密码是否只包含数字和英文大小写字母
        // ^[a-zA-Z0-9]+$ 表示字符串必须由一个或多个字母或数字组成
        return password.matches("^[a-zA-Z0-9]+$");
    }

    private String getPlayerIp(Player player) {
        String address = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
        return address;
    }

    public void cleanupPlayer(UUID playerId) {
        loginAttempts.remove(playerId);
        lockedPlayers.remove(playerId);
    }

    public static class RegistrationResult {
        private final boolean success;
        private final String messageKey;
        private final Object[] args;

        public RegistrationResult(boolean success, String messageKey, Object... args) {
            this.success = success;
            this.messageKey = messageKey;
            this.args = args;
        }

        public boolean isSuccess() { return success; }
        public String getMessageKey() { return messageKey; }
        public Object[] getArgs() { return args; }
    }

    public static class AuthenticationResult {
        private final boolean success;
        private final String messageKey;
        private final Object extraData;

        public AuthenticationResult(boolean success, String messageKey, Object extraData) {
            this.success = success;
            this.messageKey = messageKey;
            this.extraData = extraData;
        }

        public AuthenticationResult(boolean success, String messageKey) {
            this(success, messageKey, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessageKey() { return messageKey; }
        public Object getExtraData() { return extraData; }
    }
}
