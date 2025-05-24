package com.aibuilder.command;

import com.aibuilder.AIStructureBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Command to check building progress
 */
public class ProgressCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;
    
    public ProgressCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();
        
        // Check if player has active build
        if (!plugin.getBuildManager().hasActiveBuild(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "You don't have any active builds.");
            return true;
        }
        
        int progress = plugin.getBuildManager().getBuildProgress(playerId);
        player.sendMessage(ChatColor.GREEN + "Build Progress: " + progress + "%");
        
        return true;
    }
}
