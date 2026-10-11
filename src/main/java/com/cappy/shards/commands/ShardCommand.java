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
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>This command can only be executed by players!</red>"));
                return true;
            }
            if (!player.hasPermission("cappyshards.use.shop")) {
                ChatUtils.sendMessage(player, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                return true;
            }
            plugin.getShopGUI().open(player);
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                long balance = plugin.getPlayerDataManager().getShards(player.getUniqueId());
                ChatUtils.sendMessage(player, getMessage("shard-balance", "<green>Your Shard balance: <gold>%amount%</gold></green>")
                        .replace("%amount%", String.valueOf(balance)));
            } else {
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>This command can only be executed by players!</red>"));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("reload")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                return true;
            }
            plugin.reloadConfig();
            plugin.loadMessages();
            loadMessages();
            plugin.getShopGUI().loadShop();
            plugin.getAfkRewardManager().loadConfig();
            plugin.getAfkRewardManager().stop();
            plugin.getAfkRewardManager().start();
            ChatUtils.sendMessage(sender, getMessage("reload-success", "<green>CappyShards configuration has been successfully reloaded!</green>"));
            return true;
        }

        if (subCommand.equals("shop")) {
            if (!(sender instanceof Player player)) {
                ChatUtils.sendMessage(sender, getMessage("player-only", "<red>This command can only be executed by players!</red>"));
                return true;
            }
            if (!player.hasPermission("cappyshards.use.shop")) {
                ChatUtils.sendMessage(player, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                return true;
            }
            plugin.getShopGUI().open(player);
            return true;
        }

        if (subCommand.equals("give")) {
            if (!sender.hasPermission("cappyshards.admin")) {
                ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                return true;
            }

            // Syntax: /cappy give [player] <shard_multiplier> <multiplier> [duration_minutes]
            Player target;
            String itemType;
            String multArg;
            String durationArg = "30";

            if (args.length >= 3 && (args[1].toLowerCase().contains("multi") || args[1].equalsIgnoreCase("shard_multiplier") || args[1].equalsIgnoreCase("shard_mutiplayer"))) {
                if (!(sender instanceof Player p)) {
                    ChatUtils.sendMessage(sender, "<red>Please specify a target player when executing from console: /cappy give <player> shard_multiplier <multiplier> [minutes]</red>");
                    return true;
                }
                target = p;
                itemType = args[1];
                multArg = args[2];
                if (args.length >= 4) {
                    durationArg = args[3];
                }
            } else if (args.length >= 4) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    ChatUtils.sendMessage(sender, getMessage("player-not-found", "<red>Player not found (or they have never joined)!</red>"));
                    return true;
                }
                itemType = args[2];
                multArg = args[3];
                if (args.length >= 5) {
                    durationArg = args[4];
                }
            } else {
                ChatUtils.sendMessage(sender, "<red>Usage: /cappy give [player] shard_multiplier <multiplier> [duration_minutes]</red>");
                return true;
            }

            if (!itemType.toLowerCase().contains("multi")) {
                ChatUtils.sendMessage(sender, "<red>Unknown item type! Available: shard_multiplier</red>");
                return true;
            }

            double multiplier;
            try {
                multiplier = Double.parseDouble(multArg);
                if (multiplier <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(sender, getMessage("invalid-amount", "<red>Invalid multiplier!</red>"));
                return true;
            }

            int durationMinutes = 30;
            try {
                durationMinutes = Integer.parseInt(durationArg);
                if (durationMinutes <= 0) durationMinutes = 30;
            } catch (NumberFormatException ignored) {}

            org.bukkit.inventory.ItemStack bottle = com.cappy.shards.items.ShardMultiplierItem.create(multiplier, durationMinutes);
            target.getInventory().addItem(bottle);

            String formattedMult = (multiplier % 1 == 0 ? String.valueOf((long) multiplier) : String.valueOf(multiplier));
            ChatUtils.sendMessage(sender, "<green>Given <white>1x Shard Multiplier (x" + formattedMult + ", " + durationMinutes + "m)</white> to <gold>" + target.getName() + "</gold>.</green>");
            if (!sender.equals(target)) {
                ChatUtils.sendMessage(target, "<green>You received <white>1x Shard Multiplier (x" + formattedMult + ", " + durationMinutes + "m)</white>!</green>");
            }
            return true;
        }

        if (args.length < 3) {
            ChatUtils.sendMessage(sender, "<red>Usage: /cappyshard [set|add|remove|pay|give] [player] [amount]</red>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatUtils.sendMessage(sender, getMessage("player-not-found", "<red>Player not found (or they have never joined)!</red>"));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            ChatUtils.sendMessage(sender, getMessage("invalid-amount", "<red>Invalid amount!</red>"));
            return true;
        }

        switch (subCommand) {
            case "set":
                if (!sender.hasPermission("cappyshards.admin")) {
                    ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                    return true;
                }
                plugin.getPlayerDataManager().setShards(target.getUniqueId(), amount);
                ChatUtils.sendMessage(sender, getMessage("shard-set", "<green>Set the balance of <white>%player%</white> to <gold>%amount%</gold> Shards.</green>")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                break;
            case "add":
                if (!sender.hasPermission("cappyshards.admin")) {
                    ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                    return true;
                }
                plugin.getPlayerDataManager().addShards(target.getUniqueId(), amount);
                ChatUtils.sendMessage(sender, getMessage("shard-add", "<green>Added <gold>%amount%</gold> Shards to <white>%player%</white>.</green>")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                break;
            case "remove":
                if (!sender.hasPermission("cappyshards.admin")) {
                    ChatUtils.sendMessage(sender, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                    return true;
                }
                plugin.getPlayerDataManager().removeShards(target.getUniqueId(), amount);
                ChatUtils.sendMessage(sender, getMessage("shard-remove", "<green>Removed <gold>%amount%</gold> Shards from <white>%player%</white>.</green>")
                        .replace("%player%", target.getName())
                        .replace("%amount%", String.valueOf(amount)));
                break;
            case "pay":
                if (!(sender instanceof Player player)) {
                    ChatUtils.sendMessage(sender, getMessage("player-only", "<red>This command can only be executed by players!</red>"));
                    return true;
                }
                if (!player.hasPermission("cappyshards.use.pay")) {
                    ChatUtils.sendMessage(player, getMessage("no-permission", "<red>You do not have permission to use this command!</red>"));
                    return true;
                }
                if (player.equals(target)) {
                    ChatUtils.sendMessage(player, getMessage("cannot-pay-self", "<red>You cannot send Shards to yourself!</red>"));
                    return true;
                }
                
                if (plugin.getPlayerDataManager().isLocked(player.getUniqueId())) {
                    ChatUtils.sendMessage(player, getMessage("action-locked", "<red>You are clicking too fast! Please try again.</red>"));
                    return true;
                }

                if (plugin.getPlayerDataManager().lock(player.getUniqueId())) {
                    try {
                        if (plugin.getPlayerDataManager().removeShards(player.getUniqueId(), amount)) {
                            plugin.getPlayerDataManager().addShards(target.getUniqueId(), amount);
                            ChatUtils.sendMessage(player, getMessage("shard-pay-success", "<green>You have sent <gold>%amount%</gold> Shards to <white>%player%</white>.</green>")
                                    .replace("%player%", target.getName())
                                    .replace("%amount%", String.valueOf(amount)));
                            ChatUtils.sendMessage(target, getMessage("shard-pay-receive", "<green>You have received <gold>%amount%</gold> Shards from <white>%player%</white>.</green>")
                                    .replace("%player%", player.getName())
                                    .replace("%amount%", String.valueOf(amount)));
                        } else {
                            ChatUtils.sendMessage(player, getMessage("not-enough-shards", "<red>You do not have enough Shards to perform this transaction!</red>"));
                        }
                    } finally {
                        plugin.getPlayerDataManager().unlock(player.getUniqueId());
                    }
                }
                break;
            default:
                ChatUtils.sendMessage(sender, getMessage("invalid-command", "<red>Unknown command! Type /cappyshard for help.</red>"));
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
                completions.add("give");
            }
            if (sender.hasPermission("cappyshards.use.pay")) {
                completions.add("pay");
            }
            if (sender.hasPermission("cappyshards.use.shop")) {
                completions.add("shop");
            }
        } else if (args[0].equalsIgnoreCase("give") && sender.hasPermission("cappyshards.admin")) {
            if (args.length == 2) {
                completions.add("shard_multiplier");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (args.length == 3) {
                if (args[1].toLowerCase().contains("multi")) {
                    completions.addAll(List.of("1.5", "2.0", "3.0"));
                } else {
                    completions.add("shard_multiplier");
                }
            } else if (args.length == 4) {
                if (args[1].toLowerCase().contains("multi")) {
                    completions.addAll(List.of("15", "30", "60", "120"));
                } else {
                    completions.addAll(List.of("1.5", "2.0", "3.0"));
                }
            } else if (args.length == 5) {
                completions.addAll(List.of("15", "30", "60", "120"));
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

