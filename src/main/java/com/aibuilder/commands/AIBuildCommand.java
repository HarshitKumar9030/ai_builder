package com.aibuilder.commands;

import com.aibuilder.AIStructureBuilder;
import com.aibuilder.model.StructureData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Handles /aibuild command
 */
public class AIBuildCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;

    public AIBuildCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("aibuilder.build")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        // Check if AI is configured
        if (!plugin.getAiManager().isConfigured()) {
            player.sendMessage(plugin.getMessage("api-key-not-set"));
            return true;
        }

        // Check arguments
        if (args.length == 0) {
            player.sendMessage(plugin.getMessage("invalid-usage", "/aibuild <description>"));
            return true;
        }

        // Join arguments to form description
        String description = String.join(" ", args);

        // Check for active build
        if (plugin.getBuildManager().hasActiveBuild(player.getUniqueId())) {
            player.sendMessage("§cYou already have an active build! Please wait for it to complete.");
            return true;
        }

        // Start building process
        player.sendMessage("§eGenerating structure with AI... Please wait.");
        
        int maxSize = plugin.getConfigManager().getMaxStructureSize();
        
        CompletableFuture<StructureData> future = plugin.getAiManager().generateStructureWithProgress(description, maxSize,
            progress -> player.sendMessage("§7[AI] " + progress));        future.thenAccept(structureData -> {
            // Check if player is still online
            if (!player.isOnline()) {
                return;
            }
              // Log structure data for debugging
            plugin.getLogger().info("=== STRUCTURE DATA DEBUG ===");
            if (structureData == null) {
                plugin.getLogger().severe("StructureData is NULL!");
                player.sendMessage("§cStructure generation failed - null data");
                return;
            }
            
            // Log to file for detailed analysis
            logStructureDataToFile(structureData);
            
            plugin.getLogger().info("Structure Name: " + structureData.getName());
            plugin.getLogger().info("Structure Description: " + structureData.getDescription());
            
            if (structureData.getBlocks() == null) {
                plugin.getLogger().severe("Blocks list is NULL!");
                player.sendMessage("§cStructure generation failed - no blocks");
                return;
            }
            
            plugin.getLogger().info("Total blocks: " + structureData.getBlocks().size());
            
            // Log first few blocks for debugging
            for (int i = 0; i < Math.min(5, structureData.getBlocks().size()); i++) {
                StructureData.Block block = structureData.getBlocks().get(i);
                if (block == null) {
                    plugin.getLogger().warning("Block " + i + " is NULL!");
                } else {
                    plugin.getLogger().info("Block " + i + ": " + block.getMaterial() + " at (" + 
                        block.getX() + "," + block.getY() + "," + block.getZ() + ")");
                }
            }
            plugin.getLogger().info("=== END STRUCTURE DATA DEBUG ===");
            
            // Check if confirmation is required
            int estimatedSize = plugin.getBuildManager().getEstimatedSize(structureData);
            if (plugin.getConfigManager().requireConfirmation() && 
                estimatedSize > plugin.getConfigManager().getConfirmationThreshold()) {
                
                player.sendMessage("§eStructure '" + structureData.getName() + "' will use " + estimatedSize + " blocks.");
                player.sendMessage("§eType '/aibuild confirm' to proceed or '/aibuild cancel' to cancel.");
                
                // TODO: Store structure data for confirmation system
                // For now, we'll skip building when confirmation is required
                return;
            }
            
            // Start building with progress updates (only if confirmation not required)
            plugin.getBuildManager().buildStructureWithProgress(player, structureData,
                buildProgress -> player.sendMessage("§7[Build] " + buildProgress));
            
        }).exceptionally(throwable -> {
            if (player.isOnline()) {
                player.sendMessage(plugin.getMessage("building-failed", throwable.getMessage()));
            }
            return null;
        });

        return true;
    }
}
