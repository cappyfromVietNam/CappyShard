package com.cappy.shards.managers;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class OfflineRewardManager {

    private final CappyShards plugin;
    private boolean enabled;
    private double defaultMaxHours;

    public OfflineRewardManager(CappyShards plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("offline-settings.enabled", true);
        this.defaultMaxHours = plugin.getConfig().getDouble("offline-settings.default-max-hours", 2.0);
    }

    public void processOfflineReward(Player player, long lastQuit) {
        if (!enabled || lastQuit <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long offlineMillis = now - lastQuit;

        // Ignore if player was offline for less than 1 minute
        if (offlineMillis < 60_000L) {
            return;
        }

        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) {
            return;
        }

        String matchedRank = null;
        double maxHours = defaultMaxHours;
        long amount = 0;

        for (String rankKey : ranks.getKeys(false)) {
            ConfigurationSection sec = ranks.getConfigurationSection(rankKey);
            if (sec == null) continue;

            double rankMaxHours = sec.getDouble("max-offline-hours", 0.0);
            if (rankMaxHours <= 0) {
                continue; // Rank does not support offline accumulation
            }

            String offlinePerm = sec.getString("offline-permission");
            String mainPerm = sec.getString("permission");

            boolean hasSpecific = offlinePerm != null && !offlinePerm.isEmpty() && player.hasPermission(offlinePerm);
            boolean hasBaseAndRank = player.hasPermission("cappy.offline.shard") &&
                    mainPerm != null && !mainPerm.isEmpty() && player.hasPermission(mainPerm);

            if (hasSpecific || hasBaseAndRank) {
                matchedRank = rankKey;
                maxHours = rankMaxHours;
                amount = sec.getLong("amount", 10);
                break;
            }
        }

        if (matchedRank == null || amount <= 0 || maxHours <= 0) {
            return;
        }

        long maxMillis = (long) (maxHours * 3600.0 * 1000.0);
        long effectiveMillis = Math.min(offlineMillis, maxMillis);

        int intervalSeconds = plugin.getConfig().getInt("afk-settings.interval-seconds", 60);
        if (intervalSeconds <= 0) intervalSeconds = 60;

        long effectiveSeconds = effectiveMillis / 1000L;
        long cycles = effectiveSeconds / intervalSeconds;
        long earned = cycles * amount;

        if (earned > 0) {
            plugin.getPlayerDataManager().addShards(player.getUniqueId(), earned);
            plugin.getPlayerDataManager().updateLastQuit(player.getUniqueId(), now);

            long hours = effectiveSeconds / 3600;
            long mins = (effectiveSeconds % 3600) / 60;
            String timeFormatted = hours > 0 ? (hours + "h " + mins + "m") : (mins + "m");

            String rawMsg = plugin.getMessage("offline-reward",
                    "<green>While you were offline, you earned <gold>%amount%</gold> Shards (<white>%time%</white> offline)!</green>");
            String formattedMsg = rawMsg
                .replace("%amount%", String.valueOf(earned))
                .replace("%time%", timeFormatted);
            ChatUtils.sendMessage(player, formattedMsg);
        }
    }

    public double getMaxOfflineHours(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("ranks");
        if (ranks == null) return 0.0;

        for (String rankKey : ranks.getKeys(false)) {
            ConfigurationSection sec = ranks.getConfigurationSection(rankKey);
            if (sec == null) continue;

            double rankMaxHours = sec.getDouble("max-offline-hours", 0.0);
            if (rankMaxHours <= 0) {
                continue;
            }

            String offlinePerm = sec.getString("offline-permission");
            String mainPerm = sec.getString("permission");

            boolean hasSpecific = offlinePerm != null && !offlinePerm.isEmpty() && player.hasPermission(offlinePerm);
            boolean hasBaseAndRank = player.hasPermission("cappy.offline.shard") &&
                    mainPerm != null && !mainPerm.isEmpty() && player.hasPermission(mainPerm);

            if (hasSpecific || hasBaseAndRank) {
                return rankMaxHours;
            }
        }
        return 0.0;
    }
}

