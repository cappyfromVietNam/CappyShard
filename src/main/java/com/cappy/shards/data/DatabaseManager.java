package com.cappy.shards.data;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.cappy.shards.CappyShards;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final CappyShards plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(CappyShards plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File dbFile = new File(plugin.getDataFolder(), "database.db");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("CappyShards-SQLite-Pool");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        
        dataSource = new HikariDataSource(config);
        
        createTables();
    }

    private void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS player_shards (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "shards BIGINT NOT NULL DEFAULT 0," +
                "last_quit BIGINT NOT NULL DEFAULT 0" +
                ");";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            // Safely alter existing table to add last_quit if migrating from an older version
            try {
                stmt.execute("ALTER TABLE player_shards ADD COLUMN last_quit BIGINT NOT NULL DEFAULT 0;");
            } catch (SQLException ignored) {
                // Column already exists
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating database table: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT shards, last_quit FROM player_shards WHERE uuid = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return new PlayerData(rs.getLong("shards"), rs.getLong("last_quit"));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error in getPlayerData: " + e.getMessage());
            }
            return new PlayerData(0L, 0L);
        });
    }

    public CompletableFuture<Long> getShards(UUID uuid) {
        return getPlayerData(uuid).thenApply(PlayerData::getShards);
    }

    public CompletableFuture<Void> savePlayerData(UUID uuid, long amount, long lastQuit) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO player_shards (uuid, shards, last_quit) VALUES (?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET shards = ?, last_quit = ?;";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, uuid.toString());
                stmt.setLong(2, amount);
                stmt.setLong(3, lastQuit);
                stmt.setLong(4, amount);
                stmt.setLong(5, lastQuit);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error in savePlayerData: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> saveShards(UUID uuid, long amount) {
        return savePlayerData(uuid, amount, System.currentTimeMillis());
    }
}

