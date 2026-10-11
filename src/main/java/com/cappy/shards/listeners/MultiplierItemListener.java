package com.cappy.shards.listeners;

import com.cappy.shards.CappyShards;
import com.cappy.shards.items.ShardMultiplierItem;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class MultiplierItemListener implements Listener {

    private final CappyShards plugin;

    public MultiplierItemListener(CappyShards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (ShardMultiplierItem.isMultiplierBottle(item)) {
            Player player = event.getPlayer();
            double multiplier = ShardMultiplierItem.getMultiplier(item);
            int duration = ShardMultiplierItem.getDurationMinutes(item);

            plugin.getAfkRewardManager().activateMultiplier(player.getUniqueId(), multiplier, duration);

            String formattedMult = (multiplier % 1 == 0 ? String.valueOf((long) multiplier) : String.valueOf(multiplier));
            String rawMsg = plugin.getMessage("multiplier-activated",
                    "<green>You activated a <gold>x%multiplier%</gold> Shard Multiplier for <white>%duration%</white> minutes!</green>");
            String formattedMsg = rawMsg
                    .replace("%multiplier%", formattedMult)
                    .replace("%duration%", String.valueOf(duration));
            ChatUtils.sendMessage(player, formattedMsg);
        }
    }
}
