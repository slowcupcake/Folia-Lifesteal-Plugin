package com.squeakybagco.lifesteal.utils;

import com.squeakybagco.lifesteal.LifestealPlugin;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class HeartUtils {
    
    private static final LifestealPlugin plugin = LifestealPlugin.getInstance();
    
    /**
     * Updates a player's maximum health based on their heart count
     * @param player The player to update
     * @param hearts The number of half-hearts (20 = 10 full hearts)
     */
    public static void updatePlayerMaxHealth(Player player, int hearts) {
        double maxHealth = Math.max(1.0, hearts); // Ensure at least 0.5 hearts
        
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            
            // Heal player if their current health is above new max
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
            
            plugin.getConfigManager().debug("Updated " + player.getName() + " max health to " + maxHealth);
        }
    }
    
    /**
     * Creates a heart item that can be consumed
     * @param amount Number of hearts this item provides
     * @return ItemStack representing the heart item
     */
    public static ItemStack createHeartItem(int amount) {
        ItemStack heart = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = heart.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§c❤ Heart §c❤"));
            
            List<net.kyori.adventure.text.Component> lore = Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Right-click to consume"),
                net.kyori.adventure.text.Component.text("§7and gain §c" + (amount / 2.0) + " §7heart" + (amount == 2 ? "" : "s")),
                net.kyori.adventure.text.Component.text(""),
                net.kyori.adventure.text.Component.text("§8Lifesteal Heart Item")
            );
            meta.lore(lore);
            
            // Add enchantment glint
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            heart.setItemMeta(meta);
        }
        
        return heart;
    }
    
    /**
     * Checks if an ItemStack is a heart item
     * @param item The item to check
     * @return true if it's a heart item
     */
    public static boolean isHeartItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        // Use Adventure Component API to compare display names (for modern Bukkit/Spigot)
        if (meta.hasDisplayName() && meta.displayName() != null) {
            // Compare legacy text to component
            net.kyori.adventure.text.Component expected = net.kyori.adventure.text.Component.text("§c❤ Heart §c❤");
            return meta.displayName().equals(expected);
        }
        return false;
    }
    
    /**
     * Gets the heart value from a heart item
     * @param item The heart item
     * @return Number of half-hearts this item provides
     */
    public static int getHeartValue(ItemStack item) {
        if (!isHeartItem(item)) {
            return 0;
        }
        
        // Default heart value is 2 half-hearts (1 full heart)
        return 2;
    }
    
    /**
     * Converts half-hearts to a display string
     * @param halfHearts Number of half-hearts
     * @return Formatted string (e.g., "10.5 hearts")
     */
    public static String formatHearts(int halfHearts) {
        double hearts = halfHearts / 2.0;
        if (hearts == 1.0) {
            return "1 heart";
        } else if (hearts == (int) hearts) {
            return (int) hearts + " hearts";
        } else {
            return hearts + " hearts";
        }
    }
    
    /**
     * Gets a player's heart display for chat/scoreboard
     * @param player The player
     * @return Formatted heart display (e.g., "❤❤❤❤❤ 10.0")
     */
    public static String getHeartDisplay(Player player) {
        int hearts = plugin.getPlayerDataManager().getPlayerHearts(player.getUniqueId());
        double fullHearts = hearts / 2.0;
        
        StringBuilder display = new StringBuilder();
        
        // Add heart symbols
        int fullHeartCount = hearts / 2;
        boolean hasHalfHeart = hearts % 2 == 1;
        
        // Full hearts
        for (int i = 0; i < fullHeartCount; i++) {
            display.append("§c❤");
        }
        
        // Half heart
        if (hasHalfHeart) {
            display.append("§6❤");
        }
        
        // Add numeric value
        display.append(" §f").append(fullHearts);
        
        return display.toString();
    }
    
    /**
     * Calculates the health percentage for visual effects
     * @param player The player
     * @return Health percentage (0.0 to 1.0)
     */
    public static double getHealthPercentage(Player player) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        return player.getHealth() / maxHealth;
    }
    
    /**
     * Validates heart amount is within bounds
     * @param hearts Number of half-hearts
     * @return Bounded heart value
     */
    public static int clampHearts(int hearts) {
        int min = plugin.getConfigManager().getMinHearts();
        int max = plugin.getConfigManager().getMaxHearts();
        return Math.max(min, Math.min(max, hearts));
    }
}