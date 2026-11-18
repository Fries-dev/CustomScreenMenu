# CustomScreenMenu

A Minecraft plugin that lets you build custom, interactive 3D GUIs right in the game world using entities.

I originally remade and translated this project from [CustomScreenMenu](https://github.com/JSGDJD/CustomScreenMenu) into English because I wanted to use it on my own server and thought it would be a fun project to work on.

## What it does
* **3D World Menus:** Renders clickable, cursor-controlled menus directly in 3D space instead of standard chest GUIs.
* **Entity-Powered:** Uses Minecraft entities to display buttons, text, items, and interactive input fields (including built-in login/register screens).
* **Folia Supported:** Built to run on Spigot, Paper, and Folia (`1.19+`).
* **Translated to English:** Full English translation of the codebase, configs, and in-game messages.

## A quick note on lag
Since this plugin uses actual entities to render the menus, **having too many of them loaded at once will cause lag** (both for the server and players' FPS). It works best for spawn hubs, login screens, or specific NPC interactions rather than everywhere at once.

## Requirements
* **Server:** Spigot, Paper, or Folia (`1.19` or higher)
* **Required Dependencies:**
  * [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
  * [PacketEvents](https://modrinth.com/plugin/packetevents)
* **Optional Dependencies:**
  * [FancyNpcs](https://modrinth.com/plugin/fancynpcs)
  * [SkinsRestorer](https://modrinth.com/plugin/skinsrestorer)

## Setup
1. Install **PlaceholderAPI** and **PacketEvents** on your server.
2. Grab the `CustomScreenMenu` `.jar` from the [Releases]([link](https://github.com/Fries-dev/CustomScreenMenu/releases/tag/new)) tab and drop it into your `plugins` folder.
3. Restart your server to generate the config files.
4. Edit the configs in `/plugins/CustomScreenMenu/` and run `/cmenu reload`.

## Commands & Permissions
Base command: `/cursormenu` (alias: `/cmenu`)

*All permissions default to `op`.*

### General & Menu Controls
* `/cmenu start` (`cursormenu.start`) - Starts/opens the 3D menu
* `/cmenu stop` (`cursormenu.stop`) - Stops the active menu
* `/cmenu close` (`cursormenu.close`) - Closes the current screen
* `/cmenu reload` (`cursormenu.reload`) - Reloads the plugin configuration
* `/cmenu items` / `/cmenu itemsstop` (`cursormenu.items`, `cursormenu.itemsstop`) - Toggles item display elements
* `/cmenu text` (`cursormenu.text`) - Manages text display elements

### Auth / Login Screen
* `/cmenu login` (`cursormenu.login`) - Triggers the login screen (`cursormenu.menu.login`)
* `/cmenu resetpassword` (`cursormenu.resetpassword`) - Resets a player's password
* `/cmenu input` (`cursormenu.input`) - Handles text input fields (`cursormenu.set_input_field`)

### NPC Integration
* `/cmenu npc toggle` (`cursormenu.npc.toggle`) - Toggles NPC menu triggers
* `/cmenu npc status` (`cursormenu.npc.status`) - Checks NPC integration status
* `/cmenu npc reload` (`cursormenu.npc.reload`) - Reloads NPC settings
* `/cmenu npc rotate` (`cursormenu.npc.rotate`) - Rotates the NPC display

*(Note: Internal UI button triggers also use permissions like `cursormenu.register_confirm`, `cursormenu.check_registered`, `cursormenu.check_and_register`, `cursormenu.check_password_before_login`, and `cursormenu.toggle_password_visibility`.)*

## Credits
* Original plugin by **野比大雄** ([Original Repo]([link](https://github.com/JSGDJD/)))
* English remake by [Kevin Fries]([link](https://github.com/Fries-dev/))
