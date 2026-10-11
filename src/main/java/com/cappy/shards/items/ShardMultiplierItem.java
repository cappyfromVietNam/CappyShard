package com.cappy.shards.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public class ShardMultiplierItem {

    public static final NamespacedKey MULTIPLIER_KEY = new NamespacedKey(CappyShards.getInstance(), "shard_multiplier");
    public static final NamespacedKey DURATION_KEY = new NamespacedKey(CappyShards.getInstance(), "multiplier_duration");

    public static ItemStack create(double multiplier, int durationMinutes) {
        ItemStack bottle = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) bottle.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.WATER);
            meta.displayName(ChatUtils.parse("<gradient:#FFAA00:#FF5500><bold>Shard Multiplier</bold></gradient>").decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(ChatUtils.parse("<dark_gray>------------------------</dark_gray>").decoration(TextDecoration.ITALIC, false));
            lore.add(ChatUtils.parse("<gray>Boost: <gold>x" + (multiplier % 1 == 0 ? (long) multiplier : multiplier) + "</gold></gray>").decoration(TextDecoration.ITALIC, false));
            lore.add(ChatUtils.parse("<gray>Duration: <gold>" + durationMinutes + "m</gold></gray>").decoration(TextDecoration.ITALIC, false));
            lore.add(ChatUtils.parse("").decoration(TextDecoration.ITALIC, false));
            lore.add(ChatUtils.parse("<yellow>» Right-Click to instantly activate!</yellow>").decoration(TextDecoration.ITALIC, false));
            lore.add(ChatUtils.parse("<dark_gray>------------------------</dark_gray>").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(MULTIPLIER_KEY, PersistentDataType.DOUBLE, multiplier);
            pdc.set(DURATION_KEY, PersistentDataType.INTEGER, durationMinutes);

            bottle.setItemMeta(meta);
        }
        return bottle;
    }

    public static boolean isMultiplierBottle(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(MULTIPLIER_KEY, PersistentDataType.DOUBLE);
    }

    public static double getMultiplier(ItemStack item) {
        if (!isMultiplierBottle(item)) return 1.0;
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) return 1.0;
        Double val = meta.getPersistentDataContainer().get(MULTIPLIER_KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 1.0;
    }

    public static int getDurationMinutes(ItemStack item) {
        if (!isMultiplierBottle(item)) return 30;
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) return 30;
        Integer val = meta.getPersistentDataContainer().get(DURATION_KEY, PersistentDataType.INTEGER);
        return val != null ? val : 30;
    }
}

