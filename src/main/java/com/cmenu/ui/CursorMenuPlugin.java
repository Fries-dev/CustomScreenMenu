package com.cmenu.ui;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import com.cmenu.ui.layout.MenuLayout;
import com.cmenu.ui.section.PlayerLocationOverride;
import com.cmenu.ui.section.Section;
import com.cmenu.ui.section.SectionManager;
import com.cmenu.ui.npc.NPCModule;
import com.cmenu.ui.wasd.WASDModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import space.arim.morepaperlib.MorePaperLib;
import org.bukkit.util.Vector;
import org.bukkit.metadata.FixedMetadataValue;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
public class CursorMenuPlugin extends JavaPlugin {
    public static boolean creatureSpawnLimitEnabled;
    public static int creatureSpawnLimitRadius;
    private final Map<String, Set<Long>> forcedLoadedChunks = new HashMap<>();
    private final Map<String, Set<Long>> persistentChunks = new HashMap<>();
    private int chunkLoaderTaskId = -1;
    public static List<String> joinRunCommands = new ArrayList<>();
    public static boolean cameraBlockCheckEnabled;
    public static int cameraBlockCheckRadius;
    public static float exitYaw;
    public static float exitPitch;
    public static List<String> allowedCommands = new ArrayList<>();
    public static TextDisplayManager textDisplayManager;
    public static double cursorZOffset;
    public static double cursorX;
    public static double cursorY;
    public static ItemDisplayManager itemDisplayManager;
    public static CursorMenuPlugin plugin;
    public static Map<Player, ArmorStand> playerCursors = new ConcurrentHashMap<>();
    private final Map<Player, List<TextDisplay>> playerDisplays = new ConcurrentHashMap<>();
    private final Map<Player, ItemDisplay> playerItemDisplays = new ConcurrentHashMap<>();
    private final Map<Player, ItemDisplay> playerPumpkinDisplays = new ConcurrentHashMap<>();
    public static Map<Player, Location> playerLocations = new ConcurrentHashMap<>();
    public static Map<Player, Pig> playerSit = new ConcurrentHashMap<>();
    public static Map<Player, Location> cursorExactLocations = new ConcurrentHashMap<>();
    public static Set<String> playingSound = ConcurrentHashMap.newKeySet();
    private boolean debugMode;
    public static MorePaperLib foliaLib;
    public static SectionManager sectionManager = new SectionManager();
    public static boolean soundLoop;
    public static int soundRate;
    public static String soundName;
    public static float soundVolume;
    public static float soundPitch;
    public static boolean joinRunBool;
    public static String joinRunSection;
    public static String cursorItem;
    public static int cursorModelData;
    public static double maxX;
    public static double maxY;
    public static double cursorScale;
    public static int runDelay;
    public static boolean hasPAPI;
    public static boolean usePumpkinOverlay;
    public static boolean cursorMovementRangeEnabled;
    public static double cursorMovementRangeXMin;
    public static double cursorMovementRangeXMax;
    public static double cursorMovementRangeYMin;
    public static double cursorMovementRangeYMax;
    public static boolean cursorDefaultPositionEnabled;
    public static double cursorDefaultPositionX;
    public static double cursorDefaultPositionY;
    private HoverEnlargeManager hoverEnlargeManager;
    private Map<Player, String> currentPlayerMenus = new ConcurrentHashMap<>();
    public Map<Player, MenuLayout> selectedLayouts = new ConcurrentHashMap<>();
    private UserDataManager userDataManager;
    private DatabaseManager databaseManager;
    private AdminIpValidator adminIpValidator;
    private Map<UUID, String> currentPlayerInputFields = new ConcurrentHashMap<>();
    private Map<UUID, Map<String, String>> userInputData = new ConcurrentHashMap<>();
    private Map<UUID, Boolean> passwordVisibility = new ConcurrentHashMap<>();
    private Set<UUID> loggedInPlayers = ConcurrentHashMap.newKeySet();
    private static boolean ipBindingEnabled = true;
    private static boolean ipBindingStrict = false;
    private static boolean allowSameIpLogin = true;
    private static int lockoutDuration = 5;
    private static int maxLoginAttempts = 5;
    private static boolean ipWhitelistEnabled = false;
    private static List<String> ipWhitelist = new ArrayList<>();
    private static boolean ipBlacklistEnabled = false;
    private static List<String> ipBlacklist = new ArrayList<>();
    private List<String> loginRequiredNameTags = new ArrayList<>();
    private List<String> loginRequiredKeyTags = new ArrayList<>();
    private List<String> noDuplicateRegNameTags = new ArrayList<>();
    private List<String> noDuplicateRegKeyTags = new ArrayList<>();
    public boolean isPlayerLoggedIn(Player player) {
        return loggedInPlayers.contains(player.getUniqueId());
    }
    public void setPlayerLoggedIn(Player player, boolean loggedIn) {
        UUID playerId = player.getUniqueId();
        if (loggedIn) {
            loggedInPlayers.add(playerId);
        } else {
            loggedInPlayers.remove(playerId);
        }
    }
    public boolean requiresLogin(String name, String key) {
        for (String tag : loginRequiredNameTags) {
            if (name != null && name.contains(tag)) {
                return true;
            }
        }
        for (String tag : loginRequiredKeyTags) {
            if (key != null && key.contains(tag)) {
                return true;
            }
        }
        return false;
    }
    public boolean requiresNotRegistered(String name, String key) {
        for (String tag : noDuplicateRegNameTags) {
            if (name != null && name.contains(tag)) {
                return true;
            }
        }
        for (String tag : noDuplicateRegKeyTags) {
            if (key != null && key.contains(tag)) {
                return true;
            }
        }

        return false;
    }
    public boolean isIpBindingEnabled() {
        return ipBindingEnabled;
    }
    public boolean isIpBindingStrict() {
        return ipBindingStrict;
    }
    public boolean isAllowSameIpLogin() {
        return allowSameIpLogin;
    }
    public int getLockoutDuration() {
        return lockoutDuration;
    }
    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }
    public boolean isIpWhitelistEnabled() {
        return ipWhitelistEnabled;
    }
    public List<String> getIpWhitelist() {
        return ipWhitelist;
    }
    public boolean isIpBlacklistEnabled() {
        return ipBlacklistEnabled;
    }
    public List<String> getIpBlacklist() {
        return ipBlacklist;
    }
    public boolean isIpInWhitelist(String ip) {
        if (!ipWhitelistEnabled || ipWhitelist.isEmpty()) {
            return false;
        }
        return matchesIpList(ip, ipWhitelist);
    }
    public boolean isIpInBlacklist(String ip) {
        if (!ipBlacklistEnabled || ipBlacklist.isEmpty()) {
            return false;
        }
        return matchesIpList(ip, ipBlacklist);
    }
    private boolean matchesIpList(String ip, List<String> ipList) {
        if (ip == null || ipList == null) {
            return false;
        }
        for (String pattern : ipList) {
            pattern = pattern.trim();
            if (pattern.isEmpty()) {
                continue;
            }
            if (pattern.equals(ip)) {
                return true;
            }
            if (pattern.contains("/")) {
                if (matchesCidr(ip, pattern)) {
                    return true;
                }
            }
        }
        return false;
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
            if (ipBytes == null || networkBytes == null || ipBytes.length != networkBytes.length) {
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
    public String getCurrentPlayerMenu(Player player) {
        return currentPlayerMenus.get(player);
    }
    public MenuLayout getSelectedLayout(Player player) {
        return selectedLayouts.get(player);
    }
    public ItemDisplayManager getItemDisplayManager() {
        return itemDisplayManager;
    }
    public UserDataManager getUserDataManager() {
        return userDataManager;
    }
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    public AdminIpValidator getAdminIpValidator() {
        return adminIpValidator;
    }
    public String getCurrentPlayerInputField(Player player) {
        return currentPlayerInputFields.get(player.getUniqueId());
    }
    public void setCurrentPlayerInputField(Player player, String field) {
        currentPlayerInputFields.put(player.getUniqueId(), field);
    }
    public Map<String, String> getPlayerInputData(Player player) {
        UUID playerId = player.getUniqueId();
        return userInputData.computeIfAbsent(playerId, k -> new HashMap<>());
    }
    public void setPlayerInputData(Player player, String field, String value) {
        UUID playerId = player.getUniqueId();
        userInputData.computeIfAbsent(playerId, k -> new HashMap<>()).put(field, value);
    }
    public boolean getPasswordVisibility(Player player) {
        return passwordVisibility.getOrDefault(player.getUniqueId(), false); // Default: show plaintext
    }
    public void togglePasswordVisibility(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentVisibility = passwordVisibility.getOrDefault(playerId, false);
        passwordVisibility.put(playerId, !currentVisibility);
    }
    private void purgeAllEntities() {
        playerCursors.values().forEach(e -> { if (e != null && !e.isDead()) e.remove(); });
        playerSit.values().forEach(e -> { if (e != null && !e.isDead()) e.remove(); });
        playerDisplays.values().forEach(list -> list.forEach(e -> { if (e != null && !e.isDead()) e.remove(); }));
        playerItemDisplays.values().forEach(e -> { if (e != null && !e.isDead()) e.remove(); });
        if (itemDisplayManager != null) Bukkit.getOnlinePlayers().forEach(itemDisplayManager::hideItem);
        if (textDisplayManager != null) textDisplayManager.cleanup();
        if (hoverEnlargeManager != null) hoverEnlargeManager.cleanup();
        if (databaseManager != null) databaseManager.closeConnection();
    }
    private void mergeYamlFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            saveResource(fileName, false);
            return;
        }
        YamlConfiguration defaultConfig;
        try {
            defaultConfig = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(getResource(fileName), java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            getLogger().warning("Failed to load default configuration " + fileName + ": " + e.getMessage());
            defaultConfig = new YamlConfiguration();
        }
        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        for (String key : defaultConfig.getKeys(true)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
            }
        }
        if (defaultConfig.contains("version") && !userConfig.getString("version").equals(defaultConfig.getString("version"))) {
            userConfig.set("version", defaultConfig.getString("version"));
        }
        try {
            userConfig.save(file);
        } catch (java.io.IOException e) {
            getLogger().warning("Failed to save merged " + fileName + ": " + e.getMessage());
        }
    }
    private void ensureDefaultsOnReload(String... files) {
        for (String name : files) {
            File file = new File(getDataFolder(), name);
            if (!file.exists()) {
                saveResource(name, false);
                getLogger().info("[CustomScreenMenu] Missing file detected, regenerated default configuration: " + name);
            }
        }
    }
    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        mergeYamlFile("config.yml");
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false)
                .bStats(true);
        PacketEvents.getAPI().load();
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        foliaLib = new MorePaperLib(this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            hasPAPI = true;
        }
        reloadLangConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        itemDisplayManager = new ItemDisplayManager(this);
        registerUseEntityPacketListener();
        startChunkLoaderTask();
        getServer().getPluginManager().registerEvents(new CreatureSpawnListener(), this);

        getServer().getPluginManager().registerEvents(new SessionCleanupListener(), this);

        getServer().getPluginManager().registerEvents((Listener)new MenuListener(this), this);

        getServer().getPluginManager().registerEvents(new AttackBreakListener(), this);

        // Register player join event listener
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);


        Bukkit.getPluginCommand("cursormenu").setExecutor(new Commands());

        textDisplayManager = new TextDisplayManager(this);

        // Initialize hover-enlarge manager
        hoverEnlargeManager = new HoverEnlargeManager(this);

        // Initialize database manager
        databaseManager = new DatabaseManager(this);

        // Initialize user data manager
        userDataManager = new UserDataManager(this, databaseManager);

        // Initialize admin IP validator
        adminIpValidator = new AdminIpValidator(this);

        // Initialize NPC mirror module
        NPCModule.initialize(this);

        // Initialize WASD navigation module
        WASDModule.initialize(this);


        getLogger().info("====================================");
        getLogger().info("CustomScreenMenu plugin started");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Author: Nobita " + getDescription().getAuthors());
        getLogger().info("Thank you for using this plugin!");
        getLogger().info("====================================");

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CursorMenuPlaceholder(this).register();
            getLogger().info("PlaceholderAPI placeholder support registered");
        }

        // Ensure all language files are created
        String[] langFiles = {"lang.yml", "lang/zh_cn.yml", "lang/en_us.yml", "lang/ru_ru.yml"};
        for (String file : langFiles) {
            File targetFile = new File(getDataFolder(), file);
            if (!targetFile.exists()) {
                saveResource(file, false);
            }
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!playerCursors.containsKey(player)) continue;

                Location loc = player.getLocation();
                float yaw = loc.getYaw();
                float pitch = loc.getPitch();

                updateCursorPosition(player, yaw, pitch);

                // Update pumpkin head position
                ItemDisplay pumpkinDisplay = playerPumpkinDisplays.get(player);
                if (pumpkinDisplay != null && pumpkinDisplay.isValid()) {
                    Location pumpkinLoc = loc.clone().add(0, 1.7, 0);
                    pumpkinDisplay.teleport(pumpkinLoc);
                    pumpkinDisplay.setRotation(loc.getYaw(), loc.getPitch());
                }
            }
        }, 0L, 1L);
    }

    @Override
    public void onDisable() {
        // Shut down WASD navigation module
        WASDModule.shutdown();

        // Shut down NPC mirror module
        NPCModule.shutdown();

        // Unregister BungeeCord/Velocity channel
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");

        PacketEvents.getAPI().terminate();
        purgeAllEntities();

        getLogger().info("====================================");
        getLogger().info("CustomScreenMenu plugin disabled");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Thank you for using, see you next time!");
        getLogger().info("====================================");
    }


    // Language configuration
    private YamlConfiguration langConfig;

    // Get message from language configuration
    public String getLangMessage(String key, String defaultValue) {
        if (langConfig == null) {
            reloadLangConfig();
        }
        // Reload config from file each time to ensure freshness
        File langFile = new File(getDataFolder(), "lang.yml");
        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        }
        return ChatColor.translateAlternateColorCodes('&', langConfig.getString(key, defaultValue));
    }

    // Reload language configuration
    public void reloadLangConfig() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        mergeYamlFile("lang.yml");
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void reloadPluginConfig() {
        // Reload language configuration
        reloadLangConfig();

        getLogger().info("Reloading plugin configuration...");

        // Reload main configuration
        reloadConfig();

        if (itemDisplayManager != null) {
            itemDisplayManager.reloadConfig();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerCursors.containsKey(player)) {

                ArmorStand cursor = playerCursors.remove(player);
                if (cursor != null && !cursor.isDead()) {
                    cursor.remove();
                }

                List<TextDisplay> textDisplays = playerDisplays.remove(player);
                if (textDisplays != null) {
                    textDisplays.forEach(display -> {
                        if (display != null && !display.isDead()) {
                            display.remove();
                        }
                    });
                    textDisplays.clear();
                }

                ItemDisplay itemDisplay = playerItemDisplays.remove(player);
                if (itemDisplay != null && !itemDisplay.isDead()) {
                    itemDisplay.remove();
                }

                Pig sit = playerSit.remove(player);
                if (sit != null) {
                    if (sit.getPassengers().contains(player)) {
                        sit.removePassenger(player);
                    }
                    sit.remove();
                }

                playingSound.remove(player.getName());
                player.stopAllSounds();
                sendCameraPacket(player, player);
                player.setInvisible(false);
                player.setCollidable(true);

                Location originalLoc = playerLocations.remove(player);
                if (originalLoc != null) {
                    player.teleport(originalLoc);
                }

                // Clean up hover-enlarge effects
                if (hoverEnlargeManager != null) {
                    hoverEnlargeManager.cleanupPlayer(player);
                }

                // Clean up user input data
                currentPlayerInputFields.remove(player.getUniqueId());
                userInputData.remove(player.getUniqueId());
                passwordVisibility.remove(player.getUniqueId());

                // Restore blocking blocks
                stopCursor(player, true);
            }

            itemDisplayManager.hideItem(player);
        }

        for (List<TextDisplay> displays : playerDisplays.values()) {
            displays.forEach(display -> {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            });
        }

        if (textDisplayManager != null) {
            textDisplayManager.cleanup();
        }

        if (hoverEnlargeManager != null) {
            hoverEnlargeManager.cleanup();
        }

        purgeAllEntities();

        playerCursors.clear();
        playerDisplays.clear();
        playerItemDisplays.clear();
        playerLocations.clear();
        playerSit.clear();
        playingSound.clear();
        sectionManager.clear();
        currentPlayerInputFields.clear();
        userInputData.clear();
        passwordVisibility.clear();
        loggedInPlayers.clear();

        loadConfig();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasMetadata(HELMET_META_KEY)) {
                p.removeMetadata(HELMET_META_KEY, this);
            }
        });

        if (textDisplayManager != null) {
            textDisplayManager.reloadConfig();
        }

        ensureDefaultsOnReload(
                "config.yml",
                "items.yml",
                "lang.yml",
                "lang/zh_cn.yml",
                "lang/en_us.yml",
                "lang/ru_ru.yml",
                "commands.yml"
        );

        cursorModelData = getConfig().getInt("cursor-item.custom-model-data", 0);
        getLogger().info("Configuration reloaded successfully, all menu elements refreshed");
        File commandsFile = new File(getDataFolder(), "commands.yml");
        YamlConfiguration commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
        allowedCommands = commandsConfig.getStringList("allowed-commands");
        allowedCommands.replaceAll(String::toLowerCase);
        updatePersistentChunks();
        // Reload creature spawn limit configuration
        creatureSpawnLimitEnabled = getConfig().getBoolean("creature-spawn-limits.enabled", false);
        creatureSpawnLimitRadius = getConfig().getInt("creature-spawn-limits.radius", 10);

        // Re-initialize hover-enlarge manager
        if (hoverEnlargeManager != null) {
            hoverEnlargeManager.cleanup();
        }
        hoverEnlargeManager = new HoverEnlargeManager(this);

        // Re-initialize database manager
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        databaseManager = new DatabaseManager(this);

        // Re-initialize user data manager
        if (userDataManager != null) {
            // Note: userDataManager needs to be re-created here
        }
        userDataManager = new UserDataManager(this, databaseManager);

        // Re-initialize admin IP validator
        if (adminIpValidator != null) {
            // Reload configuration
            adminIpValidator.loadConfig();
        } else {
            adminIpValidator = new AdminIpValidator(this);
        }

        // Reload NPC mirror module
        NPCModule.reload();

        // Reload WASD navigation module
        WASDModule.reload();

        // Re-create menu elements for all online players to apply new configuration
        for (Player player : Bukkit.getOnlinePlayers()) {
            String currentMenu = currentPlayerMenus.get(player);
            if (currentMenu != null) {
                // Re-create menu elements for player using new configuration
                setupCursor(player, currentMenu);
            }
        }
    }


    private void loadConfig() {

        reloadConfig();

        File config = new File(this.getDataFolder(), "config.yml");
        if (!config.exists()) {
            saveDefaultConfig();
        }

        // Load button access control configuration
        loadButtonAccessControlConfig();

        // Load IP binding configuration
        loadIpBindingConfig();

        cameraBlockCheckEnabled = getConfig().getBoolean("camera-block-check.enabled", false);
        cameraBlockCheckRadius = getConfig().getInt("camera-block-check.radius", 5);
        cursorZOffset = getConfig().getDouble("cursor-item.z-offset", 0.0);
        cursorX = getConfig().getDouble("cursor-item.x", 0.0);
        cursorY = getConfig().getDouble("cursor-item.y", 0.0);
        soundLoop = getConfig().getBoolean("sound.loop.enabled");
        soundRate = getConfig().getInt("sound.loop.duration");
        soundName = getConfig().getString("sound.name");
        soundVolume = Float.parseFloat(getConfig().getString("sound.volume"));
        soundPitch = Float.parseFloat(getConfig().getString("sound.pitch"));
        debugMode = getConfig().getBoolean("Debug", false);
        usePumpkinOverlay = getConfig().getBoolean("use-pumpkin-overlay", false);
        joinRunBool = getConfig().getBoolean("join-run.enabled", false);
        joinRunSection = getConfig().getString("join-run.menu","test");
        cursorItem = getConfig().getString("cursor-item.material", "ARROW");
        cursorScale = getConfig().getDouble("cursor-item.scale",1);
        cursorModelData = getConfig().getInt("cursor-item.custom-model-data", 0);
        maxX = getConfig().getDouble("cursor-item.max-x");
        maxY = getConfig().getDouble("cursor-item.max-y");
        // Read cursor movement range limit configuration
        cursorMovementRangeEnabled = getConfig().getBoolean("cursor-item.movement-range.enabled", false);
        cursorMovementRangeXMin = getConfig().getDouble("cursor-item.movement-range.x.min", -2.0);
        cursorMovementRangeXMax = getConfig().getDouble("cursor-item.movement-range.x.max", 2.0);
        cursorMovementRangeYMin = getConfig().getDouble("cursor-item.movement-range.y.min", -3.0);
        cursorMovementRangeYMax = getConfig().getDouble("cursor-item.movement-range.y.max", 3.0);
        // Read cursor default position configuration
        cursorDefaultPositionEnabled = getConfig().getBoolean("cursor-item.default-position.enabled", false);
        cursorDefaultPositionX = getConfig().getDouble("cursor-item.default-position.x", 0.0);
        cursorDefaultPositionY = getConfig().getDouble("cursor-item.default-position.y", 0.0);
        runDelay = getConfig().getInt("join-run.delay", 0);
        joinRunCommands = getConfig().getStringList("join-run.commands");
        exitYaw = (float) getConfig().getDouble("exit-camera.yaw", 0.0);
        exitPitch = (float) getConfig().getDouble("exit-camera.pitch", 0.0);

        // Load creature spawn limit configuration
        creatureSpawnLimitEnabled = getConfig().getBoolean("creature-spawn-limits.enabled", false);
        creatureSpawnLimitRadius = getConfig().getInt("creature-spawn-limits.radius", 10);

        PlayerLocationOverride.reload(getConfig().getBoolean("use-player-location", false));

        saveDefaultMenuFiles(); // Generate default menu configuration
        sectionManager.loadAllMenuConfigs();

        File commandsFile = new File(getDataFolder(), "commands.yml");
        if (!commandsFile.exists()) {
            saveResource("commands.yml", false);
        }
        YamlConfiguration commandsConfig = YamlConfiguration.loadConfiguration(commandsFile);
        allowedCommands = commandsConfig.getStringList("allowed-commands");
        allowedCommands.replaceAll(String::toLowerCase);
        updatePersistentChunks();
    }

    /**
     * Load button access control configuration
     */
    private void loadButtonAccessControlConfig() {
        // Load login-required button tag configuration
        loginRequiredNameTags = getConfig().getStringList("button-access-control.login-required.name-tags");
        loginRequiredKeyTags = getConfig().getStringList("button-access-control.login-required.key-tags");

        // Load no-duplicate-registration button tag configuration
        noDuplicateRegNameTags = getConfig().getStringList("button-access-control.no-duplicate-registration.name-tags");
        noDuplicateRegKeyTags = getConfig().getStringList("button-access-control.no-duplicate-registration.key-tags");

        // If configuration is empty, use default values
        if (loginRequiredNameTags.isEmpty()) {
            loginRequiredNameTags.add("[Login Required]");
            loginRequiredNameTags.add("[LOGIN_REQUIRED]");
        }

        if (loginRequiredKeyTags.isEmpty()) {
            loginRequiredKeyTags.add("login_required");
        }

        if (noDuplicateRegNameTags.isEmpty()) {
            noDuplicateRegNameTags.add("[No Duplicate Registration]");
            noDuplicateRegNameTags.add("[NO_DUPLICATE_REG]");
        }

        if (noDuplicateRegKeyTags.isEmpty()) {
            noDuplicateRegKeyTags.add("no_duplicate_reg");
        }
    }

    /**
     * Load IP binding security configuration
     */
    private void loadIpBindingConfig() {
        ipBindingEnabled = getConfig().getBoolean("ip-binding.enabled", true);
        ipBindingStrict = getConfig().getBoolean("ip-binding.strict", false);
        allowSameIpLogin = getConfig().getBoolean("ip-binding.allow-same-ip-login", true);
        lockoutDuration = getConfig().getInt("ip-binding.lockout-duration", 5);
        maxLoginAttempts = getConfig().getInt("ip-binding.max-login-attempts", 5);

        // Load IP whitelist configuration
        ipWhitelistEnabled = getConfig().getBoolean("ip-binding.whitelist.enabled", false);
        ipWhitelist = getConfig().getStringList("ip-binding.whitelist.ips");

        // Load IP blacklist configuration
        ipBlacklistEnabled = getConfig().getBoolean("ip-binding.blacklist.enabled", false);
        ipBlacklist = getConfig().getStringList("ip-binding.blacklist.ips");

        getLogger().info("IP binding security config loaded - Enabled: " + ipBindingEnabled + ", Strict mode: " + ipBindingStrict);
        getLogger().info("IP whitelist: " + (ipWhitelistEnabled ? "Enabled" : "Disabled") + ", Rules: " + ipWhitelist.size());
        getLogger().info("IP blacklist: " + (ipBlacklistEnabled ? "Enabled" : "Disabled") + ", Rules: " + ipBlacklist.size());
    }

    public void setupCursor(Player player, String key) {
        if (playerCursors.containsKey(player)) {
            // Check whether a refresh is needed instead of a full menu rebuild
            String currentMenuKey = currentPlayerMenus.get(player);
            Section currentSection = sectionManager.get(currentMenuKey);
            Section newSection = sectionManager.get(key);

            // If both menus are at the same world/coordinates, only refresh content.
            // Fix: ensure a refresh happens when menu keys differ, or force-refresh (e.g. password visibility toggle)
            if (currentSection != null && newSection != null &&
                    isSameLocation(currentSection, newSection)) {
                refreshMenuContent(player, key, currentMenuKey);
                return;
            }

            // Otherwise follow the original logic
            stopCursor(player, false);
        }

        currentPlayerMenus.put(player, key);
        Section section = sectionManager.get(key);

        // Null check to prevent NullPointerException
        if (section == null) {
            getLogger().warning("Menu '" + key + "' not found! Please check your menu configuration.");
            player.sendMessage(getLangMessage("command.menu_not_found", "&c[CursorMenu] Menu '%menu%' not found, please check your configuration file!").replace("%menu%", key));
            return;
        }

        PlayerLocationOverride.apply(player, section);
        World world = Bukkit.getWorld(section.world);
        if (world == null) {
            getLogger().warning("World " + section.world + " not found!");
            return;
        }

        playerLocations.put(player, player.getLocation());

        Location targetLoc = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);


        world.getChunkAt(targetLoc).load(true);

        foliaLib.scheduling().regionSpecificScheduler(targetLoc).runDelayed(task -> {
            if (!player.isOnline()) return;

            player.teleport(new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch));

            if (!player.getWorld().equals(world)) {
                player.teleport(targetLoc);
            }
            player.setMetadata("cursor_original_gamemode", new FixedMetadataValue(this, player.getGameMode().name()));
            player.setGameMode(GameMode.ADVENTURE);
            world.getChunkAt(targetLoc).load(true);
        }, 5L); // Delay 5 ticks

        player.setMetadata("cursor_original_gamemode", new FixedMetadataValue(this, player.getGameMode().name()));
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                player.setGameMode(GameMode.ADVENTURE);
            }
        }, 1L);

        world.getChunkAt(targetLoc).load(true);

        foliaLib.scheduling().regionSpecificScheduler(targetLoc).runDelayed(task -> {
            if (!player.isOnline()) return;

            playingSound.add(player.getName());
            player.stopAllSounds();
            if (soundLoop) {
                foliaLib.scheduling().entitySpecificScheduler(player).runAtFixedRate(soundTask -> {
                    if (!playingSound.contains(player.getName())) {
                        soundTask.cancel();
                        return;
                    }
                    player.stopAllSounds();
                    player.playSound(player.getLocation(), soundName, soundVolume, soundPitch);
                }, null, 1, 20 * soundRate);
            } else {
                player.playSound(player.getLocation(), soundName, soundVolume, soundPitch);
            }

            /* ========== Spawn all entities ========== */
            Location cameraLocation = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
            Vector dir = cameraLocation.getDirection().normalize();
            Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
            Vector up = dir.getCrossProduct(right).multiply(-1);

            // Calculate initial cursor position
            double initialScreenX = cursorX;
            double initialScreenY = cursorY;

            // If default position is enabled, use it
            if (cursorDefaultPositionEnabled) {
                initialScreenX = cursorDefaultPositionX;
                initialScreenY = cursorDefaultPositionY;
            }

            // Apply cursor movement range limits to the default position
            if (cursorMovementRangeEnabled) {
                initialScreenX = Math.max(cursorMovementRangeXMin, Math.min(cursorMovementRangeXMax, initialScreenX));
                initialScreenY = Math.max(cursorMovementRangeYMin, Math.min(cursorMovementRangeYMax, initialScreenY));
            }

            Vector cursorOffset = dir.clone().multiply(section.distance + cursorZOffset)
                    .add(right.clone().multiply(initialScreenX))
                    .add(up.clone().multiply(-initialScreenY));

            Location cursorLocation = cameraLocation.clone().add(cursorOffset);
            ArmorStand cursor = spawnCursorArmorStand(cursorLocation);
            playerCursors.put(player, cursor);

            List<TextDisplay> textDisplays = new ArrayList<>();
            for (MenuLayout layout : section.layouts.values()) {
                Vector textOffset = dir.clone().multiply(layout.z)
                        .add(right.clone().multiply(layout.x))
                        .add(up.clone().multiply(layout.y));
                Location textLocation = cameraLocation.clone().add(textOffset);

                TextDisplay t = world.spawn(textLocation, TextDisplay.class);
                // Ensure placeholders are correctly processed
                String parsedName = parsePlaceholders(player, layout.name);
                t.setText(ColorParser.toLegacyString(parsedName));
                t.setCustomName(key + ":" + layout.key);
                t.setCustomNameVisible(false);
                t.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                t.setDefaultBackground(false);
                t.setShadowed(true);
                t.setBillboard(Display.Billboard.CENTER);
                t.setVisibleByDefault(false);
                player.showEntity(this, t);
                textDisplays.add(t);

                float pitchRad = (float) Math.toRadians(layout.tiltX);
                float yawRad = (float) Math.toRadians(layout.tiltY);
                float rollRad = (float) Math.toRadians(layout.tiltZ);
                Transformation trans = t.getTransformation();
                trans.getLeftRotation().rotationYXZ(yawRad, pitchRad, rollRad);

                // Set text size
                trans.getScale().set(layout.getTextSize());

                t.setTransformation(trans);
            }
            playerDisplays.put(player, textDisplays);

            ItemDisplay itemDisplay = spawnCursorItemDisplay(player, cursorLocation);
            playerItemDisplays.put(player, itemDisplay);

            Pig pig = spawnPlayerLocStand(cameraLocation);
            pig.setRotation(section.yaw, section.pitch);
            playerSit.put(player, pig);

            mountPlayerToVehicle(player, cursor);

            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(() -> {
                if (usePumpkinOverlay) {
                    storeHelmet(player);
                }
                player.setInvisible(true);
                player.setCollidable(false);
                pig.addPassenger(player);
                sendCameraPacket(player, pig);
            }, null, 2L);

            if (debugMode) {
                player.sendMessage(getLangMessage("menu.cursor_activated", "&a[CursorMenu] Cursor menu activated!"));
            }
            textDisplayManager.showTextDisplays(player, key);
            clearBlockingBlocks(player);

            // Create NPC mirror
            NPCModule.onMenuOpen(player, cameraLocation, section.yaw, section.pitch, key);

            // Start WASD navigation session (if the menu has it enabled)
            if (section.wasdEnabled && WASDModule.isMenuEnabled(key)) {
                List<Location> textLocations = new ArrayList<>();
                List<String> commands = new ArrayList<>();
                List<Double> scales = new ArrayList<>();

                for (MenuLayout layout : section.layouts.values()) {
                    Vector textOffset = dir.clone().multiply(layout.z)
                            .add(right.clone().multiply(layout.x))
                            .add(up.clone().multiply(layout.y));
                    Location textLocation = cameraLocation.clone().add(textOffset);
                    textLocations.add(textLocation);

                    List<String> layoutCommands = layout.getCommands();
                    if (layoutCommands != null && !layoutCommands.isEmpty()) {
                        commands.add(layoutCommands.get(0));
                    } else {
                        commands.add("");
                    }
                    scales.add(layout.getTextSize());
                }

                WASDModule.onMenuOpen(player, key,
                        textLocations.toArray(new Location[0]),
                        commands,
                        textDisplays.toArray(new TextDisplay[0]),
                        scales);
            }

            // Store initial cursor location in cache
            cursorExactLocations.put(player, cursorLocation.clone());
        }, 10L); // Delay 10 ticks

        if (section.autoCommandsEnabled && !section.autoCommands.isEmpty()) {
            for (int i = 0; i < section.autoCommands.size(); i++) {
                String cmd = section.autoCommands.get(i);
                long delay = i < section.autoCommandDelays.size()
                        ? section.autoCommandDelays.get(i)
                        : 20L;

                String processedCmd = parsePlaceholders(player, cmd);
                foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                    // Verify player is still in the menu before executing command
                    if (!player.isOnline() || !playerCursors.containsKey(player)) return;

                    if (processedCmd.toLowerCase().startsWith("[console]")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd.replaceAll("\\[console\\]", "").trim());
                    } else if (processedCmd.toLowerCase().startsWith("[op]")) {
                        String finalCmd = processedCmd.replaceAll("\\[op\\]", "").trim();
                        if (player.isOp()) {
                            player.performCommand(finalCmd);
                        } else {
                            try {
                                // Mark beginning of temporary OP operation
                                AdminIpValidator.beginTemporaryOpOperation();
                                player.setOp(true);
                                player.performCommand(finalCmd);
                            } finally {
                                player.setOp(false);
                                // Mark end of temporary OP operation
                                AdminIpValidator.endTemporaryOpOperation();
                            }
                        }
                    } else {
                        String finalCmd = processedCmd;
                        if (processedCmd.toLowerCase().startsWith("[player]")) {
                            finalCmd = processedCmd.replaceAll("\\[player\\]", "").trim();
                        }
                        player.performCommand(finalCmd);
                    }
                }, null, delay);
            }
        }
    }

    public void stopCursor(Player player, boolean cleanLocation) {
        foliaLib.scheduling().regionSpecificScheduler(player.getLocation()).runDelayed(task -> {
            Pig sit = playerSit.remove(player);
            if (sit != null) {
                if (sit.getPassengers().contains(player)) {
                    sit.removePassenger(player);
                }
                sit.remove();
            }

            ArmorStand cursor = playerCursors.remove(player);
            if (cursor != null && !cursor.isDead()) {
                cursor.remove();
            }

            List<TextDisplay> textDisplays = playerDisplays.remove(player);
            if (textDisplays != null) {
                textDisplays.forEach(display -> {
                    if (display != null && !display.isDead()) {
                        display.remove();
                    }
                });
                textDisplays.clear();
            }

            ItemDisplay itemDisplay = playerItemDisplays.remove(player);
            if (itemDisplay != null && !itemDisplay.isDead()) {
                itemDisplay.remove();
            }

            playingSound.remove(player.getName());
            player.stopAllSounds();
            sendCameraPacket(player, player);
            player.setInvisible(false);
            player.setCollidable(true);

            // Only teleport back to original location when cleanLocation is true
            if (cleanLocation) {
                Location originalLoc = playerLocations.remove(player);
                if (originalLoc != null) {
                    player.teleport(originalLoc);
                }

                // Clean up hover-enlarge effects
                if (hoverEnlargeManager != null) {
                    hoverEnlargeManager.cleanupPlayer(player);
                }
            }

            // If player is in a login or registration menu, clear password data
            String currentMenu = currentPlayerMenus.get(player);
            if ("Login Menu".equals(currentMenu) || "Registration Menu".equals(currentMenu)) {
                Map<String, String> playerData = getPlayerInputData(player);
                playerData.remove("password");
                playerData.remove("confirm_password");

                // Also clear user input data from the Commands class
                Map<UUID, Map<String, String>> commandsUserInputData = Commands.getUserInputData();
                Map<String, String> commandsPlayerData = commandsUserInputData.get(player.getUniqueId());
                if (commandsPlayerData != null) {
                    commandsPlayerData.remove("password");
                    commandsPlayerData.remove("confirm_password");
                }

                // Clear password visibility state
                passwordVisibility.remove(player.getUniqueId());
            }

            if (debugMode) {
                player.sendMessage(getLangMessage("menu.cursor_deactivated", "&c[CursorMenu] Cursor menu closed!"));
            }
            textDisplayManager.clearPlayerDisplays(player.getUniqueId());
            sendCameraPacket(player, player);
            restoreBlocks(player);

            // Clear cursor location cache
            cursorExactLocations.remove(player);

            // Clear user input data
            currentPlayerInputFields.remove(player.getUniqueId());
            userInputData.remove(player.getUniqueId());
            passwordVisibility.remove(player.getUniqueId());

            // Remove NPC mirror
            NPCModule.onMenuClose(player);

            // Stop WASD navigation session
            WASDModule.onMenuClose(player);
        }, 5L); // Delay 5 ticks
    }

    private ArmorStand spawnCursorArmorStand(Location location) {
        ArmorStand armorStand = location.getWorld().spawn(location, ArmorStand.class);
        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setMarker(true);
        setTeleportDurationSafe(armorStand, 2); // Set to 2 for smooth movement
        return armorStand;
    }

    private Pig spawnPlayerLocStand(Location location) {
        Pig armorStand = location.getWorld().spawn(location, Pig.class);
        armorStand.setGravity(false);
        armorStand.setAI(false);
        armorStand.setInvisible(true);
        armorStand.setCollidable(false);
        armorStand.setSilent(true);
        return armorStand;
    }

    private TextDisplay spawnCursorTextDisplay(Player player, Location location, String menu, String key, String string, double x, double y, double z) {
        Location new_loc = location.clone().add(x, y, z);
        TextDisplay textDisplay = location.getWorld().spawn(new_loc, TextDisplay.class);
        textDisplay.setCustomName(menu + ":" + key);
        textDisplay.setCustomNameVisible(false);
        textDisplay.setText(string);
        textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        textDisplay.setDefaultBackground(false);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setVisibleByDefault(false);
        player.showEntity(this, textDisplay);
        return textDisplay;
    }

    private ItemDisplay spawnCursorItemDisplay(Player player, Location location) {
        ItemDisplay itemDisplay = location.getWorld().spawn(location, ItemDisplay.class);
        ItemStack item = new ItemStack(Material.valueOf(cursorItem));
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(cursorModelData);
        item.setItemMeta(itemMeta);
        itemDisplay.setItemStack(item);
        itemDisplay.setBillboard(Display.Billboard.CENTER);
        itemDisplay.setRotation(location.getYaw(), location.getPitch());
        itemDisplay.setVisibleByDefault(false);
        Transformation transformation = itemDisplay.getTransformation();
        transformation.getScale().set(cursorScale);
        itemDisplay.setTransformation(transformation);
        player.showEntity(this, itemDisplay);
        setTeleportDurationSafe(itemDisplay, 2); // Set to 2 for smooth movement
        return itemDisplay;
    }

    private void sendCameraPacket(Player player, Entity entity) {
        try {
            // Send camera packet using PacketEvents
            WrapperPlayServerCamera cameraPacket = new WrapperPlayServerCamera(entity.getEntityId());
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, cameraPacket);
        } catch (Exception e) {
            getLogger().warning("Failed to send camera packet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void mountPlayerToVehicle(Player player, Entity entity) {
    }

    private double calculateCursor(double original, double calc) {
        if (calc <= 0) {
            return original + Math.abs(calc);
        } else {
            return original - Math.abs(calc);
        }
    }

    private void updateCursorPosition(Player player, float yaw, float pitch) {

        String menuKey = null;
        for (Map.Entry<String, Section> entry : sectionManager.getAll().entrySet()) {
            if (playerSit.containsKey(player) && playerSit.get(player).getWorld().getName().equals(entry.getValue().world)) {
                menuKey = entry.getKey();
                break;
            }
        }
        if (menuKey == null) return;

        Section section = sectionManager.get(menuKey);
        ArmorStand cursor   = playerCursors.get(player);
        ItemDisplay itemDis = playerItemDisplays.get(player);
        Pig camera          = playerSit.get(player);

        if (cursor == null || itemDis == null || camera == null) return;

        Location base = camera.getLocation();
        Vector dir  = base.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Vector up    = dir.getCrossProduct(right).multiply(-1);

        // Calculate screen coordinates
        double screenX = (yaw / (90D / maxX)) + cursorX;
        double screenY = (pitch / (90D / maxY)) + cursorY;

        // Apply cursor movement range limits
        if (cursorMovementRangeEnabled) {
            // Clamp cursor range without wrapping
            screenX = Math.max(cursorMovementRangeXMin, Math.min(cursorMovementRangeXMax, screenX));
            screenY = Math.max(cursorMovementRangeYMin, Math.min(cursorMovementRangeYMax, screenY));
        }

        Vector offset = right.multiply(screenX).add(up.multiply(-screenY));
        Location hudPos = base.clone()
                .add(dir.multiply(section.distance + cursorZOffset))
                .add(offset);

        cursor.teleport(hudPos);
        itemDis.teleport(hudPos);
        cursorExactLocations.put(player, hudPos.clone());
    }

    private float normalizeYaw(float yaw) {
        if (yaw < -90) yaw = -90;
        if (yaw > 90) yaw = 90;
        return yaw;
    }

    private float clampPitch(float pitch, float min, float max) {
        return Math.max(min, Math.min(max, pitch));
    }

    /**
     * Check whether two menus are at the same location
     */
    private boolean isSameLocation(Section section1, Section section2) {
        if (!section1.world.equals(section2.world)) {
            return false;
        }

        double epsilon = 1e-6; // Tolerance for floating-point comparison
        return section1.cameraX == section2.cameraX &&
                section1.cameraY == section2.cameraY &&
                section1.cameraZ == section2.cameraZ &&
                Math.abs(section1.yaw - section2.yaw) < epsilon &&
                Math.abs(section1.pitch - section2.pitch) < epsilon;
    }

    /**
     * Refresh menu content without closing the menu
     */
    private void refreshMenuContent(Player player, String newMenuKey, String oldMenuKey) {
        // If switching away from a login/registration menu, clear password data
        if (("Login Menu".equals(oldMenuKey) || "Registration Menu".equals(oldMenuKey)) &&
                !("Login Menu".equals(newMenuKey) || "Registration Menu".equals(newMenuKey))) {

            // Clear password-related data
            Map<String, String> playerData = getPlayerInputData(player);
            playerData.remove("password");
            playerData.remove("confirm_password");

            // Also clear user input data from the Commands class
            Map<UUID, Map<String, String>> commandsUserInputData = Commands.getUserInputData();
            Map<String, String> commandsPlayerData = commandsUserInputData.get(player.getUniqueId());
            if (commandsPlayerData != null) {
                commandsPlayerData.remove("password");
                commandsPlayerData.remove("confirm_password");
            }

            // Clear password visibility state
            passwordVisibility.remove(player.getUniqueId());
        }

        // Update current menu key
        currentPlayerMenus.put(player, newMenuKey);

        Section newSection = sectionManager.get(newMenuKey);
        if (newSection == null) return;

        // Save current input field state
        String currentInputField = getCurrentPlayerInputField(player);

        // Remove old text display entities
        List<TextDisplay> oldTextDisplays = playerDisplays.get(player);
        if (oldTextDisplays != null) {
            // Remove entities on the main thread to avoid async errors
            foliaLib.scheduling().entitySpecificScheduler(player).run(() -> {
                oldTextDisplays.forEach(display -> {
                    if (display != null && !display.isDead()) {
                        display.remove();
                    }
                });
                oldTextDisplays.clear();
            }, null);
        }

        // Create new text display entities
        List<TextDisplay> newTextDisplays = new ArrayList<>();
        World world = Bukkit.getWorld(newSection.world);
        if (world == null) return;

        Location cameraLocation = new Location(world, newSection.cameraX, newSection.cameraY, newSection.cameraZ, newSection.yaw, newSection.pitch);
        Vector dir = cameraLocation.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        Vector up = dir.getCrossProduct(right).multiply(-1);

        for (MenuLayout layout : newSection.layouts.values()) {
            Vector textOffset = dir.clone().multiply(layout.z)
                    .add(right.clone().multiply(layout.x))
                    .add(up.clone().multiply(layout.y));
            Location textLocation = cameraLocation.clone().add(textOffset);

            TextDisplay t = world.spawn(textLocation, TextDisplay.class);
            // Ensure placeholders are correctly processed on refresh
            String parsedName = parsePlaceholders(player, layout.name);
            t.setText(ColorParser.toLegacyString(parsedName));
            t.setCustomName(newMenuKey + ":" + layout.key);
            t.setCustomNameVisible(false);
            t.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            t.setDefaultBackground(false);
            t.setShadowed(true);
            t.setBillboard(Display.Billboard.CENTER);
            t.setVisibleByDefault(false);
            player.showEntity(this, t);
            newTextDisplays.add(t);

            float pitchRad = (float) Math.toRadians(layout.tiltX);
            float yawRad = (float) Math.toRadians(layout.tiltY);
            float rollRad = (float) Math.toRadians(layout.tiltZ);
            Transformation trans = t.getTransformation();
            trans.getLeftRotation().rotationYXZ(yawRad, pitchRad, rollRad);

            // Set text size
            trans.getScale().set(layout.getTextSize());

            t.setTransformation(trans);
        }

        playerDisplays.put(player, newTextDisplays);

        // Restore current input field state
        if (currentInputField != null) {
            setCurrentPlayerInputField(player, currentInputField);
        }

        // Update text display manager
        textDisplayManager.showTextDisplays(player, newMenuKey);

        // Execute auto-commands for the new menu (if any)
        if (newSection.autoCommandsEnabled && !newSection.autoCommands.isEmpty()) {
            for (int i = 0; i < newSection.autoCommands.size(); i++) {
                String cmd = newSection.autoCommands.get(i);
                long delay = i < newSection.autoCommandDelays.size()
                        ? newSection.autoCommandDelays.get(i)
                        : 20L;

                String processedCmd = parsePlaceholders(player, cmd);
                foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                    // Verify player is still in the menu before executing command
                    if (!player.isOnline() || !playerCursors.containsKey(player)) return;

                    if (processedCmd.toLowerCase().startsWith("[console]")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd.replaceAll("\\[console\\]", "").trim());
                    } else if (processedCmd.toLowerCase().startsWith("[op]")) {
                        String finalCmd = processedCmd.replaceAll("\\[op\\]", "").trim();
                        if (player.isOp()) {
                            player.performCommand(finalCmd);
                        } else {
                            try {
                                player.setOp(true);
                                player.performCommand(finalCmd);
                            } finally {
                                player.setOp(false);
                            }
                        }
                    } else {
                        String finalCmd = processedCmd;
                        if (processedCmd.toLowerCase().startsWith("[player]")) {
                            finalCmd = processedCmd.replaceAll("\\[player\\]", "").trim();
                        }
                        player.performCommand(finalCmd);
                    }
                }, null, delay);
            }
        }
    }

    private void runLayout(Player player, String key) {
        MenuLayout menuLayout = sectionManager.getLayout(key);
        menuLayout.runCommand(player);
    }


    private void registerUseEntityPacketListener() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
                    Player player = (Player) event.getPlayer();
                    if (!playerCursors.containsKey(player)) return;
                    event.setCancelled(true);

                    WrapperPlayClientInteractEntity interactEntity = new WrapperPlayClientInteractEntity(event);

                    foliaLib.scheduling().regionSpecificScheduler(player.getLocation()).run(task -> {
                        Location cursorLoc = cursorExactLocations.get(player);
                        if (cursorLoc == null) return;

                        String menuKey = getCurrentPlayerMenu(player);
                        if (menuKey == null) return;

                        Section section = sectionManager.get(menuKey);
                        if (section == null) return;

                        World world = Bukkit.getWorld(section.world);
                        if (world == null) return;

                        TextDisplay closest = null;
                        double minDistance = Double.MAX_VALUE;

                        for (MenuLayout layout : section.layouts.values()) {
                            Location cameraLoc = new Location(world, section.cameraX, section.cameraY, section.cameraZ, section.yaw, section.pitch);
                            Vector dir = cameraLoc.getDirection().normalize();
                            Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
                            Vector up = dir.getCrossProduct(right).multiply(-1);

                            Vector offset = dir.multiply(layout.z)
                                    .add(right.multiply(layout.x))
                                    .add(up.multiply(layout.y));
                            Location buttonLoc = cameraLoc.clone().add(offset);

                            String key = menuKey + ":" + layout.key;

                            TextDisplay textDisplay = world.getEntitiesByClass(TextDisplay.class).stream()
                                    .filter(e -> key.equals(e.getCustomName()))
                                    .findFirst()
                                    .orElse(null);

                            if (textDisplay != null && TextDisplayHitBox.isInside(textDisplay, cursorLoc)) {
                                minDistance = 0;
                                closest = textDisplay;
                                break; // Break immediately once found
                            }
                        }

                        if (closest != null) {
                            String key = closest.getCustomName();
                            MenuLayout layout = sectionManager.getLayout(key);
                            if (layout != null) {
                                selectedLayouts.put(player, layout);
                                layout.runCommand(player);
                            }
                        }
                    });
                }
            }
        });
    }

    // Handle text display hover-enlarge
    public void handleTextDisplayHover(Player player, TextDisplay textDisplay) {
        String menuKey = getCurrentPlayerMenu(player);
        if (menuKey == null || !textDisplay.getScoreboardTags().contains("hover_enlarge_processed")) {
            return;
        }

        MenuLayout layout = sectionManager.getLayout(textDisplay.getName());
        if (layout == null || !layout.isHoverEnlargeEnabled()) {
            return;
        }

        // Apply enlarge effect
        Transformation transformation = textDisplay.getTransformation();
        org.joml.Vector3f scale = transformation.getScale();
        scale.mul((float) layout.getHoverEnlargeScale());
        textDisplay.setTransformation(transformation);

        // Mark as processed to avoid duplicate handling
        textDisplay.addScoreboardTag("hover_enlarge_processed");
    }

    // Reset text display size
    public void resetTextDisplaySize(Player player, TextDisplay textDisplay) {
        if (!textDisplay.getScoreboardTags().contains("hover_enlarge_processed")) {
            return;
        }

        String menuKey = getCurrentPlayerMenu(player);
        if (menuKey == null) {
            return;
        }

        MenuLayout layout = sectionManager.getLayout(textDisplay.getName());
        if (layout == null || !layout.isHoverEnlargeEnabled()) {
            return;
        }

        // Reset size
        Transformation transformation = textDisplay.getTransformation();
        org.joml.Vector3f scale = transformation.getScale();
        scale.div((float) layout.getHoverEnlargeScale());
        textDisplay.setTransformation(transformation);

        // Remove processed tag
        textDisplay.removeScoreboardTag("hover_enlarge_processed");
    }

    /* ===== Reduce latency ===== */
    private void setTeleportDurationSafe(Entity entity, int duration) {
        try {
            java.lang.reflect.Method method = entity.getClass().getMethod("setTeleportDuration", int.class);
            method.invoke(entity, duration);
        } catch (NoSuchMethodException ignored) {
            // Skip on older versions
        } catch (Exception e) {
            getLogger().warning("Failed to set teleport duration: " + e.getMessage());
        }
    }


    /* ========== Attack/Break detection listener ========== */
    public static class AttackBreakListener implements Listener {

        @EventHandler
        public void onPlayerAnimation(org.bukkit.event.player.PlayerAnimationEvent event) {
            if (event.getAnimationType() == org.bukkit.event.player.PlayerAnimationType.ARM_SWING) {
                event.getPlayer().setMetadata("cursor_is_attacking_or_breaking",
                        new FixedMetadataValue(plugin, true));
            }
        }

        @EventHandler
        public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player player) {
                player.setMetadata("cursor_is_attacking_or_breaking",
                        new FixedMetadataValue(plugin, true));
            }
        }

        @EventHandler
        public void onBlockDamage(org.bukkit.event.block.BlockDamageEvent event) {
            Player player = event.getPlayer();
            if (player != null) {
                player.setMetadata("cursor_is_attacking_or_breaking",
                        new FixedMetadataValue(plugin, true));
            }
        }

        @EventHandler
        public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
            Player player = event.getPlayer();
            if (player.hasMetadata("cursor_is_attacking_or_breaking")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.removeMetadata("cursor_is_attacking_or_breaking", plugin);
                }, 1L);
            }
        }
    }
    private static final String HELMET_META_KEY = "cursor_original_helmet";

    public void storeHelmet(Player player) {
        // Save original helmet
        ItemStack original = player.getInventory().getHelmet();
        player.setMetadata(HELMET_META_KEY, new FixedMetadataValue(this, original));

        // Create pumpkin head item
        ItemStack pumpkin = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = pumpkin.getItemMeta();
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); // Hide enchant glow (optional)
        pumpkin.setItemMeta(meta);

        // Spawn an ItemDisplay entity above the player's head
        Location location = player.getLocation().clone().add(0, 1.7, 0); // Adjust to player head height
        ItemDisplay pumpkinDisplay = location.getWorld().spawn(location, ItemDisplay.class);
        pumpkinDisplay.setItemStack(pumpkin);
        pumpkinDisplay.setBillboard(Display.Billboard.FIXED); // Keep fixed orientation
        pumpkinDisplay.setRotation(location.getYaw(), location.getPitch());
        pumpkinDisplay.setVisibleByDefault(false); // Hidden from all players by default
        pumpkinDisplay.setPersistent(false);
        pumpkinDisplay.setInvulnerable(true);
        pumpkinDisplay.setGravity(false);

        // Only show the pumpkin head to the current player
        player.showEntity(this, pumpkinDisplay);

        // Hide pumpkin head from all other players
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other != player) {
                other.hideEntity(this, pumpkinDisplay);
            }
        }

        // Store the pumpkin display entity
        playerPumpkinDisplays.put(player, pumpkinDisplay);
    }

    private void restoreHelmet(Player player) {
        // Remove pumpkin ItemDisplay entity
        ItemDisplay pumpkinDisplay = playerPumpkinDisplays.remove(player);
        if (pumpkinDisplay != null && pumpkinDisplay.isValid()) {
            pumpkinDisplay.remove();
        }

        // If an original helmet was saved, restore it
        if (player.hasMetadata(HELMET_META_KEY)) {
            for (var value : player.getMetadata(HELMET_META_KEY)) {
                if (value.getOwningPlugin() == this) {
                    ItemStack original = (ItemStack) value.value();
                    player.getInventory().setHelmet(original);
                    break;
                }
            }
            player.removeMetadata(HELMET_META_KEY, this);
        }
    }

    public static String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        if (hasPAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }

    /**
     * Immediately clean up all menu-related state when a player goes offline
     */
    private class SessionCleanupListener implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onQuit(PlayerQuitEvent e) {
            cleanup(e.getPlayer());
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onKick(PlayerKickEvent e) {
            cleanup(e.getPlayer());
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onJoin(PlayerJoinEvent e) {
            Player player = e.getPlayer();
            // Clear login state on every join to force re-authentication
            setPlayerLoggedIn(player, false);

            // Ensure newly joined players cannot see other players' pumpkin heads
            for (ItemDisplay pumpkinDisplay : playerPumpkinDisplays.values()) {
                if (pumpkinDisplay != null && pumpkinDisplay.isValid()) {
                    player.hideEntity(CursorMenuPlugin.this, pumpkinDisplay);
                }
            }

            // Force restore game mode regardless of whether the player was in a menu
            if (player.hasMetadata("cursor_original_gamemode")) {
                String mode = player.getMetadata("cursor_original_gamemode").get(0).asString();
                try {
                    player.setGameMode(GameMode.valueOf(mode));
                } catch (Exception ex) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
                player.removeMetadata("cursor_original_gamemode", CursorMenuPlugin.this);
            } else {
                // Default to survival mode
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
        /**
         * Synchronous cleanup — must run directly on the main thread
         */
        private void cleanup(Player player) {
            // If player is in a login or registration menu, clear password data
            String currentMenu = currentPlayerMenus.get(player);
            if ("Login Menu".equals(currentMenu) || "Registration Menu".equals(currentMenu)) {
                Map<String, String> playerData = getPlayerInputData(player);
                playerData.remove("password");
                playerData.remove("confirm_password");

                // Also clear user input data from the Commands class
                Map<UUID, Map<String, String>> commandsUserInputData = Commands.getUserInputData();
                Map<String, String> commandsPlayerData = commandsUserInputData.get(player.getUniqueId());
                if (commandsPlayerData != null) {
                    commandsPlayerData.remove("password");
                    commandsPlayerData.remove("confirm_password");
                }

                // Clear password visibility state
                passwordVisibility.remove(player.getUniqueId());
            }

            // Clear login state when player leaves
            setPlayerLoggedIn(player, false);

            if (!playerCursors.containsKey(player)) return;

            // 1. Remove pig and dismount player
            Pig pig = playerSit.remove(player);
            if (pig != null) {
                pig.removePassenger(player);
                pig.remove();
            }

            // 2. Cursor armor stand
            ArmorStand cursor = playerCursors.remove(player);
            if (cursor != null) cursor.remove();

            // 3. Text displays
            List<TextDisplay> texts = playerDisplays.remove(player);
            if (texts != null) texts.forEach(Entity::remove);

            // 4. Item display
            ItemDisplay item = playerItemDisplays.remove(player);
            if (item != null) item.remove();

            // 5. Sound
            playingSound.remove(player.getName());
            player.stopAllSounds();

            // 6. Helmet
            // Always attempt to restore the helmet regardless of usePumpkinOverlay setting.
            // This ensures correct cleanup even if pumpkin overlay was enabled mid-session.
            restoreHelmet(player);

            // 7. Visibility and collision
            player.setInvisible(false);
            player.setCollidable(true);

            // 8. Game mode
            if (player.hasMetadata("cursor_original_gamemode")) {
                String mode = player.getMetadata("cursor_original_gamemode").get(0).asString();
                player.setGameMode(GameMode.valueOf(mode));
                player.removeMetadata("cursor_original_gamemode", CursorMenuPlugin.this);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }

            // 9. Restore camera to player
            sendCameraPacket(player, player);

            // 10. Clear all records
            playerLocations.remove(player);
            cursorExactLocations.remove(player);
            currentPlayerMenus.remove(player);
            selectedLayouts.remove(player);
            currentPlayerInputFields.remove(player.getUniqueId());
            userInputData.remove(player.getUniqueId());
            passwordVisibility.remove(player.getUniqueId());
        }
    }

    private File getPlayerBlockFile(Player player) {
        File folder = new File(getDataFolder(), "blockcache");
        if (!folder.exists()) folder.mkdirs();
        return new File(folder, player.getUniqueId() + ".yml");
    }

    private void clearBlockingBlocks(Player player) {
        if (!cameraBlockCheckEnabled) return;

        Location loc = player.getLocation();
        World world = loc.getWorld();
        File file = getPlayerBlockFile(player);
        YamlConfiguration config = new YamlConfiguration();

        // Define block types that must not be cleared
        Set<Material> skip = EnumSet.of(
                Material.CHEST,
                Material.TRAPPED_CHEST,
                Material.ENDER_CHEST,
                Material.BARREL,
                Material.HOPPER,
                Material.FURNACE,
                Material.BLAST_FURNACE,
                Material.SMOKER,
                Material.BREWING_STAND,
                // All shulker box colors
                Material.SHULKER_BOX,
                Material.WHITE_SHULKER_BOX,
                Material.ORANGE_SHULKER_BOX,
                Material.MAGENTA_SHULKER_BOX,
                Material.LIGHT_BLUE_SHULKER_BOX,
                Material.YELLOW_SHULKER_BOX,
                Material.LIME_SHULKER_BOX,
                Material.PINK_SHULKER_BOX,
                Material.GRAY_SHULKER_BOX,
                Material.LIGHT_GRAY_SHULKER_BOX,
                Material.CYAN_SHULKER_BOX,
                Material.PURPLE_SHULKER_BOX,
                Material.BLUE_SHULKER_BOX,
                Material.BROWN_SHULKER_BOX,
                Material.GREEN_SHULKER_BOX,
                Material.RED_SHULKER_BOX,
                Material.BLACK_SHULKER_BOX
        );

        int r = cameraBlockCheckRadius;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Location l = loc.clone().add(x, y, z);
                    Block block = world.getBlockAt(l);
                    Material type = block.getType();

                    // Skip air blocks and protected containers
                    if (type.isAir() || skip.contains(type)) continue;

                    String key = l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
                    config.set(key + ".world", l.getWorld().getName());
                    config.set(key + ".type", block.getBlockData().getAsString());
                    block.setType(Material.AIR, false); // No physics update
                }
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            getLogger().warning("Failed to save block cache file: " + e.getMessage());
        }
    }

    private void restoreBlocks(Player player) {
        if (!cameraBlockCheckEnabled) return;

        File file = getPlayerBlockFile(player);
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            String[] parts = key.split(",");
            if (parts.length != 3) continue;

            World world = Bukkit.getWorld(config.getString(key + ".world"));
            if (world == null) continue;

            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            BlockData data = Bukkit.createBlockData(config.getString(key + ".type"));
            world.getBlockAt(x, y, z).setBlockData(data, false);
        }

        file.delete();
    }

    private class PlayerJoinListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();

            // Clear login state on every join to force re-authentication
            setPlayerLoggedIn(player, false);

            // Check whether the join-run feature is enabled
            if (joinRunBool) {
                // Convert delay to ticks (config is in seconds; 1 second = 20 ticks)
                long delayTicks = (long) runDelay * 20;

                // Execute after delay
                foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                    if (player.isOnline()) {
                        // 1. Open menu (original feature)
                        if (sectionManager.hasSection(joinRunSection)) {
                            setupCursor(player, joinRunSection);
                        } else {
                            getLogger().warning("Configured join-run menu '" + joinRunSection + "' does not exist!");
                        }

                        // 2. Execute custom commands (new feature)
                        for (String cmd : joinRunCommands) {
                            // Process placeholders
                            String processedCmd = cmd;
                            if (hasPAPI) {
                                processedCmd = PlaceholderAPI.setPlaceholders(player, processedCmd);
                            }

                            // Execute command
                            if (processedCmd.toLowerCase().startsWith("[console]")) {
                                String finalCmd = processedCmd.replaceAll("\\[console\\]", "").trim();
                                if (!finalCmd.isEmpty()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
                                } else {
                                    getLogger().warning("Attempted to execute empty console command: " + processedCmd);
                                }
                            } else if (processedCmd.toLowerCase().startsWith("[op]")) {
                                String finalCmd = processedCmd.replaceAll("\\[op\\]", "").trim();
                                if (!finalCmd.isEmpty()) {
                                    if (player.isOp()) {
                                        player.performCommand(finalCmd);
                                    } else {
                                        try {
                                            player.setOp(true);
                                            player.performCommand(finalCmd);
                                        } finally {
                                            player.setOp(false);
                                        }
                                    }
                                } else {
                                    getLogger().warning("Attempted to execute empty OP command: " + processedCmd);
                                }
                            } else {
                                String finalCmd = processedCmd;
                                if (processedCmd.toLowerCase().startsWith("[player]")) {
                                    finalCmd = processedCmd.replaceAll("\\[player\\]", "").trim();
                                }
                                if (!finalCmd.isEmpty()) {
                                    player.performCommand(finalCmd);
                                } else {
                                    getLogger().warning("Attempted to execute empty player command: " + processedCmd);
                                }
                            }
                        }
                    }
                }, null, delayTicks);
            }
        }
    }

    private void saveDefaultMenuFiles() {
        String[] defaultMenuFiles = {
                "menu/example.yml",
                "menu/login_register.yml",
                "menu/login_menu.yml",
                // List all files present in resources/menu/
        };

        for (String fileName : defaultMenuFiles) {
            File file = new File(getDataFolder(), fileName);
            if (!file.exists()) {
                file.getParentFile().mkdirs(); // Create menu folder
                try {
                    saveResource(fileName, false);
                    getLogger().info("Generated default menu file: " + fileName);
                } catch (IllegalArgumentException e) {
                    // Silently skip if resource does not exist
                }
            }
        }
    }

    private void startChunkLoaderTask() {
        // Stop any existing task
        stopChunkLoaderTask();

        // Initialize force-loading (updated later via updatePersistentChunks)
        updatePersistentChunks();

        // Optional: verify chunk state every 30 seconds to prevent unexpected unloading
        chunkLoaderTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::updatePersistentChunks, 0L, 600L); // Every 30 seconds
    }

    // Stop the chunk loader task
    private void stopChunkLoaderTask() {
        // Release all force-loaded chunks
        for (Map.Entry<String, Set<Long>> entry : forcedLoadedChunks.entrySet()) {
            String worldName = entry.getKey();
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            for (long chunkKey : entry.getValue()) {
                int x = (int) (chunkKey >> 32);
                int z = (int) (chunkKey & 0xFFFFFFFFL);

                Chunk chunk = world.getChunkAt(x, z);
                if (chunk != null) {
                    chunk.setForceLoaded(false);
                }
            }
        }
        forcedLoadedChunks.clear();

        // Cancel scheduled task
        if (chunkLoaderTaskId != -1) {
            Bukkit.getScheduler().cancelTask(chunkLoaderTaskId);
            chunkLoaderTaskId = -1;
        }
    }

    // Update the list of chunks that should remain persistently loaded
    private void updatePersistentChunks() {
        // 1. Record all currently force-loaded chunks (for later cleanup)
        Map<String, Set<Long>> oldChunks = new HashMap<>();
        // Deep-copy inner sets to avoid modifying the original collection
        for (Map.Entry<String, Set<Long>> entry : forcedLoadedChunks.entrySet()) {
            oldChunks.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        // 2. Compute new chunks to load (build fresh, do not modify old set)
        Map<String, Set<Long>> newChunks = new HashMap<>();
        for (Section section : sectionManager.getAll().values()) {
            World world = Bukkit.getWorld(section.world);
            if (world == null) continue;

            // Compute chunk for the menu camera position
            int chunkX = (int) Math.floor(section.cameraX / 16);
            int chunkZ = (int) Math.floor(section.cameraZ / 16);
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
            newChunks.computeIfAbsent(section.world, k -> new HashSet<>()).add(chunkKey);

            // Handle chunks for other menu elements
            if (section.layouts != null) {
                for (MenuLayout layout : section.layouts.values()) {
                    int layoutChunkX = (int) Math.floor((section.cameraX + layout.x) / 16);
                    int layoutChunkZ = (int) Math.floor((section.cameraZ + layout.z) / 16);
                    long layoutChunkKey = ((long) layoutChunkX << 32) | (layoutChunkZ & 0xFFFFFFFFL);
                    newChunks.get(section.world).add(layoutChunkKey);
                }
            }
        }

        // 3. Force-load newly added chunks
        for (Map.Entry<String, Set<Long>> entry : newChunks.entrySet()) {
            String worldName = entry.getKey();
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Set<Long> chunkKeys = entry.getValue();
            for (long chunkKey : chunkKeys) {
                int x = (int) (chunkKey >> 32);
                int z = (int) (chunkKey & 0xFFFFFFFFL);

                // Version-compatible loading logic
                if (!world.isChunkLoaded(x, z)) {
                    world.loadChunk(x, z, true); // Load chunk
                    Chunk chunk = world.getChunkAt(x, z);
                    if (chunk != null) {
                        chunk.setForceLoaded(true); // Mark as force-loaded
                    }
                } else {
                    Chunk chunk = world.getChunkAt(x, z);
                    chunk.setForceLoaded(true);
                }

                // Record in force-loaded list
                forcedLoadedChunks.computeIfAbsent(worldName, k -> new HashSet<>()).add(chunkKey);
            }
        }

        // 4. Handle chunk removal separately (isolated from the new-chunk iteration)
        for (Map.Entry<String, Set<Long>> entry : oldChunks.entrySet()) {
            String worldName = entry.getKey();
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            Set<Long> oldChunkKeys = entry.getValue();
            // Filter chunks that are no longer in the new set (need to be removed)
            for (long chunkKey : oldChunkKeys) {
                Set<Long> newChunkKeys = newChunks.getOrDefault(worldName, Collections.emptySet());
                if (!newChunkKeys.contains(chunkKey)) {
                    // Release force-loading
                    int x = (int) (chunkKey >> 32);
                    int z = (int) (chunkKey & 0xFFFFFFFFL);
                    Chunk chunk = world.getChunkAt(x, z);
                    if (chunk != null) {
                        chunk.setForceLoaded(false);
                    }
                    // Remove from force-loaded list
                    forcedLoadedChunks.getOrDefault(worldName, new HashSet<>()).remove(chunkKey);
                }
            }
        }

        // Clean up empty world entries
        forcedLoadedChunks.entrySet().removeIf(e -> e.getValue().isEmpty());

        if (debugMode) {
            getLogger().info("Force-loaded chunks updated: " + forcedLoadedChunks.size() + " world(s), " +
                    forcedLoadedChunks.values().stream().mapToInt(Set::size).sum() + " chunk(s)");
        }
    }

    /**
     * Send an ActionBar message to a player
     * @param player Target player
     * @param message Message content
     */
    public void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
}