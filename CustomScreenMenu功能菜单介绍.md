# CustomScreenMenu 功能菜单介绍

## 📋 插件概述

CustomScreenMenu 是一款功能强大的 Minecraft 自定义屏幕菜单插件，允许服务器管理员创建交互式的屏幕菜单系统，为玩家提供直观的操作界面。

### ✨ 核心功能

- **自定义屏幕菜单**：创建具有多个按钮的交互式菜单
- **多类型命令执行**：支持玩家命令、控制台命令、OP权限命令
- **菜单跳转系统**：实现菜单之间的无缝切换
- **智能传送系统**：支持自定义传送坐标和传回原始位置
- **安全访问控制**：基于登录状态和权限的按钮访问控制
- **丰富的视觉效果**：悬停放大、文字大小调整、倾斜效果
- **随机命令执行**：基于概率的随机命令执行系统
- **IP绑定安全**：防止账号盗用和恶意登录
- **生物生成限制**：在菜单区域限制生物生成
- **摄像机视角检测**：确保菜单显示区域的方块安全

## 🎮 菜单结构

### 主菜单 (example)

主菜单包含以下按钮：

| 按钮名称 | 功能描述 | 权限需求 | 操作效果 |
|---------|---------|---------|----------|
| **Logo** | 服务器标志 | `cursormenu.button.test.logo` | 显示服务器标志 |
| **注册账号** | 跳转到注册菜单 | `cursormenu.button.test.layout1` | 执行OP命令并跳转到登录菜单 |
| **退出游戏** | 退出菜单并传送 | `cursormenu.button.test.layout2` | 执行OP命令并传送到指定坐标 |
| **进入服务器** | 进入游戏服务器 | `cursormenu.button.test.layout2` | 执行OP命令并跳转到example2菜单 |
| **制作人员** | 显示制作人员信息 | `cursormenu.button.test.layout4` | 执行OP命令 |

### 菜单配置示例

```yaml
example:
  permission: cursormenu.menu.example
  camera-position:
    distance: 1.5
    world: lobby
    x: -187
    y: 70
    z: 347
    yaw: 190
    pitch: 0.0
  layout:
    logo:
      name: "%img_logo%"
      x: -2
      y: 1
      z: 3.5
    layout1:
      name: "注册账号"
      x: -2
      y: 0
      z: 3.5
      command:
        - '[op] say 注册账号'
      next-menu:
        enabled: true
        menu: 登录菜单
```

## 🛠️ 功能详解

### 1. 命令执行系统

支持多种类型的命令执行：

- **[player]**：以玩家身份执行命令
- **[console]**：以控制台身份执行命令
- **[op]**：以OP权限执行命令
- **[server]**：跨服传送命令

### 2. 传送系统

- **传回原始位置**：`back-original: true`
- **自定义传送坐标**：`back-original: false` + 坐标设置
- **支持世界指定**：可指定目标世界

### 3. 视觉效果

- **悬停放大**：`hover-enlarge.enabled: true`
- **文字大小调整**：`text-size: 1.2`
- **按钮倾斜效果**：`tilt` 配置

### 4. 安全系统

- **IP绑定**：防止账号盗用
- **IP白名单/黑名单**：控制访问来源
- **管理员IP白名单**：保护OP权限
- **登录尝试限制**：防止暴力破解

### 5. 智能控制

- **需要登录的按钮**：配置 `login-required` 标签
- **防止重复注册**：配置 `no-duplicate-registration` 标签
- **命令延迟执行**：`command-delay` 配置
- **随机命令执行**：基于概率的随机奖励系统

## 🔧 配置选项

### 核心配置

- **`use-player-location`**：是否使用玩家当前位置作为菜单相机位置
- **`exit-camera`**：配置玩家结束菜单时的朝向
- **`creature-spawn-limits`**：菜单区域生物生成限制
- **`camera-block-check`**：摄像机视角方块检测

### 安全配置

- **`ip-binding`**：IP绑定安全设置
- **`button-access-control`**：按钮访问控制配置
- **`login-required`**：需要登录才能点击的按钮标识
- **`no-duplicate-registration`**：防止重复注册的按钮标识

### 音效配置

- **`sound`**：菜单音效设置
- **`loop`**：是否循环播放音效

### 光标配置

