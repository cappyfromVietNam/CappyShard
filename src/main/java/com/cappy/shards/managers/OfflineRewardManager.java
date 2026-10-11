package com.cappy.shards.managers;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.entity.Player;

public class OfflineRewardManager {

    private final CappyShards plugin;

    public OfflineRewardManager(CappyShards plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        // Handled via RankManager
    }

    public void processOfflineReward(Player player, long lastQuit) {
        if (lastQuit <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long offlineMillis = now - lastQuit;

        // Ignore if player was offline for less than 1 minute
        if (offlineMillis < 60_000L) {
            return;
        }

        RankManager.RankData matchedRank = plugin.getRankManager().getMatchedOfflineRank(player);
        if (matchedRank == null || matchedRank.amount() <= 0 || matchedRank.maxOfflineHours() <= 0) {
            return;
        }

        long maxMillis = (long) (matchedRank.maxOfflineHours() * 3600.0 * 1000.0);
        long effectiveMillis = Math.min(offlineMillis, maxMillis);

        int intervalSeconds = plugin.getConfig().getInt("afk-settings.interval-seconds", 60);
        if (intervalSeconds <= 0) intervalSeconds = 60;

        long effectiveSeconds = effectiveMillis / 1000L;
        long cycles = effectiveSeconds / intervalSeconds;
        long earned = cycles * matchedRank.amount();

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
        return plugin.getRankManager().getMaxOfflineHours(player);
    }
}
