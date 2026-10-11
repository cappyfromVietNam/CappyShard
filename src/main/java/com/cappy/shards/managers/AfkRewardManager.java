package com.cappy.shards.managers;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkRewardManager {

    private final CappyShards plugin;
    private final ConcurrentHashMap<UUID, Long> nextRewardTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> rewardDisplayUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> activeRewardMessage = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> activeMultipliers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> multiplierExpiry = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int defaultInterval;
    private List<String> enabledWorlds;
    private String actionbarMsg;
    private String rewardMsg;

    public AfkRewardManager(CappyShards plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        defaultInterval = plugin.getConfig().getInt("afk-settings.interval-seconds", 60);
        enabledWorlds = plugin.getConfig().getStringList("afk-settings.enabled-worlds");
        actionbarMsg = plugin.getConfig().getString("afk-settings.actionbar-message", "<yellow>Receiving Shards in: <white>%time%s</white></yellow>");
        rewardMsg = plugin.getConfig().getString("afk-settings.actionbar-reward-message", "<green>You received %amount% Shards for being AFK!</green>");
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("afk-settings.enabled", true)) return;

        // Run every 10 ticks (0.5s) to guarantee Action Bar priority over other plugins in the AFK world
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();

            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();

                boolean bypass = player.hasPermission("cappyshards.bypass.zone");
                boolean inWorld = enabledWorlds.contains(player.getWorld().getName());

                if (!bypass && !inWorld) {
                    nextRewardTimes.remove(uuid);
                    rewardDisplayUntil.remove(uuid);
                    activeRewardMessage.remove(uuid);
                    continue;
                }

                long nextTime = nextRewardTimes.computeIfAbsent(uuid, k -> now + (long) defaultInterval * 1000L);

                if (now >= nextTime) {
                    // Reset interval timer
                    nextRewardTimes.put(uuid, now + (long) defaultInterval * 1000L);
                    // Reward on Main Thread
                    Bukkit.getScheduler().runTask(plugin, () -> rewardPlayer(player));
                } else {
                    // If in reward banner display window (3 seconds), display reward banner
                    Long showUntil = rewardDisplayUntil.get(uuid);
                    if (showUntil != null && now < showUntil) {
                        String activeMsg = activeRewardMessage.get(uuid);
                        if (activeMsg != null) {
                            player.sendActionBar(ChatUtils.parse(activeMsg));
                        }
                    } else {
                        // Priority countdown action bar refresh
                        long remainingSec = Math.max(1, (nextTime - now + 999L) / 1000L);
                        double mult = getActiveMultiplier(uuid);
                        String actionText = actionbarMsg.replace("%time%", String.valueOf(remainingSec));
                        if (mult > 1.0) {
                            actionText += " <gold>(x" + (mult % 1 == 0 ? (int) mult : mult) + ")</gold>";
                        }
                        player.sendActionBar(ChatUtils.parse(actionText));
                    }
                }
            }
        }, 10L, 10L); // 0.5s priority interval
    }

    private void rewardPlayer(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        long rewardAmount = 0;

        if (ranks != null) {
            for (String key : ranks.getKeys(false)) {
                String perm = ranks.getString(key + ".permission");
                if (perm != null && player.hasPermission(perm)) {
                    rewardAmount = ranks.getLong(key + ".amount", 0);
                    break;
                }
            }
        }

        if (rewardAmount > 0) {
            double multiplier = getActiveMultiplier(player.getUniqueId());
            long finalReward = Math.max(1, Math.round(rewardAmount * multiplier));
            plugin.getPlayerDataManager().addShards(player.getUniqueId(), finalReward);

            String formattedReward = rewardMsg.replace("%amount%", String.valueOf(finalReward));
            if (multiplier > 1.0) {
                formattedReward += " <gold>(x" + (multiplier % 1 == 0 ? (int) multiplier : multiplier) + " Boosted)</gold>";
            }

            activeRewardMessage.put(player.getUniqueId(), formattedReward);
            rewardDisplayUntil.put(player.getUniqueId(), System.currentTimeMillis() + 3000L);
            player.sendActionBar(ChatUtils.parse(formattedReward));
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        nextRewardTimes.clear();
        rewardDisplayUntil.clear();
        activeRewardMessage.clear();
        activeMultipliers.clear();
        multiplierExpiry.clear();
    }

    public void addPlayer(Player player) {
        boolean bypass = player.hasPermission("cappyshards.bypass.zone");
        boolean inWorld = enabledWorlds.contains(player.getWorld().getName());

        if (bypass || inWorld) {
            nextRewardTimes.putIfAbsent(player.getUniqueId(), System.currentTimeMillis() + (long) defaultInterval * 1000L);
        }
    }

    public void removePlayer(UUID uuid) {
        nextRewardTimes.remove(uuid);
        rewardDisplayUntil.remove(uuid);
        activeRewardMessage.remove(uuid);
    }

    public long getRemainingSeconds(UUID uuid) {
        Long nextTime = nextRewardTimes.get(uuid);
        if (nextTime == null) return 0;
        long now = System.currentTimeMillis();
        return Math.max(0, (nextTime - now + 999L) / 1000L);
    }

    public long getRewardAmount(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks != null) {
            for (String key : ranks.getKeys(false)) {
                String perm = ranks.getString(key + ".permission");
                if (perm != null && player.hasPermission(perm)) {
                    return ranks.getLong(key + ".amount", 0);
                }
            }
        }
        return 0;
    }

    public void activateMultiplier(UUID uuid, double multiplier, int durationMinutes) {
        long durationMillis = (long) durationMinutes * 60 * 1000L;
        activeMultipliers.put(uuid, multiplier);
        multiplierExpiry.put(uuid, System.currentTimeMillis() + durationMillis);
    }

    public double getActiveMultiplier(UUID uuid) {
        Long expiry = multiplierExpiry.get(uuid);
        if (expiry == null || System.currentTimeMillis() >= expiry) {
            activeMultipliers.remove(uuid);
            multiplierExpiry.remove(uuid);
            return 1.0;
        }
        return activeMultipliers.getOrDefault(uuid, 1.0);
    }

    public long getMultiplierRemainingSeconds(UUID uuid) {
        Long expiry = multiplierExpiry.get(uuid);
        if (expiry == null) return 0;
        long diff = expiry - System.currentTimeMillis();
        return diff > 0 ? (diff + 999L) / 1000L : 0;
    }
}

