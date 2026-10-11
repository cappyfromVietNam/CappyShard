package com.cappy.shards.managers;

import com.cappy.shards.CappyShards;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RankManager {

    public record RankData(
            String id,
            String permission,
            String offlinePermission,
            long amount,
            double maxOfflineHours
    ) {}

    private final CappyShards plugin;
    private List<RankData> cachedRanks = Collections.emptyList();

    public RankManager(CappyShards plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        ConfigurationSection ranksSec = plugin.getConfig().getConfigurationSection("ranks");
        if (ranksSec == null) {
            cachedRanks = Collections.emptyList();
            return;
        }

        List<RankData> list = new ArrayList<>();
        for (String key : ranksSec.getKeys(false)) {
            ConfigurationSection sec = ranksSec.getConfigurationSection(key);
            if (sec == null) continue;

            String perm = sec.getString("permission");
            String offlinePerm = sec.getString("offline-permission");
            long amount = sec.getLong("amount", 10);
            double maxOffline = sec.getDouble("max-offline-hours", 0.0);

            list.add(new RankData(key, perm, offlinePerm, amount, maxOffline));
        }
        this.cachedRanks = Collections.unmodifiableList(list);
    }

    public long getRewardAmount(Player player) {
        for (RankData rank : cachedRanks) {
            if (rank.permission() != null && player.hasPermission(rank.permission())) {
                return rank.amount();
            }
        }
        return 0;
    }

    public RankData getMatchedOfflineRank(Player player) {
        for (RankData rank : cachedRanks) {
            if (rank.maxOfflineHours() <= 0) continue;

            boolean hasSpecific = rank.offlinePermission() != null &&
                    !rank.offlinePermission().isEmpty() &&
                    player.hasPermission(rank.offlinePermission());

            boolean hasBaseAndRank = player.hasPermission("cappy.offline.shard") &&
                    rank.permission() != null &&
                    !rank.permission().isEmpty() &&
                    player.hasPermission(rank.permission());

            if (hasSpecific || hasBaseAndRank) {
                return rank;
            }
        }
        return null;
    }

    public double getMaxOfflineHours(Player player) {
        RankData matched = getMatchedOfflineRank(player);
        return matched != null ? matched.maxOfflineHours() : 0.0;
    }

    public List<RankData> getCachedRanks() {
        return cachedRanks;
    }
}

