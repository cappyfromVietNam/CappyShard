package com.cappy.shards.items;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
        if (CappyShards.getInstance() != null && CappyShards.getInstance().getItemManager() != null) {
            return CappyShards.getInstance().getItemManager().createMultiplierItem(multiplier, durationMinutes);
        }
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

    public record MultiplierData(double multiplier, int durationMinutes) {}

    public static MultiplierData getMultiplierData(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Double mult = pdc.get(MULTIPLIER_KEY, PersistentDataType.DOUBLE);
        if (mult == null) return null;
        Integer dur = pdc.get(DURATION_KEY, PersistentDataType.INTEGER);
        return new MultiplierData(mult, dur != null ? dur : 30);
    }

    public static boolean isMultiplierBottle(ItemStack item) {
        return getMultiplierData(item) != null;
    }

    public static double getMultiplier(ItemStack item) {
        MultiplierData data = getMultiplierData(item);
        return data != null ? data.multiplier() : 1.0;
    }

    public static int getDurationMinutes(ItemStack item) {
        MultiplierData data = getMultiplierData(item);
        return data != null ? data.durationMinutes() : 30;
    }
}

