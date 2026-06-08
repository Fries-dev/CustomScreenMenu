package com.cmenu.ui;

import static com.cmenu.ui.CursorMenuPlugin.itemDisplayManager;

import com.cmenu.ui.npc.NPCMirrorHook;
import com.cmenu.ui.npc.NPCModule;
import com.cmenu.ui.section.Section;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.cmenu.ui.CursorMenuPlugin.plugin;
import static com.cmenu.ui.CursorMenuPlugin.sectionManager;

public class Commands implements CommandExecutor, TabExecutor {

    private static final Map<UUID, Map<String, String>> userInputData = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        switch (args.length) {

            case 0:
                sender.sendMessage(plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] Unknown argument"));
                return true;

            case 1:
                switch (args[0].toLowerCase()) {
                    case "run":
                        sender.sendMessage(plugin.getLangMessage("command.input_menu_hint", "&c[CursorMenu] Enter the menu option"));
                        return true;

                    case "stop":
                        if(sender instanceof Player) {
                            if (sender.hasPermission("cursormenu.stop")) {
                                plugin.stopCursor((Player) sender,true);
                            } else {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "reload":
                        if (sender.hasPermission("cursormenu.reload")) {
                            plugin.reloadPluginConfig();
                            sender.sendMessage(plugin.getLangMessage("command.reload", "&a[CursorMenu] Plugin reloaded..."));
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                        }
                        return true;

                    case "itemsstop":
                        if(sender instanceof Player) {
                            if (sender.hasPermission("cursormenu.itemsstop")) {
                                itemDisplayManager.hideItem((Player) sender);
                                sender.sendMessage(plugin.getLangMessage("command.item_disabled", "&a[CursorMenu] Item display disabled"));
                            } else {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "register_confirm":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.register_confirm")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;
                            UUID playerId = player.getUniqueId();

                            // 首先尝试从新系统获取数据
                            Map<String, String> userData = plugin.getPlayerInputData(player);
                            // 如果新系统没有数据，则尝试从旧系统获取
                            if (userData.isEmpty() ||
                                    !userData.containsKey("password") || !userData.containsKey("confirm_password")) {
                                Map<String, String> oldUserData = userInputData.get(playerId);
                                if (oldUserData != null) {
                                    userData = oldUserData;
                                }
                            }

                            if (userData.isEmpty() ||
                                    !userData.containsKey("password") || !userData.containsKey("confirm_password")) {
                                sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                                plugin.setupCursor(player, "login_menu");
                                return true;
                            }

                            // 自动生成用户名（使用玩家名称）
                            String username = player.getName();
                            String password = userData.get("password");
                            String confirmPassword = userData.get("confirm_password");
                            
                            // 添加密码空值检查
                            if (password == null || password.isEmpty()) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.empty_password", "&c[CursorMenu] 密码不能为空"));
                                plugin.setupCursor(player, "login_menu");
                                return true;
                            }
                            
                            if (confirmPassword == null || confirmPassword.isEmpty()) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.empty_confirm_password", "&c[CursorMenu] Confirm password cannot be empty"));
                                plugin.setupCursor(player, "login_menu");
                                return true;
                            }

                            // 检查用户是否已存在
                            if (plugin.getUserDataManager().isUserRegistered(username)) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.user_exists", "&c[CursorMenu] This account is already registered"));
                                plugin.setupCursor(player, "login");
                                return true;
                            }

                            if (!password.equals(confirmPassword)) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.password_mismatch", "&c[CursorMenu] Passwords do not match"));
                                plugin.setupCursor(player, "login_menu");
                                return true;
                            }

                            // 检查密码长度
                            if (password.length() < 6 || password.length() > 12) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.invalid_password_length", "&c[CursorMenu] Password length must be between 6 and 12 characters"));
                                plugin.setupCursor(player, "login_menu");
                                return true;
                            }

                            // 检查密码强度
                            if (isWeakPassword(password)) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.weak_password", "&c[CursorMenu] Password is too weak, please use a stronger password"));
                                plugin.setupCursor(player, "login_menu");
                                return true;
                            }

