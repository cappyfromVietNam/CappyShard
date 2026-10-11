package com.cappy.shards.gui;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ShopGUI {

    private final CappyShards plugin;

    // Catalogue cache
    private String catalogueTitle = "<gradient:#FFAA00:#FF5500>Shard Shop Catalogue</gradient>";
    private int catalogueSize = 27;
    private final Map<Integer, CatalogueCategory> catalogueCategories = new HashMap<>();

    // Category items cache: categoryId -> (slot -> ShopItem)
    private final Map<String, CategoryData> categories = new HashMap<>();

    public ShopGUI(CappyShards plugin) {
        this.plugin = plugin;
    }

    public void loadShop() {
        catalogueCategories.clear();
        categories.clear();

        // 1. Load Catalogue
        File catFile = new File(plugin.getDataFolder(), "Shop/catalogue.yml");
        if (catFile.exists()) {
            YamlConfiguration catConfig = YamlConfiguration.loadConfiguration(catFile);
            catalogueTitle = catConfig.getString("title", "<gradient:#FFAA00:#FF5500>Shard Shop Catalogue</gradient>");
            catalogueSize = catConfig.getInt("size", 27);

            ConfigurationSection sec = catConfig.getConfigurationSection("categories");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    ConfigurationSection catSec = sec.getConfigurationSection(key);
                    if (catSec != null) {
                        int slot = catSec.getInt("slot", 0);
                        Material mat = Material.matchMaterial(catSec.getString("material", "CHEST"));
                        String name = catSec.getString("name", key);
                        List<String> lore = catSec.getStringList("lore");

                        catalogueCategories.put(slot, new CatalogueCategory(key, slot, mat != null ? mat : Material.CHEST, name, lore));
                    }
                }
            }
        }

        // 2. Load Shop items per category
        File shopFile = new File(plugin.getDataFolder(), "Shop/shop.yml");
        if (shopFile.exists()) {
            YamlConfiguration shopConfig = YamlConfiguration.loadConfiguration(shopFile);
            ConfigurationSection catSec = shopConfig.getConfigurationSection("categories");
            if (catSec != null) {
                for (String catKey : catSec.getKeys(false)) {
                    ConfigurationSection cSec = catSec.getConfigurationSection(catKey);
                    if (cSec != null) {
                        String title = cSec.getString("title", "Shop » " + catKey);
                        int size = cSec.getInt("size", 27);
                        Map<Integer, ShopItem> items = new HashMap<>();

                        ConfigurationSection itemsSec = cSec.getConfigurationSection("items");
                        if (itemsSec != null) {
                            for (String itemKey : itemsSec.getKeys(false)) {
                                ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
                                if (itemSec != null) {
                                    int slot = itemSec.getInt("slot", 0);
                                    Material mat = Material.matchMaterial(itemSec.getString("material", "STONE"));
                                    String name = itemSec.getString("name", "");
                                    List<String> lore = itemSec.getStringList("lore");
                                    long price = itemSec.getLong("price", 10);

                                    Material giveMat = Material.matchMaterial(itemSec.getString("give-material", "STONE"));
                                    int giveAmt = itemSec.getInt("give-amount", 1);
                                    String giveName = itemSec.getString("give-name", "");

                                    ShopItem item = new ShopItem(itemKey, slot, mat != null ? mat : Material.STONE,
                                            name, lore, price, giveMat != null ? giveMat : (mat != null ? mat : Material.STONE), giveAmt, giveName);
                                    items.put(slot, item);
                                }
                            }
                        }
                        categories.put(catKey, new CategoryData(catKey, title, size, items));
                    }
                }
            }
        }
    }

    /**
     * Tier 1: Open Catalogue GUI
     */
    public void open(Player player) {
        openCatalogue(player);
    }

    public void openCatalogue(Player player) {
        CatalogueHolder holder = new CatalogueHolder();
        Inventory inv = Bukkit.createInventory(holder, catalogueSize, ChatUtils.parse(catalogueTitle));
        holder.setInventory(inv);

        // Decorate background with glass
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < catalogueSize; i++) {
            inv.setItem(i, filler);
        }

        // Place categories
        for (CatalogueCategory cat : catalogueCategories.values()) {
            ItemStack item = createItem(cat.material, cat.name, cat.lore);
            inv.setItem(cat.slot, item);
        }

        player.openInventory(inv);
    }

    /**
     * Tier 2: Open Category items GUI
     */
    public void openCategory(Player player, String categoryId) {
        CategoryData catData = categories.get(categoryId);
        if (catData == null) {
            openCatalogue(player);
            return;
        }

        CategoryHolder holder = new CategoryHolder(categoryId);
        Inventory inv = Bukkit.createInventory(holder, catData.size, ChatUtils.parse(catData.title));
        holder.setInventory(inv);

        // Place items
        for (ShopItem item : catData.items.values()) {
            ItemStack stack = createItem(item.material, item.name, item.lore);
            inv.setItem(item.slot, stack);
        }

        // Place Back button at bottom right (size - 1) or bottom middle (size - 5)
        int backSlot = catData.size - 1;
        ItemStack backButton = createItem(Material.ARROW, "<red>« Back to Catalogues</red>",
                List.of("<gray>Click to return to the catalogue menu.</gray>"));
        inv.setItem(backSlot, backButton);

        player.openInventory(inv);
    }

    /**
     * Tier 3: Open Quantity Adjustment & Confirmation GUI
     */
    public void openConfirm(Player player, String categoryId, ShopItem shopItem, int quantity) {
        quantity = Math.max(1, Math.min(quantity, 64));

        ConfirmHolder holder = new ConfirmHolder(categoryId, shopItem, quantity);
        Inventory inv = Bukkit.createInventory(holder, 27, ChatUtils.parse("<dark_gray>Confirm Purchase</dark_gray>"));
        holder.setInventory(inv);

        renderConfirmInventory(inv, player, holder);
        player.openInventory(inv);
    }

    /**
     * Re-renders the contents of the Confirmation GUI when quantity changes
     */
    public void renderConfirmInventory(Inventory inv, Player player, ConfirmHolder holder) {
        ShopItem shopItem = holder.getShopItem();
        int quantity = holder.getQuantity();
        long totalCost = shopItem.getPrice() * quantity;
        long playerBalance = plugin.getPlayerDataManager().getShards(player.getUniqueId());

        // Fill background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Adjustment buttons
        inv.setItem(18, createItem(Material.IRON_NUGGET, "<yellow>Min (1)</yellow>", List.of("<gray>Set quantity to 1</gray>")));
        inv.setItem(10, createItem(Material.RED_STAINED_GLASS_PANE, "<red><bold>-10</bold></red>", List.of("<gray>Decrease quantity by 10</gray>")));
        inv.setItem(11, createItem(Material.RED_CONCRETE, "<red><bold>-1</bold></red>", List.of("<gray>Decrease quantity by 1</gray>")));

        // Center item (Display product with updated quantity & total cost)
        List<String> displayLore = new ArrayList<>();
        displayLore.add("<dark_gray>------------------------</dark_gray>");
        displayLore.add("<gray>Unit Price: <gold>" + shopItem.getPrice() + " Shards</gold></gray>");
        displayLore.add("<gray>Quantity: <white>" + quantity + "</white></gray>");
        displayLore.add("<gray>Total Cost: <gold>" + totalCost + " Shards</gold></gray>");
        displayLore.add("<dark_gray>------------------------</dark_gray>");
        if (playerBalance >= totalCost) {
            displayLore.add("<green>Your Balance: <gold>" + playerBalance + " Shards</gold></green>");
        } else {
            displayLore.add("<red>Your Balance: <gold>" + playerBalance + " Shards</gold> (Not enough!)</red>");
        }

        ItemStack centerItem = createItem(shopItem.material, shopItem.name.isEmpty() ? null : shopItem.name, displayLore);
        centerItem.setAmount(Math.max(1, Math.min(quantity, 64)));
        inv.setItem(13, centerItem);

        inv.setItem(15, createItem(Material.LIME_CONCRETE, "<green><bold>+1</bold></green>", List.of("<gray>Increase quantity by 1</gray>")));
        inv.setItem(16, createItem(Material.LIME_STAINED_GLASS_PANE, "<green><bold>+10</bold></green>", List.of("<gray>Increase quantity by 10</gray>")));
        inv.setItem(26, createItem(Material.GOLD_NUGGET, "<yellow>Max (64)</yellow>", List.of("<gray>Set quantity to 64</gray>")));

        // Cancel button
        inv.setItem(21, createItem(Material.BARRIER, "<red><bold>✖ CANCEL</bold></red>",
                List.of("<gray>Return to items catalogue</gray>")));

        // Confirm button
        Material confirmMat = (playerBalance >= totalCost) ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        String confirmName = (playerBalance >= totalCost) ? "<green><bold>✔ CONFIRM PURCHASE</bold></green>" : "<red><bold>✖ NOT ENOUGH SHARDS</bold></red>";
        List<String> confirmLore = List.of(
                "<gray>Total Cost: <gold>" + totalCost + " Shards</gold></gray>",
                "<yellow>» Click to finalize transaction!</yellow>"
        );
        inv.setItem(23, createItem(confirmMat, confirmName, confirmLore));
    }

    public CatalogueCategory getCatalogueCategory(int slot) {
        return catalogueCategories.get(slot);
    }

    public ShopItem getShopItem(String categoryId, int slot) {
        CategoryData cat = categories.get(categoryId);
        return cat != null ? cat.items.get(slot) : null;
    }

    public CategoryData getCategoryData(String categoryId) {
        return categories.get(categoryId);
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(ChatUtils.parse(name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> compLore = new ArrayList<>();
                for (String l : lore) {
                    compLore.add(ChatUtils.parse(l));
                }
                meta.lore(compLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // --- Data structures & Holders ---

    public static class CatalogueHolder implements InventoryHolder {
        private Inventory inventory;
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public static class CategoryHolder implements InventoryHolder {
        private final String categoryId;
        private Inventory inventory;
        public CategoryHolder(String categoryId) { this.categoryId = categoryId; }
        public String getCategoryId() { return categoryId; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public static class ConfirmHolder implements InventoryHolder {
        private final String categoryId;
        private final ShopItem shopItem;
        private int quantity;
        private Inventory inventory;

        public ConfirmHolder(String categoryId, ShopItem shopItem, int quantity) {
            this.categoryId = categoryId;
            this.shopItem = shopItem;
            this.quantity = quantity;
        }

        public String getCategoryId() { return categoryId; }
        public ShopItem getShopItem() { return shopItem; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public static class CatalogueCategory {
        public final String id;
        public final int slot;
        public final Material material;
        public final String name;
        public final List<String> lore;

        public CatalogueCategory(String id, int slot, Material material, String name, List<String> lore) {
            this.id = id;
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
        }
    }

    public static class CategoryData {
        public final String id;
        public final String title;
        public final int size;
        public final Map<Integer, ShopItem> items;

        public CategoryData(String id, String title, int size, Map<Integer, ShopItem> items) {
            this.id = id;
            this.title = title;
            this.size = size;
            this.items = items;
        }
    }

    public static class ShopItem {
        public final String id;
        public final int slot;
        public final Material material;
        public final String name;
        public final List<String> lore;
        public final long price;
        public final Material giveMaterial;
        public final int giveAmount;
        public final String giveName;

        public ShopItem(String id, int slot, Material material, String name, List<String> lore,
                        long price, Material giveMaterial, int giveAmount, String giveName) {
            this.id = id;
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.price = price;
            this.giveMaterial = giveMaterial;
            this.giveAmount = giveAmount;
            this.giveName = giveName;
        }

        public long getPrice() { return price; }
        public Material getMaterial() { return giveMaterial != null ? giveMaterial : material; }
        public int getAmount() { return giveAmount; }
        public String getName() { return (giveName != null && !giveName.isEmpty()) ? giveName : name; }
    }
}
