package com.aibuilder.command;

import com.aibuilder.AIStructureBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command to show plugin status and configuration
 */
public class StatusCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;
    
    public StatusCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=== AI Structure Builder Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        
        // AI Configuration Status
        boolean aiConfigured = plugin.getAiManager().isConfigured();
        sender.sendMessage(ChatColor.YELLOW + "AI Status: " + 
            (aiConfigured ? ChatColor.GREEN + "Configured" : ChatColor.RED + "Not Configured"));
        
        if (!aiConfigured) {
            sender.sendMessage(ChatColor.RED + "Please set your Gemini API key in config.yml");
        }
        
        // Active builds count
        int activeBuilds = plugin.getBuildManager().getActiveBuildCount();
        sender.sendMessage(ChatColor.YELLOW + "Active Builds: " + ChatColor.WHITE + activeBuilds);
        
        // Configuration details (for ops only)
        if (sender.hasPermission("aibuilder.admin")) {
            sender.sendMessage(ChatColor.GOLD + "=== Configuration ===");
            sender.sendMessage(ChatColor.YELLOW + "Model: " + ChatColor.WHITE + plugin.getConfigManager().getGeminiModel());
            sender.sendMessage(ChatColor.YELLOW + "Max Tokens: " + ChatColor.WHITE + plugin.getConfigManager().getMaxTokens());
            sender.sendMessage(ChatColor.YELLOW + "Temperature: " + ChatColor.WHITE + plugin.getConfigManager().getTemperature());
            sender.sendMessage(ChatColor.YELLOW + "Build Delay: " + ChatColor.WHITE + plugin.getConfigManager().getBuildDelay() + " ticks");
            sender.sendMessage(ChatColor.YELLOW + "Blocks Per Tick: " + ChatColor.WHITE + plugin.getConfigManager().getBlocksPerTick());
        }
        
        return true;
    }
}
