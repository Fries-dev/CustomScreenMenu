# CustomScreenMenu 更新日志 / Changelog

## [1.4.2] - 2026-03-15

### 🔒 安全性更新 / Security Updates

**密码哈希算法升级** - 从不安全的SHA-256升级到BCrypt加密算法。

- 新注册用户自动使用BCrypt加密
- 旧用户首次登录成功后自动升级密码格式
- BCrypt轮次设置为12，提供良好的安全性和性能平衡

**Password Hashing Algorithm Upgrade** - Upgraded from insecure SHA-256 to BCrypt encryption algorithm.

- New users automatically use BCrypt encryption
- Legacy passwords auto-upgrade after first successful login
- BCrypt rounds set to 12 for good security and performance balance

**临时OP操作安全性增强** - 改进了`[op]`命令执行的安全机制，确保OP状态正确恢复。

**Temporary OP Operation Security Enhancement** - Improved security mechanism for `[op]` command execution, ensuring OP status is correctly restored.

### 🧵 并发安全修复 / Concurrency Safety Fixes

**修复非线程安全集合** - 将所有HashMap/HashSet替换为线程安全的ConcurrentHashMap，解决了多线程环境下可能出现的并发问题。

**Fixed Non-Thread-Safe Collections** - Replaced all HashMap/HashSet with thread-safe ConcurrentHashMap, resolving potential concurrency issues in multi-threaded environments.

### 📝 消息配置化 / Message Configuration

**全面配置化消息** - 所有提示信息现在都可以在`lang.yml`中自定义配置。

- 支持热加载，修改配置后使用`/cursormenu reload`即可生效
- 新增50+条可配置消息
- 配置分类：登录提示、注册提示、权限提示、命令提示、玩家管理提示、菜单提示、系统提示、NPC提示、WASD导航提示

**Full Message Configuration** - All prompt messages can now be customized in `lang.yml`.

- Hot reload support, changes take effect after `/cursormenu reload`
- Added 50+ configurable messages
- Configuration categories: login, register, permission, command, player management, menu, system, NPC, WASD navigation

### 🧹 代码质量改进 / Code Quality Improvements

**新增命令执行工具类** - 统一了命令执行逻辑，支持`[console]`、`[op]`、`[player]`、`[server]`、`[message]`、`[actionbar]`等命令前缀。

**Added Command Execution Utility Class** - Unified command execution logic, supporting `[console]`, `[op]`, `[player]`, `[server]`, `[message]`, `[actionbar]` command prefixes.

**代码清理** - 清理了重复的导入语句，简化了代码结构。

**Code Cleanup** - Cleaned up duplicate import statements, simplified code structure.

### 📦 文件变更 / File Changes

| 文件 / File | 变更内容 / Changes |
|-------------|-------------------|
| `pom.xml` | 添加BCrypt依赖，版本更新至1.4.2 |
| `plugin.yml` | 版本更新至1.4.2 |
| `lang.yml` | 新增50+条可配置消息 |
| `CommandUtils.java` | 新增命令执行工具类 |
| `Commands.java` | 使用配置化消息 |
| `CursorMenuPlugin.java` | 使用配置化消息、线程安全集合 |
| `MenuLayout.java` | 使用配置化消息 |
| `UserDataManager.java` | 升级密码哈希算法 |
| `DatabaseManager.java` | 新增获取密码哈希方法 |

### ⚠️ 注意事项 / Important Notes

- BCrypt计算比SHA-256慢，这是正常的安全特性
- 旧密码在首次成功登录后自动升级为BCrypt格式
- 无需手动迁移数据库

- BCrypt computation is slower than SHA-256, this is a normal security feature
- Legacy passwords auto-upgrade to BCrypt format after first successful login
- No manual database migration required

---

## [1.4.1] - 2026-03-10

📌 更新日志 – v1.4.1

🔒 安全性更新

**密码哈希算法升级** - 从不安全的SHA-256升级到BCrypt加密算法。

新注册用户自动使用BCrypt加密，旧用户首次登录成功后自动升级密码格式。

**临时OP操作安全性增强** - 改进了`[op]`命令执行的安全机制，确保OP状态正确恢复。

🧵 并发安全修复

**修复非线程安全集合** - 将所有HashMap/HashSet替换为线程安全的ConcurrentHashMap，解决了多线程环境下可能出现的并发问题。

