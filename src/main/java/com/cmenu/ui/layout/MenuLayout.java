package com.cmenu.ui.layout;

import com.cmenu.ui.CursorMenuPlugin;
import com.cmenu.ui.util.CommandUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

import static com.cmenu.ui.CursorMenuPlugin.foliaLib;
import static com.cmenu.ui.CursorMenuPlugin.hasPAPI;

public class MenuLayout {
    public final String key;
    public String name;
    private final List<String> command;
    private final boolean stop;
    public final double x, y, z;
    private boolean isClick = false;
    private final boolean teleportBool;
    private final boolean teleportOriginal;
    private final Location teleportLoc;
    private final boolean stopCommandBool;
    private final List<String> stopCommands;
    public final float tiltX, tiltY, tiltZ;
    private final String permission;
    private final boolean nextMenuEnabled;
    private final String nextMenuKey;
    private long commandDelay = 20;
    private boolean useCommandDelay = false;
    private final String linkCommand; // New field

    // PAPI condition fields
    private String conditionVariable;
    private String conditionOperator;
    private String conditionValue;

    // Random command fields
    private List<String> randomCommands;
    private List<Integer> randomChances;
    private boolean useRandomCommands;

    // Hover enlarge fields
    private boolean hoverEnlargeEnabled;
    private double hoverEnlargeScale;

    // Text size field
    private double textSize = 1.0;

    public MenuLayout(String key, String name, List<String> command, boolean stop, double x, double y, double z, boolean teleportBool, boolean tBack, Location teleportLoc, boolean stopCommandBool, List<String> stopCommands, float tiltX, float tiltY, float tiltZ, String permission, boolean nextMenuEnabled, String nextMenuKey) {
        this(key, name, command, stop, x, y, z, teleportBool, tBack, teleportLoc, stopCommandBool, stopCommands, tiltX, tiltY, tiltZ, permission, nextMenuEnabled, nextMenuKey, ""); // Delegates to the new constructor
    }

    // New constructor to support the linkCommand field
    public MenuLayout(String key, String name, List<String> command, boolean stop, double x, double y, double z,
                      boolean teleportBool, boolean tBack, Location teleportLoc, boolean stopCommandBool,
                      List<String> stopCommands, float tiltX, float tiltY, float tiltZ, String permission,
                      boolean nextMenuEnabled, String nextMenuKey, String linkCommand) {
        this.key = key;
        this.name = name;
        this.command = command;
        this.stop = stop;
        this.x = x;
        this.y = y;
        this.z = z;
        this.isClick = false;
        this.teleportBool = teleportBool;
        this.teleportLoc = teleportLoc;
        this.stopCommandBool = stopCommandBool;
        this.stopCommands = stopCommands;
        this.teleportOriginal = tBack;
        this.tiltX = tiltX;
        this.tiltY = tiltY;
        this.tiltZ = tiltZ;
        this.permission = permission;
        this.nextMenuEnabled = nextMenuEnabled;
        this.nextMenuKey = nextMenuKey;
        this.commandDelay = 20;
        this.linkCommand = linkCommand; // Initialize new field
    }

    public void setCommandDelay(long delay) {
        this.commandDelay = delay;
        this.useCommandDelay = delay > 0;
    }

    public boolean isHoverEnlargeEnabled() {
        return hoverEnlargeEnabled;
    }

    public double getHoverEnlargeScale() {
        return hoverEnlargeScale;
    }

    public double getTextSize() {
        return textSize;
    }

    public List<String> getCommands() {
        return command;
    }

    /**
     * Loads configuration settings.
     * @param config the configuration section
     */
    public void loadConfig(ConfigurationSection config) {
        // Load command delay
        if (config.contains("command-delay")) {
            long delay = config.getLong("command-delay", 20);
            setCommandDelay(delay);
        }

        // Load PAPI condition
        if (config.contains("condition.variable") && config.contains("condition.operator") && config.contains("condition.value")) {
            this.conditionVariable = config.getString("condition.variable");
            this.conditionOperator = config.getString("condition.operator");
            this.conditionValue = config.getString("condition.value");
        }

        // Load random command configuration
        if (config.contains("random-commands") && config.contains("random-chances")) {
            this.randomCommands = config.getStringList("random-commands");
            this.randomChances = config.getIntegerList("random-chances");
            this.useRandomCommands = this.randomCommands.size() == this.randomChances.size() && !this.randomCommands.isEmpty();
        }

        // Load hover enlarge configuration
        if (config.contains("hover-enlarge.enabled")) {
            this.hoverEnlargeEnabled = config.getBoolean("hover-enlarge.enabled");
            this.hoverEnlargeScale = config.getDouble("hover-enlarge.scale", 1.2);
        }

        // Load text size configuration
        if (config.contains("text-size")) {
            this.textSize = config.getDouble("text-size", 1.0);
        }
    }

