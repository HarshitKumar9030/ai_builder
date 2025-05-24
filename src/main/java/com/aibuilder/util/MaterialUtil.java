package com.aibuilder.util;

import org.bukkit.Material;

import java.util.Set;
import java.util.HashSet;

/**
 * Utility class for Material validation and handling
 */
public class MaterialUtil {
    
    private static final Set<Material> VALID_BUILDING_MATERIALS = new HashSet<>();
    
    static {
        // Add common building materials
        VALID_BUILDING_MATERIALS.add(Material.STONE);
        VALID_BUILDING_MATERIALS.add(Material.COBBLESTONE);
        VALID_BUILDING_MATERIALS.add(Material.STONE_BRICKS);
        VALID_BUILDING_MATERIALS.add(Material.OAK_PLANKS);
        VALID_BUILDING_MATERIALS.add(Material.SPRUCE_PLANKS);
        VALID_BUILDING_MATERIALS.add(Material.BIRCH_PLANKS);
        VALID_BUILDING_MATERIALS.add(Material.JUNGLE_PLANKS);
        VALID_BUILDING_MATERIALS.add(Material.ACACIA_PLANKS);
        VALID_BUILDING_MATERIALS.add(Material.DARK_OAK_PLANKS);
        VALID_BUILDING_MATERIALS.add(Material.GLASS);
        VALID_BUILDING_MATERIALS.add(Material.BRICK);
        VALID_BUILDING_MATERIALS.add(Material.SANDSTONE);
        VALID_BUILDING_MATERIALS.add(Material.QUARTZ_BLOCK);
        VALID_BUILDING_MATERIALS.add(Material.IRON_BLOCK);
        VALID_BUILDING_MATERIALS.add(Material.GOLD_BLOCK);
        VALID_BUILDING_MATERIALS.add(Material.DIAMOND_BLOCK);
        VALID_BUILDING_MATERIALS.add(Material.EMERALD_BLOCK);        VALID_BUILDING_MATERIALS.add(Material.NETHERITE_BLOCK);
        VALID_BUILDING_MATERIALS.add(Material.WHITE_WOOL);
        VALID_BUILDING_MATERIALS.add(Material.WHITE_CONCRETE);
        VALID_BUILDING_MATERIALS.add(Material.TERRACOTTA);
    }

    /**
     * Check if material is safe for building
     */
    public static boolean isSafeBuildingMaterial(Material material) {
        if (material == null) return false;
        
        // Block dangerous materials
        if (material == Material.TNT || 
            material == Material.LAVA || 
            material == Material.WATER ||
            material.name().contains("SPAWN") ||
            material.name().contains("COMMAND")) {
            return false;
        }
        
        return material.isBlock() && material.isSolid();
    }

    /**
     * Get material by name with fallback
     */
    public static Material getMaterialSafely(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.STONE;
        }
        
        Material material = Material.matchMaterial(materialName.toUpperCase());
        
        if (material == null || !isSafeBuildingMaterial(material)) {
            return Material.STONE; // Safe fallback
        }
        
        return material;
    }

    /**
     * Get a random valid building material
     */
    public static Material getRandomBuildingMaterial() {
        Material[] materials = VALID_BUILDING_MATERIALS.toArray(new Material[0]);
        return materials[(int) (Math.random() * materials.length)];
    }
}
