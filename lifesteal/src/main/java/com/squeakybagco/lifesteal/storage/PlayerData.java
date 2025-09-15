package com.squeakybagco.lifesteal.storage;

import java.util.UUID;

public class PlayerData {
    
    private final UUID playerId;
    private int hearts;
    private long lastDeath;
    private int kills;
    private int deaths;
    
    public PlayerData(UUID playerId, int hearts, long lastDeath, int kills, int deaths) {
        this.playerId = playerId;
        this.hearts = hearts;
        this.lastDeath = lastDeath;
        this.kills = kills;
        this.deaths = deaths;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public int getHearts() {
        return hearts;
    }
    
    public void setHearts(int hearts) {
        this.hearts = hearts;
    }
    
    public long getLastDeath() {
        return lastDeath;
    }
    
    public void setLastDeath(long lastDeath) {
        this.lastDeath = lastDeath;
    }
    
    public int getKills() {
        return kills;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    public double getKDRatio() {
        if (deaths == 0) {
            return kills > 0 ? kills : 0.0;
        }
        return (double) kills / deaths;
    }
    
    @Override
    public String toString() {
        return "PlayerData{" +
                "playerId=" + playerId +
                ", hearts=" + hearts +
                ", lastDeath=" + lastDeath +
                ", kills=" + kills +
                ", deaths=" + deaths +
                '}';
    }
}