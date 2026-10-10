package com.cappy.shards.listeners;

import java.io.File;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.cappy.shards.CappyShards;
import com.cappy.shards.gui.ShopGUI;
import com.cappy.shards.utils.ChatUtils;

public class InventoryListener implements Listener {

    private final CappyShards plugin;
    private YamlConfiguration messages;

    public InventoryListener(CappyShards plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File msgFile = new File(plugin.getDataFolder(), "messages.yml");
        if (msgFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(msgFile);
        }
    }

    private String getMessage(String key, String def) {
        if (messages == null) return def;
        return messages.getString(key, def);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null) return;

        // Compare inventory directly
        if (event.getView().getTopInventory().equals(plugin.getShopGUI().getInventory())) {
            event.setCancelled(true); // Anti-Drag/Drop

            if (clickedInv.equals(plugin.getShopGUI().getInventory())) {
                int slot = event.getSlot();
                ShopGUI.ShopItem shopItem = plugin.getShopGUI().getItem(slot);

                if (shopItem != null) {
                    if (plugin.getPlayerDataManager().isLocked(player.getUniqueId())) {
                        ChatUtils.sendMessage(player, getMessage("action-locked", "<red>Thao tác quá nhanh! Vui lòng thử lại.</red>"));
                        return;
                    }

                    if (plugin.getPlayerDataManager().lock(player.getUniqueId())) {
                        try {
                            if (plugin.getPlayerDataManager().getShards(player.getUniqueId()) >= shopItem.getPrice()) {
                                
                                // Check if inventory is full
                                if (player.getInventory().firstEmpty() == -1) {
                                    ChatUtils.sendMessage(player, getMessage("inventory-full", "<red>Túi đồ của bạn đã đầy!</red>"));
                                    return;
                                }

                                // Perform transaction
                                plugin.getPlayerDataManager().removeShards(player.getUniqueId(), shopItem.getPrice());
                                
                                ItemStack reward = new ItemStack(shopItem.getMaterial() == null ? Material.STONE : shopItem.getMaterial(), shopItem.getAmount());
                                if (!shopItem.getName().isEmpty()) {
                                    ItemMeta meta = reward.getItemMeta();
                                    if (meta != null) {
                                        meta.displayName(ChatUtils.parse(shopItem.getName()));
                                        reward.setItemMeta(meta);
                                    }
                                }
                                
                                player.getInventory().addItem(reward);
                                
                                String successMsg = getMessage("shop-purchase-success", "<green>Mua thành công <white>%item%</white> với giá <gold>%price%</gold> Shard.</green>");
                                successMsg = successMsg.replace("%item%", shopItem.getName().isEmpty() ? shopItem.getMaterial().name() : shopItem.getName())
                                                       .replace("%price%", String.valueOf(shopItem.getPrice()));
                                ChatUtils.sendMessage(player, successMsg);
                            } else {
                                ChatUtils.sendMessage(player, getMessage("not-enough-shards", "<red>Bạn không đủ Shard!</red>"));
                            }
                        } finally {
                            // Ensure unlock even if exception occurs
                            plugin.getPlayerDataManager().unlock(player.getUniqueId());
                        }
                    }
                }
            }
        }
    }
}

