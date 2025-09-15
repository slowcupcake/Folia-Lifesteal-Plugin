package com.squeakybagco.lifesteal.commands;

import com.squeakybagco.lifesteal.LifestealPlugin;
import com.squeakybagco.lifesteal.utils.HeartUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class HeartCommand implements CommandExecutor, TabCompleter, Listener {
    
    private final LifestealPlugin plugin;
    
    public HeartCommand(LifestealPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (command.getName().equalsIgnoreCase("withdraw")) {
            return handleWithdrawCommand(sender);
        }
        
        // Handle /hearts command
        if (!sender.hasPermission("lifesteal.hearts")) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(prefix + "§cUsage: /hearts <player> [amount]");
            return true;
        }
        
        // Get target player
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("player-not-found"));
            return true;
        }
        
        UUID targetId = target.getUniqueId();
        
        if (args.length == 1) {
            // Show player's heart info
            int hearts = plugin.getPlayerDataManager().getPlayerHearts(targetId);
            sender.sendMessage(prefix + "§6" + target.getName() + " §7has §c" + HeartUtils.formatHearts(hearts));
            return true;
        }
        
        // Set player's hearts
        if (!sender.hasPermission("lifesteal.admin")) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        try {
            int hearts = Integer.parseInt(args[1]);
            hearts = HeartUtils.clampHearts(hearts);
            
            plugin.getPlayerDataManager().setPlayerHearts(targetId, hearts);
            
            // Update online player's health
            if (target.isOnline()) {
                HeartUtils.updatePlayerMaxHealth(target.getPlayer(), hearts);
            }
            
            sender.sendMessage(prefix + "§aSet §6" + target.getName() + "§a's hearts to §c" + HeartUtils.formatHearts(hearts));
            
            if (target.isOnline()) {
                target.getPlayer().sendMessage(prefix + plugin.getConfigManager().getMessage("hearts-updated")
                    .replace("{hearts}", HeartUtils.formatHearts(hearts)));
            }
            
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid number: " + args[1]);
        }
        
        return true;
    }
    
    private boolean handleWithdrawCommand(CommandSender sender) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(prefix + "§cThis command can only be used by players.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!plugin.getConfigManager().isWithdrawEnabled()) {
            player.sendMessage(prefix + plugin.getConfigManager().getMessage("withdraw-disabled", "§cWithdraw is disabled!"));
            return true;
        }
        
        if (!player.hasPermission("lifesteal.withdraw")) {
            player.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Check cooldown
        if (plugin.getPlayerDataManager().isOnWithdrawCooldown(playerId)) {
            long remainingSeconds = plugin.getPlayerDataManager().getWithdrawCooldownRemaining(playerId);
            player.sendMessage(prefix + plugin.getConfigManager().getMessage("withdraw-cooldown")
                .replace("{time}", formatTime(remainingSeconds)));
            return true;
        }
        
        // Check if player has enough hearts
        int currentHearts = plugin.getPlayerDataManager().getPlayerHearts(playerId);
        int minHearts = plugin.getConfigManager().getMinHearts();
        
        if (currentHearts <= minHearts + 1) { // Need at least 1 more than minimum to withdraw
            player.sendMessage(prefix + plugin.getConfigManager().getMessage("not-enough-hearts", 
                "§cYou need more hearts to withdraw!"));
            return true;
        }
        
        // Check inventory space
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(prefix + plugin.getConfigManager().getMessage("inventory-full", 
                "§cYour inventory is full!"));
            return true;
        }
        
        // Remove hearts from player and give heart item
        plugin.getPlayerDataManager().setPlayerHearts(playerId, currentHearts - 2);
        HeartUtils.updatePlayerMaxHealth(player, currentHearts - 2);
        
        // Give heart item
        ItemStack heartItem = HeartUtils.createHeartItem(2);
        player.getInventory().addItem(heartItem);
        
        // Set cooldown
        plugin.getPlayerDataManager().setWithdrawCooldown(playerId);
        
        player.sendMessage(prefix + plugin.getConfigManager().getMessage("heart-withdrawn", 
            "§aYou withdrew a heart! You now have §c" + HeartUtils.formatHearts(currentHearts - 2)));
        
        return true;
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!HeartUtils.isHeartItem(item)) {
            return;
        }
        
        if (!event.getAction().name().contains("RIGHT_CLICK")) {
            return;
        }
        
        event.setCancelled(true);
        
        if (!plugin.getConfigManager().isHeartItemsEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("heart-items-disabled", "§cHeart items are disabled!"));
            return;
        }
        
        UUID playerId = player.getUniqueId();
        int currentHearts = plugin.getPlayerDataManager().getPlayerHearts(playerId);
        int maxHearts = plugin.getConfigManager().getMaxHearts();
        
        if (currentHearts >= maxHearts) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("max-hearts-reached", "§cYou already have maximum hearts!"));
            return;
        }
        
        int heartValue = HeartUtils.getHeartValue(item);
        int newHearts = Math.min(currentHearts + heartValue, maxHearts);
        
        // Update player data and health
        plugin.getPlayerDataManager().setPlayerHearts(playerId, newHearts);
        HeartUtils.updatePlayerMaxHealth(player, newHearts);
        
        // Remove the item
        item.setAmount(item.getAmount() - 1);
        
        // Send message
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("heart-consumed")
                .replace("{hearts}", HeartUtils.formatHearts(newHearts)));
        
        // Play sound effect
        player.playSound(player.getLocation(), 
            org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("withdraw")) {
            return completions; // No tab completion for withdraw
        }
        
        if (args.length == 1) {
            // Complete player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2 && sender.hasPermission("lifesteal.admin")) {
            // Complete heart amounts
            completions.addAll(Arrays.asList("2", "4", "6", "8", "10", "20", "40"));
        }
        
        return completions;
    }
    
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            
            if (remainingSeconds == 0) {
                return minutes + " minute" + (minutes == 1 ? "" : "s");
            } else {
                return minutes + "m " + remainingSeconds + "s";
            }
        }
    }
}