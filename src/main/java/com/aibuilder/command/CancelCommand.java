package com.aibuilder.command;

import com.aibuilder.AIStructureBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Command to cancel an active build
 */
public class CancelCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;
    
    public CancelCommand(AIStructureBuilder plugin) {
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
            player.sendMessage(ChatColor.YELLOW + "You don't have any active builds to cancel.");
            return true;
        }
        
        plugin.getBuildManager().cancelBuild(playerId);
        player.sendMessage(ChatColor.GREEN + "Build cancelled successfully!");
        
        return true;
    }
}
