# CappyShards

<div align="center">
  <b>Advanced Shard Economy & AFK Rewards Plugin for PaperMC 1.21+</b>
  <br><br>
  
  [![Paper](https://img.shields.io/badge/Paper-1.21+-333333?style=flat-square&logo=papermc)](https://papermc.io/)
  [![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=java)](https://adoptium.net/)
  [![SQLite](https://img.shields.io/badge/SQLite-Async-003B57?style=flat-square&logo=sqlite)](https://www.sqlite.org/)
  [![GitHub Releases](https://img.shields.io/github/v/release/quocdat251107-del/CappyShard?style=flat-square&logo=github&color=blue)](https://github.com/quocdat251107-del/CappyShard/releases)
</div>

<br>

CappyShards is a high-performance, robust, and completely thread-safe Minecraft plugin designed for Paper servers (1.21+ / Java 21). It introduces a custom Shards economy with AFK rewards, custom shops, and strict security measures to prevent duping, alt-accounting, and VPN usage.

---

## 🚀 Features

- 🛡️ **Thread-Safe & Anti-Dupe**: Built with `ConcurrentHashMap` and Atomic Locks to strictly prevent GUI drag-and-drop duping, race conditions, and click-spamming.
- 💾 **Asynchronous SQLite Storage**: Uses HikariCP connection pooling to perform all database transactions asynchronously without blocking the main server thread.
- ⏳ **AFK Shard Rewards**: A customizable countdown loop that gives players Shards for being AFK in specified zones. Includes permission-based rewards (Ranks) and bypasses.
- 🎨 **Modern MiniMessage UI**: Utilizes Adventure's MiniMessage for stunning and performant Action Bars, Chat Messages, and Item Lores.
- 🚫 **Connection Security**: Built-in Anti-Alt (limits max accounts per IP) and Anti-VPN (async HTTP proxy checks on pre-login).
- 🛒 **GUI Shard Shop**: Fully configurable shop system with page configurations and price settings.

## 📥 Installation

1. Download the latest `CappyShards-x.x.x.jar` from the [Releases](https://github.com/quocdat251107-del/CappyShard/releases) page.
2. Drop it into your Paper server's `plugins/` folder.
3. Start the server to generate the configuration files.
4. Modify `config.yml`, `messages.yml`, and `Shop/shop.yml` to your liking.
5. Restart the server or run `/cappyshard reload`.

## 📜 Commands & Permissions

### Commands
- `/cappyshard` (Aliases: `/shard`, `/shards`, `/cshard`) - View your balance.
- `/shardshop` (Alias for `/cappyshard shop`) - Open the shard shop.
- `/cappyshard pay <player> <amount>` - Send shards to another player.
- `/cappyshard set <player> <amount>` - Set a player's shard balance.
- `/cappyshard add <player> <amount>` - Add shards to a player's balance.
- `/cappyshard remove <player> <amount>` - Remove shards from a player's balance.
- `/cappyshard reload` - Reload the plugin configurations.

### Permissions
- `cappyshards.use` - Allows basic use of commands. (Default: true)
- `cappyshards.use.pay` - Allows paying shards to other players. (Default: true)
- `cappyshards.use.shop` - Allows opening the shop GUI. (Default: true)
- `cappyshards.admin` - Access to set/add/remove and reload. (Default: OP)
- `cappyshards.bypass.zone` - Bypass the AFK zone requirement and receive shards anywhere. (Default: OP)
- `cappyshards.rank.<name>` - Used to determine the reward amount for AFK drops (configured in `config.yml`).

## 🛠️ Configuration

The plugin generates several configuration files:

- **`config.yml`**: Configure AFK intervals, enabled worlds, VPN checks, and Rank rewards.
- **`messages.yml`**: Fully customize all chat messages and prefixes using MiniMessage format.
- **`Shop/shop.yml`**: Configure items, slot positions, prices, display names, and lore for the shop.

## ⚙️ Building from Source

To compile the project yourself:

1. Clone the repository: `git clone https://github.com/quocdat251107-del/CappyShard.git`
2. Navigate to the folder: `cd CappyShard`
3. Build using Gradle Wrapper:
   - On Windows: `.\gradlew build`
   - On Linux/Mac: `./gradlew build`
4. The compiled `.jar` file will be located in `build/libs/`.

---
<div align="center">
  <i>Built with ❤️ by <b>Cappy Community</b> for the PaperMC community.</i>
</div>