package com.cappy.shards.data;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cappy.shards.CappyShards;

public class PlayerDataManager {

    private final CappyShards plugin;
    private final ConcurrentHashMap<UUID, Long> shardCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicBoolean> operationLocks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long> lastQuitCache = new ConcurrentHashMap<>();

    public PlayerDataManager(CappyShards plugin) {
        this.plugin = plugin;
    }

    public java.util.concurrent.CompletableFuture<PlayerData> loadData(UUID uuid) {
        return plugin.getDatabaseManager().getPlayerData(uuid).thenApply(data -> {
            shardCache.put(uuid, data.getShards());
            lastQuitCache.put(uuid, data.getLastQuit());
            operationLocks.putIfAbsent(uuid, new AtomicBoolean(false));
            return data;
        });
    }

    public void saveData(UUID uuid) {
        Long shards = shardCache.get(uuid);
        if (shards != null) {
            long now = System.currentTimeMillis();
            plugin.getDatabaseManager().savePlayerData(uuid, shards, now);
            shardCache.remove(uuid);
            lastQuitCache.remove(uuid);
            operationLocks.remove(uuid);
        }
    }

    public void updateLastQuit(UUID uuid, long timestamp) {
        lastQuitCache.put(uuid, timestamp);
        Long shards = shardCache.get(uuid);
        if (shards != null) {
            plugin.getDatabaseManager().savePlayerData(uuid, shards, timestamp);
        }
    }

    public long getShards(UUID uuid) {
        return shardCache.getOrDefault(uuid, 0L);
    }

    public void setShards(UUID uuid, long amount) {
        shardCache.put(uuid, Math.max(0, amount));
    }

    public void addShards(UUID uuid, long amount) {
        setShards(uuid, getShards(uuid) + amount);
    }

    public boolean removeShards(UUID uuid, long amount) {
        long current = getShards(uuid);
        if (current >= amount) {
            setShards(uuid, current - amount);
            return true;
        }
        return false;
    }

    /**
     * Checks if a player's operations are currently locked (Anti-Dupe).
     */
    public boolean isLocked(UUID uuid) {
        AtomicBoolean lock = operationLocks.get(uuid);
        return lock != null && lock.get();
    }

    /**
     * Locks the player's operations safely. Returns true if lock was acquired.
     */
    public boolean lock(UUID uuid) {
        AtomicBoolean lock = operationLocks.computeIfAbsent(uuid, k -> new AtomicBoolean(false));
        return lock.compareAndSet(false, true);
    }

    /**
     * Unlocks the player's operations.
     */
    public void unlock(UUID uuid) {
        AtomicBoolean lock = operationLocks.get(uuid);
        if (lock != null) {
            lock.set(false);
        }
    }
}

