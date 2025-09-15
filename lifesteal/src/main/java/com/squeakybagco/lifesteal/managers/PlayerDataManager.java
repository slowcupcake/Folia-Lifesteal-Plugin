package com.squeakybagco.lifesteal.managers;

import com.squeakybagco.lifesteal.LifestealPlugin;
import com.squeakybagco.lifesteal.storage.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    
    private final LifestealPlugin plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<UUID, Long> withdrawCooldowns;
    
    public PlayerDataManager(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        this.playerDataCache = new ConcurrentHashMap<>();
        this.withdrawCooldowns = new ConcurrentHashMap<>();
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    public void loadPlayerData(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (playerDataCache.containsKey(playerId)) {
            return; // Already loaded
        }
        
        File playerFile = new File(dataFolder, playerId + ".yml");
        PlayerData data;
        
        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data = new PlayerData(
                playerId,
                config.getInt("hearts", plugin.getConfigManager().getDefaultHearts()),
                config.getLong("last-death", 0),
                config.getInt("kills", 0),
                config.getInt("deaths", 0)
            );
        } else {
            data = new PlayerData(playerId, plugin.getConfigManager().getDefaultHearts(), 0, 0, 0);
            savePlayerData(data); // Save default data
        }
        
        playerDataCache.put(playerId, data);
        plugin.getConfigManager().debug("Loaded data for player: " + player.getName() + " (Hearts: " + data.getHearts() + ")");
    }
    
    public void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId());
    }
    
    public void savePlayerData(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null) {
            return;
        }
        
        savePlayerData(data);
    }
    
    private void savePlayerData(PlayerData data) {
        File playerFile = new File(dataFolder, data.getPlayerId() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("hearts", data.getHearts());
        config.set("last-death", data.getLastDeath());
        config.set("kills", data.getKills());
        config.set("deaths", data.getDeaths());
        config.set("last-updated", System.currentTimeMillis());
        
        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + data.getPlayerId() + ": " + e.getMessage());
        }
    }
    
    public void saveAllData() {
        plugin.getLogger().info("Saving all player data...");
        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }
        plugin.getLogger().info("Saved data for " + playerDataCache.size() + " players.");
    }
    
    public int getPlayerHearts(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        return data != null ? data.getHearts() : plugin.getConfigManager().getDefaultHearts();
    }
    
    public void setPlayerHearts(UUID playerId, int hearts) {
        PlayerData data = playerDataCache.get(playerId);
        if (data == null) {
            data = new PlayerData(playerId, hearts, 0, 0, 0);
            playerDataCache.put(playerId, data);
        } else {
            data.setHearts(hearts);
        }
        
        plugin.getConfigManager().debug("Set hearts for " + playerId + " to " + hearts);
    }
    
    public void addPlayerHearts(UUID playerId, int hearts) {
        int currentHearts = getPlayerHearts(playerId);
        int newHearts = Math.min(currentHearts + hearts, plugin.getConfigManager().getMaxHearts());
        setPlayerHearts(playerId, newHearts);
    }
    
    public void removePlayerHearts(UUID playerId, int hearts) {
        int currentHearts = getPlayerHearts(playerId);
        int newHearts = Math.max(currentHearts - hearts, plugin.getConfigManager().getMinHearts());
        setPlayerHearts(playerId, newHearts);
    }
    
    public void addKill(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data != null) {
            data.setKills(data.getKills() + 1);
        }
    }
    
    public void addDeath(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        if (data != null) {
            data.setDeaths(data.getDeaths() + 1);
            data.setLastDeath(System.currentTimeMillis());
        }
    }
    
    public int getKills(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        return data != null ? data.getKills() : 0;
    }
    
    public int getDeaths(UUID playerId) {
        PlayerData data = playerDataCache.get(playerId);
        return data != null ? data.getDeaths() : 0;
    }
    
    public boolean isOnWithdrawCooldown(UUID playerId) {
        Long lastWithdraw = withdrawCooldowns.get(playerId);
        if (lastWithdraw == null) {
            return false;
        }
        
        long cooldownTime = plugin.getConfigManager().getWithdrawCooldown() * 1000L; // Convert to milliseconds
        return (System.currentTimeMillis() - lastWithdraw) < cooldownTime;
    }
    
    public void setWithdrawCooldown(UUID playerId) {
        withdrawCooldowns.put(playerId, System.currentTimeMillis());
    }
    
    public long getWithdrawCooldownRemaining(UUID playerId) {
        Long lastWithdraw = withdrawCooldowns.get(playerId);
        if (lastWithdraw == null) {
            return 0;
        }
        
        long cooldownTime = plugin.getConfigManager().getWithdrawCooldown() * 1000L;
        long remaining = cooldownTime - (System.currentTimeMillis() - lastWithdraw);
        return Math.max(0, remaining / 1000); // Return in seconds
    }
    
    public void unloadPlayerData(UUID playerId) {
        savePlayerData(playerId);
        playerDataCache.remove(playerId);
        withdrawCooldowns.remove(playerId);
    }
    
    public PlayerData getPlayerData(UUID playerId) {
        return playerDataCache.get(playerId);
    }
}