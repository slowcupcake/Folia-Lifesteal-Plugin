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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class LifestealCommand implements CommandExecutor, TabCompleter {
    
    private final LifestealPlugin plugin;
    
    public LifestealCommand(LifestealPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                break;
                
            case "reload":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("config-reloaded", "§aConfiguration reloaded!"));
                break;
                
            case "info":
            case "stats":
                handleStatsCommand(sender, args);
                break;
                
            case "set":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                handleSetCommand(sender, args);
                break;
                
            case "give":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                handleGiveCommand(sender, args);
                break;
                
            case "top":
            case "leaderboard":
                handleLeaderboardCommand(sender);
                break;
                
            default:
                sender.sendMessage(prefix + "§cUnknown subcommand. Use §e/lifesteal help §cfor help.");
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        sender.sendMessage("§8§m----§r " + prefix.trim() + " §cHelp §8§m----§r");
        sender.sendMessage("§e/lifesteal help §8- §7Show this help message");
        sender.sendMessage("§e/lifesteal info [player] §8- §7Show heart information");
        sender.sendMessage("§e/hearts <player> [amount] §8- §7Manage player hearts");
        sender.sendMessage("§e/withdraw §8- §7Withdraw a heart item");
        
        if (sender.hasPermission("lifesteal.admin")) {
            sender.sendMessage("§c§lAdmin Commands:");
            sender.sendMessage("§e/lifesteal reload §8- §7Reload configuration");
            sender.sendMessage("§e/lifesteal set <player> <hearts> §8- §7Set player hearts");
            sender.sendMessage("§e/lifesteal give <player> <amount> §8- §7Give heart items");
            sender.sendMessage("§e/lifesteal top §8- §7Show leaderboard");
        }
    }
    
    private void handleStatsCommand(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        Player target;
        if (args.length > 1) {
            if (!sender.hasPermission("lifesteal.admin")) {
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("no-permission"));
                return;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(prefix + plugin.getConfigManager().getMessage("player-not-found", "§cPlayer not found!"));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + "§cYou must specify a player name from console.");
                return;
            }
            target = (Player) sender;
        }
        
        UUID playerId = target.getUniqueId();
        int hearts = plugin.getPlayerDataManager().getPlayerHearts(playerId);
        int kills = plugin.getPlayerDataManager().getKills(playerId);
        int deaths = plugin.getPlayerDataManager().getDeaths(playerId);
        
        sender.sendMessage("§8§m----§r " + prefix.trim() + " §eStats for §6" + target.getName() + " §8§m----§r");
        sender.sendMessage("§cHearts: §f" + HeartUtils.formatHearts(hearts) + " §7(" + hearts + " half-hearts)");
        sender.sendMessage("§eKills: §f" + kills);
        sender.sendMessage("§cDeaths: §f" + deaths);
        
        if (deaths > 0) {
            double kdr = (double) kills / deaths;
            sender.sendMessage("§aK/D Ratio: §f" + String.format("%.2f", kdr));
        } else {
            sender.sendMessage("§aK/D Ratio: §f" + (kills > 0 ? "∞" : "N/A"));
        }
    }
    
    private void handleSetCommand(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (args.length < 3) {
            sender.sendMessage(prefix + "§cUsage: /lifesteal set <player> <hearts>");
            return;
        }
        
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        try {
            int hearts = Integer.parseInt(args[2]);
            hearts = HeartUtils.clampHearts(hearts);
            
            plugin.getPlayerDataManager().setPlayerHearts(target.getUniqueId(), hearts);
            
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
            sender.sendMessage(prefix + "§cInvalid number: " + args[2]);
        }
    }
    
    private void handleGiveCommand(CommandSender sender, String[] args) {
        String prefix = plugin.getConfigManager().getPrefix();
        
        if (args.length < 3) {
            sender.sendMessage(prefix + "§cUsage: /lifesteal give <player> <amount>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(prefix + plugin.getConfigManager().getMessage("player-not-found"));
            return;
        }
        
        try {
            int amount = Integer.parseInt(args[2]);
            if (amount <= 0 || amount > 64) {
                sender.sendMessage(prefix + "§cAmount must be between 1 and 64.");
                return;
            }
            
            for (int i = 0; i < amount; i++) {
                if (target.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(prefix + "§c" + target.getName() + "'s inventory is full!");
                    break;
                }
                target.getInventory().addItem(HeartUtils.createHeartItem(2));
            }
            
            sender.sendMessage(prefix + "§aGave §c" + amount + " §aheart items to §6" + target.getName());
            target.sendMessage(prefix + plugin.getConfigManager().getMessage("received-heart-items")
                .replace("{amount}", String.valueOf(amount)));
            
        } catch (NumberFormatException e) {
            sender.sendMessage(prefix + "§cInvalid number: " + args[2]);
        }
    }
    
    private void handleLeaderboardCommand(CommandSender sender) {
        // This would require more complex data storage to implement efficiently
        // For now, just show a placeholder message
        String prefix = plugin.getConfigManager().getPrefix();
        sender.sendMessage(prefix + "§cLeaderboard feature coming soon!");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("help", "info", "stats", "top");
            
            if (sender.hasPermission("lifesteal.admin")) {
                subcommands = Arrays.asList("help", "info", "stats", "reload", "set", "give", "top");
            }
            
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("stats") || 
                args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give")) {
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("set")) {
                completions.addAll(Arrays.asList("2", "4", "10", "20", "40"));
            } else if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(Arrays.asList("1", "5", "10", "16", "32", "64"));
            }
        }
        
        return completions;
    }
}