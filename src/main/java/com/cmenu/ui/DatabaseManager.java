package com.cmenu.ui;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Ensure the data folder exists
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // Connect to the SQLite database (auto-created if it does not exist)
            String url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "users.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // Create the users table (enhanced version with IP binding fields)
            try (Statement stmt = connection.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT NOT NULL UNIQUE," +
                        "password_hash TEXT NOT NULL," +
                        "registered BOOLEAN NOT NULL DEFAULT 0," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "register_ip TEXT NOT NULL DEFAULT ''," +
                        "last_login_ip TEXT," +
                        "login_attempts INTEGER DEFAULT 0," +
                        "last_login_time TIMESTAMP" +
                        ")";
                stmt.execute(sql);
            }

            // Check whether new columns need to be added (legacy database upgrade)
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)");
                boolean hasRegisterIp = false;
                boolean hasLastLoginIp = false;

                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("register_ip".equals(columnName)) hasRegisterIp = true;
                    if ("last_login_ip".equals(columnName)) hasLastLoginIp = true;
                }

                // Add register_ip column
                if (!hasRegisterIp) {
                    stmt.execute("ALTER TABLE users ADD COLUMN register_ip TEXT NOT NULL DEFAULT ''");
                    plugin.getLogger().info("Added register_ip column to users table");
                }

                // Add last_login_ip column
                if (!hasLastLoginIp) {
                    stmt.execute("ALTER TABLE users ADD COLUMN last_login_ip TEXT");
                    plugin.getLogger().info("Added last_login_ip column to users table");
                }

                // Add login attempt count and last login time columns
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN login_attempts INTEGER DEFAULT 0");
                } catch (SQLException ignored) {}
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN last_login_time TIMESTAMP");
                } catch (SQLException ignored) {}
            }

            plugin.getLogger().info("User database initialized successfully (enhanced - IP binding)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error initializing database: " + e.getMessage());
        }
    }

    // Get database connection, re-establishing it if the connection is closed
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Re-establish database connection
            String url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "users.db").getAbsolutePath();
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Database connection re-established successfully");
        }
        return connection;
    }

    public boolean isUserRegistered(String username) {
        String sql = "SELECT registered FROM users WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBoolean("registered");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error querying user registration status: " + e.getMessage());
        }

        return false;
    }

    public boolean isUsernameExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking whether username exists: " + e.getMessage());
        }

        return false;
    }

    public boolean isIpAlreadyRegistered(String registerIp) {
        String sql = "SELECT id FROM users WHERE register_ip = ? AND registered = 1";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, registerIp);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error checking IP registration status: " + e.getMessage());
        }

        return false;
    }

    public boolean registerUser(String username, String passwordHash, String registerIp) {
        String sql = "INSERT INTO users(username, password_hash, registered, register_ip) VALUES(?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setBoolean(3, true);
            pstmt.setString(4, registerIp);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error registering user: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticateUser(String username, String passwordHash) {
        String sql = "SELECT password_hash FROM users WHERE username = ? AND registered = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setBoolean(2, true);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                return storedHash.equals(passwordHash);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error authenticating user: " + e.getMessage());
        }

        return false;
    }

    public String getStoredPasswordHash(String username) {
        String sql = "SELECT password_hash FROM users WHERE username = ? AND registered = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setBoolean(2, true);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("password_hash");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving user password hash: " + e.getMessage());
        }

        return null;
    }

    public UserInfo getUserInfo(String username) {
        String sql = "SELECT username, register_ip, last_login_ip, login_attempts, last_login_time FROM users WHERE username = ? AND registered = 1";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UserInfo(
                        rs.getString("username"),
                        rs.getString("register_ip"),
                        rs.getString("last_login_ip"),
                        rs.getInt("login_attempts"),
                        rs.getTimestamp("last_login_time")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving user info: " + e.getMessage());
        }

        return null;
    }

    public UserInfo getUserInfoByIp(String ip) {
        String sql = "SELECT username, register_ip, last_login_ip, login_attempts, last_login_time FROM users WHERE register_ip = ? AND registered = 1 LIMIT 1";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, ip);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UserInfo(
                        rs.getString("username"),
                        rs.getString("register_ip"),
                        rs.getString("last_login_ip"),
                        rs.getInt("login_attempts"),
                        rs.getTimestamp("last_login_time")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving user info by IP: " + e.getMessage());
        }

        return null;
    }

    public boolean updateLoginInfo(String username, String currentIp, boolean success) {
        String sql = "UPDATE users SET last_login_ip = ?, login_attempts = ?, last_login_time = CURRENT_TIMESTAMP WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, currentIp);
            pstmt.setInt(2, success ? 0 : -1);
            pstmt.setString(3, username);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating login info: " + e.getMessage());
            return false;
        }
    }

    public boolean updateLoginAttempts(String username, int attempts) {
        String sql = "UPDATE users SET login_attempts = ? WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, attempts);
            pstmt.setString(2, username);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating login attempt count: " + e.getMessage());
            return false;
        }
    }

    public int getLoginAttempts(String username) {
        String sql = "SELECT login_attempts FROM users WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("login_attempts");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving login attempt count: " + e.getMessage());
        }

        return 0;
    }

    public boolean updateUserPassword(String username, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, newPasswordHash);
            pstmt.setString(2, username);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error updating user password: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, username);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
        }
    }

    public static class UserInfo {
        private final String username;
        private final String registerIp;
        private final String lastLoginIp;
        private final int loginAttempts;
        private final java.sql.Timestamp lastLoginTime;

        public UserInfo(String username, String registerIp, String lastLoginIp, int loginAttempts, java.sql.Timestamp lastLoginTime) {
            this.username = username;
            this.registerIp = registerIp;
            this.lastLoginIp = lastLoginIp;
            this.loginAttempts = loginAttempts;
            this.lastLoginTime = lastLoginTime;
        }

        public String getUsername() { return username; }
        public String getRegisterIp() { return registerIp; }
        public String getLastLoginIp() { return lastLoginIp; }
        public int getLoginAttempts() { return loginAttempts; }
        public java.sql.Timestamp getLastLoginTime() { return lastLoginTime; }

        public boolean isSameIp(String currentIp) {
            return registerIp != null && !registerIp.isEmpty() && registerIp.equals(currentIp);
        }
    }
}