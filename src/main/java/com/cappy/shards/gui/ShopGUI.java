package com.cappy.shards.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;

public class ShopGUI {

    private final CappyShards plugin;
    private Inventory inventory;
    private final Map<Integer, ShopItem> shopItems = new HashMap<>();

    public ShopGUI(CappyShards plugin) {
        this.plugin = plugin;
    }

    public void loadShop() {
        shopItems.clear();
        File shopFile = new File(plugin.getDataFolder(), "Shop/shop.yml");
        if (!shopFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
        String title = config.getString("title", "Cửa hàng Shard");
        int size = config.getInt("size", 27);
        
        // Adventure components cannot be directly used in Bukkit.createInventory title natively without Paper component support
        // We use string for now, but Adventure is supported by Paper
        inventory = Bukkit.createInventory(null, size, ChatUtils.parse(title));

        ConfigurationSection itemsSec = config.getConfigurationSection("items");
        if (itemsSec != null) {
            for (String key : itemsSec.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSec.getConfigurationSection(key);
                if (itemConfig != null) {
                    int slot = itemConfig.getInt("slot", 0);
                    long price = itemConfig.getLong("price", 0);
                    Material displayMat = Material.matchMaterial(itemConfig.getString("material", "STONE"));
                    String name = itemConfig.getString("name", "");
                    List<String> lore = itemConfig.getStringList("lore");
                    
                    Material giveMat = Material.matchMaterial(itemConfig.getString("give-material", "STONE"));
                    int giveAmt = itemConfig.getInt("give-amount", 1);
                    String giveName = itemConfig.getString("give-name", "");

                    ShopItem shopItem = new ShopItem(price, giveMat, giveAmt, giveName);
                    shopItems.put(slot, shopItem);

                    ItemStack displayItem = new ItemStack(displayMat == null ? Material.STONE : displayMat);
                    ItemMeta meta = displayItem.getItemMeta();
                    if (meta != null) {
                        if (!name.isEmpty()) meta.displayName(ChatUtils.parse(name));
                        List<net.kyori.adventure.text.Component> advLore = new ArrayList<>();
                        for (String l : lore) {
                            advLore.add(ChatUtils.parse(l));
                        }
                        meta.lore(advLore);
                        displayItem.setItemMeta(meta);
                    }
                    inventory.setItem(slot, displayItem);
                }
            }
        }
    }

    public void open(Player player) {
        if (inventory != null) {
            player.openInventory(inventory);
        }
    }

    public ShopItem getItem(int slot) {
        return shopItems.get(slot);
    }
    
    public Inventory getInventory() {
        return inventory;
    }

    public static class ShopItem {
        private final long price;
        private final Material material;
        private final int amount;
        private final String name;

        public ShopItem(long price, Material material, int amount, String name) {
            this.price = price;
            this.material = material;
            this.amount = amount;
            this.name = name;
        }

        public long getPrice() { return price; }
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public String getName() { return name; }
    }
}

