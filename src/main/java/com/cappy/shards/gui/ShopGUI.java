package com.cappy.shards.gui;

import com.cappy.shards.CappyShards;
import com.cappy.shards.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;

public class ShopGUI {

    private static final ItemStack FILLER_GLASS = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
    private static final ItemStack BACK_BUTTON = createItem(Material.ARROW, "<red>\u00AB Back to Catalogues</red>",
            List.of("<gray>Click to return to the catalogue menu.</gray>"));

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

                                    List<String> commands = new ArrayList<>();
                                    if (itemSec.isList("commands")) {
                                        commands.addAll(itemSec.getStringList("commands"));
                                    } else if (itemSec.isString("command")) {
                                        commands.add(itemSec.getString("command"));
                                    } else if (itemSec.isString("commands")) {
                                        commands.add(itemSec.getString("commands"));
                                    }

                                    ShopItem item = new ShopItem(itemKey, slot, mat != null ? mat : Material.STONE,
                                            name, lore, price, giveMat != null ? giveMat : (mat != null ? mat : Material.STONE), giveAmt, giveName, commands);
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

        // Decorate background with pre-cached glass
        for (int i = 0; i < catalogueSize; i++) {
            inv.setItem(i, FILLER_GLASS);
        }

        // Place categories with pre-cached icons
        for (CatalogueCategory cat : catalogueCategories.values()) {
            inv.setItem(cat.slot, cat.icon);
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

        // Place items with pre-cached display items
        for (ShopItem item : catData.items.values()) {
            inv.setItem(item.slot, item.displayItem);
        }

        // Place pre-cached Back button at bottom right (size - 1)
        int backSlot = catData.size - 1;
        inv.setItem(backSlot, BACK_BUTTON);

        player.openInventory(inv);
    }

    /**
     * Tier 3: Open 45-slot Quantity Adjustment & Confirmation GUI
     */
    public void openConfirm(Player player, String categoryId, ShopItem shopItem, int quantity) {
        openConfirm(player, categoryId, shopItem, quantity, false);
    }

    public void openConfirm(Player player, String categoryId, ShopItem shopItem, int quantity, boolean stackMode) {
        if (shopItem.getMaterial().getMaxStackSize() <= 1) {
            stackMode = false;
        }
        int maxQty = stackMode ? 36 : 64;
        quantity = Math.max(1, Math.min(quantity, maxQty));

        ConfirmHolder holder = new ConfirmHolder(categoryId, shopItem, quantity, stackMode);
        String title = stackMode ? "<dark_gray>Buy by Stack</dark_gray>" : "<dark_gray>Choose Quantity to Buy</dark_gray>";
        Inventory inv = Bukkit.createInventory(holder, 45, ChatUtils.parse(title));
        holder.setInventory(inv);

        renderConfirmInventory(inv, player, holder);
        player.openInventory(inv);
    }

