package com.cappy.shards.data;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cappy.shards.CappyShards;

public class PlayerDataManager {

    private final CappyShards plugin;
    private final ConcurrentHashMap<UUID, Long> shardCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicBoolean> operationLocks = new ConcurrentHashMap<>();

    public PlayerDataManager(CappyShards plugin) {
        this.plugin = plugin;
    }

    public void loadData(UUID uuid) {
        plugin.getDatabaseManager().getShards(uuid).thenAccept(shards -> {
            shardCache.put(uuid, shards);
            operationLocks.putIfAbsent(uuid, new AtomicBoolean(false));
        });
    }

    public void saveData(UUID uuid) {
        Long shards = shardCache.get(uuid);
        if (shards != null) {
            plugin.getDatabaseManager().saveShards(uuid, shards);
            shardCache.remove(uuid);
            operationLocks.remove(uuid);
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

