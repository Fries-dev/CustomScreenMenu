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
    private final String linkCommand; // 新增字段

    // PAPI条件相关字段
    private String conditionVariable;
    private String conditionOperator;
    private String conditionValue;

    // 随机命令相关字段
    private List<String> randomCommands;
    private List<Integer> randomChances;
    private boolean useRandomCommands;

    // 文本悬停放大相关字段
    private boolean hoverEnlargeEnabled;
    private double hoverEnlargeScale;
    
    // 文本大小相关字段
    private double textSize = 1.0;

    public MenuLayout(String key,String name, List<String> command, boolean stop, double x, double y, double z,boolean teleportBool,boolean tBack,Location teleportLoc, boolean stopCommandBool, List<String> stopCommands, float tiltX, float tiltY, float tiltZ, String permission, boolean nextMenuEnabled, String nextMenuKey) {
        this(key, name, command, stop, x, y, z, teleportBool, tBack, teleportLoc, stopCommandBool, stopCommands, tiltX, tiltY, tiltZ, permission, nextMenuEnabled, nextMenuKey, ""); // 调用新增的构造函数
    }

    // 新增构造函数以支持linkCommand字段
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
        this.linkCommand = linkCommand; // 新增字段初始化
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
     * 加载配置信息
     * @param config 配置节
     */
    public void loadConfig(ConfigurationSection config) {
        // 加载命令延迟
        if (config.contains("command-delay")) {
            long delay = config.getLong("command-delay", 20);
            setCommandDelay(delay);
        }

        // 加载PAPI条件
        if (config.contains("condition.variable") && config.contains("condition.operator") && config.contains("condition.value")) {
            this.conditionVariable = config.getString("condition.variable");
            this.conditionOperator = config.getString("condition.operator");
            this.conditionValue = config.getString("condition.value");
        }

        // 加载随机命令配置
        if (config.contains("random-commands") && config.contains("random-chances")) {
            this.randomCommands = config.getStringList("random-commands");
            this.randomChances = config.getIntegerList("random-chances");
            this.useRandomCommands = this.randomCommands.size() == this.randomChances.size() && !this.randomCommands.isEmpty();
        }

        // 加载悬停放大配置
        if (config.contains("hover-enlarge.enabled")) {
            this.hoverEnlargeEnabled = config.getBoolean("hover-enlarge.enabled");
            this.hoverEnlargeScale = config.getDouble("hover-enlarge.scale", 1.2);
        }
        
        // 加载文本大小配置
        if (config.contains("text-size")) {
            this.textSize = config.getDouble("text-size", 1.0);
        }
    }

    public void runCommand(Player player) {
        if (!permission.isEmpty() && !player.hasPermission(permission) && !player.isOp()) {
            player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("permission.no_permission_button", "&c[CursorMenu] 你没有权限执行该按钮"));
            return;
        }

        // 检查登录条件 - 只有已登录玩家才能点击需要登录的按钮
        if (CursorMenuPlugin.plugin.requiresLogin(name, key) && !CursorMenuPlugin.plugin.isPlayerLoggedIn(player)) {
            CursorMenuPlugin.plugin.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("menu.require_login", "&c[CursorMenu] 请先登录后再执行此操作"));
            return;
        }

        // 检查注册条件 - 已注册玩家不能点击注册按钮
        if (CursorMenuPlugin.plugin.requiresNotRegistered(name, key) && CursorMenuPlugin.plugin.getUserDataManager().isUserRegistered(player.getName())) {
            CursorMenuPlugin.plugin.sendActionBarMessage(player, CursorMenuPlugin.plugin.getLangMessage("menu.already_registered", "&c[CursorMenu] 您已注册，请直接登录"));
            // 跳转到登录菜单
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                CursorMenuPlugin.plugin.setupCursor(player, "登录菜单");
            }, null, 5L);
            return;
        }

        // 检查PAPI条件
        if (conditionVariable != null && conditionOperator != null && conditionValue != null) {
            if (!checkCondition(player)) {
                player.sendMessage(CursorMenuPlugin.plugin.getLangMessage("menu.condition_not_met", "&c[CursorMenu] 条件不满足，无法执行该操作"));
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

        //plugin.stopCursor(player, false);

        CursorMenuPlugin.plugin.selectedLayouts.put(player, this);
        if (this.isClick) return;
        this.isClick = true;

        foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
            this.isClick = false;
        }, null, 1L);

        // 处理登录注册菜单的特殊输入逻辑
        // 但如果有 next-menu 配置，则优先执行跳转逻辑
        if (CursorMenuPlugin.plugin.getCurrentPlayerMenu(player) != null &&
                (CursorMenuPlugin.plugin.getCurrentPlayerMenu(player).equals("login_menu") ||
                        CursorMenuPlugin.plugin.getCurrentPlayerMenu(player).equals("register_menu") ||
                        CursorMenuPlugin.plugin.getCurrentPlayerMenu(player).equals("login")) &&
                !(nextMenuEnabled && !nextMenuKey.isEmpty())) {

            // 根据点击的按钮设置当前输入字段
            switch (this.key) {
                case "cancel_button":
                    // 清除用户输入数据并关闭菜单
                    CursorMenuPlugin.plugin.stopCursor(player, true);
                    break;
            }
        }

        // 如果配置了链接命令，则发送链接到聊天栏
        if (linkCommand != null && !linkCommand.isEmpty()) {
            // 发送可点击链接到聊天栏
            sendClickableLink(player, linkCommand);
        } else {
            // 如果配置了随机命令，则使用随机命令逻辑，否则使用默认命令逻辑
            if (useRandomCommands) {
                executeRandomCommands(player);
            } else {
                // 修改命令执行循环，添加延迟 {new}
                for (String cmd : command) {  // {new}
                    // 处理命令占位符 {new}
                    String processedCmd = cmd; // 不再替换 %player%
                    if (hasPAPI) {  // {new}
                        processedCmd = PlaceholderAPI.setPlaceholders(player, processedCmd);  // {new}
                    }  // {new}

                    // 使用延迟调度器执行命令 {new}
                    // 使用延迟调度器执行命令 {new}
                    String finalProcessedCmd = processedCmd;
                    if (useCommandDelay) {
                        foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                            // 检查玩家是否仍在菜单中再执行命令
                            if (CursorMenuPlugin.plugin.playerCursors.containsKey(player)) {
                                dispatchCommand(player, finalProcessedCmd);
                            }
                        }, null, commandDelay);
                    } else {
                        dispatchCommand(player, finalProcessedCmd);
                    }
                }  // {new}
            }
        }

        // ✅ 跳转菜单逻辑（移出 for 循环）
        if (nextMenuEnabled && !nextMenuKey.isEmpty()) {
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                CursorMenuPlugin.plugin.setupCursor(player, nextMenuKey);
            }, null, 5L);
            return;
        }

        // ✅ 如果没有跳转，才执行 stop
        if (stop) {
            foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                // 只有在启用了传送功能且需要传回原始位置时才设置cleanLocation为true
                boolean teleportBack = teleportBool && teleportOriginal;
                CursorMenuPlugin.plugin.stopCursor(player, teleportBack);
                this.teleport(player);
                this.runStopCommand(player);
            }, null, 2L);
        }


        foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
            this.isClick = false;
        }, null, 15L);

        // 使用插件记录器而不是System.out
        //plugin.getLogger().info("玩家 " + player.getName() + " 目前状态: " + player.isInsideVehicle());
    }

    // 发送可点击链接到聊天栏的新方法
    private void sendClickableLink(Player player, String linkCommand) {
        // 解析链接格式 [link]URL|显示文本
        if (linkCommand.startsWith("[link]")) {
            String linkPart = linkCommand.substring(6); // 移除 "[link]" 前缀
            String[] parts = linkPart.split("\\|", 2);
            
            String url = parts[0];
            String displayText = parts.length > 1 ? parts[1] : url;
            
            // 创建可点击的文本组件
            net.md_5.bungee.api.chat.TextComponent linkComponent = new net.md_5.bungee.api.chat.TextComponent(
                ChatColor.translateAlternateColorCodes('&', displayText)
            );
            linkComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url
            ));
            
            // 发送到聊天栏
            player.spigot().sendMessage(ChatMessageType.CHAT, linkComponent);
        } else {
            // 如果不是标准格式，按原文本处理
            String linkMessage = ChatColor.translateAlternateColorCodes('&', linkCommand);
            player.spigot().sendMessage(ChatMessageType.CHAT, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(linkMessage));
        }
    }

    // 执行随机命令的方法
    private void executeRandomCommands(Player player) {
        Random random = new Random();
        int totalChance = 0;

        // 计算总概率
        for (int chance : randomChances) {
            totalChance += chance;
        }

        // 如果总概率为0，则不执行任何命令
        if (totalChance <= 0) {
            return;
        }

        // 生成随机数
        int randomValue = random.nextInt(totalChance) + 1;
        int currentChance = 0;

        // 确定执行哪个命令
        for (int i = 0; i < randomCommands.size(); i++) {
            currentChance += randomChances.get(i);
            if (randomValue <= currentChance) {
                String cmd = randomCommands.get(i);
                // 处理命令占位符
                String processedCmd = cmd; // 不再替换 %player%
                if (hasPAPI) {
                    processedCmd = PlaceholderAPI.setPlaceholders(player, processedCmd);
                }

                // 使用延迟调度器执行命令
                String finalProcessedCmd = processedCmd;
                if (useCommandDelay) {
                    foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(task -> {
                        // 检查玩家是否仍在菜单中再执行命令
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
            // 应用退出摄像机的 yaw/pitch
            loc.setYaw(CursorMenuPlugin.exitYaw);
            loc.setPitch(CursorMenuPlugin.exitPitch);

            // 确保世界存在
            org.bukkit.World targetWorld = loc.getWorld();
            if (targetWorld != null) {
                player.teleport(loc);
            } else {
                // 如果世界不存在，使用玩家当前世界
                Location safeLoc = player.getLocation().clone();
                safeLoc.setYaw(CursorMenuPlugin.exitYaw);
                safeLoc.setPitch(CursorMenuPlugin.exitPitch);
                player.teleport(safeLoc);
            }
        } else {
            // ✅ 如果没有传送，仍然应用 exit-camera 的 yaw/pitch
            Location loc2 = player.getLocation();
            loc2.setYaw(CursorMenuPlugin.exitYaw);
            loc2.setPitch(CursorMenuPlugin.exitPitch);
            player.teleport(loc2);
        }
    }

    private void runStopCommand(Player player) {
        if(this.stopCommandBool) {
            for(String cmd : stopCommands) {
                CommandUtils.executeCommand(player, cmd);
            }
        }
    }

    private boolean checkCondition(Player player) {
        if (!hasPAPI) return true;

        // 获取PAPI变量的值
        String variableValue = PlaceholderAPI.setPlaceholders(player, conditionVariable);

        try {
            // 尝试将两个值都解析为数字进行数值比较
            double varValue = Double.parseDouble(variableValue);
            double condValue = Double.parseDouble(conditionValue);

            switch (conditionOperator) {
                case ">":
                    return varValue > condValue;
                case ">=":
                    return varValue >= condValue;
                case "<":
                    return varValue < condValue;
                case "<=":
                    return varValue <= condValue;
                case "==":
                    return varValue == condValue;
                case "!=":
                    return varValue != condValue;
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            // 如果无法解析为数字，则进行字符串比较
            switch (conditionOperator) {
                case "==":
                    return variableValue.equals(conditionValue);
                case "!=":
                    return !variableValue.equals(conditionValue);
                default:
                    return false;
            }
        }
    }

    // 检查按钮是否需要登录才能使用
    private boolean requiresLogin() {
        // 此方法已废弃，现在使用CursorMenuPlugin.requiresLogin()方法
        return false;
    }

    // 检查按钮是否为注册按钮且需要防止重复注册
    private boolean requiresNotRegistered() {
        // 此方法已废弃，现在使用CursorMenuPlugin.RequiresNotRegistered()方法
        return false;
    }
}