    /**
     * Re-renders the contents of the 45-slot Confirmation GUI (Supports both Unit and Stack modes)
     */
    public void renderConfirmInventory(Inventory inv, Player player, ConfirmHolder holder) {
        ShopItem shopItem = holder.getShopItem();
        int itemMaxStack = Math.max(1, shopItem.getMaterial().getMaxStackSize());
        boolean stackMode = holder.isStackMode();
        if (itemMaxStack <= 1) {
            stackMode = false;
            holder.setStackMode(false);
        }
        int maxQty = stackMode ? 36 : 64;
        int quantity = Math.max(1, Math.min(holder.getQuantity(), maxQty));
        holder.setQuantity(quantity);

        int totalItems = stackMode ? quantity * itemMaxStack : quantity * Math.max(1, shopItem.getAmount());
        long totalCost = stackMode ? (long) quantity * itemMaxStack * shopItem.getPrice() : (long) quantity * shopItem.getPrice();
        long playerBalance = plugin.getPlayerDataManager().getShards(player.getUniqueId());

        // Fill all 45 slots with pre-cached gray glass by default
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, FILLER_GLASS);
        }

        // --- Slot 13: Paper (Confirm Button) ---
        List<String> paperLore = new ArrayList<>();
        paperLore.add("<dark_gray>------------------------</dark_gray>");
        paperLore.add("<gray>Item: <white>" + (shopItem.getName().isEmpty() ? shopItem.getMaterial().name() : shopItem.getName()) + "</white></gray>");
        if (stackMode) {
            paperLore.add("<gray>Quantity: <white>" + quantity + " Stack(s) (" + totalItems + " items)</white></gray>");
        } else {
            paperLore.add("<gray>Quantity: <white>" + quantity + " items</white></gray>");
        }
        paperLore.add("<gray>Total Cost: <gold>" + totalCost + " Shards</gold></gray>");
        paperLore.add("<dark_gray>------------------------</dark_gray>");
        if (playerBalance >= totalCost) {
            paperLore.add("<yellow>\u00BB Click to confirm purchase!</yellow>");
        } else {
            paperLore.add("<red>\u2716 Not enough Shards to purchase!</red>");
        }
        ItemStack paper = createItem(Material.PAPER, "<green><bold>Confirm Purchase" + (stackMode ? " (Stack)" : "") + "</bold></green>", paperLore);
        inv.setItem(13, paper);

        // --- Slot 22: Center Display Item ---
        List<String> displayLore = new ArrayList<>();
        displayLore.add("<dark_gray>------------------------</dark_gray>");
        displayLore.add("<gray>Unit Price: <gold>" + shopItem.getPrice() + " Shards/item</gold></gray>");
        if (stackMode) {
            long stackCost = (long) itemMaxStack * shopItem.getPrice();
            displayLore.add("<gray>1 Stack Price: <gold>" + stackCost + " Shards</gold></gray>");
            displayLore.add("<gray>Selected: <white>" + quantity + " Stack(s) (" + totalItems + " items)</white></gray>");
        } else {
            displayLore.add("<gray>Selected Quantity: <white>" + quantity + " items</white></gray>");
        }
        displayLore.add("<gray>Total Cost: <gold>" + totalCost + " Shards</gold></gray>");
        displayLore.add("<dark_gray>------------------------</dark_gray>");
        if (playerBalance >= totalCost) {
            displayLore.add("<green>Your Balance: <gold>" + playerBalance + " Shards</gold></green>");
        } else {
            displayLore.add("<red>Your Balance: <gold>" + playerBalance + " Shards</gold> (Insufficient!)</red>");
        }
        ItemStack centerItem = createItem(shopItem.material, shopItem.name.isEmpty() ? null : shopItem.name, displayLore);
        centerItem.setAmount(Math.max(1, Math.min(quantity, 64)));
        inv.setItem(22, centerItem);

        // --- Decrease Buttons: Slots 19, 20, 21 ---
        // Only visible if quantity > 1
        if (quantity > 1) {
            if (stackMode) {
                inv.setItem(21, createGlassButton(Material.RED_STAINED_GLASS_PANE, 1, "<red><bold>-1 Stack</bold></red>", "<gray>Decrease by 1 stack (" + itemMaxStack + " items)</gray>"));
                inv.setItem(20, createGlassButton(Material.RED_STAINED_GLASS_PANE, 8, "<red><bold>-8 Stacks</bold></red>", "<gray>Decrease by 8 stacks</gray>"));
                inv.setItem(19, createGlassButton(Material.RED_STAINED_GLASS_PANE, 16, "<red><bold>-16 Stacks</bold></red>", "<gray>Decrease by 16 stacks</gray>"));
            } else {
                inv.setItem(21, createGlassButton(Material.RED_STAINED_GLASS_PANE, 1, "<red><bold>-1</bold></red>", "<gray>Decrease by 1</gray>"));
                inv.setItem(20, createGlassButton(Material.RED_STAINED_GLASS_PANE, 16, "<red><bold>-16</bold></red>", "<gray>Decrease by 16</gray>"));
                inv.setItem(19, createGlassButton(Material.RED_STAINED_GLASS_PANE, 32, "<red><bold>-32</bold></red>", "<gray>Decrease by 32</gray>"));
            }
        } else {
            inv.setItem(19, FILLER_GLASS);
            inv.setItem(20, FILLER_GLASS);
            inv.setItem(21, FILLER_GLASS);
        }

        // --- Increase Buttons: Slots 23, 24, 25 ---
        // Only visible if quantity < maxQty
        if (quantity < maxQty) {
            if (stackMode) {
                inv.setItem(23, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, "<aqua><bold>+1 Stack</bold></aqua>", "<gray>Increase by 1 stack (" + itemMaxStack + " items)</gray>"));
                inv.setItem(24, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 8, "<aqua><bold>+8 Stacks</bold></aqua>", "<gray>Increase by 8 stacks</gray>"));
                inv.setItem(25, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 16, "<aqua><bold>+16 Stacks</bold></aqua>", "<gray>Increase by 16 stacks</gray>"));
            } else {
                inv.setItem(23, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, "<aqua><bold>+1</bold></aqua>", "<gray>Increase by 1</gray>"));
                inv.setItem(24, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 16, "<aqua><bold>+16</bold></aqua>", "<gray>Increase by 16</gray>"));
                inv.setItem(25, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 32, "<aqua><bold>+32</bold></aqua>", "<gray>Increase by 32</gray>"));
            }
        } else {
            inv.setItem(23, FILLER_GLASS);
            inv.setItem(24, FILLER_GLASS);
            inv.setItem(25, FILLER_GLASS);
        }

        // --- Slot 31: Chest / Hopper (Toggle between Stack Mode & Unit Mode) ---
        // Hide stack option if item cannot stack (maxStackSize <= 1)
        if (itemMaxStack > 1) {
            if (stackMode) {
                ItemStack switchUnit = createItem(Material.HOPPER, "<yellow><bold>Switch to: Buy by Unit</bold></yellow>",
                        List.of("<gray>Click to switch to single-item purchase mode (1-64 items).</gray>"));
                inv.setItem(31, switchUnit);
            } else {
                ItemStack switchStack = createItem(Material.CHEST, "<gold><bold>Switch to: Buy by Stack</bold></gold>",
                        List.of("<gray>Click to open Stack purchase mode (1-36 Stacks).</gray>"));
                inv.setItem(31, switchStack);
            }
        } else {
            inv.setItem(31, FILLER_GLASS);
        }

        // --- Slot 36: Player Head (Profile / Balance) ---
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.displayName(ChatUtils.parse("<yellow><bold>Your Account</bold></yellow>").decoration(TextDecoration.ITALIC, false));
            skullMeta.lore(List.of(
                    ChatUtils.parse("<gray>Player: <white>" + player.getName() + "</white></gray>").decoration(TextDecoration.ITALIC, false),
                    ChatUtils.parse("<gray>Shard Balance: <gold>" + playerBalance + " Shards</gold></gray>").decoration(TextDecoration.ITALIC, false)
            ));
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(36, skull);

        // --- Slot 44: Arrow (Back Button) ---
        ItemStack backArrow = createItem(Material.ARROW, "<red><bold>\u00AB Back</bold></red>",
                List.of("<gray>Click to return to the category menu.</gray>"));
        inv.setItem(44, backArrow);
    }

    private ItemStack createGlassButton(Material mat, int amount, String name, String loreText) {
        ItemStack item = new ItemStack(mat, Math.max(1, Math.min(amount, 64)));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ChatUtils.parse(name).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(ChatUtils.parse(loreText).decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
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

    public static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat != null ? mat : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(ChatUtils.parse(name).decoration(TextDecoration.ITALIC, false));
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> compLore = new ArrayList<>();
                for (String l : lore) {
                    compLore.add(ChatUtils.parse(l).decoration(TextDecoration.ITALIC, false));
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
        private boolean stackMode;
        private Inventory inventory;

        public ConfirmHolder(String categoryId, ShopItem shopItem, int quantity, boolean stackMode) {
            this.categoryId = categoryId;
            this.shopItem = shopItem;
            this.quantity = quantity;
            this.stackMode = stackMode;
        }

        public String getCategoryId() { return categoryId; }
        public ShopItem getShopItem() { return shopItem; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public boolean isStackMode() { return stackMode; }
        public void setStackMode(boolean stackMode) { this.stackMode = stackMode; }
        public void setInventory(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public static class CatalogueCategory {
        public final String id;
        public final int slot;
        public final Material material;
        public final String name;
        public final List<String> lore;
        public final ItemStack icon;

        public CatalogueCategory(String id, int slot, Material material, String name, List<String> lore) {
            this.id = id;
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.icon = createItem(material, name, lore);
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
        public final List<String> commands;
        public final ItemStack displayItem;

        public ShopItem(String id, int slot, Material material, String name, List<String> lore,
                        long price, Material giveMaterial, int giveAmount, String giveName, List<String> commands) {
            this.id = id;
            this.slot = slot;
            this.material = material;
            this.name = name;
            this.lore = lore;
            this.price = price;
            this.giveMaterial = giveMaterial;
            this.giveAmount = giveAmount;
            this.giveName = giveName;
            this.commands = commands != null ? commands : Collections.emptyList();
            this.displayItem = createItem(material, name, lore);
        }

        public long getPrice() { return price; }
        public Material getMaterial() { return giveMaterial != null ? giveMaterial : material; }
        public int getAmount() { return giveAmount; }
        public String getName() { return (giveName != null && !giveName.isEmpty()) ? giveName : name; }
        public List<String> getCommands() { return commands; }
    }
}
