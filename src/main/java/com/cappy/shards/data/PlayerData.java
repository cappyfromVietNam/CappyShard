package com.cappy.shards.data;

public class PlayerData {
    private final long shards;
    private final long lastQuit;

    public PlayerData(long shards, long lastQuit) {
        this.shards = shards;
        this.lastQuit = lastQuit;
    }

    public long getShards() {
        return shards;
    }

    public long getLastQuit() {
        return lastQuit;
    }
}
