package com.aibuilder.command;

import com.aibuilder.AIStructureBuilder;
import com.aibuilder.model.StructureData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Command to preview structure details before building
 */
public class PreviewCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;
    
    public PreviewCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /aipreview <description>");
            return true;
        }
        
        Player player = (Player) sender;
        String description = String.join(" ", args);
        
        if (!plugin.getAiManager().isConfigured()) {
            player.sendMessage(ChatColor.RED + "AI is not configured! Please set API key in config.yml");
            return true;
        }
        
        player.sendMessage(ChatColor.YELLOW + "Generating structure preview for: " + description);
        player.sendMessage(ChatColor.GRAY + "💡 Generating large, detailed structure...");
        
        // Generate structure without building - increased limit for preview
        plugin.getAiManager().generateStructureWithProgress(description, 5000,
            progress -> player.sendMessage(ChatColor.GRAY + "[Preview] " + progress))
            .thenAccept(structureData -> {
                // Show preview information
                player.sendMessage(ChatColor.GOLD + "=== 🏗️ Structure Preview ===");
                player.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.WHITE + structureData.getName());
                player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + structureData.getDescription());
                
                StructureData.Size size = structureData.getSize();
                player.sendMessage(ChatColor.YELLOW + "Dimensions: " + ChatColor.WHITE + 
                    size.getWidth() + "x" + size.getHeight() + "x" + size.getDepth());
                player.sendMessage(ChatColor.YELLOW + "Total blocks: " + ChatColor.WHITE + structureData.getBlocks().size());
                
                // Enhanced material breakdown with categories
                var materialCount = structureData.getBlocks().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        StructureData.Block::getMaterial,
                        java.util.stream.Collectors.counting()));
                
                player.sendMessage(ChatColor.YELLOW + "📦 Materials needed (" + materialCount.size() + " types):");
                materialCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10) // Show top 10 materials
                    .forEach(entry -> 
                        player.sendMessage(ChatColor.WHITE + "  • " + entry.getKey() + ": " + entry.getValue()));
                
                if (materialCount.size() > 10) {
                    player.sendMessage(ChatColor.GRAY + "  ... and " + (materialCount.size() - 10) + " more materials");
                }
                
                // Height analysis
                int minY = structureData.getBlocks().stream().mapToInt(StructureData.Block::getY).min().orElse(0);
                int maxY = structureData.getBlocks().stream().mapToInt(StructureData.Block::getY).max().orElse(0);
                player.sendMessage(ChatColor.YELLOW + "Height range: " + ChatColor.WHITE + "Y" + minY + " to Y" + maxY + 
                    " (" + (maxY - minY + 1) + " layers)");
                
                player.sendMessage(ChatColor.GREEN + "✨ Use '/aibuild " + description + "' to build this epic structure!");
            })
            .exceptionally(throwable -> {
                player.sendMessage(ChatColor.RED + "Failed to generate preview: " + throwable.getMessage());
                return null;
            });
        
        return true;
    }
}