    public void runCommand(Player player) {
        if (!permission.isEmpty() && !player.hasPermission(permission) && !player.isOp()) {
            player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission_button", "&c[CursorMenu] You do not have permission to use this button"));
            return;
        }

        // Check login requirement — only logged-in players can click buttons that require login
        if (CursorMenuPlugin.plugin.requiresLogin(name, key) && !CursorMenuPlugin.plugin.isPlayerLoggedIn(player)) {
            CursorMenuPlugin.plugin.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("menu.require_login", "&c[CursorMenu] Please log in before performing this action"));
            return;
        }

        // Check registration requirement — already-registered players cannot click the register button
        if (CursorMenuPlugin.plugin.requiresNotRegistered(name, key) && CursorMenuPlugin.plugin.getUserDataManager().isUserRegistered(player.getName())) {
            CursorMenuPlugin.plugin.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("menu.already_registered", "&c[CursorMenu] You are already registered, please log in directly"));
            // Redirect to the login menu
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                CursorMenuPlugin.plugin.setupCursor(player, "Login Menu");
            }, null, 5L);
            return;
        }

        // Check PAPI condition
        if (conditionVariable != null && conditionOperator != null && conditionValue != null) {
            if (!checkCondition(player)) {
                player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("menu.condition_not_met", "&c[CursorMenu] Condition not met, cannot perform this action"));
                return;
            }
        }

        if (stop && player.isInsideVehicle()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                vehicle.removePassenger(player);
                vehicle.remove();
            }
        }

        CursorMenuPlugin.plugin.selectedLayouts.put(player, this);
        if (this.isClick) return;
        this.isClick = true;

        foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
            this.isClick = false;
        }, null, 1L);

        // Handle special input logic for login/register menus.
        // If next-menu is configured, the redirect takes priority.
        if (CursorMenuPlugin.plugin.getCurrentPlayerMenu(player) != null &&
                (CursorMenuPlugin.plugin.getCurrentPlayerMenu(player).equals("login_menu") ||
                        CursorMenuPlugin.plugin.getCurrentPlayerMenu(player).equals("register_menu") ||
                        CursorMenuPlugin.plugin.getCurrentPlayerMenu(player).equals("login")) &&
                !(nextMenuEnabled && !nextMenuKey.isEmpty())) {

            // Set the current input field based on the button clicked
            switch (this.key) {
                case "cancel_button":
                    // Clear user input data and close the menu
                    CursorMenuPlugin.plugin.stopCursor(player, true);
                    break;
            }
        }

        // If a link command is configured, send the clickable link to the chat bar
        if (linkCommand != null && !linkCommand.isEmpty()) {
            sendClickableLink(player, linkCommand);
        } else {
            // Use random command logic if configured, otherwise use default command logic
            if (useRandomCommands) {
                executeRandomCommands(player);
            } else {
                // Execute commands with optional delay
                for (String cmd : command) {
                    // Process command placeholders
                    String processedCmd = cmd; // %player% is no longer replaced here
                    if (hasPAPI) {
                        processedCmd = PlaceholderAPI.setPlaceholders(player, processedCmd);
                    }

                    // Execute command using delayed scheduler
                    String finalProcessedCmd = processedCmd;
                    if (useCommandDelay) {
                        foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                            // Only execute if the player is still in a menu
                            if (CursorMenuPlugin.plugin.playerCursors.containsKey(player)) {
                                dispatchCommand(player, finalProcessedCmd);
                            }
                        }, null, commandDelay);
                    } else {
                        dispatchCommand(player, finalProcessedCmd);
                    }
                }
            }
        }

        // Menu redirect logic (outside the for loop)
        if (nextMenuEnabled && !nextMenuKey.isEmpty()) {
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                CursorMenuPlugin.plugin.setupCursor(player, nextMenuKey);
            }, null, 5L);
            return;
        }

        // Only execute stop if there is no redirect
        if (stop) {
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                // Only set cleanLocation=true if teleport is enabled and back-to-origin is required
                boolean teleportBack = teleportBool && teleportOriginal;
                CursorMenuPlugin.plugin.stopCursor(player, teleportBack);
                this.teleport(player);
                this.runStopCommand(player);
            }, null, 2L);
        }

        foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
            this.isClick = false;
        }, null, 15L);
    }

    // Sends a clickable link to the player's chat bar
    private void sendClickableLink(Player player, String linkCommand) {
        // Parse link format: [link]URL|display text
        if (linkCommand.startsWith("[link]")) {
            String linkPart = linkCommand.substring(6); // Remove the "[link]" prefix
            String[] parts = linkPart.split("\\|", 2);

            String url = parts[0];
            String displayText = parts.length > 1 ? parts[1] : url;

            // Create a clickable text component
            net.md_5.bungee.api.chat.TextComponent linkComponent = new net.md_5.bungee.api.chat.TextComponent(
                    ChatColor.translateAlternateColorCodes('&', displayText)
            );
            linkComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                    net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url
            ));

            // Send to chat bar
            player.spigot().sendMessage(ChatMessageType.CHAT, linkComponent);
        } else {
            // If not a standard format, treat as plain text
            String linkMessage = ChatColor.translateAlternateColorCodes('&', linkCommand);
            player.spigot().sendMessage(ChatMessageType.CHAT, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(linkMessage));
        }
    }

    // Executes a randomly selected command based on weighted chances
    private void executeRandomCommands(Player player) {
        Random random = new Random();
        int totalChance = 0;

        // Calculate total weight
        for (int chance : randomChances) {
            totalChance += chance;
        }

        // If total weight is zero, do nothing
        if (totalChance <= 0) {
            return;
        }

        // Generate a random number
        int randomValue = random.nextInt(totalChance) + 1;
        int currentChance = 0;

        // Determine which command to execute
        for (int i = 0; i < randomCommands.size(); i++) {
            currentChance += randomChances.get(i);
            if (randomValue <= currentChance) {
                String cmd = randomCommands.get(i);
                // Process command placeholders
                String processedCmd = cmd; // %player% is no longer replaced here
                if (hasPAPI) {
                    processedCmd = PlaceholderAPI.setPlaceholders(player, processedCmd);
                }

                // Execute command using delayed scheduler
                String finalProcessedCmd = processedCmd;
                if (useCommandDelay) {
                    foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                        // Only execute if the player is still in a menu
                        if (CursorMenuPlugin.plugin.playerCursors.containsKey(player)) {
                            dispatchCommand(player, finalProcessedCmd);
                        }
                    }, null, commandDelay);
                } else {
                    dispatchCommand(player, finalProcessedCmd);
                }
                break;
            }
        }
    }

    private void dispatchCommand(Player player, String cmd) {
        CommandUtils.executeCommand(player, cmd);
    }

    private void connectToServer(Player player, String serverName) {
        CommandUtils.connectToServer(player, serverName);
    }

    private void teleport(Player player) {
        if (!teleportBool) return;

        Location loc;
        if (teleportOriginal) {
            loc = CursorMenuPlugin.plugin.playerLocations.get(player);
        } else {
            loc = teleportLoc.clone();
        }

        if (loc != null && loc.getWorld() != null) {
            // Apply exit-camera yaw/pitch
            loc.setYaw(CursorMenuPlugin.exitYaw);
            loc.setPitch(CursorMenuPlugin.exitPitch);

            // Ensure the target world exists
            org.bukkit.World targetWorld = loc.getWorld();
            if (targetWorld != null) {
                player.teleport(loc);
            } else {
                // If the world doesn't exist, use the player's current world
                Location safeLoc = player.getLocation().clone();
                safeLoc.setYaw(CursorMenuPlugin.exitYaw);
                safeLoc.setPitch(CursorMenuPlugin.exitPitch);
                player.teleport(safeLoc);
            }
        } else {
            // If no teleport target, still apply exit-camera yaw/pitch
            Location loc2 = player.getLocation();
            loc2.setYaw(CursorMenuPlugin.exitYaw);
            loc2.setPitch(CursorMenuPlugin.exitPitch);
            player.teleport(loc2);
        }
    }

    private void runStopCommand(Player player) {
        if (this.stopCommandBool) {
            for (String cmd : stopCommands) {
                CommandUtils.executeCommand(player, cmd);
            }
        }
    }

    private boolean checkCondition(Player player) {
        if (!hasPAPI) return true;

        // Get the value of the PAPI variable
        String variableValue = PlaceholderAPI.setPlaceholders(player, conditionVariable);

        try {
            // Try parsing both values as numbers for numeric comparison
            double varValue = Double.parseDouble(variableValue);
            double condValue = Double.parseDouble(conditionValue);

            switch (conditionOperator) {
                case ">":  return varValue > condValue;
                case ">=": return varValue >= condValue;
                case "<":  return varValue < condValue;
                case "<=": return varValue <= condValue;
                case "==": return varValue == condValue;
                case "!=": return varValue != condValue;
                default:   return false;
            }
        } catch (NumberFormatException e) {
            // If numeric parsing fails, fall back to string comparison
            switch (conditionOperator) {
                case "==": return variableValue.equals(conditionValue);
                case "!=": return !variableValue.equals(conditionValue);
                default:   return false;
            }
        }
    }

    // Checks whether the button requires login to use
    private boolean requiresLogin() {
        // Deprecated — use CursorMenuPlugin.requiresLogin() instead
        return false;
    }

    // Checks whether the button is a register button that should block re-registration
    private boolean requiresNotRegistered() {
        // Deprecated — use CursorMenuPlugin.RequiresNotRegistered() instead
        return false;
    }
}