- **`cursor-item`**：光标物品设置
- **`movement-range`**：光标移动范围限制
- **`default-position`**：光标默认位置

## 🚀 使用方法

### 打开菜单

```bash
# 打开指定菜单
/cursormenu run <菜单名称>

# 为其他玩家打开菜单
/cursormenu run <菜单名称> <玩家名称>
```

### 停止菜单

```bash
# 停止当前菜单
/cursormenu stop

# 关闭菜单
/cursormenu close
```

### 管理命令

```bash
# 重载插件配置
/cursormenu reload

# 删除玩家注册信息
/cursormenu deleteuser <玩家名称>

# 重置玩家密码
/cursormenu resetpassword <玩家名称> <新密码>
```

## 📁 菜单文件结构

菜单文件位于 `plugins/CustomScreenMenu/menu/` 目录下：

- **example.yml**：示例菜单配置
- **link_example.yml**：链接菜单示例

### 菜单配置结构

```yaml
菜单名称:
  permission: 菜单权限
  camera-position: 摄像机位置配置
  layout:
    按钮1:
      name: 按钮名称
      x: X轴位置
      y: Y轴位置
      z: Z轴位置
      command: 执行命令
      stop-menu: 结束菜单配置
      next-menu: 下一个菜单配置
```

## 🔒 权限系统

### 菜单权限

- `cursormenu.menu.<菜单名称>`：打开指定菜单的权限
- `cursormenu.button.<菜单名称>.<按钮名称>`：点击指定按钮的权限

### 命令权限

- `cursormenu.run`：运行菜单命令
- `cursormenu.stop`：停止菜单命令
- `cursormenu.reload`：重载插件配置
- `cursormenu.deleteuser`：删除用户注册信息
- `cursormenu.resetpassword`：重置用户密码

## 🎯 最佳实践

### 1. 登录注册系统

- 创建登录和注册菜单
- 使用 `login-required` 标签保护敏感按钮
- 配置 IP 绑定增强账号安全

### 2. 服务器导航

- 创建服务器导航菜单
- 配置不同区域的传送坐标
- 使用菜单跳转实现分类导航

### 3. 奖励系统

- 使用随机命令执行功能
- 配置不同概率的奖励
- 结合权限系统实现等级奖励

### 4. 管理员面板

- 创建管理员专用菜单
- 使用 OP 命令执行管理操作
- 配置管理员 IP 白名单保护

## 📝 配置示例

### 登录菜单配置

```yaml
登录菜单:
  camera-position:
    distance: 1.5
    world: lobby
    x: 0
    y: 64
    z: 0
  layout:
    login_button:
      name: "登录"
      x: 0
      y: 0.5
      command:
        - '[player] cursormenu login'
      stop-menu:
        enabled: true
    register_button:
      name: "注册"
      x: 0
      y: -0.5
      command:
        - '[player] cursormenu register_confirm'
      stop-menu:
        enabled: true
```

### 服务器导航菜单

```yaml
导航菜单:
  layout:
    spawn:
      name: "出生点"
      command:
        - '[player] spawn'
      stop-menu:
        enabled: true
        teleport:
          enabled: true
          back-original: false
          world: world
          x: 0
          y: 64
          z: 0
    mining:
      name: "挖矿区"
      command:
        - '[player] warp mining'
      stop-menu:
        enabled: true
        teleport:
          enabled: true
          back-original: false
          world: world
          x: 100
          y: 64
          z: 100
```

## 🔄 版本更新

### 1.3.7 版本特性

- **修复了传送系统**：解决了自定义坐标传送被拉回出生点的问题
- **增强了安全系统**：改进了IP绑定和管理员IP白名单功能
- **优化了菜单系统**：提高了菜单加载速度和响应性能
- **增加了视觉效果**：添加了更多的按钮视觉效果选项

## 📞 支持与反馈

如果您在使用过程中遇到任何问题或有任何建议，欢迎联系插件开发者。

### 常见问题

- **菜单不显示**：检查世界名称和坐标配置是否正确
- **传送失败**：确保目标世界存在且坐标有效
- **按钮无响应**：检查权限配置和命令格式
- **IP绑定失败**：确保IP格式正确且网络连接稳定

---

**CustomScreenMenu** - 为您的服务器提供专业的菜单解决方案！

*版本：1.3.7*
*最后更新：2026-02-09*
