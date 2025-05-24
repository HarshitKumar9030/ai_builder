package com.aibuilder.manager;

import com.aibuilder.AIStructureBuilder;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Manages plugin configuration
 */
public class ConfigManager {
    
    private final AIStructureBuilder plugin;
    @Getter
    private FileConfiguration config;

    public ConfigManager(AIStructureBuilder plugin) {
        this.plugin = plugin;
    }

    /**
     * Load configuration from file
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Save configuration to file
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Get Gemini API key
     */
    public String getGeminiApiKey() {
        return config.getString("gemini.api-key", "");
    }

    /**
     * Set Gemini API key
     */
    public void setGeminiApiKey(String apiKey) {
        config.set("gemini.api-key", apiKey);
        saveConfig();
    }

    /**
     * Get Gemini model
     */
    public String getGeminiModel() {
        return config.getString("gemini.model", "gemini-2.0-flash");
    }

    /**
     * Get maximum tokens
     */
    public int getMaxTokens() {
        return config.getInt("gemini.max-tokens", 4000);
    }

    /**
     * Get temperature
     */
    public double getTemperature() {
        return config.getDouble("gemini.temperature", 0.7);
    }

    /**
     * Get maximum structure size
     */
    public int getMaxStructureSize() {
        return config.getInt("building.max-structure-size", 100);
    }

    /**
     * Get default materials
     */
    public List<String> getDefaultMaterials() {
        return config.getStringList("building.default-materials");
    }

    /**
     * Check if confirmation is required
     */
    public boolean requireConfirmation() {
        return config.getBoolean("building.require-confirmation", true);
    }

    /**
     * Get confirmation threshold
     */
    public int getConfirmationThreshold() {
        return config.getInt("building.confirmation-threshold", 50);
    }

    /**
     * Get blocks per tick
     */
    public int getBlocksPerTick() {
        return config.getInt("performance.blocks-per-tick", 10);
    }

    /**
     * Get build delay
     */
    public int getBuildDelay() {
        return config.getInt("performance.build-delay", 2);
    }

    /**
     * Check if async building is enabled
     */
    public boolean isAsyncBuildingEnabled() {
        return config.getBoolean("performance.async-building", true);
    }

    /**
     * Check if AI request logging is enabled
     */
    public boolean shouldLogAIRequests() {
        return config.getBoolean("logging.log-ai-requests", false);
    }

    /**
     * Check if building logging is enabled
     */
    public boolean shouldLogBuilding() {
        return config.getBoolean("logging.log-building", true);
    }

    /**
     * Check if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("logging.debug", false);
    }

    /**
     * Get formatted message
     */
    public String getMessage(String key, Object... args) {
        String prefix = config.getString("messages.prefix", "&8[&6AI Builder&8] &r");
        String message = config.getString("messages." + key, key);
        
        // Replace placeholders
        for (int i = 0; i < args.length; i++) {
            message = message.replace("%" + getPlaceholderName(i) + "%", String.valueOf(args[i]));
        }
        
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    /**
     * Get placeholder name for index
     */
    private String getPlaceholderName(int index) {
        switch (index) {
            case 0: return "description";
            case 1: return "error";
            case 2: return "usage";
            case 3: return "key";
            case 4: return "value";
            default: return "arg" + index;
        }
    }

    /**
     * Set configuration value
     */
    public void setValue(String path, Object value) {
        config.set(path, value);
        saveConfig();
    }

    /**
     * Get configuration value
     */
    public Object getValue(String path) {
        return config.get(path);
    }
}
