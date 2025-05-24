package com.aibuilder.manager;

import com.aibuilder.AIStructureBuilder;
import com.aibuilder.model.StructureData;
import com.aibuilder.processor.EnhancedResponseProcessor;
import com.google.gson.Gson;
import lombok.Getter;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles chunked generation for large structures
 */
public class ChunkedGenerationManager {
      private final AIStructureBuilder plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final EnhancedResponseProcessor responseProcessor;
    
    @Getter
    private static class ChunkInfo {
        private final int chunkX;
        private final int chunkZ;
        private final String description;
        private final String context;
        
        public ChunkInfo(int chunkX, int chunkZ, String description, String context) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.description = description;
            this.context = context;
        }
    }
      public ChunkedGenerationManager(AIStructureBuilder plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(plugin.getConfigManager().getConnectTimeout(), java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(plugin.getConfigManager().getReadTimeout(), java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(plugin.getConfigManager().getWriteTimeout(), java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.responseProcessor = new EnhancedResponseProcessor(plugin);
    }
    
    /**
     * Generate a large structure using chunked approach
     */
    public CompletableFuture<StructureData> generateLargeStructure(String description, int targetSize, Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                progressCallback.accept("Planning large structure generation...");
                
                // Calculate structure dimensions based on target size
                int estimatedDimensions = (int) Math.cbrt(targetSize) * 2; // Rough cube root * 2 for variety
                int chunkSize = plugin.getConfigManager().getChunkSize();
                int chunksPerSide = Math.max(1, estimatedDimensions / chunkSize);
                
                progressCallback.accept("Structure will be " + chunksPerSide + "x" + chunksPerSide + " chunks (" + (chunksPerSide * chunksPerSide) + " total chunks)");
                
                // Generate overall structure plan first
                String overallPlan = generateOverallPlan(description, chunksPerSide, progressCallback);
                
                // Create chunks info
                List<ChunkInfo> chunks = createChunkPlan(overallPlan, chunksPerSide, description);
                
                // Generate each chunk
                StructureData combinedStructure = new StructureData();
                combinedStructure.setName("Large " + description);
                combinedStructure.setDescription("AI-generated large structure: " + description);
                
                StructureData.Size totalSize = new StructureData.Size();
                totalSize.setWidth(chunksPerSide * chunkSize);
                totalSize.setHeight(chunkSize);
                totalSize.setDepth(chunksPerSide * chunkSize);
                combinedStructure.setSize(totalSize);
                
                List<StructureData.Block> allBlocks = new ArrayList<>();
                
                int chunkCount = 0;
                for (ChunkInfo chunk : chunks) {
                    chunkCount++;
                    progressCallback.accept("Generating chunk " + chunkCount + "/" + chunks.size() + " (" + chunk.getDescription() + ")");
                    
                    try {
                        StructureData chunkData = generateSingleChunk(chunk, chunkSize);
                        
                        // Offset blocks to correct position
                        int offsetX = chunk.getChunkX() * chunkSize;
                        int offsetZ = chunk.getChunkZ() * chunkSize;
                        
                        for (StructureData.Block block : chunkData.getBlocks()) {
                            StructureData.Block offsetBlock = new StructureData.Block();
                            offsetBlock.setX(block.getX() + offsetX);
                            offsetBlock.setY(block.getY());
                            offsetBlock.setZ(block.getZ() + offsetZ);
                            offsetBlock.setMaterial(block.getMaterial());
                            offsetBlock.setData(block.getData());
                            allBlocks.add(offsetBlock);
                        }
                        
                        progressCallback.accept("Chunk " + chunkCount + " completed with " + chunkData.getBlocks().size() + " blocks");
                        
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to generate chunk " + chunkCount + ": " + e.getMessage());
                        progressCallback.accept("Chunk " + chunkCount + " failed, creating fallback...");
                        
                        // Create simple fallback for this chunk
                        List<StructureData.Block> fallbackBlocks = createFallbackChunk(chunk, chunkSize);
                        allBlocks.addAll(fallbackBlocks);
                    }
                    
                    // Add small delay to prevent rate limiting
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                combinedStructure.setBlocks(allBlocks);
                
                progressCallback.accept("Large structure generation completed! Total blocks: " + allBlocks.size());
                plugin.getLogger().info("Generated large structure with " + allBlocks.size() + " blocks across " + chunks.size() + " chunks");
                
                return combinedStructure;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error generating large structure: " + e.getMessage());
                throw new RuntimeException("Failed to generate large structure: " + e.getMessage());
            }
        });
    }
      /**
     * Generate overall structure plan
     */
    private String generateOverallPlan(String description, int chunksPerSide, Consumer<String> progressCallback) throws IOException {
        progressCallback.accept("Creating overall structure plan...");
        
        String planPrompt = "Create a brief plan for a Minecraft structure: " + description + "\n\n" +
                "Structure will be " + chunksPerSide + "x" + chunksPerSide + " chunks.\n" +
                "For each chunk position (0,0) to (" + (chunksPerSide-1) + "," + (chunksPerSide-1) + "), describe in 1-2 words:\n" +
                "What should be built in that chunk.\n\n" +
                "Keep it simple and under 500 characters total.\n" +
                "NO comments, NO extra text, just the plan.\n\n" +
                "Example: entrance, walls, courtyard, tower, etc.";
        
        return callGeminiAPI(planPrompt);
    }
    
    /**
     * Create chunk plan from overall plan
     */
    private List<ChunkInfo> createChunkPlan(String overallPlan, int chunksPerSide, String baseDescription) {
        List<ChunkInfo> chunks = new ArrayList<>();
        
        // Parse the overall plan and create specific chunk descriptions
        String[] planLines = overallPlan.split("\n");
        Map<String, String> chunkDescriptions = new HashMap<>();
        
        // Extract chunk-specific information from the plan
        for (String line : planLines) {
            if (line.contains("chunk") || line.contains("Chunk") || line.contains("section") || line.contains("area")) {
                // Simple parsing - in a real implementation you might use more sophisticated NLP
                if (line.length() > 20) { // Ignore very short lines
                    String key = "general_" + chunkDescriptions.size();
                    chunkDescriptions.put(key, line.trim());
                }
            }
        }
        
        // Create chunks with descriptions
        List<String> descriptions = new ArrayList<>(chunkDescriptions.values());
        if (descriptions.isEmpty()) {
            // Fallback descriptions
            descriptions.add("main entrance and foundation");
            descriptions.add("central courtyard area");
            descriptions.add("residential quarters");
            descriptions.add("decorative gardens");
            descriptions.add("defensive walls and towers");
            descriptions.add("storage and utility areas");
            descriptions.add("ceremonial halls");
            descriptions.add("connecting pathways");
        }
        
        int descIndex = 0;
        for (int x = 0; x < chunksPerSide; x++) {
            for (int z = 0; z < chunksPerSide; z++) {
                String chunkDesc = descriptions.get(descIndex % descriptions.size());
                String contextualDesc = baseDescription + " - " + chunkDesc + 
                        " (chunk " + x + "," + z + " of " + chunksPerSide + "x" + chunksPerSide + " structure)";
                
                chunks.add(new ChunkInfo(x, z, contextualDesc, overallPlan));
                descIndex++;
            }
        }
        
        return chunks;
    }
    
    /**
     * Generate a single chunk
     */    private StructureData generateSingleChunk(ChunkInfo chunk, int chunkSize) throws IOException {
        String chunkPrompt = createChunkPrompt(chunk, chunkSize);
        String response = callGeminiAPI(chunkPrompt);
        return responseProcessor.processResponse(response, chunk.getDescription());
    }
      /**
     * Create prompt for individual chunk
     */
    private String createChunkPrompt(ChunkInfo chunk, int chunkSize) {
        return "Generate a Minecraft structure chunk for: " + chunk.getDescription() + "\n\n" +
                "CRITICAL REQUIREMENTS:\n" +
                "- Chunk size: " + chunkSize + "x" + chunkSize + "x" + chunkSize + " blocks\n" +
                "- Coordinates: X[0-" + (chunkSize-1) + "], Y[0-" + (chunkSize-1) + "], Z[0-" + (chunkSize-1) + "]\n" +
                "- This is chunk (" + chunk.getChunkX() + "," + chunk.getChunkZ() + ") in a larger structure\n" +
                "- Return ONLY valid JSON - NO comments, NO explanations, NO markdown\n" +
                "- Do NOT use // comments or any other text outside JSON\n" +
                "- Keep response under 3000 characters\n" +
                "- Maximum " + (chunkSize * chunkSize * chunkSize / 4) + " blocks\n\n" +
                "CONTEXT:\n" + chunk.getContext() + "\n\n" +
                "Return this exact JSON format:\n" +
                "{\n" +
                "  \"name\": \"structure name\",\n" +
                "  \"description\": \"description\",\n" +
                "  \"size\": {\"width\": " + chunkSize + ", \"height\": " + chunkSize + ", \"depth\": " + chunkSize + "},\n" +
                "  \"blocks\": [\n" +
                "    {\"x\": 0, \"y\": 0, \"z\": 0, \"material\": \"STONE\", \"data\": \"\"}\n" +
                "  ]\n" +
                "}\n\n" +                "Use these materials: STONE, COBBLESTONE, STONE_BRICKS, MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS, " +
                "CHISELED_STONE_BRICKS, SMOOTH_STONE, GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, " +
                "ANDESITE, POLISHED_ANDESITE, DEEPSLATE, COBBLED_DEEPSLATE, POLISHED_DEEPSLATE, " +
                "OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS, " +
                "MANGROVE_PLANKS, CHERRY_PLANKS, BAMBOO_PLANKS, CRIMSON_PLANKS, WARPED_PLANKS, " +
                "OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG, " +
                "CHERRY_LOG, BAMBOO_BLOCK, CRIMSON_STEM, WARPED_STEM, STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, " +
                "GLASS, WHITE_STAINED_GLASS, ORANGE_STAINED_GLASS, MAGENTA_STAINED_GLASS, LIGHT_BLUE_STAINED_GLASS, " +
                "YELLOW_STAINED_GLASS, LIME_STAINED_GLASS, PINK_STAINED_GLASS, GRAY_STAINED_GLASS, " +
                "LIGHT_GRAY_STAINED_GLASS, CYAN_STAINED_GLASS, PURPLE_STAINED_GLASS, BLUE_STAINED_GLASS, " +
                "BROWN_STAINED_GLASS, GREEN_STAINED_GLASS, RED_STAINED_GLASS, BLACK_STAINED_GLASS, " +
                "GLASS_PANE, WHITE_STAINED_GLASS_PANE, IRON_BARS, " +
                "BRICKS, MUD_BRICKS, PACKED_MUD, SANDSTONE, RED_SANDSTONE, SMOOTH_SANDSTONE, " +
                "CUT_SANDSTONE, CHISELED_SANDSTONE, QUARTZ_BLOCK, SMOOTH_QUARTZ, QUARTZ_PILLAR, " +
                "CHISELED_QUARTZ_BLOCK, PRISMARINE, PRISMARINE_BRICKS, DARK_PRISMARINE, " +
                "BLACKSTONE, POLISHED_BLACKSTONE, POLISHED_BLACKSTONE_BRICKS, CRACKED_POLISHED_BLACKSTONE_BRICKS, " +
                "NETHER_BRICKS, RED_NETHER_BRICKS, CHISELED_NETHER_BRICKS, CRACKED_NETHER_BRICKS, " +
                "END_STONE, END_STONE_BRICKS, PURPUR_BLOCK, PURPUR_PILLAR, " +
                "TERRACOTTA, WHITE_TERRACOTTA, ORANGE_TERRACOTTA, MAGENTA_TERRACOTTA, LIGHT_BLUE_TERRACOTTA, " +
                "YELLOW_TERRACOTTA, LIME_TERRACOTTA, PINK_TERRACOTTA, GRAY_TERRACOTTA, LIGHT_GRAY_TERRACOTTA, " +
                "CYAN_TERRACOTTA, PURPLE_TERRACOTTA, BLUE_TERRACOTTA, BROWN_TERRACOTTA, GREEN_TERRACOTTA, " +
                "RED_TERRACOTTA, BLACK_TERRACOTTA, GLAZED_TERRACOTTA, " +
                "WHITE_CONCRETE, ORANGE_CONCRETE, MAGENTA_CONCRETE, LIGHT_BLUE_CONCRETE, YELLOW_CONCRETE, " +
                "LIME_CONCRETE, PINK_CONCRETE, GRAY_CONCRETE, LIGHT_GRAY_CONCRETE, CYAN_CONCRETE, " +
                "PURPLE_CONCRETE, BLUE_CONCRETE, BROWN_CONCRETE, GREEN_CONCRETE, RED_CONCRETE, BLACK_CONCRETE, " +
                "WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL, YELLOW_WOOL, LIME_WOOL, " +
                "PINK_WOOL, GRAY_WOOL, LIGHT_GRAY_WOOL, CYAN_WOOL, PURPLE_WOOL, BLUE_WOOL, " +
                "BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL, " +
                "OAK_STAIRS, SPRUCE_STAIRS, BIRCH_STAIRS, JUNGLE_STAIRS, ACACIA_STAIRS, DARK_OAK_STAIRS, " +
                "STONE_STAIRS, COBBLESTONE_STAIRS, STONE_BRICK_STAIRS, MOSSY_STONE_BRICK_STAIRS, " +
                "GRANITE_STAIRS, POLISHED_GRANITE_STAIRS, DIORITE_STAIRS, POLISHED_DIORITE_STAIRS, " +
                "ANDESITE_STAIRS, POLISHED_ANDESITE_STAIRS, BRICK_STAIRS, SANDSTONE_STAIRS, " +
                "RED_SANDSTONE_STAIRS, PRISMARINE_STAIRS, PRISMARINE_BRICK_STAIRS, DARK_PRISMARINE_STAIRS, " +
                "NETHER_BRICK_STAIRS, RED_NETHER_BRICK_STAIRS, BLACKSTONE_STAIRS, POLISHED_BLACKSTONE_STAIRS, " +
                "POLISHED_BLACKSTONE_BRICK_STAIRS, QUARTZ_STAIRS, PURPUR_STAIRS, END_STONE_BRICK_STAIRS, " +
                "OAK_SLAB, SPRUCE_SLAB, BIRCH_SLAB, JUNGLE_SLAB, ACACIA_SLAB, DARK_OAK_SLAB, " +
                "STONE_SLAB, COBBLESTONE_SLAB, STONE_BRICK_SLAB, MOSSY_STONE_BRICK_SLAB, " +
                "SMOOTH_STONE_SLAB, GRANITE_SLAB, POLISHED_GRANITE_SLAB, DIORITE_SLAB, POLISHED_DIORITE_SLAB, " +
                "ANDESITE_SLAB, POLISHED_ANDESITE_SLAB, BRICK_SLAB, SANDSTONE_SLAB, CUT_SANDSTONE_SLAB, " +
                "RED_SANDSTONE_SLAB, CUT_RED_SANDSTONE_SLAB, PRISMARINE_SLAB, PRISMARINE_BRICK_SLAB, " +
                "DARK_PRISMARINE_SLAB, NETHER_BRICK_SLAB, RED_NETHER_BRICK_SLAB, BLACKSTONE_SLAB, " +
                "POLISHED_BLACKSTONE_SLAB, POLISHED_BLACKSTONE_BRICK_SLAB, QUARTZ_SLAB, SMOOTH_QUARTZ_SLAB, " +
                "PURPUR_SLAB, END_STONE_BRICK_SLAB, " +
                "OAK_FENCE, SPRUCE_FENCE, BIRCH_FENCE, JUNGLE_FENCE, ACACIA_FENCE, DARK_OAK_FENCE, " +
                "NETHER_BRICK_FENCE, IRON_BARS, CHAIN, " +
                "LANTERN, SOUL_LANTERN, TORCH, SOUL_TORCH, REDSTONE_TORCH, " +
                "OAK_DOOR, SPRUCE_DOOR, BIRCH_DOOR, JUNGLE_DOOR, ACACIA_DOOR, DARK_OAK_DOOR, IRON_DOOR, " +
                "OAK_TRAPDOOR, SPRUCE_TRAPDOOR, BIRCH_TRAPDOOR, JUNGLE_TRAPDOOR, ACACIA_TRAPDOOR, DARK_OAK_TRAPDOOR, IRON_TRAPDOOR, " +
                "LADDER, VINE, SCAFFOLDING, " +
                "DIRT, GRASS_BLOCK, PODZOL, MYCELIUM, DIRT_PATH, FARMLAND, " +
                "SAND, RED_SAND, GRAVEL, CLAY, " +
                "WATER, LAVA, ICE, PACKED_ICE, BLUE_ICE, SNOW_BLOCK, SNOW, " +
                "OBSIDIAN, CRYING_OBSIDIAN, RESPAWN_ANCHOR, " +
                "BOOKSHELF, CHISELED_BOOKSHELF, LECTERN, " +
                "CHEST, TRAPPED_CHEST, BARREL, SHULKER_BOX, " +
                "CRAFTING_TABLE, FURNACE, BLAST_FURNACE, SMOKER, " +
                "BELL, ANVIL, ENCHANTING_TABLE, BREWING_STAND, CAULDRON, " +
                "FLOWER_POT, DECORATED_POT, " +
                "COBWEB, MUSHROOM_STEM, BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK, " +
                "SPONGE, WET_SPONGE, " +
                "BEDROCK, BARRIER, STRUCTURE_VOID, AIR";
    }
    
    /**
     * Create fallback chunk when AI generation fails
     */
    private List<StructureData.Block> createFallbackChunk(ChunkInfo chunk, int chunkSize) {
        List<StructureData.Block> blocks = new ArrayList<>();
        
        int offsetX = chunk.getChunkX() * chunkSize;
        int offsetZ = chunk.getChunkZ() * chunkSize;
        
        // Create a simple structure for this chunk
        for (int x = 0; x < chunkSize; x += 4) {
            for (int z = 0; z < chunkSize; z += 4) {
                for (int y = 0; y < 4; y++) {
                    StructureData.Block block = new StructureData.Block();
                    block.setX(x + offsetX);
                    block.setY(y);
                    block.setZ(z + offsetZ);
                    block.setMaterial(y == 0 ? "STONE" : "COBBLESTONE");
                    block.setData("");
                    blocks.add(block);
                }
            }
        }
        
        return blocks;
    }
    
    /**
     * Call Gemini API
     */
    private String callGeminiAPI(String prompt) throws IOException {
        String apiKey = plugin.getConfigManager().getGeminiApiKey();
        String model = plugin.getConfigManager().getGeminiModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        
        String jsonBody = "{\n" +
                "  \"contents\": [{\n" +
                "    \"parts\": [{\n" +
                "      \"text\": \"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"\n" +
                "    }]\n" +
                "  }],\n" +
                "  \"generationConfig\": {\n" +
                "    \"temperature\": " + plugin.getConfigManager().getTemperature() + ",\n" +
                "    \"maxOutputTokens\": " + plugin.getConfigManager().getMaxTokens() + "\n" +
                "  }\n" +
                "}";
        
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed: " + response.code() + " " + response.message());
            }
            
            String responseBody = response.body().string();
            
            // Parse Gemini response format            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = gson.fromJson(responseBody, Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> firstCandidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            
            throw new IOException("No valid response content found");
        }
    }
    
    /**
     * Parse AI response into StructureData
     */
    private StructureData parseResponse(String response) {
        try {
            // Clean up response
            String jsonResponse = response.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            jsonResponse = jsonResponse.trim();
            
            StructureData structureData = gson.fromJson(jsonResponse, StructureData.class);
            
            if (structureData == null || structureData.getBlocks() == null || structureData.getBlocks().isEmpty()) {
                throw new RuntimeException("No blocks found in structure data");
            }
            
            return structureData;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse chunk response: " + e.getMessage());
            throw new RuntimeException("Failed to parse chunk response");
        }
    }
}
