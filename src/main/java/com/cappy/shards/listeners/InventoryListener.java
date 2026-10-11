package com.cappy.shards.listeners;

import com.cappy.shards.CappyShards;
import com.cappy.shards.gui.ShopGUI;
import com.cappy.shards.utils.ChatUtils;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InventoryListener implements Listener {

    private final CappyShards plugin;

    public InventoryListener(CappyShards plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;

        InventoryHolder holder = topInv.getHolder();
        if (holder instanceof ShopGUI.CatalogueHolder ||
            holder instanceof ShopGUI.CategoryHolder ||
            holder instanceof ShopGUI.ConfirmHolder) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory topInv = event.getView().getTopInventory();
        if (topInv == null) return;

        InventoryHolder topHolder = topInv.getHolder();
        if (topHolder == null) return;

        boolean isShopGUI = topHolder instanceof ShopGUI.CatalogueHolder ||
                           topHolder instanceof ShopGUI.CategoryHolder ||
                           topHolder instanceof ShopGUI.ConfirmHolder;

        if (!isShopGUI) return;

        // 1. Immediately cancel the click and deny result to prevent ANY item dragging or stealing
        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        // 2. Ignore clicks outside the window or in the player's bottom inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInv)) {
            return;
        }

        int slot = event.getSlot();

        try {
            // Tier 1: Catalogue Menu
            if (topHolder instanceof ShopGUI.CatalogueHolder) {
                ShopGUI.CatalogueCategory category = plugin.getShopGUI().getCatalogueCategory(slot);
                if (category != null) {
                    plugin.getShopGUI().openCategory(player, category.id);
                }
                return;
            }

            // Tier 2: Category Items Menu
            if (topHolder instanceof ShopGUI.CategoryHolder catHolder) {
                int backSlot = event.getClickedInventory().getSize() - 1;
                if (slot == backSlot) {
                    plugin.getShopGUI().openCatalogue(player);
                    return;
                }

                ShopGUI.ShopItem shopItem = plugin.getShopGUI().getShopItem(catHolder.getCategoryId(), slot);
                if (shopItem != null) {
                    plugin.getShopGUI().openConfirm(player, catHolder.getCategoryId(), shopItem, 1);
                }
                return;
            }

            // Tier 3: Quantity Adjustment & Confirmation Menu
            if (topHolder instanceof ShopGUI.ConfirmHolder confirmHolder) {
                handleConfirmClick(player, confirmHolder, slot, event.getClickedInventory());
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error handling shop click for " + player.getName() + ": " + e.getMessage());
        } finally {
            player.updateInventory();
        }
    }

    private void handleConfirmClick(Player player, ShopGUI.ConfirmHolder holder, int slot, Inventory inv) {
        int currentQty = holder.getQuantity();
        boolean stackMode = holder.isStackMode();
        int maxQty = stackMode ? 36 : 64;

        switch (slot) {
            case 13 -> { // PAPER: Confirm Purchase
                executePurchase(player, holder);
            }
            case 23 -> { // +1 or +1 Stack
                int delta = 1;
                if (currentQty < maxQty) {
                    holder.setQuantity(Math.min(maxQty, currentQty + delta));
                    plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
                }
            }
            case 24 -> { // +16 or +8 Stacks
                int delta = stackMode ? 8 : 16;
                if (currentQty < maxQty) {
                    holder.setQuantity(Math.min(maxQty, currentQty + delta));
                    plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
                }
            }
            case 25 -> { // +32 or +16 Stacks
                int delta = stackMode ? 16 : 32;
                if (currentQty < maxQty) {
                    holder.setQuantity(Math.min(maxQty, currentQty + delta));
                    plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
                }
            }
            case 21 -> { // -1 or -1 Stack
                int delta = 1;
                if (currentQty > 1) {
                    holder.setQuantity(Math.max(1, currentQty - delta));
                    plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
                }
            }
            case 20 -> { // -16 or -8 Stacks
                int delta = stackMode ? 8 : 16;
                if (currentQty > 1) {
                    holder.setQuantity(Math.max(1, currentQty - delta));
                    plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
                }
            }
            case 19 -> { // -32 or -16 Stacks
                int delta = stackMode ? 16 : 32;
                if (currentQty > 1) {
                    holder.setQuantity(Math.max(1, currentQty - delta));
                    plugin.getShopGUI().renderConfirmInventory(inv, player, holder);
                }
            }
            case 31 -> { // CHEST / HOPPER: Switch between Stack Mode and Unit Mode!
                if (holder.getShopItem().getMaterial().getMaxStackSize() > 1) {
                    plugin.getShopGUI().openConfirm(player, holder.getCategoryId(), holder.getShopItem(), 1, !stackMode);
                }
            }
            case 44 -> { // ARROW: Back to category
                plugin.getShopGUI().openCategory(player, holder.getCategoryId());
            }
            default -> {
                // Clicking display item (slot 22), skull (slot 36), or glass panes does nothing
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
                boolean stackMode = holder.isStackMode();
                int qty = holder.getQuantity();
                Material mat = item.getMaterial();
                int maxStack = Math.max(1, mat.getMaxStackSize());

                int totalGiveAmount = stackMode ? qty * maxStack : qty * Math.max(1, item.getAmount());
                long totalCost = stackMode ? (long) qty * maxStack * item.getPrice() : (long) qty * item.getPrice();

                // 1. Balance check
                if (plugin.getPlayerDataManager().getShards(player.getUniqueId()) < totalCost) {
                    ChatUtils.sendMessage(player, plugin.getMessage("not-enough-shards", "<red>You do not have enough Shards to perform this transaction!</red>"));
                    return;
                }

                // 2. Inventory space check (only for standard item delivery)
                boolean hasCommands = item.getCommands() != null && !item.getCommands().isEmpty();
                if (!hasCommands) {
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
                }

                // 3. Deduct shards
                plugin.getPlayerDataManager().removeShards(player.getUniqueId(), totalCost);

                // 4. Give items securely or dispatch optional console commands
                if (hasCommands) {
                    for (String cmd : item.getCommands()) {
                        String formattedCmd = cmd
                                .replace("%player%", player.getName())
                                .replace("%amount%", String.valueOf(totalGiveAmount));
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCmd);
                    }
                } else {
                    int remaining = totalGiveAmount;
                    while (remaining > 0) {
                        int giveThis = Math.min(remaining, maxStack);
                        ItemStack reward = new ItemStack(mat, giveThis);
                        if (item.getName() != null && !item.getName().isEmpty()) {
                            ItemMeta meta = reward.getItemMeta();
                            if (meta != null) {
                                meta.displayName(ChatUtils.parse(item.getName()).decoration(TextDecoration.ITALIC, false));
                                reward.setItemMeta(meta);
                            }
                        }
                        player.getInventory().addItem(reward);
                        remaining -= giveThis;
                    }
                }

                // 5. Send success message
                String itemName = item.getName() != null && !item.getName().isEmpty() ? item.getName() : mat.name();
                String rawTemplate = plugin.getMessage("shop-purchase-success", "<green>Successfully purchased <white>%amount%x %item%</white> for <gold>%price%</gold> Shards.</green>");
                String formatted = rawTemplate
                        .replace("%amount%", String.valueOf(totalGiveAmount))
                        .replace("%item%", itemName)
                        .replace("%price%", String.valueOf(totalCost));
                ChatUtils.sendMessage(player, formatted);

                // 6. Return to category view
                plugin.getShopGUI().openCategory(player, holder.getCategoryId());

            } catch (Exception e) {
                plugin.getLogger().severe("Error executing purchase for " + player.getName() + ": " + e.getMessage());
            } finally {
                plugin.getPlayerDataManager().unlock(player.getUniqueId());
                player.updateInventory();
            }
        }
    }
}
