package com.aibuilder.commands;

import com.aibuilder.AIStructureBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles /aihelp command
 */
public class AIHelpCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;

    public AIHelpCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("aibuilder.help")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        sendHelpMessage(sender);
        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("§6§l        AI Structure Builder Help");
        sender.sendMessage("§8§m                                                  ");
        sender.sendMessage("");        sender.sendMessage("§e/aibuild <description> §7- Build a structure using AI");
        sender.sendMessage("§7  Example: §f/aibuild a medieval castle with towers");
        sender.sendMessage("");
        sender.sendMessage("§e/aipreview <description> §7- Preview a structure before building");
        sender.sendMessage("§7  Example: §f/aipreview a small house");
        sender.sendMessage("");
        sender.sendMessage("§e/aiprogress §7- Check your current build progress");
        sender.sendMessage("§e/aicancel §7- Cancel your current build");
        sender.sendMessage("§e/aistatus §7- Show plugin status and configuration");
        sender.sendMessage("");
        sender.sendMessage("§e/aiconfig set <key> <value> §7- Set configuration");
        sender.sendMessage("§7  Example: §f/aiconfig set gemini.api-key YOUR_KEY");
        sender.sendMessage("");
        sender.sendMessage("§e/aiconfig get <key> §7- Get configuration value");
        sender.sendMessage("§7  Example: §f/aiconfig get gemini.model");
        sender.sendMessage("");
        sender.sendMessage("§e/aihelp §7- Show this help message");
        sender.sendMessage("");
        sender.sendMessage("§6Configuration Keys:");
        sender.sendMessage("§7- §egemini.api-key §7- Your Gemini API key");
        sender.sendMessage("§7- §egemini.model §7- AI model to use");
        sender.sendMessage("§7- §egemini.temperature §7- AI creativity (0.0-1.0)");
        sender.sendMessage("§7- §ebuilding.max-structure-size §7- Max structure size");
        sender.sendMessage("");
        sender.sendMessage("§6Tips:");
        sender.sendMessage("§7- Be specific in your descriptions");
        sender.sendMessage("§7- Try: 'a small wooden house with a red roof'");
        sender.sendMessage("§7- Try: 'a modern glass tower 20 blocks tall'");
        sender.sendMessage("§7- Try: 'a stone bridge over water'");
        sender.sendMessage("");
        sender.sendMessage("§6Setup:");
        sender.sendMessage("§71. Get API key from: §bhttps://makersuite.google.com/app/apikey");
        sender.sendMessage("§72. Set it with: §f/aiconfig set gemini.api-key YOUR_KEY");
        sender.sendMessage("§73. Start building with: §f/aibuild your description");
        sender.sendMessage("");
        sender.sendMessage("§8§m                                                  ");
    }
}
