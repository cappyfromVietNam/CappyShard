package com.cappy.shards.hook;

import java.text.NumberFormat;
import java.util.Locale;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cappy.shards.CappyShards;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class CappyShardsExpansion extends PlaceholderExpansion {

    private final CappyShards plugin;

    public CappyShardsExpansion(CappyShards plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cappyshards";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        long balance = plugin.getPlayerDataManager().getShards(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "balance", "shards" -> String.valueOf(balance);
            case "balance_formatted", "shards_formatted" -> NumberFormat.getInstance(Locale.US).format(balance);
            case "afk_time", "afk_remaining" -> {
                if (player.isOnline()) {
                    long remaining = plugin.getAfkRewardManager().getRemainingSeconds(player.getUniqueId());
                    yield String.valueOf(remaining);
                }
                yield "0";
            }
            case "rank_reward" -> {
                if (player instanceof Player onlinePlayer) {
                    yield String.valueOf(plugin.getAfkRewardManager().getRewardAmount(onlinePlayer));
                }
                yield "0";
            }
            case "multiplier" -> {
                double mult = plugin.getAfkRewardManager().getActiveMultiplier(player.getUniqueId());
                yield mult % 1 == 0 ? String.valueOf((long) mult) : String.valueOf(mult);
            }
            case "multiplier_remaining", "multiplier_time" -> {
                yield String.valueOf(plugin.getAfkRewardManager().getMultiplierRemainingSeconds(player.getUniqueId()));
            }
            default -> null;
        };
    }
}

