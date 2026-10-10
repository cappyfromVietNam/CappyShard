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
    private final ConcurrentHashMap<UUID, Integer> countdowns = new ConcurrentHashMap<>();
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
        actionbarMsg = plugin.getConfig().getString("afk-settings.actionbar-message", "<yellow>Nhận Shard sau: <white>%time%s</white></yellow>");
        rewardMsg = plugin.getConfig().getString("afk-settings.actionbar-reward-message", "<green>Bạn vừa nhận được %amount% Shard từ AFK!</green>");
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("afk-settings.enabled", true)) return;

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, Integer> entry : countdowns.entrySet()) {
                UUID uuid = entry.getKey();
                Player player = Bukkit.getPlayer(uuid);
                
                if (player == null || !player.isOnline()) {
                    countdowns.remove(uuid);
                    continue;
                }

                // Check world condition
                boolean bypass = player.hasPermission("cappyshards.bypass.zone");
                boolean inWorld = enabledWorlds.contains(player.getWorld().getName());

                if (!bypass && !inWorld) {
                    countdowns.remove(uuid);
                    continue;
                }

                int current = entry.getValue() - 1;
                
                if (current <= 0) {
                    // Reward player on Main Thread
                    Bukkit.getScheduler().runTask(plugin, () -> rewardPlayer(player));
                    countdowns.put(uuid, defaultInterval);
                } else {
                    countdowns.put(uuid, current);
                    player.sendActionBar(ChatUtils.parse(actionbarMsg.replace("%time%", String.valueOf(current))));
                }
            }
            
            // Periodically check if anyone needs to be added (if they entered the world)
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!countdowns.containsKey(p.getUniqueId())) {
                    addPlayer(p);
                }
            }
            
        }, 20L, 20L); // 1 second interval
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
            plugin.getPlayerDataManager().addShards(player.getUniqueId(), rewardAmount);
            player.sendActionBar(ChatUtils.parse(rewardMsg.replace("%amount%", String.valueOf(rewardAmount))));
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        countdowns.clear();
    }

    public void addPlayer(Player player) {
        boolean bypass = player.hasPermission("cappyshards.bypass.zone");
        boolean inWorld = enabledWorlds.contains(player.getWorld().getName());
        
        if (bypass || inWorld) {
            countdowns.putIfAbsent(player.getUniqueId(), defaultInterval);
        }
    }

    public void removePlayer(UUID uuid) {
        countdowns.remove(uuid);
    }
}

