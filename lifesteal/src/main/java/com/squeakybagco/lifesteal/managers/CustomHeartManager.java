package com.squeakybagco.lifesteal.managers;

import com.squeakybagco.lifesteal.LifestealPlugin;

import net.kyori.adventure.text.TextComponent;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public class CustomHeartManager {
    
    private final LifestealPlugin plugin;
    private final NamespacedKey heartItemKey;
    private final NamespacedKey recipeKey;
    
    public CustomHeartManager(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.heartItemKey = new NamespacedKey(plugin, "lifesteal_heart");
        this.recipeKey = new NamespacedKey(plugin, "lifesteal_heart_recipe");
    }
    
    public void registerRecipes() {
        // Remove existing recipe if it exists
        plugin.getServer().removeRecipe(recipeKey);
        
        // Create the heart item
        ItemStack heartItem = createCustomHeartItem();
        
        // Create shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, heartItem);
        
        // Define the crafting pattern (configurable in config.yml)
        String[] pattern = plugin.getConfigManager().getCraftingPattern();
        recipe.shape(pattern[0], pattern[1], pattern[2]);
        
        // Set the ingredients (also configurable)
        recipe.setIngredient('G', Material.GOLD_BLOCK);        // G = Gold Block
        recipe.setIngredient('D', Material.DIAMOND);           // D = Diamond  
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);    // R = Redstone Block
        recipe.setIngredient('N', Material.NETHER_STAR);       // N = Nether Star
        recipe.setIngredient('E', Material.EMERALD_BLOCK);     // E = Emerald Block
        recipe.setIngredient('B', Material.BEACON);            // B = Beacon
        recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);  // T = Totem
        recipe.setIngredient('H', Material.GOLDEN_APPLE);      // H = Golden Apple (Heart themed)
        
        // Register the recipe
        plugin.getServer().addRecipe(recipe);
        plugin.getLogger().info("Registered custom heart crafting recipe!");
    }
    
    /**
     * Creates a custom heart item using a base material with NBT data
     * This makes it unique and different from normal items
     */
    public ItemStack createCustomHeartItem() {
        // Use configurable base material (default: paper for flexibility)
        Material baseMaterial = Material.valueOf(plugin.getConfigManager().getHeartItemMaterial());
        ItemStack heart = new ItemStack(baseMaterial, 1);
        
        ItemMeta meta = heart.getItemMeta();
        if (meta == null) return heart;
        
        // Set custom name with configurable colors
        String heartName = plugin.getConfigManager().getHeartItemName();
        // Use Adventure API for display name to avoid deprecation
        meta.displayName(net.kyori.adventure.text.Component.text(heartName));
        
        // Set custom lore
        List<String> lore = plugin.getConfigManager().getHeartItemLore();
        List<TextComponent> adventureLore = lore.stream()
            .map(net.kyori.adventure.text.Component::text)
            .toList();
        meta.lore(adventureLore);
        
        // Add custom NBT data to make it unique
        meta.getPersistentDataContainer().set(heartItemKey, PersistentDataType.STRING, "lifesteal_heart");
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "heart_value"), PersistentDataType.INTEGER, 2);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "creation_time"), PersistentDataType.LONG, System.currentTimeMillis());
        
        // Set custom model data for resource pack support
        int customModelData = plugin.getConfigManager().getHeartItemModelData();
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        // Make it unbreakable and hide attributes
        meta.setUnbreakable(true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
        
        // Apply enchantment glow if enabled
        if (plugin.getConfigManager().isHeartItemGlowEnabled()) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        
        heart.setItemMeta(meta);
        return heart;
    }
    
    /**
     * Check if an ItemStack is our custom heart item
     */
    public boolean isCustomHeartItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        
        // Check for our custom NBT data
        return meta.getPersistentDataContainer().has(heartItemKey, PersistentDataType.STRING) &&
               "lifesteal_heart".equals(meta.getPersistentDataContainer().get(heartItemKey, PersistentDataType.STRING));
    }
    
    /**
     * Get the heart value from a custom heart item
     */
    public int getHeartValue(ItemStack item) {
        if (!isCustomHeartItem(item)) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        
        NamespacedKey valueKey = new NamespacedKey(plugin, "heart_value");
        return meta.getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.INTEGER, 2);
    }
    
    /**
     * Create a heart item with specific value
     */
    public ItemStack createHeartItem(int heartValue) {
        ItemStack heart = createCustomHeartItem();
        ItemMeta meta = heart.getItemMeta();
        
        if (meta != null) {
            // Update the heart value in NBT
            NamespacedKey valueKey = new NamespacedKey(plugin, "heart_value");
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.INTEGER, heartValue);
            
            // Update lore to reflect the value
            List<net.kyori.adventure.text.Component> lore = meta.lore();
            if (lore != null && !lore.isEmpty()) {
                // Replace heart value in lore
                for (int i = 0; i < lore.size(); i++) {
                    net.kyori.adventure.text.Component line = lore.get(i);
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                    if (plain.contains("{hearts}")) {
                        String replaced = plain.replace("{hearts}", String.valueOf(heartValue / 2.0));
                        lore.set(i, net.kyori.adventure.text.Component.text(replaced));
                    }
                }
                meta.lore(lore);
            }
            
            heart.setItemMeta(meta);
        }
        
        return heart;
    }
    
    /**
     * Remove all registered recipes (for plugin disable)
     */
    public void unregisterRecipes() {
        plugin.getServer().removeRecipe(recipeKey);
    }
}