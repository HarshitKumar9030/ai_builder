package com.aibuilder.command;

import com.aibuilder.AIStructureBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuildCommand implements CommandExecutor {

    private final AIStructureBuilder plugin;

    public BuildCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /aibuild <description>");
            sender.sendMessage(ChatColor.YELLOW + "Example: /aibuild medieval castle with towers and moat");
            return true;
        }

        Player player = (Player) sender;
        String description = String.join(" ", args);

        if (!plugin.getAiManager().isConfigured()) {
            player.sendMessage(ChatColor.RED + "AI is not configured! Please set API key in config.yml");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "🏗️ Generating structure: " + ChatColor.YELLOW + description);
        player.sendMessage(ChatColor.GRAY + "💡 Tip: This will create a large, detailed structure with height variation!");        // Generate with increased block limit for larger structures
        plugin.getAiManager().generateStructureWithProgress(description, 5000, // Increased from default
                progress -> player.sendMessage(ChatColor.AQUA + progress))
                .thenAccept(structureData -> {
                    player.sendMessage(ChatColor.GREEN + "✅ Generation complete! Starting construction...");
                    player.sendMessage(ChatColor.YELLOW + "📏 Structure size: " +
                            structureData.getSize().getWidth() + "x" +
                            structureData.getSize().getHeight() + "x" +
                            structureData.getSize().getDepth());

                    // Start building with progress updates
                    plugin.getBuildManager().buildStructureWithProgress(player, structureData,
                            progress -> player.sendMessage(ChatColor.GREEN + progress));
                    
                    player.sendMessage(ChatColor.GOLD + "🎉 " + ChatColor.BOLD + "Structure completed successfully!");
                    player.sendMessage(ChatColor.YELLOW + "Enjoy your new creation!");
                })
                .exceptionally(throwable -> {
                    player.sendMessage(ChatColor.RED + "❌ Failed to build structure: " + throwable.getMessage());
                    plugin.getLogger().severe("Build error: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return null;
                });

        return true;
    }
}