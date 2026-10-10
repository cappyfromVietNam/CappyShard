package com.cappy.shards;

import com.cappy.shards.commands.ShardCommand;
import com.cappy.shards.data.DatabaseManager;
import com.cappy.shards.data.PlayerDataManager;
import com.cappy.shards.gui.ShopGUI;
import com.cappy.shards.listeners.InventoryListener;
import com.cappy.shards.listeners.PlayerConnectionListener;
import com.cappy.shards.managers.AfkRewardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class CappyShards extends JavaPlugin {

    private static CappyShards instance;
    private Logger logger;

    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private AfkRewardManager afkRewardManager;
    private ShopGUI shopGUI;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();

        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("Shop/catalogue.yml", false);
        saveResource("Shop/shop.yml", false);

        // Init Data
        databaseManager = new DatabaseManager(this);
        databaseManager.init();
        playerDataManager = new PlayerDataManager(this);

        // Load configs
        shopGUI = new ShopGUI(this);
        shopGUI.loadShop();

        afkRewardManager = new AfkRewardManager(this);
        afkRewardManager.start();

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        // Commands
        ShardCommand command = new ShardCommand(this);
        getCommand("cappyshard").setExecutor(command);
        getCommand("cappyshard").setTabCompleter(command);
        getCommand("shardshop").setExecutor(command);

        // Load for online players if reloaded
        for (Player p : getServer().getOnlinePlayers()) {
            playerDataManager.loadData(p.getUniqueId());
        }

        logger.info("CappyShards has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (afkRewardManager != null) {
            afkRewardManager.stop();
        }
        
        if (playerDataManager != null && databaseManager != null) {
            for (Player p : getServer().getOnlinePlayers()) {
                playerDataManager.saveData(p.getUniqueId());
            }
        }
        
        if (databaseManager != null) {
            databaseManager.close();
        }

        logger.info("CappyShards has been disabled.");
        instance = null;
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
}
