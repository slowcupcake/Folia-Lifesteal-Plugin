package com.squeakybagco.lifesteal.listeners;

import com.squeakybagco.lifesteal.LifestealPlugin;
import com.squeakybagco.lifesteal.managers.PlayerDataManager;
import com.squeakybagco.lifesteal.utils.HeartUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CombatListener implements Listener {
    
    private final LifestealPlugin plugin;
    private final PlayerDataManager dataManager;
    
    public CombatListener(LifestealPlugin plugin) {
        this.plugin = plugin;
        this.dataManager = plugin.getPlayerDataManager();
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data on join
        plugin.scheduleRegionTask(player.getLocation(), () -> {
            dataManager.loadPlayerData(player);
            HeartUtils.updatePlayerMaxHealth(player, dataManager.getPlayerHearts(player.getUniqueId()));
        });
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save player data on quit
        dataManager.savePlayerData(player);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // Only process PvP deaths
        if (killer == null || killer == victim) {
            return;
        }
        
        // Check if victim has bypass permission
        if (victim.hasPermission("lifesteal.bypass")) {
            return;
        }
        
        // Check if lifesteal is enabled in this world
        if (!plugin.getConfigManager().isLifestealEnabledInWorld(victim.getWorld().getName())) {
            return;
        }
        
        // Schedule the heart transfer on the victim's region
        plugin.scheduleRegionTask(victim.getLocation(), () -> {
            processHeartSteal(victim, killer);
        });
    }
    
    private void processHeartSteal(Player victim, Player killer) {
        int victimHearts = dataManager.getPlayerHearts(victim.getUniqueId());
        int killerHearts = dataManager.getPlayerHearts(killer.getUniqueId());
        
        int maxHearts = plugin.getConfigManager().getMaxHearts();
        int minHearts = plugin.getConfigManager().getMinHearts();
        int heartsToSteal = plugin.getConfigManager().getHeartsPerKill();
        
        // Check if victim would go below minimum
        if (victimHearts - heartsToSteal < minHearts) {
            heartsToSteal = Math.max(0, victimHearts - minHearts);
        }
        
        // Check if killer would go above maximum
        if (killerHearts + heartsToSteal > maxHearts) {
            heartsToSteal = Math.max(0, maxHearts - killerHearts);
        }
        
        if (heartsToSteal <= 0) {
            return;
        }
        
        // Transfer hearts
        int newVictimHearts = victimHearts - heartsToSteal;
        int newKillerHearts = killerHearts + heartsToSteal;
        
        dataManager.setPlayerHearts(victim.getUniqueId(), newVictimHearts);
        dataManager.setPlayerHearts(killer.getUniqueId(), newKillerHearts);
        
        // Update max health for both players
        HeartUtils.updatePlayerMaxHealth(victim, newVictimHearts);
        HeartUtils.updatePlayerMaxHealth(killer, newKillerHearts);
        
        // Send messages
        String victimMessage = plugin.getConfigManager().getMessage("heart-lost")
            .replace("{amount}", String.valueOf(heartsToSteal))
            .replace("{killer}", killer.getName())
            .replace("{hearts}", String.valueOf(newVictimHearts));
        
        String killerMessage = plugin.getConfigManager().getMessage("heart-gained")
            .replace("{amount}", String.valueOf(heartsToSteal))
            .replace("{victim}", victim.getName())
            .replace("{hearts}", String.valueOf(newKillerHearts));
        
        victim.sendMessage(victimMessage);
        killer.sendMessage(killerMessage);
        
        // Check if victim should be eliminated
        if (newVictimHearts <= 0) {
            handlePlayerElimination(victim);
        }
    }
    
    private void handlePlayerElimination(Player player) {
        if (!plugin.getConfigManager().isEliminationEnabled()) {
            return;
        }
        
        // Schedule elimination task
        plugin.scheduleRegionTaskLater(player.getLocation(), () -> {
            String eliminationMessage = plugin.getConfigManager().getMessage("player-eliminated")
                .replace("{player}", player.getName());
            
            // Broadcast elimination message using Adventure API
            Component messageComponent = Component.text(eliminationMessage);
            plugin.getServer().broadcast(messageComponent);
            
            // Execute elimination commands
            for (String command : plugin.getConfigManager().getEliminationCommands()) {
                String processedCommand = command.replace("{player}", player.getName());
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), processedCommand);
            }
            
            // Ban the player if configured (using /ban command)
            if (plugin.getConfigManager().isBanOnElimination()) {
                String banReason = "You have been eliminated from the lifesteal server!";
                String banCommand = "ban " + player.getName() + " " + banReason;
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), banCommand);
            }
        }, 20L); // 1 second delay
    }
}