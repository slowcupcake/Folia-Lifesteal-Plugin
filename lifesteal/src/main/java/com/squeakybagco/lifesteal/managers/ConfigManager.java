package com.squeakybagco.lifesteal.managers;

import com.squeakybagco.lifesteal.LifestealPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    
    private final LifestealPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;
    
    public ConfigManager(LifestealPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }
    
    public void loadConfigs() {
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Load messages.yml
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public void reloadConfigs() {
        loadConfigs();
    }
    
    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }
    
    // Config getters
    public int getDefaultHearts() {
        return config.getInt("hearts.default", 20);
    }
    
    public int getMaxHearts() {
        return config.getInt("hearts.max", 40);
    }
    
    public int getMinHearts() {
        return config.getInt("hearts.min", 2);
    }
    
    public int getHeartsPerKill() {
        return config.getInt("hearts.per-kill", 2);
    }
    
    public boolean isLifestealEnabledInWorld(String worldName) {
        List<String> enabledWorlds = config.getStringList("enabled-worlds");
        return enabledWorlds.isEmpty() || enabledWorlds.contains(worldName);
    }
    
    public boolean isEliminationEnabled() {
        return config.getBoolean("elimination.enabled", true);
    }
    
    public boolean isBanOnElimination() {
        return config.getBoolean("elimination.ban-player", false);
    }
    
    public List<String> getEliminationCommands() {
        return config.getStringList("elimination.commands");
    }
    
    public boolean isWithdrawEnabled() {
        return config.getBoolean("heart-items.withdraw-enabled", true);
    }
    
    public int getWithdrawCooldown() {
        return config.getInt("heart-items.withdraw-cooldown", 300); // 5 minutes
    }
    
    public boolean isHeartItemsEnabled() {
        return config.getBoolean("heart-items.enabled", true);
    }
    
    // Message getters with color code translation
    public String getMessage(String key) {
        String message = messages.getString("messages." + key, "&cMessage not found: " + key);
        return translateColorCodes(message);
    }
    
    public String getMessage(String key, String defaultMessage) {
        String message = messages.getString("messages." + key, defaultMessage);
        return translateColorCodes(message);
    }
    
    public String getPrefix() {
        return translateColorCodes(messages.getString("prefix", "&8[&cLifesteal&8] &r"));
    }
    
    private String translateColorCodes(String message) {
        return message.replace('&', '§');
    }
    
    // Utility methods
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    public void debug(String message) {
        if (isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}