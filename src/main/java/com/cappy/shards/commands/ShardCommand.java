package com.cappy.shards.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.cappy.shards.CappyShards;
import com.cappy.shards.gui.ShopGUI;
import com.cappy.shards.utils.ChatUtils;

public class ShardCommand implements CommandExecutor, TabCompleter {

    private final CappyShards plugin;
    private YamlConfiguration messages;

    public ShardCommand(CappyShards plugin) {
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
        String prefix = messages != null ? messages.getString("prefix", "") : "";
        String msg = messages != null ? messages.getString(key, def) : def;
        return prefix + msg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shardshop")) {
            if (!(sender instanceof Player player)) {
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>Lệnh này chỉ dành cho người chơi!</red>"));
                return true;
            }
            if (!player.hasPermission("cappyshards.use.shop")) {
                ChatUtils.sendMessage(player, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            plugin.getShopGUI().open(player);
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                long balance = plugin.getPlayerDataManager().getShards(player.getUniqueId());
                ChatUtils.sendMessage(player, getMessage("shard-balance", "<green>Số dư Shard: <gold>%amount%</gold></green>")
                        .replace("%amount%", String.valueOf(balance)));
            } else {
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>Lệnh này chỉ dành cho người chơi!</red>"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadMessages();
            loadMessages();
            plugin.getShopGUI().loadShop();
            plugin.getAfkRewardManager().loadConfig();
            plugin.getAfkRewardManager().stop();
            plugin.getAfkRewardManager().start();
            ChatUtils.sendMessage(sender, getMessage("reload-success", "<green>Đã tải lại cấu hình!</green>"));
            return true;
        }

        if (subCommand.equals("shop")) {
            if (!(sender instanceof Player player)) {
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>Lệnh này chỉ dành cho người chơi!</red>"));
                return true;
            }
            if (!player.hasPermission("cappyshards.use.shop")) {
                ChatUtils.sendMessage(player, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            plugin.getShopGUI().open(player);
            return true;
        }

        // --- Subcommand: createcatalogue <id> <slot> [name...] ---
        if (subCommand.equals("createcatalogue") || subCommand.equals("addcatalogue")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            if (args.length < 3) {
                ChatUtils.sendMessage(sender, "<yellow>Sử dụng: <white>/cappyshard createcatalogue <id> <slot> [tên_hiển_thị]</white></yellow>");
                return true;
            }
            String id = args[1].toLowerCase();
            int slot;
            try {
                slot = Integer.parseInt(args[2]);
                if (slot < 0 || slot >= 54) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(sender, "<red>Slot không hợp lệ (phải từ 0 đến 53)!</red>");
                return true;
            }

            Material mat = Material.CHEST;
            String displayName = "<yellow><bold>" + id + "</bold></yellow>";
            if (sender instanceof Player p) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand != null && !hand.getType().isAir()) {
                    mat = hand.getType();
                }
            }

            if (args.length >= 4) {
                StringBuilder sb = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) sb.append(" ");
                    sb.append(args[i]);
                }
                displayName = sb.toString();
            }

            boolean success = plugin.getShopGUI().createCatalogue(id, slot, mat, displayName);
            if (success) {
                ChatUtils.sendMessage(sender, "<green>Tạo danh mục <gold>" + id + "</gold> thành công tại slot <yellow>" + slot + "</yellow> (Icon: <white>" + mat.name() + "</white>)!</green>");
            } else {
                ChatUtils.sendMessage(sender, "<red>Không thể lưu danh mục vào catalogue.yml!</red>");
            }
            return true;
        }

        // --- Subcommand: removecatalogue <id> ---
        if (subCommand.equals("removecatalogue") || subCommand.equals("deletecatalogue")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            if (args.length < 2) {
                ChatUtils.sendMessage(sender, "<yellow>Sử dụng: <white>/cappyshard removecatalogue <id></white></yellow>");
                return true;
            }
            String id = args[1].toLowerCase();
            boolean success = plugin.getShopGUI().removeCatalogue(id);
            if (success) {
                ChatUtils.sendMessage(sender, "<green>Đã xóa danh mục <gold>" + id + "</gold> thành công!</green>");
            } else {
                ChatUtils.sendMessage(sender, "<red>Không tìm thấy hoặc không thể xóa danh mục <gold>" + id + "</gold>!</red>");
            }
            return true;
        }

