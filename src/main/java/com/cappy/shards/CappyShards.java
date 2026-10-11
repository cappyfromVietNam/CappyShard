package com.cappy.shards;

import com.cappy.shards.commands.ShardCommand;
import com.cappy.shards.data.DatabaseManager;
import com.cappy.shards.data.PlayerDataManager;
import com.cappy.shards.gui.ShopGUI;
import com.cappy.shards.listeners.InventoryListener;
import com.cappy.shards.listeners.PlayerConnectionListener;
import com.cappy.shards.managers.AfkRewardManager;
import com.cappy.shards.utils.ChatUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public final class CappyShards extends JavaPlugin {

    private static CappyShards instance;
    private Logger logger;

    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private AfkRewardManager afkRewardManager;
    private ShopGUI shopGUI;
    private com.cappy.shards.managers.ItemManager itemManager;
    private com.cappy.shards.managers.OfflineRewardManager offlineRewardManager;
    private com.cappy.shards.managers.RankManager rankManager;
    private YamlConfiguration messages;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("item.yml", false);
        saveResource("Shop/catalogue.yml", false);
        saveResource("Shop/shop.yml", false);

        loadMessages();

        // Init Data
        databaseManager = new DatabaseManager(this);
        databaseManager.init();
        playerDataManager = new PlayerDataManager(this);

        // Load configs & managers
        rankManager = new com.cappy.shards.managers.RankManager(this);

        itemManager = new com.cappy.shards.managers.ItemManager(this);
        itemManager.loadConfig();

        offlineRewardManager = new com.cappy.shards.managers.OfflineRewardManager(this);

        shopGUI = new ShopGUI(this);
        shopGUI.loadShop();

        afkRewardManager = new AfkRewardManager(this);
        afkRewardManager.start();

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new com.cappy.shards.listeners.MultiplierItemListener(this), this);

        // Commands
        ShardCommand command = new ShardCommand(this);
        getCommand("cappyshard").setExecutor(command);
        getCommand("cappyshard").setTabCompleter(command);
        getCommand("shardshop").setExecutor(command);

        // Load for online players if reloaded
        for (Player p : getServer().getOnlinePlayers()) {
            playerDataManager.loadData(p.getUniqueId());
        }

        // PlaceholderAPI Expansion
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.cappy.shards.hook.CappyShardsExpansion(this).register();
            logger.info("Hooked into PlaceholderAPI successfully!");
        }

        logger.info("CappyShards has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (afkRewardManager != null) {
            afkRewardManager.stop();
        }
        
        if (playerDataManager != null && databaseManager != null) {
            playerDataManager.saveAllSync();
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }

        ChatUtils.clearCache();
        logger.info("CappyShards has been disabled.");
        instance = null;
    }

    public void loadMessages() {
        File msgFile = new File(getDataFolder(), "messages.yml");
        if (msgFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(msgFile);
        }
    }

    public String getMessage(String key, String def) {
        String prefix = messages != null ? messages.getString("prefix", "") : "";
        String msg = messages != null ? messages.getString(key, def) : def;
        return prefix + msg;
    }

    public String getRawMessage(String key, String def) {
        return messages != null ? messages.getString(key, def) : def;
    }

    public static CappyShards getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public AfkRewardManager getAfkRewardManager() {
        return afkRewardManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public com.cappy.shards.managers.ItemManager getItemManager() {
        return itemManager;
    }

    public com.cappy.shards.managers.OfflineRewardManager getOfflineRewardManager() {
        return offlineRewardManager;
    }

    public com.cappy.shards.managers.RankManager getRankManager() {
        return rankManager;
    }
}
