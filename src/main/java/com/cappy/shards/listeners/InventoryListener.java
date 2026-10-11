package com.cappy.shards.listeners;

import com.cappy.shards.CappyShards;
import com.cappy.shards.gui.ShopGUI;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {

    private final CappyShards plugin;

    public InventoryListener(CappyShards plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ShopGUI.CatalogueHolder ||
            holder instanceof ShopGUI.CategoryHolder ||
            holder instanceof ShopGUI.ConfirmHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (topHolder == null) return;

        // Ensure this is one of our Shop GUIs
        boolean isShopGUI = topHolder instanceof ShopGUI.CatalogueHolder ||
                           topHolder instanceof ShopGUI.CategoryHolder ||
                           topHolder instanceof ShopGUI.ConfirmHolder;

        if (!isShopGUI) return;

        // Cancel all clicks in shop window or player shift-clicks
        event.setCancelled(true);

        // Only process clicks occurring in the upper (shop) inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();

        // 1. Handling Tier 1: Catalogue Menu
        if (topHolder instanceof ShopGUI.CatalogueHolder) {
            ShopGUI.CatalogueCategory category = plugin.getShopGUI().getCatalogueCategory(slot);
            if (category != null) {
                plugin.getShopGUI().openCategory(player, category.id);
            }
            return;
        }

        // 2. Handling Tier 2: Category Items Menu
        if (topHolder instanceof ShopGUI.CategoryHolder catHolder) {
            int backSlot = event.getClickedInventory().getSize() - 1;
            if (slot == backSlot) {
                // Return to catalogue
                plugin.getShopGUI().openCatalogue(player);
                return;
            }

            ShopGUI.ShopItem shopItem = plugin.getShopGUI().getShopItem(catHolder.getCategoryId(), slot);
            if (shopItem != null) {
                plugin.getShopGUI().openConfirm(player, catHolder.getCategoryId(), shopItem, 1);
            }
            return;
        }

        // 3. Handling Tier 3: Quantity Adjustment & Confirmation Menu
        if (topHolder instanceof ShopGUI.ConfirmHolder confirmHolder) {
            handleConfirmClick(player, confirmHolder, slot, event.getClickedInventory());
        }
    }

    private void handleConfirmClick(Player player, ShopGUI.ConfirmHolder holder, int slot, org.bukkit.inventory.Inventory inv) {
        int currentQty = holder.getQuantity();

        switch (slot) {
            case 18 -> { // Min (1)
                holder.setQuantity(1);
                plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
            }
            case 10 -> { // -10
                holder.setQuantity(Math.max(1, currentQty - 10));
                plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
            }
            case 11 -> { // -1
                holder.setQuantity(Math.max(1, currentQty - 1));
                plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
            }
            case 15 -> { // +1
                holder.setQuantity(Math.min(64, currentQty + 1));
                plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
            }
            case 16 -> { // +10
                holder.setQuantity(Math.min(64, currentQty + 10));
                plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
            }
            case 26 -> { // Max (64)
                holder.setQuantity(64);
                plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
            }
            case 21 -> { // Cancel / Back
                plugin.getShopGUI().openCategory(player, holder.getCategoryId());
            }
            case 23 -> { // Confirm Purchase
                executePurchase(player, holder);
            }
        }
    }

    private void executePurchase(Player player, ShopGUI.ConfirmHolder holder) {
        if (plugin.getPlayerDataManager().isLocked(player.getUniqueId())) {
            ChatUtils.sendMessage(player, plugin.getMessage("action-locked", "<red>You are clicking too fast! Please try again.</red>"));
            return;
        }

        if (plugin.getPlayerDataManager().lock(player.getUniqueId())) {
            try {
                ShopGUI.ShopItem item = holder.getShopItem();
                int qty = holder.getQuantity();
                long totalCost = item.getPrice() * qty;

                // Balance check
                if (plugin.getPlayerDataManager().getShards(player.getUniqueId()) < totalCost) {
                    ChatUtils.sendMessage(player, plugin.getMessage("not-enough-shards", "<red>You do not have enough Shards to perform this transaction!</red>"));
                    return;
                }

                // Inventory space check
                int totalGiveAmount = qty * Math.max(1, item.getAmount());
                Material mat = item.getMaterial();
                int maxStack = mat.getMaxStackSize();
                int neededSlots = (int) Math.ceil((double) totalGiveAmount / maxStack);

                int emptySlots = 0;
                for (ItemStack content : player.getInventory().getStorageContents()) {
                    if (content == null || content.getType().isAir()) {
                        emptySlots++;
                    }
                }

                if (emptySlots < neededSlots) {
                    ChatUtils.sendMessage(player, plugin.getMessage("inventory-full", "<red>Your inventory is full, cannot purchase this item!</red>"));
                    return;
                }

                // Deduct shards
                plugin.getPlayerDataManager().removeShards(player.getUniqueId(), totalCost);

                // Give items
                int remaining = totalGiveAmount;
                while (remaining > 0) {
                    int giveThis = Math.min(remaining, maxStack);
                    ItemStack reward = new ItemStack(mat, giveThis);
                    if (item.getName() != null && !item.getName().isEmpty()) {
                        ItemMeta meta = reward.getItemMeta();
                        if (meta != null) {
                            meta.displayName(ChatUtils.parse(item.getName()));
                            reward.setItemMeta(meta);
                        }
                    }
                    player.getInventory().addItem(reward);
                    remaining -= giveThis;
                }

                String itemName = item.getName() != null && !item.getName().isEmpty() ? item.getName() : mat.name();
                String successMsg = plugin.getMessage("shop-purchase-success", "<green>Successfully purchased <white>%amount%x %item%</white> for <gold>%price%</gold> Shards.</green>")
                        .replace("%amount%", String.valueOf(totalGiveAmount))
                        .replace("%item%", itemName)
                        .replace("%price%", String.valueOf(totalCost));
                ChatUtils.sendMessage(player, successMsg);

                // Re-open category menu so player can continue shopping
                plugin.getShopGUI().openCategory(player, holder.getCategoryId());

            } finally {
                plugin.getPlayerDataManager().unlock(player.getUniqueId());
            }
        }
    }
}
