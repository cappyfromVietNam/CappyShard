<div align="center">

# 💎 CappyShards

### **The Ultimate Crystal & Shard Economy, AFK Rewards & Smart GUI Shop for PaperMC**

*Lightweight • 100% Vanilla Client Compatible • Ultra-Secure Anti-Dupe Engine • Asynchronous SQLite*

---

[![PaperMC](https://img.shields.io/badge/Paper-1.21+-F44336?style=for-the-badge&logo=papermc&logoColor=white)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![SQLite](https://img.shields.io/badge/SQLite-HikariCP_Async-003B57?style=for-the-badge&logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![PlaceholderAPI](https://img.shields.io/badge/PlaceholderAPI-Supported-4CAF50?style=for-the-badge)](https://wiki.placeholderapi.com/)
[![bStats](https://img.shields.io/badge/bStats-34192-008CBA?style=for-the-badge)](https://bstats.org/plugin/bukkit/CappyShards/34192)
[![Modrinth](https://img.shields.io/badge/Modrinth-Available-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/)

[**Features**](#-features) • [**Quick Start**](#-quick-start) • [**In-Game Shop**](#-5-smart-in-game-shop) • [**Shard Multiplier**](#-shard-multiplier-boosters) • [**Placeholders**](#-placeholderapi-integration) • [**Commands & Permissions**](#-commands--permissions) • [**Configuration**](#-configuration-files)

<br>

🌐 **Languages:** [English](README.md) • [Tiếng Việt](README_VI.md)

</div>

<br>

---

## 🌟 Why CappyShards?

Tired of clunky economy plugins that cause TPS lag, leak memory, or get exploited by inventory click hacks? 

**CappyShards** is an advanced, production-ready server solution crafted from the ground up for modern Paper/Purpur 1.21+ Minecraft networks. It introduces a secondary premium economy (**Shards**), an AFK reward zone with an action bar display that cannot be overridden by other plugins, an intuitive in-game shop with instant unit & stack adjustments, functional consumable boosters, and bulletproof concurrency protection.

**Best of all? No client mods or resource packs required.** Everything runs seamlessly on vanilla Minecraft clients!

---

## 🚀 Features

### ⚡ 1. High-Performance Shard Economy
- **Async Database Transactions:** Backed by SQLite and **HikariCP** connection pooling. Zero lag or freezing on the main game thread.
- **Atomic Anti-Dupe Locking:** Uses thread-safe atomic flags (`AtomicBoolean`) preventing fast-click dupes, inventory drag exploits, and packet glitches.
- **Player-to-Player Transfers:** Secure `/shard pay` command with self-transfer prevention, balance validation, and transaction concurrency guards.

### ⏳ 2. AFK Shard Rewards & Dominant Action Bar
- **Ready Out-of-the-Box:** Pre-configured for world `ShardAFK`.
- **TPS-Immune Time Tracking:** Calculated using millisecond epoch timestamps (`System.currentTimeMillis()`) so server lag spikes won't drift or delay player countdowns.
- **Priority Action Bar Engine:** Pulses every **0.5s (10 ticks)** asynchronously, guaranteeing your AFK countdown remains visible even when other plugins send action bar messages.
- **Congratulatory Reward Banner:** Displays an eye-catching reward message for 3 seconds before smoothly returning to the countdown.
- **Rank Multipliers:** Reward VIPs and donors with customizable payout amounts per tier.

### 🛡️ 3. Built-In Network Security
- **Anti-Alt Protection:** Limits maximum accounts allowed per IP simultaneously (configurable in `config.yml`).
- **Anti-VPN & Proxy Shield:** Performs asynchronous pre-login HTTP checks with IP intelligence APIs (e.g. `proxycheck.io`) to reject bots and VPN users before they join.

### 🧪 4. Shard Multiplier Consumables
- **Instant-Use Potions:** Custom Water Bottles that immediately grant a timed Shard earnings multiplier upon right-click.
- **No Drinking Delay:** Consumes instantly without the 1.6-second vanilla drinking animation.
- **Visual & Audio Feedback:** Plays level-up chimes and sparkles upon activation.
- **Fully Customizable:** Configure textures, custom model data, names, lores, sounds, and particles in `item.yml`.

### 🛒 5. Smart In-Game Shop
An intuitive, player-friendly shopping experience without needing custom texture packs:
- **Clean Category Browsing:** Organize items into custom departments (Weapons, Consumables, Materials, etc.) with customizable icons and color gradients.
- **Unit & Bulk Stack Purchasing:** Players can easily switch between buying single items (1 to 64) or bulk stacks (1 to 36) with simple `+` and `-` buttons.
- **Smart Stack Detection:** Non-stackable equipment (swords, armor, tools) automatically disables stack mode to prevent issues.
- **Instant Purchase Preview:** Clear summary showing selected quantity, total cost, and current balance before confirming.
- **Virtual Rewards & Commands:** Supports optional console `commands` in `shop.yml` for giving Crate Keys, ranks, or custom plugin items.
- **Inventory Safe:** Automatically checks available inventory space before transactions to avoid dropped items.

### 💤 6. Offline Shard Accumulation
- **Passive Offline Progress:** Players can accumulate Shards even while completely logged off!
- **Rank-Based Duration Caps:** Configurable maximum accumulation time per rank in `config.yml` (e.g. 2h for Default, 6h for VIP, 12h for VIP+).
- **Flexible Permission Control:** Grant globally using `cappy.offline.shard` or per-rank with `cappy.offline.shard.<rank>`.
- **Welcome-Back Notification:** Reconnecting players receive a detailed chat notification showing their offline time and shards earned.

---

## ⚡ Shard Multiplier Boosters

Admins can distribute temporary Shard Multipliers as quest rewards, crate prizes, or store items!

```bash
# Give a 2x Multiplier for 30 minutes to yourself:
/cappy give shard_multiplier 2 30

# Give a 1.5x Multiplier for 60 minutes to a player:
/cappy give Steve shard_multiplier 1.5 60
```

- **Instant Right-Click:** Right-click anywhere to immediately activate the boost.
- **Boosted Payouts:** While active, all AFK rewards are multiplied (e.g. 10 Shards × 2.0 = 20 Shards).
- **HUD Indicator:** The action bar automatically appends the active multiplier (e.g. `Receiving Shards in: 45s (x2)`).

---

## 🧩 PlaceholderAPI Integration

CappyShards provides comprehensive **PlaceholderAPI (PAPI)** placeholders ready for your Scoreboard, TAB list, Chat, Holograms, or DeluxeMenus:

| Placeholder | Description | Example Output |
| :--- | :--- | :--- |
| `%cappyshards_balance%` | Exact Shard balance (raw number) | `1500` |
| `%cappyshards_shards%` | Alias for `%cappyshards_balance%` | `1500` |
| `%cappyshards_balance_formatted%` | Shard balance with comma separators | `1,500` |
| `%cappyshards_shards_formatted%` | Alias for formatted balance | `1,500` |
| `%cappyshards_afk_time%` | Remaining seconds until next AFK reward | `42` |
| `%cappyshards_afk_remaining%` | Alias for AFK countdown | `42` |
| `%cappyshards_rank_reward%` | Shard payout amount based on player's rank | `50` |
| `%cappyshards_multiplier%` | Active Shard multiplier booster (or `1` if none) | `2` |
| `%cappyshards_multiplayer%` | Alias for `%cappyshards_multiplier%` | `2` |
| `%cappyshards_multiplier_formatted%` | Multiplier prefixed with `x` | `x2` |
| `%cappyshards_multiplier_remaining%` | Remaining booster time in seconds | `1799` |
| `%cappyshards_multiplier_time_formatted%` | Human-readable remaining booster time | `29m 59s` |
| `%cappyshards_has_multiplier%` | Check if player has an active booster | `true` / `false` |
| `%cappyshards_multiplier_tag%` | Formatted booster tag (empty if none active) | ` (x2)` |
| `%cappyshards_offline_max_hours%` | Maximum offline shard accumulation hours for the player | `6` |

---

## 🎮 Commands & Permissions

### Commands
| Command | Aliases | Permission | Description |
| :--- | :--- | :--- | :--- |
| `/cappyshard` | `/shard`, `/shards`, `/cshard`, `/cappy` | `cappyshards.use` | View your current Shard balance |
| `/cappyshard pay <player> <amount>` | | `cappyshards.use.pay` | Transfer Shards to another online player |
| `/cappyshard shop` | `/shardshop` | `cappyshards.use.shop` | Open the Shard Catalogue shop GUI |
| `/cappyshard give [player] shard_multiplier <multiplier> [minutes]` | | `cappyshards.admin` | Give an instant Shard Multiplier potion |
| `/cappyshard add <player> <amount>` | | `cappyshards.admin` | Add Shards to a player's balance |
| `/cappyshard set <player> <amount>` | | `cappyshards.admin` | Set a player's exact Shard balance |
| `/cappyshard remove <player> <amount>` | | `cappyshards.admin` | Deduct Shards from a player's balance |
| `/cappyshard reload` | | `cappyshards.admin` | Reload all configs, items, and restart managers |

### Permissions
- `cappyshards.use` *(Default: true)* — Allows checking balance.
- `cappyshards.use.pay` *(Default: true)* — Allows sending Shards to other players.
- `cappyshards.use.shop` *(Default: true)* — Allows opening the Shard Shop GUI.
- `cappyshards.admin` *(Default: op)* — Administrative access (set, add, remove, give, reload).
- `cappyshards.bypass.zone` *(Default: op)* — Receive AFK rewards regardless of the current world.
- `cappyshards.rank.<name>` — Grants specific rank payout amounts configured in `config.yml`.
- `cappy.offline.shard` — Base permission to accumulate shards while offline.
- `cappy.offline.shard.<rank>` — Rank-specific offline accumulation permissions (`cappy.offline.shard.vipplus`, `cappy.offline.shard.vip`).

---

## 🗂️ Configuration Files

All configurations use modern **MiniMessage** gradient and hex formatting.

- **`config.yml`**: General settings, AFK intervals, enabled worlds, Anti-Alt/VPN, and Rank payout rates.
- **`messages.yml`**: 100% customizable messages, alerts, and prefixes.
- **`item.yml`**: Customize the Shard Multiplier bottle (name, lore, custom model data, sounds, and particles).
- **`Shop/catalogue.yml`**: Configure catalogue categories, layout sizes, icons, and lores.
- **`Shop/shop.yml`**: Configure items in each category, prices, quantities, and optional console reward commands.

---

## 📥 Quick Start

1. Download the latest `CappyShards-1.0.0.jar` from [Releases](https://github.com/cappyfromVietNam/CappyShard/releases) or [Modrinth](https://modrinth.com/).
2. Place the jar file into your server's `plugins/` folder.
3. *(Optional)* Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) for scoreboard & tab integration.
4. Start your server. Configuration files will generate automatically.
5. Customize `config.yml`, `messages.yml`, `item.yml`, and `Shop/shop.yml` to fit your server's needs.
6. Run `/cappyshard reload` to apply your changes on the fly!

---

## 🛠️ Building from Source

```bash
git clone https://github.com/cappyfromVietNam/CappyShard.git
cd CappyShard
./gradlew build
```
The compiled jar will be available in `build/libs/CappyShards-1.0.0.jar`.

---

<div align="center">

**CappyShards** is an open-source project created with ❤️ by the **Cappy Community**.  
If you enjoy this plugin, consider starring the repository on GitHub!

</div>