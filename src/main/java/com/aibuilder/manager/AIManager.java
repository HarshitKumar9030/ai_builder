package com.aibuilder.manager;

import com.aibuilder.AIStructureBuilder;
import com.aibuilder.model.StructureData;
import com.aibuilder.processor.EnhancedResponseProcessor;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages AI interactions with Google Gemini API
 */
public class AIManager {
      private final AIStructureBuilder plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ChunkedGenerationManager chunkedManager;
    private final EnhancedResponseProcessor responseProcessor;
    @Getter
    private boolean configured = false;
      public AIManager(AIStructureBuilder plugin) {
        this.plugin = plugin;
        // Configure HTTP client with timeouts
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.chunkedManager = new ChunkedGenerationManager(plugin);
        this.responseProcessor = new EnhancedResponseProcessor(plugin);
        // Don't call updateConfiguration() here - will be called after config is loaded
    }

    /**
     * Update configuration status
     */
    public void updateConfiguration() {
        String apiKey = plugin.getConfigManager().getGeminiApiKey();
        this.configured = apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY_HERE");
    }
    
    /**
     * Generate structure instructions using Gemini AI with retry logic
     */
    public CompletableFuture<StructureData> generateStructure(String description, int maxSize) {
        return CompletableFuture.supplyAsync(() -> {
            final int maxRetries = 3;
            Exception lastException = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    if (!configured) {
                        throw new RuntimeException("AI not configured - please set API key");
                    }

                    plugin.getLogger().info("Generating structure (attempt " + attempt + "/" + maxRetries + "): " + description);
                    
                    String prompt = createPrompt(description, maxSize);
                    String response = callGeminiAPI(prompt);
                    StructureData result = parseResponse(response);
                    
                    plugin.getLogger().info("Structure generated successfully!");
                    return result;
                    
                } catch (Exception e) {
                    lastException = e;
                    plugin.getLogger().warning("Attempt " + attempt + " failed: " + e.getMessage());
                    
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(2000 * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }            }
            
            // If all attempts failed, generate a simple fallback structure
            plugin.getLogger().warning("AI generation failed after " + maxRetries + " attempts, creating simple fallback structure");
            if (lastException != null) {
                plugin.getLogger().warning("Last error: " + lastException.getMessage());
            }
            return createFallbackStructure(description);
        });
    }    /**
     * Generate structure instructions using Gemini AI with progress updates
     */
    public CompletableFuture<StructureData> generateStructureWithProgress(String description, int maxSize, Consumer<String> progressCallback) {
        // Check if we should use chunked generation for large structures
        if (plugin.getConfigManager().isChunkedGenerationEnabled() && 
            maxSize >= plugin.getConfigManager().getChunkedThreshold()) {
            
            progressCallback.accept("Large structure detected, using chunked generation...");
            plugin.getLogger().info("Using chunked generation for large structure: " + description + " (target size: " + maxSize + ")");
            
            return chunkedManager.generateLargeStructure(description, maxSize, progressCallback);
        }
        
        // Use regular generation for smaller structures
        return generateRegularStructure(description, maxSize, progressCallback);
    }
    
    /**
     * Generate regular-sized structure
     */
    private CompletableFuture<StructureData> generateRegularStructure(String description, int maxSize, Consumer<String> progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            final int maxRetries = 3;
            Exception lastException = null;
            
            progressCallback.accept("Starting AI structure generation...");
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    if (!configured) {
                        throw new RuntimeException("AI not configured - please set API key");
                    }

                    progressCallback.accept("Attempt " + attempt + "/" + maxRetries + " - Preparing AI request...");
                    plugin.getLogger().info("Generating structure (attempt " + attempt + "/" + maxRetries + "): " + description);
                    
                    String prompt = createPrompt(description, maxSize);
                    progressCallback.accept("Sending request to Gemini AI...");
                      String response = callGeminiAPI(prompt);
                    progressCallback.accept("Processing AI response...");
                    
                    StructureData result = responseProcessor.processResponse(response, description);
                    progressCallback.accept("Structure generation completed successfully!");
                    
                    plugin.getLogger().info("Structure generated successfully!");
                    return result;
                    
                } catch (Exception e) {
                    lastException = e;
                    progressCallback.accept("Attempt " + attempt + " failed: " + e.getMessage());
                    plugin.getLogger().warning("Attempt " + attempt + " failed: " + e.getMessage());
                    
                    if (attempt < maxRetries) {
                        progressCallback.accept("Retrying in " + (2 * attempt) + " seconds...");
                        try {
                            Thread.sleep(2000 * attempt); // Exponential backoff
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
              // If all attempts failed, generate a simple fallback structure
            progressCallback.accept("AI generation failed, creating fallback structure...");
            plugin.getLogger().warning("AI generation failed after " + maxRetries + " attempts, creating simple fallback structure");
            if (lastException != null) {
                plugin.getLogger().warning("Last error: " + lastException.getMessage());
            }
            return createFallbackStructure(description);
        });
    }

    /**
     * Create a simple fallback structure when AI fails
     */
    private StructureData createFallbackStructure(String description) {
        StructureData fallback = new StructureData();
        fallback.setName("Simple " + description);
        fallback.setDescription("Fallback structure created when AI was unavailable");
        
        // Create a simple 3x3x3 structure
        StructureData.Size size = new StructureData.Size();
        size.setWidth(3);
        size.setHeight(3);
        size.setDepth(3);
        fallback.setSize(size);
        
        // Create a simple house structure
        java.util.List<StructureData.Block> blocks = new java.util.ArrayList<>();
        
        // Foundation
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                blocks.add(createBlock(x, 0, z, "STONE"));
            }
        }
        
        // Walls (y=1)
        for (int x = 0; x < 3; x++) {
            blocks.add(createBlock(x, 1, 0, "OAK_PLANKS")); // Front wall
            blocks.add(createBlock(x, 1, 2, "OAK_PLANKS")); // Back wall
        }
        for (int z = 0; z < 3; z++) {
            blocks.add(createBlock(0, 1, z, "OAK_PLANKS")); // Left wall
            blocks.add(createBlock(2, 1, z, "OAK_PLANKS")); // Right wall
        }
        
        // Roof
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                blocks.add(createBlock(x, 2, z, "STONE_BRICKS"));
            }
        }
        
        // Add a door
        blocks.add(createBlock(1, 1, 0, "AIR")); // Remove wall block for door
        
        fallback.setBlocks(blocks);
        plugin.getLogger().info("Created fallback structure with " + blocks.size() + " blocks");
        return fallback;
    }
    
    /**
     * Helper method to create a block
     */
    private StructureData.Block createBlock(int x, int y, int z, String material) {
        StructureData.Block block = new StructureData.Block();
        block.setX(x);
        block.setY(y);
        block.setZ(z);
        block.setMaterial(material);        block.setData("");
        return block;
    }
      /**
     * Create a detailed prompt for structure generation
     */
    private String createPrompt(String description, int maxSize) {
        // Use larger structure sizes for regular generation, but still cap to prevent API issues
        int actualMaxSize = Math.min(maxSize, 2000); // Much larger cap for substantial structures
        
        // Determine structure complexity based on size
        String sizeGuidance;
        String dimensionGuidance;
        if (actualMaxSize <= 100) {
            sizeGuidance = "SMALL to MEDIUM";
            dimensionGuidance = "Maximum size: 10x10x10 blocks";
        } else if (actualMaxSize <= 500) {
            sizeGuidance = "MEDIUM to LARGE";
            dimensionGuidance = "Maximum size: 15x15x15 blocks";
        } else {
            sizeGuidance = "LARGE and DETAILED";
            dimensionGuidance = "Maximum size: 20x20x20 blocks";
        }
          return String.format(
            "You are a Minecraft master architect. Create a %s, DETAILED structure for: \"%s\"\n\n" +
            "REQUIREMENTS:\n" +
            "- Target %d blocks total (aim for substantial structures with good detail)\n" +
            "- %s\n" +
            "- Use diverse materials: STONE, STONE_BRICKS, MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS, COBBLESTONE, SMOOTH_STONE, GRANITE, POLISHED_GRANITE, DIORITE, POLISHED_DIORITE, ANDESITE, POLISHED_ANDESITE, " +
            "OAK_PLANKS, SPRUCE_PLANKS, BIRCH_PLANKS, JUNGLE_PLANKS, ACACIA_PLANKS, DARK_OAK_PLANKS, OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG, " +
            "GLASS, WHITE_STAINED_GLASS, LIGHT_BLUE_STAINED_GLASS, YELLOW_STAINED_GLASS, GLASS_PANE, WHITE_STAINED_GLASS_PANE, " +
            "BRICKS, SANDSTONE, RED_SANDSTONE, QUARTZ_BLOCK, SMOOTH_QUARTZ, PRISMARINE, PRISMARINE_BRICKS, BLACKSTONE, POLISHED_BLACKSTONE, NETHER_BRICKS, END_STONE_BRICKS, " +
            "WHITE_TERRACOTTA, ORANGE_TERRACOTTA, LIGHT_BLUE_TERRACOTTA, WHITE_CONCRETE, GRAY_CONCRETE, LIGHT_GRAY_CONCRETE, WHITE_WOOL, LIGHT_GRAY_WOOL, " +
            "OAK_STAIRS, SPRUCE_STAIRS, STONE_STAIRS, STONE_BRICK_STAIRS, GRANITE_STAIRS, BRICK_STAIRS, SANDSTONE_STAIRS, QUARTZ_STAIRS, " +
            "OAK_SLAB, SPRUCE_SLAB, STONE_SLAB, STONE_BRICK_SLAB, SMOOTH_STONE_SLAB, GRANITE_SLAB, BRICK_SLAB, SANDSTONE_SLAB, QUARTZ_SLAB, " +
            "OAK_FENCE, SPRUCE_FENCE, IRON_BARS, CHAIN, LANTERN, TORCH, OAK_DOOR, IRON_DOOR, OAK_TRAPDOOR, LADDER, " +
            "DIRT, GRASS_BLOCK, SAND, GRAVEL, WATER, SNOW_BLOCK, BOOKSHELF, CHEST, CRAFTING_TABLE, FURNACE, FLOWER_POT, AIR\n" +
            "- Create realistic, detailed architecture with proper foundations, walls, roofs, and interior features\n" +
            "- Add architectural details like windows, doors, stairs, decorative elements\n" +
            "- Coordinates start at (0,0,0)\n" +
            "- Build upward and outward to create impressive structures\n\n" +
            "STRUCTURE GUIDELINES:\n" +
            "- For houses: Include foundation, walls, roof, windows, door frame\n" +
            "- For castles: Include towers, walls, battlements, courtyard\n" +
            "- For modern buildings: Include glass facades, geometric designs\n" +
            "- For bridges: Include support pillars, railings, decorative arches\n" +
            "- Always include proper foundations and structural support\n\n" +            "RESPOND WITH VALID JSON ONLY (no markdown, no explanations, no comments):\n" +
            "{\n" +
            "  \"name\": \"Structure Name\",\n" +
            "  \"description\": \"Detailed description\",\n" +
            "  \"size\": {\n" +
            "    \"width\": 15,\n" +
            "    \"height\": 12,\n" +
            "    \"depth\": 15\n" +
            "  },\n" +
            "  \"blocks\": [\n" +
            "    {\n" +
            "      \"x\": 0,\n" +
            "      \"y\": 0,\n" +
            "      \"z\": 0,\n" +
            "      \"material\": \"STONE\",\n" +
            "      \"data\": \"\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n\n" +
            "CRITICAL REQUIREMENTS:\n" +
            "- Return ONLY valid JSON - NO comments (//) anywhere\n" +
            "- NO markdown formatting (```json)\n" +
            "- NO explanations or text outside JSON\n" +
            "- Keep response under 4000 characters total\n" +
            "- Generate substantial structures with %d+ blocks for impressive builds\n" +
            "- Use realistic proportions and architectural principles\n" +
            "- Include detailed features and decorative elements\n" +
            "- Make it architecturally sound and visually impressive",
            sizeGuidance, description, actualMaxSize, dimensionGuidance, actualMaxSize);
    }

    /**
     * Call Gemini API
     */
    private String callGeminiAPI(String prompt) throws IOException {
        String apiKey = plugin.getConfigManager().getGeminiApiKey();
        String model = plugin.getConfigManager().getGeminiModel();
        
        // Create request body
        JsonObject requestBody = new JsonObject();
        JsonObject contents = new JsonObject();
        JsonObject parts = new JsonObject();
        parts.addProperty("text", prompt);
        contents.add("parts", gson.toJsonTree(new JsonObject[]{gson.fromJson(parts, JsonObject.class)}));
        requestBody.add("contents", gson.toJsonTree(new JsonObject[]{gson.fromJson(contents, JsonObject.class)}));
        
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", plugin.getConfigManager().getTemperature());
        generationConfig.addProperty("maxOutputTokens", plugin.getConfigManager().getMaxTokens());
        requestBody.add("generationConfig", generationConfig);

        RequestBody body = RequestBody.create(
            gson.toJson(requestBody),
            MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();        if (plugin.getConfigManager().shouldLogAIRequests()) {
            plugin.getLogger().info("Sending AI request: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");
        }
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                throw new IOException("API request failed with code " + response.code() + ": " + errorBody);
            }
            
            String responseBody = response.body().string();
            
            if (plugin.getConfigManager().shouldLogAIRequests()) {
                plugin.getLogger().info("AI response received");
            }
            
            // Parse the response to extract the generated text
            JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
            return responseJson.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
        } catch (java.net.SocketTimeoutException e) {
            throw new IOException("Request timed out - try again or reduce structure complexity", e);
        } catch (java.io.IOException e) {
            if (e.getMessage().contains("timeout")) {
                throw new IOException("Connection timeout - check your internet connection", e);
            }            throw e;
        }
    }

    /**
     * Parse AI response into StructureData
     */
    private StructureData parseResponse(String response) {
        try {
            // Clean up response (remove markdown formatting if present)
            String jsonResponse = response.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            jsonResponse = jsonResponse.trim();

            // Check if JSON is complete
            if (!isValidJson(jsonResponse)) {
                throw new RuntimeException("Incomplete or malformed JSON response");
            }

            // Parse JSON
            StructureData structureData = gson.fromJson(jsonResponse, StructureData.class);
            
            // Validate the parsed data
            if (structureData == null) {
                throw new RuntimeException("Failed to parse structure data");
            }
            if (structureData.getBlocks() == null || structureData.getBlocks().isEmpty()) {
                throw new RuntimeException("No blocks found in structure data");
            }
            
            plugin.getLogger().info("Successfully parsed structure with " + structureData.getBlocks().size() + " blocks");
            return structureData;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to parse AI response: " + e.getMessage());
            plugin.getLogger().severe("Response was: " + response.substring(0, Math.min(500, response.length())) + "...");
            throw new RuntimeException("Failed to parse AI response");
        }
    }

    /**
     * Check if JSON string is complete and valid
     */
    private boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        // Check basic JSON structure
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return false;
        }
        
        // Count braces to check if JSON is complete
        int openBraces = 0;
        int closeBraces = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : json.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') openBraces++;
                if (c == '}') closeBraces++;
            }
        }
        
        return openBraces == closeBraces && openBraces > 0;
    }
}
