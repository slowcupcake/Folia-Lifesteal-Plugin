package com.squeakybagco.lifesteal;

import com.squeakybagco.lifesteal.commands.HeartCommand;
import com.squeakybagco.lifesteal.commands.LifestealCommand;
import com.squeakybagco.lifesteal.listeners.CombatListener;
import com.squeakybagco.lifesteal.managers.ConfigManager;
import com.squeakybagco.lifesteal.managers.CustomHeartManager;
import com.squeakybagco.lifesteal.managers.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LifestealPlugin extends JavaPlugin {
    
    private static LifestealPlugin instance;
    private PlayerDataManager playerDataManager;
    private ConfigManager configManager;
    private CustomHeartManager customHeartManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.playerDataManager = new PlayerDataManager(this);

        // Initialize and register custom heart manager
        this.customHeartManager = new CustomHeartManager(this);
        if (configManager.isCustomCraftingEnabled()) {
            customHeartManager.registerRecipes();
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        
        // Register commands and their listeners
        HeartCommand heartCommand = new HeartCommand(this);
        getCommand("lifesteal").setExecutor(new LifestealCommand(this));
        getCommand("hearts").setExecutor(heartCommand);
        getCommand("withdraw").setExecutor(heartCommand);
        
        // Register heart item interaction listener
        getServer().getPluginManager().registerEvents(heartCommand, this);
        getLogger().info("LifestealPlugin has been enabled!");
        
        // Check if running on Folia
        if (isFolia()) {
            getLogger().info("Detected Folia server - using region-based scheduling");
        } else {
            getLogger().warning("Not running on Folia - some features may not work optimally");
        }
    }
    
    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }
        getLogger().info("LifestealPlugin has been disabled!");
    }
    
    public static LifestealPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    public CustomHeartManager getCustomHeartManager() {
        return customHeartManager;
    }

    // Add this setter so ConfigManager can update the heart manager on reload
    public void setCustomHeartManager(CustomHeartManager customHeartManager) {
        this.customHeartManager = customHeartManager;
    }
    
    /**
     * Check if the server is running Folia
     * @return true if running on Folia
     */
    public boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Schedule a task on the region that owns the specified location
     * This is Folia-compatible scheduling
     */
    public void scheduleRegionTask(org.bukkit.Location location, Runnable task) {
        if (isFolia()) {
            // Use Folia's region-based scheduling
            getServer().getRegionScheduler().run(this, location, scheduledTask -> task.run());
        } else {
            // Fallback to regular Bukkit scheduling
            getServer().getScheduler().runTask(this, task);
        }
    }
    
    /**
     * Schedule a delayed task on the region that owns the specified location
     */
    public void scheduleRegionTaskLater(org.bukkit.Location location, Runnable task, long delay) {
        if (isFolia()) {
            getServer().getRegionScheduler().runDelayed(this, location, scheduledTask -> task.run(), delay);
        } else {
            getServer().getScheduler().runTaskLater(this, task, delay);
        }
    }
}