                            // 注册用户
                            UserDataManager.RegistrationResult result = plugin.getUserDataManager().registerUser(player, password);
                            if (result.isSuccess()) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.success", "&a[CursorMenu] User %player_name% registered successfully!")
                                        .replace("%player_name%", player.getName()));
                                
                                // 设置玩家为已登录状态
                                plugin.setPlayerLoggedIn(player, true);

                                // 清除临时数据
                                userInputData.remove(playerId);
                                // 清除新系统的数据
                                plugin.setPlayerInputData(player, "password", null);
                                plugin.setPlayerInputData(player, "confirm_password", null);

                                // 不再直接跳转，而是让 next-menu 处理跳转逻辑
                                // plugin.setupCursor(player, "register_success");
                            } else {
                                String errorMessage = getRegistrationErrorMessage(result.getMessageKey());
                                sendActionBarMessage(player, errorMessage);
                                plugin.setupCursor(player, "login_menu");
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "close":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.close")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;
                            // 清除用户的临时输入数据
                            userInputData.remove(player.getUniqueId());
                            // 清除新系统的数据
                            plugin.setPlayerInputData(player, "password", null);
                            plugin.setPlayerInputData(player, "confirm_password", null);
                            plugin.stopCursor(player, true);
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "check_registered":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.check_registered")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;

                            // 自动生成用户名（使用玩家名称）
                            String username = player.getName();

                            // 检查用户是否已注册
                            if (plugin.getUserDataManager().isUserRegistered(username)) {
                                // 如果已注册，跳转到登录菜单
                                sendActionBarMessage(player, plugin.getLangMessage("register.user_exists", "&c[CursorMenu] This account is already registered"));
                                plugin.setupCursor(player, "login");
                                return true;
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "check_and_register":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.check_and_register")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;
                            UUID playerId = player.getUniqueId();

                            // 首先尝试从新系统获取数据
                            Map<String, String> userData = plugin.getPlayerInputData(player);
                            // 如果新系统没有数据，则尝试从旧系统获取
                            if (userData.isEmpty() ||
                                    !userData.containsKey("password") || !userData.containsKey("confirm_password")) {
                                Map<String, String> oldUserData = userInputData.get(playerId);
                                if (oldUserData != null) {
                                    userData = oldUserData;
                                }
                            }

                            if (userData.isEmpty() ||
                                    !userData.containsKey("password") || !userData.containsKey("confirm_password")) {
                                sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                                return true;
                            }

                            String password = userData.get("password");
                            String confirmPassword = userData.get("confirm_password");

                            // 检查密码是否为空
                            if (password == null || confirmPassword == null) {
                                sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                                return true;
                            }

                            // 检查两次输入的密码是否一致
                            if (!password.equals(confirmPassword)) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.password_mismatch", "&c[CursorMenu] Passwords do not match"));
                                return true;
                            }

                            // 检查密码长度
                            if (password.length() < 6 || password.length() > 12) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.invalid_password_length", "&c[CursorMenu] Password length must be between 6 and 12 characters"));
                                return true;
                            }

                            // 检查密码强度
                            if (isWeakPassword(password)) {
                                sendActionBarMessage(player, plugin.getLangMessage("register.weak_password", "&c[CursorMenu] Password is too weak, please use a stronger password"));
                                return true;
                            }

                            // 如果所有验证通过，则执行注册命令
                            Bukkit.dispatchCommand(player, "cursormenu register_confirm");
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "check_password_before_login":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.check_password_before_login")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;
                            UUID playerId = player.getUniqueId();

                            // 获取用户输入数据
                            Map<String, String> userData = plugin.getPlayerInputData(player);
                            // 如果新系统没有数据，则尝试从旧系统获取
                            if (userData.isEmpty() ||
                                    !userData.containsKey("password")) {
                                Map<String, String> oldUserData = userInputData.get(playerId);
                                if (oldUserData != null) {
                                    userData = oldUserData;
                                }
                            }

                            if (userData.isEmpty() ||
                                    !userData.containsKey("password")) {
                                sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                                return true;
                            }

                            // 自动生成用户名（使用玩家名称）
                            String username = player.getName();
                            String password = userData.get("password");
                            
                            // 添加密码空值检查
                            if (password == null || password.isEmpty()) {
                                sendActionBarMessage(player, plugin.getLangMessage("login.empty_password", "&c[CursorMenu] 密码不能为空"));
                                return true;
                            }

                            // 验证用户凭据
                            UserDataManager.AuthenticationResult authResult = plugin.getUserDataManager().authenticateUser(player, password);
                            if (authResult.isSuccess()) {
                                sendActionBarMessage(player, plugin.getLangMessage("login.success", "&a[CursorMenu] Login successful!"));

                                // 设置玩家为已登录状态
                                plugin.setPlayerLoggedIn(player, true);

                                // 清除临时数据
                                userInputData.remove(playerId);
                                // 清除新系统的数据
                                plugin.setPlayerInputData(player, "password", null);

                                // 不再直接执行登录命令，而是让 next-menu 处理跳转逻辑
                                // 这样只有在密码验证成功后才会执行菜单跳转
                            } else {
                                String errorMessage = getLoginErrorMessage(authResult.getMessageKey());
                                sendActionBarMessage(player, errorMessage);
                                return true;
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "login":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.login")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;
                            UUID playerId = player.getUniqueId();

                            // 获取用户输入数据
                            Map<String, String> userData = plugin.getPlayerInputData(player);
                            // 如果新系统没有数据，则尝试从旧系统获取
                            if (userData.isEmpty() ||
                                    !userData.containsKey("password")) {
                                Map<String, String> oldUserData = userInputData.get(playerId);
                                if (oldUserData != null) {
                                    userData = oldUserData;
                                }
                            }

                            if (userData.isEmpty() ||
                                    !userData.containsKey("password")) {
                                sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                                return true;
                            }

                            // 自动生成用户名（使用玩家名称）
                            String username = player.getName();
                            String password = userData.get("password");
                            
                            // 添加密码空值检查
                            if (password == null || password.isEmpty()) {
                                sendActionBarMessage(player, plugin.getLangMessage("login.empty_password", "&c[CursorMenu] 密码不能为空"));
                                return true;
                            }

                            // 验证用户凭据
                            UserDataManager.AuthenticationResult authResult = plugin.getUserDataManager().authenticateUser(player, password);
                            if (authResult.isSuccess()) {
                                sendActionBarMessage(player, plugin.getLangMessage("login.success", "&a[CursorMenu] Login successful!"));

                                // 设置玩家为已登录状态
                                plugin.setPlayerLoggedIn(player, true);

                                // 清除临时数据
                                userInputData.remove(playerId);
                                // 清除新系统的数据
                                plugin.setPlayerInputData(player, "password", null);

                                // 不再直接跳转，而是让 next-menu 处理跳转逻辑
                                // plugin.setupCursor(player, "login_success");
                            } else {
                                String errorMessage = getLoginErrorMessage(authResult.getMessageKey());
                                sendActionBarMessage(player, errorMessage);
                                return true;
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "toggle_password_visibility":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.toggle_password_visibility")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;

                            // 切换密码可见性
                            plugin.togglePasswordVisibility(player);

                            // 更新菜单显示
                            String currentMenu = plugin.getCurrentPlayerMenu(player);
                            if (currentMenu != null && !currentMenu.isEmpty()) {
                                plugin.setupCursor(player, currentMenu);
                            }

                            // 获取切换后的密码可见性状态
                            boolean isVisible = plugin.getPasswordVisibility(player);
                            
                            // 根据新状态使用不同的语言键获取提示信息
                            String languageKey = isVisible ? "password.visibility_shown" : "password.visibility_hidden";
                            String defaultMessage = isVisible ? "&a[CursorMenu] Password is now visible" : "&a[CursorMenu] Password is now hidden";
                            
                            // 从语言配置中获取自定义消息，使用默认消息作为备选
                            String message = plugin.getLangMessage(languageKey, defaultMessage);
                            
                            // 发送提示信息到动作栏
                            sendActionBarMessage(player, message);
                            // 同时在聊天框中显示更明确的信息
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                            return true;
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "set_input_field":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.set_input_field")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;

                            // 设置当前输入字段
                            if (args.length > 1) {
                                String field = args[1];
                                plugin.setCurrentPlayerInputField(player, field);
                                sendActionBarMessage(player, plugin.getLangMessage("input.field_set", "&c[CursorMenu] Item ID does not exist: %item_id%").replace("%field%", field));
                            } else {
                                sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                            }
                            return true;
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "npc":
                        handleNPCCommand(sender, args);
                        return true;

                    default:
                        sender.sendMessage(plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] Unknown argument"));
                        return true;
                }

            case 2:
                switch (args[0].toLowerCase()) {
                    case "run":
                        if (sender.hasPermission("cursormenu.start")) {
                            Section section = sectionManager.get(args[1]);
                            if (section == null) {
                                sender.sendMessage(plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] Menu option not found"));
                                return true;
                            }
                            if (!section.permission.isEmpty()
                                    && !sender.hasPermission(section.permission)
                                    && !sender.isOp()) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission_menu", "&c[CursorMenu] You do not have permission to open this menu"));
                                return true;
                            }
                            if(sectionManager.has(args[1])){
                                if (sender instanceof Player) {
                                    plugin.setupCursor((Player) sender,args[1]);
                                } else {
                                    sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                                }
                            } else {
                                sender.sendMessage(plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] Menu option not found"));
                            }
                            return true;
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                            return true;
                        }

                    case "items":
                        if (sender instanceof Player) {
                            if (sender.hasPermission("cursormenu.items")) {
                                String id = args[1];
                                if (!itemDisplayManager.showItem((Player) sender, id)) {
                                    sender.sendMessage(plugin.getLangMessage("command.item_not_found", "&c[CursorMenu] Item ID does not exist: %item_id%").replace("%item_id%", id));
                                }
                            } else {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "set_input_field":
                        if (sender instanceof Player) {
                            if (!sender.hasPermission("cursormenu.set_input_field")) {
                                sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                                return true;
                            }
                            Player player = (Player) sender;

                            // 设置当前输入字段
                            String field = args[1];
                            plugin.setCurrentPlayerInputField(player, field);
                            sendActionBarMessage(player, plugin.getLangMessage("input.field_set", "&c[CursorMenu] Item ID does not exist: %item_id%").replace("%field%", field));
                            return true;
                        } else {
                            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] Players only"));
                        }
                        return true;

                    case "deleteuser":
                        if (!sender.hasPermission("cursormenu.deleteuser")) {
                            sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                            return true;
                        }

                        String targetPlayerName = args[1];

                        // 检查玩家是否存在于数据库
                        if (!plugin.getDatabaseManager().isUsernameExists(targetPlayerName)) {
                            sender.sendMessage(plugin.getLangMessage("player.not_registered", "&c[CursorMenu] Player %player% is not registered").replace("%player%", targetPlayerName));
                            return true;
                        }

                        // 调用数据库管理器删除用户
                        boolean success = plugin.getDatabaseManager().deleteUser(targetPlayerName);

                        if (success) {
                            sender.sendMessage(plugin.getLangMessage("player.delete_success", "&a[CursorMenu] Registration data for %player% has been deleted. The player can now register again.").replace("%player%", targetPlayerName));

                            // 如果玩家在线，强制其重新登录并清除其登录状态
                            Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
                            if (targetPlayer != null && targetPlayer.isOnline()) {
                                plugin.setPlayerLoggedIn(targetPlayer, false);
                                targetPlayer.sendMessage(plugin.getLangMessage("player.delete_notify", "&e[CursorMenu] Your registration data has been deleted by an administrator. Please register again."));
                                
                                // 将玩家送回登录菜单
                                plugin.setupCursor(targetPlayer, "login_menu");
                            }
                        } else {
                            sender.sendMessage(plugin.getLangMessage("player.delete_failed", "&c[CursorMenu] Failed to delete registration data for %player%").replace("%player%", targetPlayerName));
                        }

                        return true;

                    default:
                        sender.sendMessage(plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] Unknown argument"));
                        return true;
                }

            case 3:
                // 处理用户输入命令: /cursormenu input <field> <value>
                if (args[0].equalsIgnoreCase("input") && sender instanceof Player) {
                    if (!sender.hasPermission("cursormenu.input")) {
                        sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                        return true;
                    }
                    Player player = (Player) sender;
                    String field = args[1];
                    String value = args[2];

                    // 同时更新新旧两个系统中的用户输入数据
                    UUID playerId = player.getUniqueId();
                    userInputData.computeIfAbsent(playerId, k -> new HashMap<>());
                    Map<String, String> userData = userInputData.get(playerId);

                    // 更新旧系统数据
                    switch (field) {
                        case "username":
                            userData.put("username", value);
                            sendActionBarMessage(player, plugin.getLangMessage("input.data_saved", "&a[CursorMenu] %field% 已设置为: %value%").replace("%field%", "用户名").replace("%value%", value));
                            // 更新菜单显示
                            plugin.setupCursor(player, plugin.getCurrentPlayerMenu(player));
                            break;
                        case "password":
                            userData.put("password", value);
                            sendActionBarMessage(player, plugin.getLangMessage("input.data_saved", "&a[CursorMenu] %field% 已设置为: %value%").replace("%field%", "密码").replace("%value%", "*****"));
                            // 更新菜单显示
                            plugin.setupCursor(player, plugin.getCurrentPlayerMenu(player));
                            break;
                        case "confirm_password":
                            userData.put("confirm_password", value);
                            sendActionBarMessage(player, plugin.getLangMessage("input.data_saved", "&a[CursorMenu] %field% 已设置为: %value%").replace("%field%", "确认密码").replace("%value%", "*****"));
                            // 更新菜单显示
                            plugin.setupCursor(player, plugin.getCurrentPlayerMenu(player));
                            break;
                        default:
                            sendActionBarMessage(player, plugin.getLangMessage("input.incomplete", "&c[CursorMenu] 请先完成所有输入项"));
                            return true;
                    }

                    // 同时更新新系统数据
                    plugin.setPlayerInputData(player, field, value);

                    return true;
                }

                // 处理重置密码命令: /cursormenu resetpassword <player> <newpassword>
                if (args[0].equalsIgnoreCase("resetpassword")) {
                    if (!sender.hasPermission("cursormenu.resetpassword")) {
                        sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                        return true;
                    }

                    String targetPlayerName = args[1];
                    String newPassword = args[2];

                    // 检查目标玩家是否存在
                    Player targetPlayer = Bukkit.getPlayerExact(targetPlayerName);
                    if (targetPlayer == null) {
                        sender.sendMessage(plugin.getLangMessage("player.not_found", "&c[CursorMenu] 玩家 %player% 不存在或不在线").replace("%player%", targetPlayerName));
                        return true;
                    }

                    // 检查密码长度
                    if (newPassword.length() < 6 || newPassword.length() > 12) {
                        sender.sendMessage(plugin.getLangMessage("register.invalid_password_length", "&c[CursorMenu] Password length must be between 6 and 12 characters"));
                        return true;
                    }

                    // 检查密码强度
                    if (isWeakPassword(newPassword)) {
                        sender.sendMessage(plugin.getLangMessage("register.weak_password", "&c[CursorMenu] Password is too weak, please use a stronger password"));
                        return true;
                    }

                    // 更新玩家密码
                    String hashedPassword = plugin.getUserDataManager().hashPasswordForTest(newPassword);
                    boolean success = plugin.getDatabaseManager().updateUserPassword(targetPlayerName, hashedPassword);

                    if (success) {
                        sender.sendMessage(plugin.getLangMessage("player.reset_password_success", "&a[CursorMenu] 玩家 %player% 的密码已成功重置").replace("%player%", targetPlayerName));

                        // 如果玩家在线，强制其重新登录
                        if (targetPlayer.isOnline()) {
                            plugin.setPlayerLoggedIn(targetPlayer, false);
                            targetPlayer.sendMessage(plugin.getLangMessage("player.reset_password_notify", "&e[CursorMenu] 您的密码已被管理员重置，请重新登录"));
                            
                            // 将玩家送回登录菜单
                            plugin.setupCursor(targetPlayer, "login");
                        }
                    } else {
                        sender.sendMessage(plugin.getLangMessage("player.reset_password_failed", "&c[CursorMenu] 重置玩家 %player% 的密码失败").replace("%player%", targetPlayerName));
                    }

                    return true;
                }


                if (args[0].equalsIgnoreCase("run")) {
                    Section section = sectionManager.get(args[1]);
                    if (section == null) {
                        sender.sendMessage(plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] Menu option not found"));
                        return true;
                    }
                    if (!section.permission.isEmpty()
                            && !sender.hasPermission(section.permission)
                            && !sender.isOp()) {
                        sender.sendMessage(plugin.getLangMessage("permission.no_permission_menu", "&c[CursorMenu] You do not have permission to open this menu"));
                        return true;
                    }
                    if (!sender.hasPermission("cursormenu.start")) {
                        sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                        return true;
                    }

                    String menuKey = args[1];
                    if (!sectionManager.has(menuKey)) {
                        sender.sendMessage(plugin.getLangMessage("command.menu_option_not_found", "&c[CursorMenu] Menu option not found"));
                        return true;
                    }

                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null || !target.isOnline()) {
                        sender.sendMessage(plugin.getLangMessage("player.not_found", "&c[CursorMenu] 玩家 %player% 不存在或不在线").replace("%player%", args[2]));
                        return true;
                    }

                    plugin.setupCursor(target, menuKey);
                    sender.sendMessage(plugin.getLangMessage("command.menu_opened", "&a[CursorMenu] 已为玩家 %player% 打开菜单 %menu%").replace("%player%", target.getName()).replace("%menu%", menuKey));
                    return true;
                }

                if (args[0].equalsIgnoreCase("text") && args[1].equalsIgnoreCase("open")) {
                    if (!sender.hasPermission("cursormenu.text")) {
                        sender.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                        return true;
                    }

                    String textId = args[2];
                    Player target = sender instanceof Player ? (Player) sender : null;

                    if (args.length == 4) {
                        target = Bukkit.getPlayer(args[3]);
                    }

                    if (target == null || !target.isOnline()) {
                        sender.sendMessage(plugin.getLangMessage("player.not_found", "&c[CursorMenu] 玩家 %player% 不存在或不在线").replace("%player%", args.length == 4 ? args[3] : "sender"));
                        return true;
                    }

                    boolean success = CursorMenuPlugin.textDisplayManager.showTextDisplayById(target, textId);
                    if (!success) {
                        sender.sendMessage(plugin.getLangMessage("command.text_display_not_found", "&c[CursorMenu] 文字显示ID不存在: %text_id%").replace("%text_id%", textId));
                        return true;
                    }

                    sender.sendMessage(plugin.getLangMessage("command.text_display_opened", "&a[CursorMenu] 已为玩家 %player% 打开文字显示: %text_id%").replace("%player%", target.getName()).replace("%text_id%", textId));
                    return true;
                }

                sender.sendMessage(plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] Unknown argument"));
                return true;



            default:
                sender.sendMessage(plugin.getLangMessage("command.unknown_argument", "&c[CursorMenu] Unknown argument"));
                return true;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        switch (args.length) {

            case 1:

                if (commandSender.hasPermission("cursormenu.reload")) list.add("reload");
                if (commandSender.hasPermission("cursormenu.start")) list.add("run");
                if (commandSender.hasPermission("cursormenu.stop")) list.add("stop");
                if (commandSender.hasPermission("cursormenu.items")) list.add("items");
                if (commandSender.hasPermission("cursormenu.itemsstop")) list.add("itemsstop");
                if (commandSender.hasPermission("cursormenu.text")) list.add("text");
                if (commandSender.hasPermission("cursormenu.resetpassword")) list.add("resetpassword");
                list.add("input");
                list.add("toggle_password_visibility");
                list.add("register_confirm");
                list.add("check_and_register");
                list.add("check_registered");
                list.add("login");
                list.add("close");
                list.add("npc");

                return StringUtil.copyPartialMatches(args[0].toLowerCase(), list, new ArrayList<String>());

            case 2:
                switch (args[0].toLowerCase()) {
                    case "run":
                        if (commandSender.hasPermission("cursormenu.start")){
                            list.addAll(sectionManager.keySet());
                            return StringUtil.copyPartialMatches(args[1].toLowerCase(), list, new ArrayList<String>());
                        }
                    case "items":
                        if (commandSender.hasPermission("cursormenu.items")){
                            list.addAll(itemDisplayManager.getAllItemIds());
                            return StringUtil.copyPartialMatches(args[1], list, new ArrayList<String>());
                        }
                    case "input":
                        list.add("username");
                        list.add("password");
                        list.add("confirm_password");
                        return StringUtil.copyPartialMatches(args[1], list, new ArrayList<>());
                        
                    case "resetpassword":
                        if (commandSender.hasPermission("cursormenu.resetpassword")) {
                            // 添加在线玩家列表用于自动补全
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                list.add(player.getName());
                            }
                            return StringUtil.copyPartialMatches(args[1], list, new ArrayList<>());
                        }

                    case "text":
                        if (commandSender.hasPermission("cursormenu.text")) {
                            list.add("open");
                        }
                        return StringUtil.copyPartialMatches(args[1], list, new ArrayList<>());

                    case "npc":
                        list.add("toggle");
                        list.add("enable");
                        list.add("disable");
                        list.add("status");
                        list.add("reload");
                        list.add("rotate");
                        list.add("help");
                        return StringUtil.copyPartialMatches(args[1], list, new ArrayList<>());

                    default:
                        return Collections.emptyList();
                }

            case 3:
                if (args[0].equalsIgnoreCase("resetpassword")) {
                    if (commandSender.hasPermission("cursormenu.resetpassword")) {
                        // 密码参数，不提供自动补全
                        return Collections.emptyList();
                    }
                }
                
                if (args[0].equalsIgnoreCase("run")) {
                    return null;
                }
                return Collections.emptyList();

            default:
                return Collections.emptyList();
        }
    }

    // 提供获取用户输入数据的方法
    public static Map<UUID, Map<String, String>> getUserInputData() {
        return userInputData;
    }

    // 检查密码是否过于简单
    private static boolean isWeakPassword(String password) {
        // 检查是否为常见弱密码
        String[] weakPasswords = {"123456", "password", "123456789", "12345678", "12345",
                "1234567", "1234567890", "qwerty", "abc123", "111111"};

        for (String weak : weakPasswords) {
            if (password.equalsIgnoreCase(weak)) {
                return true;
            }
        }

        // 检查是否为纯数字或纯字母
        if (password.matches("\\d+") || password.matches("[a-zA-Z]+")) {
            return true;
        }

        // 检查是否为连续数字或字母
        if (isSequential(password)) {
            return true;
        }

        return false;
    }

    // 检查是否为连续字符
    private static boolean isSequential(String str) {
        boolean isAscending = true;
        boolean isDescending = true;

        for (int i = 1; i < str.length(); i++) {
            if (str.charAt(i) != str.charAt(i-1) + 1) {
                isAscending = false;
            }
            if (str.charAt(i) != str.charAt(i-1) - 1) {
                isDescending = false;
            }
        }

        return isAscending || isDescending;
    }
    
    /**
     * 发送ActionBar消息给玩家
     * @param player 要发送消息的玩家
     * @param message 消息内容
     */
    private void sendActionBarMessage(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    private String getRegistrationErrorMessage(String messageKey) {
        if (messageKey == null) {
            return plugin.getLangMessage("register.error", "&c[CursorMenu] 注册失败，请稍后重试");
        }
        
        switch (messageKey) {
            case "ip_blacklisted":
                return plugin.getLangMessage("register.error.ip_blacklisted", "&c[CursorMenu] 您的IP地址已被禁止注册");
            case "ip_not_in_whitelist":
                return plugin.getLangMessage("register.error.ip_not_in_whitelist", "&c[CursorMenu] 您的IP地址不在允许注册的列表中");
            default:
                return plugin.getLangMessage("register.error", "&c[CursorMenu] 注册失败，请稍后重试");
        }
    }
    
    private String getLoginErrorMessage(String messageKey) {
        if (messageKey == null) {
            return plugin.getLangMessage("login.error", "&c[CursorMenu] 登录失败，请稍后重试");
        }
        
        switch (messageKey) {
            case "ip_blacklisted":
                return plugin.getLangMessage("login.error.ip_blacklisted", "&c[CursorMenu] 您的IP地址已被禁止登录");
            case "ip_not_in_whitelist":
                return plugin.getLangMessage("login.error.ip_not_in_whitelist", "&c[CursorMenu] 您的IP地址不在允许登录的列表中");
            case "locked_out":
                return plugin.getLangMessage("login.error.locked_out", "&c[CursorMenu] 账户已被锁定，请稍后重试");
            case "invalid_password":
                return plugin.getLangMessage("login.error.invalid_password", "&c[CursorMenu] Incorrect password");
            case "user_not_found":
                return plugin.getLangMessage("login.error.user_not_found", "&c[CursorMenu] User not found");
            default:
                return plugin.getLangMessage("login.error", "&c[CursorMenu] 登录失败，请稍后重试");
        }
    }

    /**
     * 处理NPC相关命令
     */
    private void handleNPCCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLangMessage("permission.player_only", "&c[CursorMenu] NPC命令仅玩家可用"));
            return;
        }

        Player player = (Player) sender;
        NPCMirrorHook npcHook = NPCMirrorHook.getInstance(plugin);

        if (npcHook == null || !npcHook.isEnabled()) {
            player.sendMessage(plugin.getLangMessage("npc.system_disabled", "&e[CursorMenu] NPC镜像系统未启用（需要安装FancyNpcs插件）"));
            return;
        }

        String subCommand = args.length > 1 ? args[1].toLowerCase() : "help";

        switch (subCommand) {
            case "toggle":
                if (!player.hasPermission("cursormenu.npc.toggle")) {
                    player.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                    return;
                }
                boolean newState = npcHook.toggleNPCForPlayer(player);
                player.sendMessage(newState ? plugin.getLangMessage("npc.toggle_on", "&a[CursorMenu] NPC镜像创建已启用") : plugin.getLangMessage("npc.toggle_off", "&c[CursorMenu] NPC镜像创建已禁用"));
                break;

            case "enable":
                if (!player.hasPermission("cursormenu.npc.toggle")) {
                    player.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                    return;
                }
                npcHook.setNPCEnabledForPlayer(player, true);
                player.sendMessage(plugin.getLangMessage("npc.toggle_on", "&a[CursorMenu] NPC镜像创建已启用"));
                break;

            case "disable":
                if (!player.hasPermission("cursormenu.npc.toggle")) {
                    player.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                    return;
                }
                npcHook.setNPCEnabledForPlayer(player, false);
                player.sendMessage(plugin.getLangMessage("npc.toggle_off", "&c[CursorMenu] NPC镜像创建已禁用"));
                break;

            case "status":
                if (!player.hasPermission("cursormenu.npc.status")) {
                    player.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                    return;
                }
                player.sendMessage(plugin.getLangMessage("npc.status_enabled", "&a[CursorMenu] NPC创建状态: 已启用"));
                break;

            case "reload":
                if (!player.hasPermission("cursormenu.npc.reload")) {
                    player.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                    return;
                }
                npcHook.reload();
                player.sendMessage(plugin.getLangMessage("npc.reloaded", "&a[CursorMenu] NPC配置已重载"));
                break;

            case "rotate":
                if (!player.hasPermission("cursormenu.npc.rotate")) {
                    player.sendMessage(plugin.getLangMessage("permission.no_permission", "&c[CursorMenu] You do not have permission to use this command"));
                    return;
                }
                if (args.length < 3) {
                    player.sendMessage(plugin.getLangMessage("npc.rotate_usage", "&c[CursorMenu] 用法: /cursormenu npc rotate <角度>"));
                    return;
                }
                try {
                    float angle = Float.parseFloat(args[2]);
                    npcHook.rotateNPC(player, angle);
                    player.sendMessage(plugin.getLangMessage("npc.rotated", "&a[CursorMenu] NPC已旋转 %angle% 度").replace("%angle%", String.valueOf(angle)));
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getLangMessage("npc.invalid_angle", "&c[CursorMenu] 无效的角度值: %angle%").replace("%angle%", args[2]));
                }
                break;

            case "help":
            default:
                player.sendMessage(ChatColor.YELLOW + "===== NPC镜像系统帮助 =====");
                player.sendMessage(ChatColor.GRAY + "/cursormenu npc toggle " + ChatColor.WHITE + "- 切换NPC创建状态");
                player.sendMessage(ChatColor.GRAY + "/cursormenu npc enable " + ChatColor.WHITE + "- 启用NPC创建");
                player.sendMessage(ChatColor.GRAY + "/cursormenu npc disable " + ChatColor.WHITE + "- 禁用NPC创建");
                player.sendMessage(ChatColor.GRAY + "/cursormenu npc status " + ChatColor.WHITE + "- 查看NPC状态");
                player.sendMessage(ChatColor.GRAY + "/cursormenu npc reload " + ChatColor.WHITE + "- 重载NPC配置");
                player.sendMessage(ChatColor.GRAY + "/cursormenu npc rotate <角度> " + ChatColor.WHITE + "- 旋转NPC");
                break;
        }
    }
}