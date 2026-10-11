package com.cappy.shards.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.cappy.shards.CappyShards;
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
            }
            if (sender.hasPermission("cappyshards.use.pay")) {
                completions.add("pay");
            }
            if (sender.hasPermission("cappyshards.use.shop")) {
                completions.add("shop");
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("shop")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && !args[0].equalsIgnoreCase("reload") && !args[0].equalsIgnoreCase("shop")) {
            completions.add("<amount>");
        }
        return completions;
    }
}