        // --- Subcommand: additem <category> <price> [slot] ---
        if (subCommand.equals("additem")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            if (!(sender instanceof Player player)) {
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>Lệnh này chỉ dành cho người chơi!</red>"));
                return true;
            }
            if (args.length < 3) {
                ChatUtils.sendMessage(player, "<yellow>Sử dụng: <white>/cappyshard additem <danh_mục> <giá_shard> [slot]</white></yellow>");
                return true;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                ChatUtils.sendMessage(player, "<red>Bạn phải cầm vật phẩm (hoặc Crate Key) trên tay chính để thêm vào shop!</red>");
                return true;
            }

            String catId = args[1].toLowerCase();
            long price;
            try {
                price = Long.parseLong(args[2]);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(player, "<red>Giá bán phải là số nguyên dương lớn hơn 0!</red>");
                return true;
            }

            int slot = -1;
            if (args.length >= 4) {
                try {
                    slot = Integer.parseInt(args[3]);
                    if (slot < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    ChatUtils.sendMessage(player, "<red>Slot không hợp lệ (phải là số >= 0)!</red>");
                    return true;
                }
            }

            int assignedSlot = plugin.getShopGUI().addShopItem(catId, slot, price, hand);
            if (assignedSlot >= 0) {
                String itemName = hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()
                        ? hand.getItemMeta().getDisplayName()
                        : hand.getType().name();
                ChatUtils.sendMessage(player, "<green>Đã thêm vật phẩm <white>" + itemName + "</white> vào danh mục <gold>" + catId + "</gold> tại slot <yellow>" + assignedSlot + "</yellow> (Giá: <gold>" + price + "</gold> Shards)!</green>");
            } else if (assignedSlot == -1) {
                ChatUtils.sendMessage(player, "<red>Danh mục '<gold>" + catId + "</gold>' không tồn tại! Dùng <white>/cappyshard createcatalogue " + catId + " <slot></white> để tạo trước.</red>");
            } else if (assignedSlot == -2) {
                ChatUtils.sendMessage(player, "<red>Danh mục '<gold>" + catId + "</gold>' đã đầy ô trống!</red>");
            } else if (assignedSlot == -3) {
                ChatUtils.sendMessage(player, "<red>Slot chỉ định vượt quá giới hạn của danh mục!</red>");
            } else {
                ChatUtils.sendMessage(player, "<red>Không thể lưu vật phẩm vào shop.yml!</red>");
            }
            return true;
        }

        // --- Subcommand: removeitem <category> <slot> ---
        if (subCommand.equals("removeitem") || subCommand.equals("delitem")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                return true;
            }
            if (args.length < 3) {
                ChatUtils.sendMessage(sender, "<yellow>Sử dụng: <white>/cappyshard removeitem <danh_mục> <slot></white></yellow>");
                return true;
            }

            String catId = args[1].toLowerCase();
            int slot;
            try {
                slot = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(sender, "<red>Slot không hợp lệ!</red>");
                return true;
            }

            boolean success = plugin.getShopGUI().removeShopItem(catId, slot);
            if (success) {
                ChatUtils.sendMessage(sender, "<green>Đã xóa vật phẩm tại slot <yellow>" + slot + "</yellow> trong danh mục <gold>" + catId + "</gold>!</green>");
            } else {
                ChatUtils.sendMessage(sender, "<red>Không tìm thấy vật phẩm tại slot <yellow>" + slot + "</yellow> trong danh mục <gold>" + catId + "</gold>!</red>");
            }
            return true;
        }

