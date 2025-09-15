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
        // Re-initialize CustomHeartManager and register recipes if enabled
        CustomHeartManager newHeartManager = new CustomHeartManager(plugin);
        plugin.setCustomHeartManager(newHeartManager);
        if (isCustomCraftingEnabled()) {
            newHeartManager.registerRecipes();
        }
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
    
    // Custom Heart Item Configuration
    public String getHeartItemMaterial() {
        return config.getString("heart-items.custom.base-material", "PAPER");
    }
    
    public String getHeartItemName() {
        return translateColorCodes(config.getString("heart-items.custom.name", "&c❤ &lLifesteal Heart &c❤"));
    }
    
    public List<String> getHeartItemLore() {
        List<String> lore = config.getStringList("heart-items.custom.lore");
        if (lore.isEmpty()) {
            lore.add("&7Right-click to consume");
            lore.add("&7and gain &c{hearts} &7heart(s)");
            lore.add("");
            lore.add("&6⚠ &eThis item is precious! &6⚠");
            lore.add("&8Custom Lifesteal Heart");
        }
        
        // Translate color codes
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, translateColorCodes(lore.get(i)));
        }
        
        return lore;
    }
    
    public int getHeartItemModelData() {
        return config.getInt("heart-items.custom.custom-model-data", 0);
    }
    
    public boolean isHeartItemGlowEnabled() {
        return config.getBoolean("heart-items.custom.glow-effect", true);
    }
    
    public boolean isCustomCraftingEnabled() {
        return config.getBoolean("heart-items.custom.crafting-enabled", true);
    }
    
    public String[] getCraftingPattern() {
        List<String> pattern = config.getStringList("heart-items.custom.crafting-pattern");
        if (pattern.size() != 3) {
            // Default pattern
            return new String[]{"GDG", "DHD", "GDG"};
        }
        return pattern.toArray(new String[0]);
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