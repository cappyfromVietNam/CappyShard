package com.cappy.shards.managers;

import com.cappy.shards.CappyShards;
import com.cappy.shards.items.ShardMultiplierItem;
import com.cappy.shards.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final CappyShards plugin;
    private YamlConfiguration itemConfig;

    private Sound multiplierSound = Sound.ENTITY_PLAYER_LEVELUP;
    private Particle multiplierParticle = Particle.HAPPY_VILLAGER;
    private int multiplierParticleCount = 16;

    public ItemManager(CappyShards plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File file = new File(plugin.getDataFolder(), "item.yml");
        if (!file.exists()) {
            File altFile = new File(plugin.getDataFolder(), "items.yml");
            if (altFile.exists()) {
                file = altFile;
            }
        }

        if (file.exists()) {
            itemConfig = YamlConfiguration.loadConfiguration(file);
        } else {
            plugin.saveResource("item.yml", false);
            file = new File(plugin.getDataFolder(), "item.yml");
            itemConfig = YamlConfiguration.loadConfiguration(file);
        }

        // Cache sounds and particles
        ConfigurationSection sec = itemConfig.getConfigurationSection("items.shard_multiplier");
        if (sec != null) {
            String soundStr = sec.getString("sound", "ENTITY_PLAYER_LEVELUP");
            try {
                multiplierSound = (!soundStr.equalsIgnoreCase("NONE") && !soundStr.isEmpty()) ? Sound.valueOf(soundStr.toUpperCase()) : null;
            } catch (Exception e) {
                multiplierSound = Sound.ENTITY_PLAYER_LEVELUP;
            }

            String partStr = sec.getString("particle", "HAPPY_VILLAGER");
            try {
                multiplierParticle = (!partStr.equalsIgnoreCase("NONE") && !partStr.isEmpty()) ? Particle.valueOf(partStr.toUpperCase()) : null;
            } catch (Exception e) {
                multiplierParticle = Particle.HAPPY_VILLAGER;
            }

            multiplierParticleCount = sec.getInt("particle-count", 16);
        }
    }

    public ItemStack createMultiplierItem(double multiplier, int durationMinutes) {
        ConfigurationSection sec = itemConfig != null ? itemConfig.getConfigurationSection("items.shard_multiplier") : null;

        String matStr = sec != null ? sec.getString("material", "POTION") : "POTION";
        Material material = Material.matchMaterial(matStr);
        if (material == null) material = Material.POTION;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String multStr = (multiplier % 1 == 0 ? String.valueOf((long) multiplier) : String.valueOf(multiplier));

        // Potion type if potion
        if (meta instanceof PotionMeta potionMeta) {
            String pTypeStr = sec != null ? sec.getString("potion-type", "WATER") : "WATER";
            try {
                potionMeta.setBasePotionType(PotionType.valueOf(pTypeStr.toUpperCase()));
            } catch (Exception ignored) {
                potionMeta.setBasePotionType(PotionType.WATER);
            }
        }

        // Custom Model Data
        int cmd = sec != null ? sec.getInt("custom-model-data", 0) : 0;
        if (cmd > 0) {
            meta.setCustomModelData(cmd);
        }

        // Display name
        String rawName = sec != null ? sec.getString("name", "<gradient:#FFAA00:#FF5500><bold>Shard Multiplier</bold></gradient>") : "<gradient:#FFAA00:#FF5500><bold>Shard Multiplier</bold></gradient>";
        rawName = rawName.replace("%multiplier%", multStr).replace("%duration%", String.valueOf(durationMinutes));
        meta.displayName(ChatUtils.parse(rawName).decoration(TextDecoration.ITALIC, false));

        // Lore
        List<String> rawLore = sec != null ? sec.getStringList("lore") : null;
        if (rawLore == null || rawLore.isEmpty()) {
            rawLore = List.of(
                    "<dark_gray>------------------------</dark_gray>",
                    "<gray>Boost: <gold>x%multiplier%</gold></gray>",
                    "<gray>Duration: <gold>%duration%m</gold></gray>",
                    "",
                    "<yellow>» Right-Click to instantly activate!</yellow>",
                    "<dark_gray>------------------------</dark_gray>"
            );
        }

        List<Component> compLore = new ArrayList<>();
        for (String line : rawLore) {
            String formatted = line.replace("%multiplier%", multStr).replace("%duration%", String.valueOf(durationMinutes));
            compLore.add(ChatUtils.parse(formatted).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);

        // PDC storage
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ShardMultiplierItem.MULTIPLIER_KEY, PersistentDataType.DOUBLE, multiplier);
        pdc.set(ShardMultiplierItem.DURATION_KEY, PersistentDataType.INTEGER, durationMinutes);

        item.setItemMeta(meta);
        return item;
    }

    public Sound getMultiplierSound() {
        return multiplierSound;
    }

    public Particle getMultiplierParticle() {
        return multiplierParticle;
    }

    public int getMultiplierParticleCount() {
        return multiplierParticleCount;
    }

    public YamlConfiguration getItemConfig() {
        return itemConfig;
    }
}
