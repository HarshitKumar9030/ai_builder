package com.aibuilder;

import com.aibuilder.commands.AIBuildCommand;
import com.aibuilder.commands.AIConfigCommand;
import com.aibuilder.commands.AIHelpCommand;
import com.aibuilder.manager.AIManager;
import com.aibuilder.manager.BuildManager;
import com.aibuilder.manager.ConfigManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for AI Structure Builder
 * A Minecraft Spigot plugin that uses Google Gemini AI to build structures
 */
public class AIStructureBuilder extends JavaPlugin {

    @Getter
    private static AIStructureBuilder instance;
    
    @Getter
    private ConfigManager configManager;
    
    @Getter
    private AIManager aiManager;
    
    @Getter
    private BuildManager buildManager;    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize config manager first
        this.configManager = new ConfigManager(this);
        
        // Load configuration before initializing other managers
        configManager.loadConfig();
          // Initialize other managers after config is loaded
        this.aiManager = new AIManager(this);
        this.buildManager = new BuildManager(this);
        
        // Update AI manager configuration after everything is initialized
        aiManager.updateConfiguration();
        
        // Register commands
        registerCommands();
        
        // Log startup
        getLogger().info("AI Structure Builder has been enabled!");
        getLogger().info("Version: " + getDescription().getVersion());
        
        // Check API key
        if (!aiManager.isConfigured()) {
            getLogger().warning("Gemini API key not configured! Use /aiconfig set gemini.api-key <key>");
        }
    }

    @Override
    public void onDisable() {
        // Cancel any ongoing builds
        if (buildManager != null) {
            buildManager.cancelAllBuilds();
        }
        
        getLogger().info("AI Structure Builder has been disabled!");
    }    /**
     * Register plugin commands
     */
    private void registerCommands() {
        getCommand("aibuild").setExecutor(new AIBuildCommand(this));
        getCommand("aiconfig").setExecutor(new AIConfigCommand(this));
        getCommand("aihelp").setExecutor(new AIHelpCommand(this));
        getCommand("aistatus").setExecutor(new com.aibuilder.command.StatusCommand(this));
        getCommand("aiprogress").setExecutor(new com.aibuilder.command.ProgressCommand(this));
        getCommand("aicancel").setExecutor(new com.aibuilder.command.CancelCommand(this));
        getCommand("aipreview").setExecutor(new com.aibuilder.command.PreviewCommand(this));
    }

    /**
     * Get formatted message with prefix
     */
    public String getMessage(String key, Object... args) {
        return configManager.getMessage(key, args);
    }
}
