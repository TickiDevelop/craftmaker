# ✨ CraftMaker

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.42-blue)
![Java](https://img.shields.io/badge/Java-21-orange)

A powerful Minecraft 1.21.11 mod on NeoForge that allows server administrators to create custom crafting recipes directly in-game through an intuitive graphical interface.

## 🎯 Features

- **In-game Recipe Editor** - Create shaped 3x3 crafting recipes with drag-and-drop
- **Recipe Priority System** - CraftMaker recipes override vanilla recipes
- **Smart Recipe Management** - View, search, and delete recipes with fuzzy matching
- **Network Synchronization** - Automatic client-server recipe sync
- **Intuitive Controls** - Left-click for full stack, right-click for single item

## 📖 Usage

### Commands

- `/cm` or `/craftmaker` - Opens the main menu
- `/cm reload` - Reloads all recipes from the server

### Creating a Recipe

1. Open the menu with `/cm`
2. Click "Add Recipe"
3. Fill the 3x3 ingredient grid with items
4. Set the result item and count
5. Enter a recipe name (auto-generated if empty)
6. Click "Save"

### Managing Recipes

1. Open the menu with `/cm`
2. Click "Recipe List"
3. Search recipes using the search bar
4. Click "✕" to delete a recipe

## 🛠️ Installation

### Requirements
- Minecraft 1.21.11
- NeoForge 21.1.42
- Java 21

### Setup
1. Download the latest version
2. Place the JAR file in the server's `mods/` folder
3. Start the server
4. Use `/cm` to open the menu

## ⚙️ Configuration

Edit `config/craftmaker-common.toml` to mod settings:
- `showWelcomeMessage` - Show version message on join (default: true)

## � Support

[GitHub Repository](https://github.com/TickiDevelop/craftmaker)

---