📝 消息配置化

**全面配置化消息** - 所有提示信息现在都可以在`lang.yml`中自定义配置。

支持热加载，修改配置后使用`/cursormenu reload`即可生效，无需重启服务器。

新增配置分类：登录提示、注册提示、权限提示、命令提示、玩家管理提示、菜单提示、系统提示、NPC提示、WASD导航提示。

🧹 代码质量改进

**新增命令执行工具类** - 统一了命令执行逻辑，支持`[console]`、`[op]`、`[player]`、`[server]`等命令前缀。

**代码清理** - 清理了重复的导入语句，简化了代码结构。

📦 文件变更

新增 `CommandUtils.java` 命令执行工具类。

更新 `lang.yml` 新增50+条可配置消息。

⚠️ 注意事项

BCrypt计算比SHA-256慢，这是正常的安全特性，无需担心性能问题。

---

## [1.4.0] - 2026-03-01

### 🆕 新增功能 / New Features

#### NPC镜像系统 / NPC Mirror System
- **新增NPC镜像模块** - 在3D菜单中创建玩家镜像NPC
  - 自动同步玩家装备（头盔、胸甲、护腿、靴子、主手、副手）
  - 支持SkinsRestorer皮肤同步
  - 可配置NPC位置偏移和朝向
  - 支持菜单关联配置，可指定哪些菜单启用NPC
  - NPC名称支持自定义格式和PlaceholderAPI变量
  - 玩家可单独禁用自己的NPC创建

- **Added NPC Mirror Module** - Create player mirror NPCs in 3D menus
  - Auto-sync player equipment (helmet, chestplate, leggings, boots, main hand, off hand)
  - Support SkinsRestorer skin synchronization
  - Configurable NPC position offset and orientation
  - Menu association configuration to specify which menus enable NPCs
  - NPC name supports custom format and PlaceholderAPI variables
  - Players can individually disable their own NPC creation

#### WASD导航系统 / WASD Navigation System
- **新增WASD导航模块** - 使用键盘导航菜单
  - W/S 键上下选择菜单项
  - A/D 键左右选择菜单项
  - 空格键确认执行命令
  - 与原有光标控制系统完全兼容，可同时使用
  - 支持菜单关联配置
  - 选择时自动放大选中项并播放音效
  - 选择冷却机制防止快速连续选择

- **Added WASD Navigation Module** - Navigate menus using keyboard
  - W/S keys for up/down selection
  - A/D keys for left/right selection
  - Space key to confirm and execute commands
  - Fully compatible with existing cursor control system, can be used simultaneously
  - Menu association configuration support
  - Auto-enlarge selected item and play sound effect on selection
  - Selection cooldown mechanism to prevent rapid consecutive selections

#### PlaceholderAPI变量 / PlaceholderAPI Variables
- **新增WASD导航PAPI变量**
  - `%cmp_wasd_menu%` - 当前WASD菜单名称
  - `%cmp_wasd_enabled%` - 是否启用WASD导航
  - `%cmp_wasd_index%` - 当前选中索引
  - `%cmp_wasd_x%` - 当前X坐标
  - `%cmp_wasd_y%` - 当前Y坐标
  - `%cmp_wasd_z%` - 当前Z坐标
  - `%cmp_wasd_location%` - 完整位置信息（格式：菜单名,x,y,z）
  - `%cmp_<菜单名>_xyz%` - 指定菜单的坐标

- **Added WASD Navigation PAPI Variables**
  - `%cmp_wasd_menu%` - Current WASD menu name
  - `%cmp_wasd_enabled%` - Whether WASD navigation is enabled
  - `%cmp_wasd_index%` - Current selected index
  - `%cmp_wasd_x%` - Current X coordinate
  - `%cmp_wasd_y%` - Current Y coordinate
  - `%cmp_wasd_z%` - Current Z coordinate
  - `%cmp_wasd_location%` - Full location info (format: menu_name,x,y,z)
  - `%cmp_<menu_name>_xyz%` - Coordinates for specified menu

### ⚙️ 配置文件 / Configuration Files

#### 新增配置文件 / New Configuration Files
- `npc_config.yml` - NPC镜像系统配置
  - 启用/禁用NPC功能
  - NPC位置偏移配置
  - 菜单关联设置（白名单/黑名单模式）
  - 名称显示配置（支持PAPI变量）
  - 装备和皮肤同步开关

