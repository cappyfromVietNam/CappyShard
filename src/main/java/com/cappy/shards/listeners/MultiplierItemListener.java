package com.cappy.shards.listeners;

import com.cappy.shards.CappyShards;
import com.cappy.shards.items.ShardMultiplierItem;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MultiplierItemListener implements Listener {

    private final CappyShards plugin;

    public MultiplierItemListener(CappyShards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;
        if (event.getHand() == null) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !ShardMultiplierItem.isMultiplierBottle(item)) return;

        // Cancel event to prevent vanilla block interaction and potion drinking animation
        event.setCancelled(true);

        // Prevent off-hand duplicate trigger if main hand also holds a multiplier bottle
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (ShardMultiplierItem.isMultiplierBottle(mainHand)) {
                return;
            }
        }

        double multiplier = ShardMultiplierItem.getMultiplier(item);
        int duration = ShardMultiplierItem.getDurationMinutes(item);

        // Instantly deduct 1 bottle from the stack
        item.subtract(1);

        // Instant audio & visual feedback
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.6f);
        player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.2, 0), 16, 0.3, 0.4, 0.3, 0.05);

        // Activate multiplier in manager
        plugin.getAfkRewardManager().activateMultiplier(player.getUniqueId(), multiplier, duration);

        String formattedMult = (multiplier % 1 == 0 ? String.valueOf((long) multiplier) : String.valueOf(multiplier));
        String rawMsg = plugin.getMessage("multiplier-activated",
                "<green>You activated a <gold>x%multiplier%</gold> Shard Multiplier for <white>%duration%</white> minutes!</green>");
        String formattedMsg = rawMsg
                .replace("%multiplier%", formattedMult)
                .replace("%duration%", String.valueOf(duration));
        ChatUtils.sendMessage(player, formattedMsg);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        if (ShardMultiplierItem.isMultiplierBottle(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
