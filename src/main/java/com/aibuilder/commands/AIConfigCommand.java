package com.aibuilder.commands;

import com.aibuilder.AIStructureBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles /aiconfig command
 */
public class AIConfigCommand implements CommandExecutor {
    
    private final AIStructureBuilder plugin;

    public AIConfigCommand(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check permission
        if (!sender.hasPermission("aibuilder.admin")) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("invalid-usage", "/aiconfig <set|get> <key> [value]"));
            return true;
        }

        String action = args[0].toLowerCase();
        String key = args[1];

        switch (action) {
            case "set":
                if (args.length < 3) {
                    sender.sendMessage(plugin.getMessage("invalid-usage", "/aiconfig set <key> <value>"));
                    return true;
                }
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                setValue(sender, key, value);
                break;
                
            case "get":
                getValue(sender, key);
                break;
                
            default:
                sender.sendMessage(plugin.getMessage("invalid-usage", "/aiconfig <set|get> <key> [value]"));
                break;
        }

        return true;
    }

    private void setValue(CommandSender sender, String key, String value) {
        try {
            // Handle special keys
            switch (key.toLowerCase()) {
                case "gemini.api-key":
                    plugin.getConfigManager().setGeminiApiKey(value);
                    plugin.getAiManager().updateConfiguration();
                    sender.sendMessage("§aGemini API key updated!");
                    break;
                    
                case "gemini.temperature":
                    double temp = Double.parseDouble(value);
                    if (temp < 0.0 || temp > 1.0) {
                        sender.sendMessage("§cTemperature must be between 0.0 and 1.0");
                        return;
                    }
                    plugin.getConfigManager().setValue(key, temp);
                    sender.sendMessage("§aTemperature set to " + temp);
                    break;
                    
                case "building.max-structure-size":
                    int size = Integer.parseInt(value);
                    if (size < 1 || size > 1000) {
                        sender.sendMessage("§cMax structure size must be between 1 and 1000");
                        return;
                    }
                    plugin.getConfigManager().setValue(key, size);
                    sender.sendMessage("§aMax structure size set to " + size);
                    break;
                    
                default:
                    // Generic config setting
                    plugin.getConfigManager().setValue(key, value);
                    sender.sendMessage(plugin.getMessage("config-updated"));
                    break;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number format for key: " + key);
        } catch (Exception e) {
            sender.sendMessage("§cFailed to set config value: " + e.getMessage());
        }
    }

    private void getValue(CommandSender sender, String key) {
        try {
            Object value = plugin.getConfigManager().getValue(key);
            if (value == null) {
                sender.sendMessage("§cConfiguration key not found: " + key);
            } else {
                sender.sendMessage("§e" + key + ": §f" + value);
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to get config value: " + e.getMessage());
        }
    }
}
