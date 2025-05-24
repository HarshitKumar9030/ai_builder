package com.aibuilder.manager;

import com.aibuilder.AIStructureBuilder;
import com.aibuilder.model.StructureData;
import com.aibuilder.util.MaterialUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages structure building operations
 */
public class BuildManager {
    
    private final AIStructureBuilder plugin;
    @Getter
    private final Map<UUID, BukkitTask> activeBuildTasks;
    @Getter
    private final Map<UUID, Integer> buildProgress;

    public BuildManager(AIStructureBuilder plugin) {
        this.plugin = plugin;
        this.activeBuildTasks = new ConcurrentHashMap<>();
        this.buildProgress = new ConcurrentHashMap<>();
    }

    /**
     * Start building a structure
     */
    public void buildStructure(Player player, StructureData structureData, Location startLocation) {
        UUID playerId = player.getUniqueId();
        
        // Cancel existing build for this player
        cancelBuild(playerId);
        
        // Validate structure
        if (!validateStructure(structureData)) {
            player.sendMessage(plugin.getMessage("building-failed", "Structure validation failed"));
            return;
        }

        // Log building start
        if (plugin.getConfigManager().shouldLogBuilding()) {
            plugin.getLogger().info("Starting build for " + player.getName() + ": " + structureData.getName());
        }

        player.sendMessage(plugin.getMessage("building-started", structureData.getDescription()));        // Start building task
        List<StructureData.Block> instructions = structureData.getBlocks();
        buildProgress.put(playerId, 0);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index = 0;
            
            @Override
            public void run() {
                try {
                    int blocksPerTick = plugin.getConfigManager().getBlocksPerTick();
                    int processed = 0;
                      while (index < instructions.size() && processed < blocksPerTick) {
                        StructureData.Block instruction = instructions.get(index);
                        placeBlock(instruction, startLocation);
                        index++;
                        processed++;
                    }
                    
                    // Update progress
                    buildProgress.put(playerId, index);
                      // Check if finished
                    if (index >= instructions.size()) {
                        // Building complete
                        BukkitTask taskToCancel = activeBuildTasks.remove(playerId);
                        buildProgress.remove(playerId);
                        player.sendMessage(plugin.getMessage("building-completed"));
                        
                        if (plugin.getConfigManager().shouldLogBuilding()) {
                            plugin.getLogger().info("Build completed for " + player.getName());
                        }
                        
                        // Cancel this task safely
                        if (taskToCancel != null) {
                            taskToCancel.cancel();
                        }
                    }
                      } catch (Exception e) {
                    plugin.getLogger().severe("Error during building: " + e.getMessage());
                    player.sendMessage(plugin.getMessage("building-failed", e.getMessage()));
                    BukkitTask taskToCancel = activeBuildTasks.remove(playerId);
                    buildProgress.remove(playerId);
                    if (taskToCancel != null) {
                        taskToCancel.cancel();
                    }
                }
            }
        }, 0L, plugin.getConfigManager().getBuildDelay());        activeBuildTasks.put(playerId, task);
    }    /**
     * Build structure with progress updates
     */
    public void buildStructureWithProgress(Player player, StructureData structureData, Consumer<String> progressCallback) {
        plugin.getLogger().info("buildStructureWithProgress called for player: " + player.getName());
        
        Location startLocation = player.getLocation();
        UUID playerId = player.getUniqueId();

        // Validate structure
        if (!validateStructure(structureData)) {
            plugin.getLogger().warning("Structure validation failed, not building");
            progressCallback.accept("Invalid structure data!");
            player.sendMessage(plugin.getMessage("invalid-structure"));
            return;
        }

        plugin.getLogger().info("Structure validation passed, proceeding to build");

        // Check if player is already building
        if (activeBuildTasks.containsKey(playerId)) {
            progressCallback.accept("Build already in progress!");
            player.sendMessage(plugin.getMessage("build-in-progress"));
            return;
        }

        if (plugin.getConfigManager().shouldLogBuilding()) {
            plugin.getLogger().info("Starting build for " + player.getName() + ": " + structureData.getName());
        }

        progressCallback.accept("Starting construction of " + structureData.getName() + "...");
        player.sendMessage(plugin.getMessage("building-started", structureData.getDescription()));

        // Start building task
        List<StructureData.Block> instructions = structureData.getBlocks();
        buildProgress.put(playerId, 0);
        
        final int totalBlocks = instructions.size();        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            private int index = 0;
            
            @Override
            public void run() {
                try {
                    int blocksPerTick = plugin.getConfigManager().getBlocksPerTick();
                    int processed = 0;
                    
                    while (index < instructions.size() && processed < blocksPerTick) {
                        StructureData.Block instruction = instructions.get(index);
                        placeBlock(instruction, startLocation);
                        index++;
                        processed++;
                        
                        // Update progress
                        int progressPercent = (int) ((double) index / totalBlocks * 100);
                        buildProgress.put(playerId, progressPercent);
                        
                        // Send progress updates every 10%
                        if (index % Math.max(1, totalBlocks / 10) == 0) {
                            progressCallback.accept("Construction progress: " + progressPercent + "% (" + index + "/" + totalBlocks + " blocks)");
                        }
                    }
                    
                    if (index >= instructions.size()) {
                        // Building complete
                        buildProgress.remove(playerId);
                        BukkitTask taskToCancel = activeBuildTasks.remove(playerId);
                        progressCallback.accept("Construction completed! Built " + totalBlocks + " blocks.");
                        player.sendMessage(plugin.getMessage("building-completed", structureData.getName()));
                        
                        if (plugin.getConfigManager().shouldLogBuilding()) {
                            plugin.getLogger().info("Build completed for " + player.getName() + ": " + structureData.getName());
                        }
                        
                        if (taskToCancel != null) {
                            taskToCancel.cancel();
                        }
                    }
                } catch (Exception e) {
                    buildProgress.remove(playerId);
                    BukkitTask taskToCancel = activeBuildTasks.remove(playerId);
                    progressCallback.accept("Build failed: " + e.getMessage());
                    player.sendMessage(plugin.getMessage("building-failed"));
                    plugin.getLogger().severe("Building failed for " + player.getName() + ": " + e.getMessage());
                    if (taskToCancel != null) {
                        taskToCancel.cancel();
                    }
                }
            }
        }, 0L, plugin.getConfigManager().getBuildDelay());

        activeBuildTasks.put(playerId, task);
    }

    /**
     * Place a block according to build instruction
     */
    private void placeBlock(StructureData.Block instruction, Location startLocation) {
        Location blockLocation = startLocation.clone().add(instruction.getX(), instruction.getY(), instruction.getZ());
        
        // Get material safely
        Material material = MaterialUtil.getMaterialSafely(instruction.getMaterial());
        if (!MaterialUtil.isSafeBuildingMaterial(material)) {
            plugin.getLogger().warning("Unsafe material: " + instruction.getMaterial() + ", using STONE instead");
            material = Material.STONE;
        }
        
        // Place block
        blockLocation.getBlock().setType(material);
        
        // Apply block data if specified
        if (instruction.getData() != null && !instruction.getData().isEmpty()) {
            // Handle block data (stairs direction, etc.)
            // This is a simplified implementation
            try {
                // You can extend this to handle more complex block data
                if (material.name().contains("STAIRS")) {
                    // Handle stairs orientation
                    // This would need more complex implementation for full support
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to apply block data: " + e.getMessage());
            }
        }
    }    /**
     * Validate structure before building
     */
    private boolean validateStructure(StructureData structureData) {
        if (structureData == null) {
            plugin.getLogger().warning("Structure validation failed: structureData is null");
            return false;
        }
        
        if (structureData.getBlocks() == null) {
            plugin.getLogger().warning("Structure validation failed: blocks list is null");
            return false;
        }
        
        int maxSize = plugin.getConfigManager().getMaxStructureSize();
        int blockCount = structureData.getBlocks().size();
        
        plugin.getLogger().info("Validating structure: " + blockCount + " blocks, maxSize: " + maxSize);
        
        // Check total block count
        if (blockCount > maxSize * maxSize * maxSize) {
            plugin.getLogger().warning("Structure validation failed: too many blocks (" + blockCount + " > " + (maxSize * maxSize * maxSize) + ")");
            return false;
        }
          // Check individual coordinates
        for (int i = 0; i < structureData.getBlocks().size(); i++) {
            StructureData.Block instruction = structureData.getBlocks().get(i);
            if (instruction == null) {
                plugin.getLogger().warning("Structure validation failed: block " + i + " is null");
                return false;
            }
            
            if (Math.abs(instruction.getX()) > maxSize || 
                Math.abs(instruction.getY()) > maxSize || 
                Math.abs(instruction.getZ()) > maxSize) {
                plugin.getLogger().warning("Structure validation failed: block " + i + " coordinates out of bounds: " + 
                    instruction.getX() + "," + instruction.getY() + "," + instruction.getZ() + " (max: " + maxSize + ")");
                return false;
            }
        }
        
        plugin.getLogger().info("Structure validation passed!");
        return true;
    }/**
     * Cancel build for specific player
     */
    public void cancelBuild(UUID playerId) {
        try {
            BukkitTask task = activeBuildTasks.remove(playerId);
            if (task != null && !task.isCancelled()) {
                task.cancel();
                if (plugin.getConfigManager().shouldLogBuilding()) {
                    plugin.getLogger().info("Cancelled build task for player: " + playerId);
                }
            }
            buildProgress.remove(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Error cancelling build for player " + playerId + ": " + e.getMessage());
        }
    }

    /**
     * Cancel all active builds
     */
    public void cancelAllBuilds() {
        for (BukkitTask task : activeBuildTasks.values()) {
            task.cancel();
        }
        activeBuildTasks.clear();
        buildProgress.clear();
    }

    /**
     * Check if player has an active build
     */
    public boolean hasActiveBuild(UUID playerId) {
        return activeBuildTasks.containsKey(playerId);
    }
    
    /**
     * Get build progress for a player
     */
    public int getBuildProgress(UUID playerId) {
        return buildProgress.getOrDefault(playerId, 0);
    }
      /**
     * Get count of active builds
     */
    public int getActiveBuildCount() {
        return activeBuildTasks.size();
    }
    
    /**
     * Get estimated size of a structure (number of blocks)
     */
    public int getEstimatedSize(StructureData structureData) {
        return structureData.getBlocks().size();
    }
}
