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

        // Place Back button at bottom right (size - 1)
        int backSlot = catData.size - 1;
        ItemStack backButton = createItem(Material.ARROW, "<red>\u00AB Back to Catalogues</red>",
                List.of("<gray>Click to return to the catalogue menu.</gray>"));
        inv.setItem(backSlot, backButton);

        player.openInventory(inv);
    }

    /**
     * Tier 3: Open 45-slot Quantity Adjustment & Confirmation GUI
     */
    public void openConfirm(Player player, String categoryId, ShopItem shopItem, int quantity) {
        openConfirm(player, categoryId, shopItem, quantity, false);
    }

    public void openConfirm(Player player, String categoryId, ShopItem shopItem, int quantity, boolean stackMode) {
        int maxQty = stackMode ? 36 : 64;
        quantity = Math.max(1, Math.min(quantity, maxQty));

        ConfirmHolder holder = new ConfirmHolder(categoryId, shopItem, quantity, stackMode);
        String title = stackMode ? "<dark_gray>Mua theo Stack</dark_gray>" : "<dark_gray>Chọn số lượng để mua</dark_gray>";
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
        boolean stackMode = holder.isStackMode();
        int itemMaxStack = Math.max(1, shopItem.getMaterial().getMaxStackSize());
        int maxQty = stackMode ? 36 : 64;
        int quantity = Math.max(1, Math.min(holder.getQuantity(), maxQty));
        holder.setQuantity(quantity);

        int totalItems = stackMode ? quantity * itemMaxStack : quantity * Math.max(1, shopItem.getAmount());
        long totalCost = stackMode ? (long) quantity * itemMaxStack * shopItem.getPrice() : (long) quantity * shopItem.getPrice();
        long playerBalance = plugin.getPlayerDataManager().getShards(player.getUniqueId());

        ItemStack grayGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);

        // Fill all 45 slots with gray glass by default
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, grayGlass);
        }

        // --- Slot 13: Paper (Confirm Button) ---
        List<String> paperLore = new ArrayList<>();
        paperLore.add("<dark_gray>------------------------</dark_gray>");
        paperLore.add("<gray>Vật phẩm: <white>" + (shopItem.getName().isEmpty() ? shopItem.getMaterial().name() : shopItem.getName()) + "</white></gray>");
        if (stackMode) {
            paperLore.add("<gray>Số lượng: <white>" + quantity + " Stack(s) (" + totalItems + " cái)</white></gray>");
        } else {
            paperLore.add("<gray>Số lượng: <white>" + quantity + " cái</white></gray>");
        }
        paperLore.add("<gray>Tổng giá: <gold>" + totalCost + " Shards</gold></gray>");
        paperLore.add("<dark_gray>------------------------</dark_gray>");
        if (playerBalance >= totalCost) {
            paperLore.add("<yellow>\u00BB Click để xác nhận mua!</yellow>");
        } else {
            paperLore.add("<red>\u2716 Không đủ Shard để mua!</red>");
        }
        ItemStack paper = createItem(Material.PAPER, "<green><bold>Xác nhận mua" + (stackMode ? " (Stack)" : "") + "</bold></green>", paperLore);
        inv.setItem(13, paper);

        // --- Slot 22: Center Display Item ---
        List<String> displayLore = new ArrayList<>();
        displayLore.add("<dark_gray>------------------------</dark_gray>");
        displayLore.add("<gray>Đơn giá: <gold>" + shopItem.getPrice() + " Shards/cái</gold></gray>");
        if (stackMode) {
            long stackCost = (long) itemMaxStack * shopItem.getPrice();
            displayLore.add("<gray>Đơn giá 1 Stack: <gold>" + stackCost + " Shards</gold></gray>");
            displayLore.add("<gray>Số stack chọn: <white>" + quantity + " Stack(s) (" + totalItems + " cái)</white></gray>");
        } else {
            displayLore.add("<gray>Số lượng chọn: <white>" + quantity + " cái</white></gray>");
        }
        displayLore.add("<gray>Tổng thanh toán: <gold>" + totalCost + " Shards</gold></gray>");
        displayLore.add("<dark_gray>------------------------</dark_gray>");
        if (playerBalance >= totalCost) {
            displayLore.add("<green>Số dư của bạn: <gold>" + playerBalance + " Shards</gold></green>");
        } else {
            displayLore.add("<red>Số dư của bạn: <gold>" + playerBalance + " Shards</gold> (Thiếu!)</red>");
        }
        ItemStack centerItem = createItem(shopItem.material, shopItem.name.isEmpty() ? null : shopItem.name, displayLore);
        centerItem.setAmount(Math.max(1, Math.min(quantity, 64)));
        inv.setItem(22, centerItem);

        // --- Decrease Buttons: Slots 19, 20, 21 ---
        // "tăng lên mới hiện bên giảm" -> Only visible if quantity > 1
        if (quantity > 1) {
            if (stackMode) {
                inv.setItem(21, createGlassButton(Material.RED_STAINED_GLASS_PANE, 1, "<red><bold>-1 Stack</bold></red>", "<gray>Giảm 1 stack (" + itemMaxStack + " cái)</gray>"));
                inv.setItem(20, createGlassButton(Material.RED_STAINED_GLASS_PANE, 8, "<red><bold>-8 Stacks</bold></red>", "<gray>Giảm 8 stacks</gray>"));
                inv.setItem(19, createGlassButton(Material.RED_STAINED_GLASS_PANE, 16, "<red><bold>-16 Stacks</bold></red>", "<gray>Giảm 16 stacks</gray>"));
            } else {
                inv.setItem(21, createGlassButton(Material.RED_STAINED_GLASS_PANE, 1, "<red><bold>-1</bold></red>", "<gray>Giảm đi 1</gray>"));
                inv.setItem(20, createGlassButton(Material.RED_STAINED_GLASS_PANE, 16, "<red><bold>-16</bold></red>", "<gray>Giảm đi 16</gray>"));
                inv.setItem(19, createGlassButton(Material.RED_STAINED_GLASS_PANE, 32, "<red><bold>-32</bold></red>", "<gray>Giảm đi 32</gray>"));
            }
        } else {
            inv.setItem(19, grayGlass);
            inv.setItem(20, grayGlass);
            inv.setItem(21, grayGlass);
        }

        // --- Increase Buttons: Slots 23, 24, 25 ---
        // "tăng max rồi hiện bên giảm th" -> Only visible if quantity < maxQty
        if (quantity < maxQty) {
            if (stackMode) {
                inv.setItem(23, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, "<aqua><bold>+1 Stack</bold></aqua>", "<gray>Tăng 1 stack (" + itemMaxStack + " cái)</gray>"));
                inv.setItem(24, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 8, "<aqua><bold>+8 Stacks</bold></aqua>", "<gray>Tăng 8 stacks</gray>"));
                inv.setItem(25, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 16, "<aqua><bold>+16 Stacks</bold></aqua>", "<gray>Tăng 16 stacks</gray>"));
            } else {
                inv.setItem(23, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 1, "<aqua><bold>+1</bold></aqua>", "<gray>Tăng thêm 1</gray>"));
                inv.setItem(24, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 16, "<aqua><bold>+16</bold></aqua>", "<gray>Tăng thêm 16</gray>"));
                inv.setItem(25, createGlassButton(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 32, "<aqua><bold>+32</bold></aqua>", "<gray>Tăng thêm 32</gray>"));
            }
        } else {
            inv.setItem(23, grayGlass);
            inv.setItem(24, grayGlass);
            inv.setItem(25, grayGlass);
        }

        // --- Slot 31: Chest / Hopper (Toggle between Stack Mode & Unit Mode) ---
        if (stackMode) {
            ItemStack switchUnit = createItem(Material.HOPPER, "<yellow><bold>Chuyển sang: Mua theo cái lẻ</bold></yellow>",
                    List.of("<gray>Click để chuyển sang chế độ mua số lượng lẻ (1-64 cái).</gray>"));
            inv.setItem(31, switchUnit);
        } else {
            ItemStack switchStack = createItem(Material.CHEST, "<gold><bold>Chuyển sang: Mua theo Stack</bold></gold>",
                    List.of("<gray>Click để mở giao diện mua theo đơn vị Stack (1-36 Stacks).</gray>"));
            inv.setItem(31, switchStack);
        }

        // --- Slot 36: Player Head (Profile / Balance) ---
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            skullMeta.displayName(ChatUtils.parse("<yellow><bold>Tài khoản của bạn</bold></yellow>").decoration(TextDecoration.ITALIC, false));
            skullMeta.lore(List.of(
                    ChatUtils.parse("<gray>Người chơi: <white>" + player.getName() + "</white></gray>").decoration(TextDecoration.ITALIC, false),
                    ChatUtils.parse("<gray>Số dư Shard: <gold>" + playerBalance + " Shards</gold></gray>").decoration(TextDecoration.ITALIC, false)
            ));
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(36, skull);

        // --- Slot 44: Arrow (Back Button) ---
        ItemStack backArrow = createItem(Material.ARROW, "<red><bold>\u00AB Quay lại</bold></red>",
                List.of("<gray>Click để quay lại danh mục sản phẩm.</gray>"));
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

    private ItemStack createItem(Material mat, String name, List<String> lore) {
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