- `wasd_config.yml` - WASD导航系统配置
  - 启用/禁用WASD导航
  - 导航阈值配置
  - 选择冷却时间
  - 音效配置
  - 菜单关联设置

- `npc_config.yml` - NPC mirror system configuration
  - Enable/disable NPC feature
  - NPC position offset configuration
  - Menu association settings (whitelist/blacklist mode)
  - Name display configuration (supports PAPI variables)
  - Equipment and skin sync toggles

- `wasd_config.yml` - WASD navigation system configuration
  - Enable/disable WASD navigation
  - Navigation threshold configuration
  - Selection cooldown time
  - Sound effect configuration
  - Menu association settings

#### 菜单配置扩展 / Menu Configuration Extensions
- 新增 `wasd-enabled` 选项 - 单独控制每个菜单的WASD导航开关
- Added `wasd-enabled` option - Individually control WASD navigation for each menu

### 🔧 命令 / Commands

#### NPC命令 / NPC Commands
- `/cursormenu npc toggle` - 切换NPC创建状态
- `/cursormenu npc enable` - 启用NPC创建
- `/cursormenu npc disable` - 禁用NPC创建
- `/cursormenu npc status` - 查看NPC状态
- `/cursormenu npc reload` - 重载NPC配置
- `/cursormenu npc rotate <角度>` - 旋转NPC

### 🔌 依赖 / Dependencies

#### 新增软依赖 / New Soft Dependencies
- **FancyNpcs** (可选) - NPC镜像功能必需
- **SkinsRestorer** (可选) - NPC皮肤同步支持

- **FancyNpcs** (optional) - Required for NPC mirror feature
- **SkinsRestorer** (optional) - NPC skin synchronization support

### 📝 权限 / Permissions

#### 新增权限 / New Permissions
- `cursormenu.npc.toggle` - 切换NPC创建状态
- `cursormenu.npc.status` - 查看NPC状态
- `cursormenu.npc.reload` - 重载NPC配置
- `cursormenu.npc.rotate` - 旋转NPC

### 🔄 兼容性 / Compatibility

- ✅ NPC镜像系统和WASD导航系统均为模块化设计
- ✅ 不修改原有CMP核心功能
- ✅ 所有新功能可独立启用/禁用
- ✅ 与原有光标控制系统完全兼容

- ✅ NPC mirror system and WASD navigation system are modularly designed
- ✅ No modifications to original CMP core functionality
- ✅ All new features can be independently enabled/disabled
- ✅ Fully compatible with existing cursor control system

### 📦 文件结构 / File Structure

```
com.cmenu.ui/
├── npc/                          # NPC镜像模块
│   ├── NPCModule.java
│   ├── NPCMirrorManager.java
│   ├── NPCMirrorHook.java
│   ├── NPCConfig.java
│   └── NPCCommandHandler.java
└── wasd/                         # WASD导航模块
    ├── WASDModule.java
    ├── WASDNavigationManager.java
    ├── WASDNavigationHook.java
    ├── WASDConfig.java
    ├── WASDSession.java
    └── WASDExpansion.java
```

---

## 升级说明 / Upgrade Notes

### 从 1.3.x 升级到 1.4.0 / Upgrading from 1.3.x to 1.4.0

1. **备份配置文件** - 升级前请备份现有配置
2. **删除旧版本jar** - 删除 `CustomScreenMenu-1.3.x.jar`
3. **放入新版本jar** - 将 `CustomScreenMenu-1.4.0.jar` 放入 plugins 目录
4. **首次启动** - 会自动生成 `npc_config.yml` 和 `wasd_config.yml`
5. **安装依赖插件**（可选）- 安装 FancyNpcs 和 SkinsRestorer 以启用完整功能

1. **Backup configuration files** - Please backup existing configurations before upgrading
2. **Delete old jar** - Delete `CustomScreenMenu-1.3.x.jar`
3. **Place new jar** - Put `CustomScreenMenu-1.4.0.jar` in plugins directory
4. **First startup** - Will auto-generate `npc_config.yml` and `wasd_config.yml`
5. **Install dependency plugins** (optional) - Install FancyNpcs and SkinsRestorer for full functionality

---