        if (args.length < 3) {
            ChatUtils.sendMessage(sender, "<red>Usage: /cappyshard [set|add|remove|pay] [player] [amount]</red>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatUtils.sendMessage(sender, getMessage("player-not-found", "<red>Không tìm thấy người chơi!</red>"));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ChatUtils.sendMessage(sender, getMessage("invalid-amount", "<red>Số lượng không hợp lệ!</red>"));
            return true;
        }

        switch (subCommand) {
            case "set":
                if (!sender.hasPermission("cappyshards.admin")) {
                    ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                    return true;
                }
                plugin.getPlayerDataManager().setShards(target.getUniqueId(), amount);
                ChatUtils.sendMessage(sender, getMessage("shard-set", "<green>Đã đặt <gold>%amount%</gold> cho <white>%player%</white>.</green>")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                break;
            case "add":
                if (!sender.hasPermission("cappyshards.admin")) {
                    ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                    return true;
                }
                plugin.getPlayerDataManager().addShards(target.getUniqueId(), amount);
                ChatUtils.sendMessage(sender, getMessage("shard-add", "<green>Đã cộng <gold>%amount%</gold> cho <white>%player%</white>.</green>")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                break;
            case "remove":
                if (!sender.hasPermission("cappyshards.admin")) {
                    ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                    return true;
                }
                plugin.getPlayerDataManager().removeShards(target.getUniqueId(), amount);
                ChatUtils.sendMessage(sender, getMessage("shard-remove", "<green>Đã trừ <gold>%amount%</gold> của <white>%player%</white>.</green>")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                break;
            case "pay":
                if (!(sender instanceof Player player)) {
                    ChatUtils.sendMessage(sender, getMessage("player-only", "<red>Lệnh này chỉ dành cho người chơi!</red>"));
                    return true;
                }
                if (!player.hasPermission("cappyshards.use.pay")) {
                    ChatUtils.sendMessage(player, getMessage("no-permission", "<red>Bạn không có quyền!</red>"));
                    return true;
                }
                if (player.equals(target)) {
                    ChatUtils.sendMessage(player, "<red>Bạn không thể chuyển cho chính mình!</red>");
                    return true;
                }
                
                if (plugin.getPlayerDataManager().isLocked(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, getMessage("action-locked", "<red>Thao tác quá nhanh! Vui lòng thử lại.</red>"));
                    return true;
                }

                if (plugin.getPlayerDataManager().lock(player.getUniqueId())) {
                    try {
                        if (plugin.getPlayerDataManager().removeShards(player.getUniqueId(), amount)) {
                            plugin.getPlayerDataManager().addShards(target.getUniqueId(), amount);
                            ChatUtils.sendMessage(player, getMessage("shard-pay-success", "<green>Chuyển thành công!</green>")
                                    .replace("%player%", target.getName())
                                    .replace("%amount%", String.valueOf(amount)));
                            ChatUtils.sendMessage(target, getMessage("shard-pay-receive", "<green>Nhận thành công!</green>")
                                    .replace("%player%", player.getName())
                                    .replace("%amount%", String.valueOf(amount)));
                        } else {
                            ChatUtils.sendMessage(player, getMessage("not-enough-shards", "<red>Bạn không đủ Shard!</red>"));
                        }
                    } finally {
                        plugin.getPlayerDataManager().unlock(player.getUniqueId());
                    }
                }
                break;
            default:
                ChatUtils.sendMessage(sender, "<red>Lệnh không hợp lệ!</red>");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("cappyshards.admin")) {
                completions.add("set");
                completions.add("add");
                completions.add("remove");
                completions.add("reload");
                completions.add("createcatalogue");
                completions.add("removecatalogue");
                completions.add("additem");
                completions.add("removeitem");
            }
            if (sender.hasPermission("cappyshards.use.pay")) {
                completions.add("pay");
            }
            if (sender.hasPermission("cappyshards.use.shop")) {
                completions.add("shop");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("additem") || sub.equals("removeitem") || sub.equals("removecatalogue")) {
                if (sender.hasPermission("cappyshards.admin")) {
                    completions.addAll(plugin.getShopGUI().getCategories().keySet());
                }
            } else if (sub.equals("createcatalogue")) {
                completions.add("<id>");
            } else if (!sub.equals("reload") && !sub.equals("shop")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("additem")) {
                completions.add("<price>");
            } else if (sub.equals("removeitem")) {
                ShopGUI.CategoryData cat = plugin.getShopGUI().getCategoryData(args[1].toLowerCase());
                if (cat != null) {
                    for (int slot : cat.items.keySet()) {
                        completions.add(String.valueOf(slot));
                    }
                }
            } else if (sub.equals("createcatalogue")) {
                completions.add("<slot>");
            } else if (!sub.equals("reload") && !sub.equals("shop") && !sub.equals("removecatalogue")) {
                completions.add("<amount>");
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sub.equals("additem")) {
                completions.add("[slot]");
            } else if (sub.equals("createcatalogue")) {
                completions.add("[tên_hiển_thị]");
            }
        }
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}

