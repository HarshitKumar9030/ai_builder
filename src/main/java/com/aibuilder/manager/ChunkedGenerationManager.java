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
        
        String planPrompt = "Create a detailed plan for a large Minecraft structure: " + description + "\n\n" +
                "The structure will be built in a " + chunksPerSide + "x" + chunksPerSide + " grid of chunks.\n" +
                "For each chunk position (0,0) to (" + (chunksPerSide-1) + "," + (chunksPerSide-1) + "), describe:\n" +
                "1. What should be built in that chunk\n" +
                "2. How it connects to neighboring chunks\n" +
                "3. The main purpose/theme of that section\n\n" +
                "Make this a cohesive, detailed " + description + " that uses the full area effectively.\n" +
                "Focus on creating variety and interesting architectural features.\n\n" +
                "Format as a grid layout plan.";
        
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
                "CONSTRAINTS:\n" +
                "- Chunk size: " + chunkSize + "x" + chunkSize + "x" + chunkSize + " blocks\n" +
                "- Coordinates: X[0-" + (chunkSize-1) + "], Y[0-" + (chunkSize-1) + "], Z[0-" + (chunkSize-1) + "]\n" +
                "- This is chunk (" + chunk.getChunkX() + "," + chunk.getChunkZ() + ") in a larger structure\n" +
                "- Make this chunk detailed and interesting\n" +
                "- Use a variety of materials and heights\n" +
                "- Consider connections to adjacent chunks\n\n" +
                "CONTEXT:\n" + chunk.getContext() + "\n\n" +
                "Generate JSON with this exact structure:\n" +
                "{\n" +
                "  \"name\": \"structure name\",\n" +
                "  \"description\": \"description\",\n" +
                "  \"size\": {\"width\": " + chunkSize + ", \"height\": " + chunkSize + ", \"depth\": " + chunkSize + "},\n" +
                "  \"blocks\": [\n" +
                "    {\"x\": 0, \"y\": 0, \"z\": 0, \"material\": \"STONE\", \"data\": \"\"}\n" +
                "  ]\n" +
                "}\n\n" +
                "Use these materials: STONE, COBBLESTONE, STONE_BRICKS, OAK_PLANKS, OAK_LOG, GLASS, " +
                "IRON_BARS, OAK_STAIRS, STONE_BRICK_STAIRS, OAK_SLAB, STONE_BRICK_SLAB, " +
                "COBBLESTONE_STAIRS, MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS, " +
                "DARK_OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, AIR";
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
