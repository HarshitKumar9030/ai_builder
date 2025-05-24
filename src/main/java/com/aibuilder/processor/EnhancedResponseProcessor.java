package com.aibuilder.processor;

import com.aibuilder.AIStructureBuilder;
import com.aibuilder.model.StructureData;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced AI response processor with robust JSON handling and fallback strategies
 */
@RequiredArgsConstructor
public class EnhancedResponseProcessor {
      private final AIStructureBuilder plugin;
    private final Gson gson = new Gson();
    private final JsonFactory jsonFactory = new JsonFactory();    /**
     * Process AI response with multiple parsing strategies
     */
    public StructureData processResponse(String response, String originalPrompt) {
        plugin.getLogger().info("Processing AI response (" + response.length() + " characters)");
        
        // Check if response looks truncated
        if (isTruncatedResponse(response)) {
            plugin.getLogger().warning("Response appears to be truncated, will attempt repair");
        }
        
        // Strategy 1: Direct JSON parsing
        try {
            StructureData result = parseDirectJson(response);
            if (result != null && isValidStructure(result)) {
                plugin.getLogger().info("Successfully parsed using direct JSON strategy");
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Direct JSON parsing failed: " + e.getMessage());
        }

        // Strategy 2: Extract JSON from markdown/text
        try {
            StructureData result = parseExtractedJson(response);
            if (result != null && isValidStructure(result)) {
                plugin.getLogger().info("Successfully parsed using JSON extraction strategy");
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("JSON extraction parsing failed: " + e.getMessage());
        }
        
        // Strategy 3: Streaming JSON parser for large responses
        try {
            StructureData result = parseStreamingJson(response);
            if (result != null && isValidStructure(result)) {
                plugin.getLogger().info("Successfully parsed using streaming JSON strategy");
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Streaming JSON parsing failed: " + e.getMessage());
        }
        
        // Strategy 4: Repair and retry
        try {
            StructureData result = parseRepairedJson(response);
            if (result != null && isValidStructure(result)) {
                plugin.getLogger().info("Successfully parsed using JSON repair strategy");
                return result;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("JSON repair parsing failed: " + e.getMessage());
        }
        
        // Strategy 5: Generate fallback structure
        plugin.getLogger().warning("All parsing strategies failed, generating fallback structure");
        return generateFallbackStructure(originalPrompt);
    }
    
    /**
     * Check if the response appears to be truncated
     */
    private boolean isTruncatedResponse(String response) {
        if (response == null || response.length() < 100) {
            return true;
        }
        
        String cleaned = cleanResponse(response);
        
        // Check for common signs of truncation
        return cleaned.endsWith(",") || // Ends with comma
               cleaned.matches(".*\"[^\"]*$") || // Ends with unterminated string
               (cleaned.contains("blocks") && cleaned.contains("[") && !cleaned.contains("]")) || // Blocks array not closed
               countChar(cleaned, '{') > countChar(cleaned, '}') || // Unmatched braces
               countChar(cleaned, '[') > countChar(cleaned, ']'); // Unmatched brackets
    }

    /**
     * Parse JSON directly
     */
    private StructureData parseDirectJson(String response) {
        String cleaned = cleanResponse(response);
        return gson.fromJson(cleaned, StructureData.class);
    }

    /**
     * Extract and parse JSON from markdown or mixed content
     */
    private StructureData parseExtractedJson(String response) {
        String jsonContent = extractJsonFromResponse(response);
        if (jsonContent != null) {
            return gson.fromJson(jsonContent, StructureData.class);
        }
        return null;
    }    /**
     * Use streaming JSON parser for large responses
     */
    private StructureData parseStreamingJson(String response) throws Exception {
        String cleaned = cleanResponse(response);
        JsonParser parser = jsonFactory.createParser(cleaned);
        
        String name = null;
        String description = null;
        StructureData.Size size = null;
        List<StructureData.Block> blocks = new ArrayList<>();
        
        try {
            while (parser.nextToken() != null && parser.getCurrentToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.getCurrentName();
                
                if ("name".equals(fieldName)) {
                    parser.nextToken();
                    name = parser.getValueAsString();
                } else if ("description".equals(fieldName)) {
                    parser.nextToken();
                    description = parser.getValueAsString();
                } else if ("size".equals(fieldName)) {
                    parser.nextToken();
                    size = parseSize(parser);
                } else if ("blocks".equals(fieldName)) {
                    parser.nextToken();
                    blocks = parseBlocksArraySafely(parser);
                }
            }
        } catch (Exception e) {
            // If parsing fails partway through, we might still have some useful data
            plugin.getLogger().warning("Streaming parser encountered error: " + e.getMessage() + 
                                     ", attempting to use partial data");
        } finally {
            parser.close();
        }
        
        // Even if parsing failed, try to create a structure with what we have
        if (name != null && blocks != null && !blocks.isEmpty()) {
            StructureData result = new StructureData();
            result.setName(name);
            result.setDescription(description != null ? description : "AI Generated Structure");
            result.setSize(size != null ? size : calculateSize(blocks));
            result.setBlocks(blocks);
            
            plugin.getLogger().info("Streaming parser recovered " + blocks.size() + " blocks");
            return result;
        }
        
        return null;
    }
    
    /**
     * Parse blocks array with error recovery
     */
    private List<StructureData.Block> parseBlocksArraySafely(JsonParser parser) throws Exception {
        List<StructureData.Block> blocks = new ArrayList<>();
        
        try {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                    try {
                        StructureData.Block block = parseBlockObject(parser);
                        if (block != null && block.getMaterial() != null) {
                            blocks.add(block);
                        }
                    } catch (Exception e) {
                        // Skip this block and continue with the next one
                        plugin.getLogger().fine("Skipped malformed block: " + e.getMessage());
                        skipToNextObject(parser);
                    }
                }
            }
        } catch (Exception e) {
            // Array might be truncated, return what we have
            plugin.getLogger().warning("Blocks array parsing incomplete: " + e.getMessage() + 
                                     ", recovered " + blocks.size() + " blocks");
        }
        
        return blocks;
    }
    
    /**
     * Parse a single block object
     */
    private StructureData.Block parseBlockObject(JsonParser parser) throws Exception {
        StructureData.Block block = new StructureData.Block();
        
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken();
            
            switch (fieldName) {
                case "x" -> block.setX(parser.getValueAsInt());
                case "y" -> block.setY(parser.getValueAsInt());
                case "z" -> block.setZ(parser.getValueAsInt());
                case "material" -> block.setMaterial(parser.getValueAsString());
                case "data" -> block.setData(parser.getValueAsString());
            }
        }
        
        return block;
    }
    
    /**
     * Skip to the next object when parsing fails
     */
    private void skipToNextObject(JsonParser parser) throws Exception {
        int depth = 0;
        do {
            JsonToken token = parser.nextToken();
            if (token == JsonToken.START_OBJECT) {
                depth++;
            } else if (token == JsonToken.END_OBJECT) {
                depth--;
            }
        } while (depth > 0 && parser.getCurrentToken() != null);
    }

    /**
     * Attempt to repair malformed JSON
     */
    private StructureData parseRepairedJson(String response) {
        String cleaned = cleanResponse(response);
        String repaired = repairJson(cleaned);
        
        if (!repaired.equals(cleaned)) {
            plugin.getLogger().info("Attempting to parse repaired JSON");
            return gson.fromJson(repaired, StructureData.class);
        }
        
        return null;
    }

    /**
     * Generate a fallback structure when parsing fails
     */
    private StructureData generateFallbackStructure(String originalPrompt) {
        plugin.getLogger().info("Generating algorithmic fallback structure for: " + originalPrompt);
        
        // Determine structure type and generate appropriate fallback
        String lowerPrompt = originalPrompt.toLowerCase();
        
        if (lowerPrompt.contains("castle")) {
            return generateCastleFallback();
        } else if (lowerPrompt.contains("house")) {
            return generateHouseFallback();
        } else if (lowerPrompt.contains("tower")) {
            return generateTowerFallback();
        } else if (lowerPrompt.contains("bridge")) {
            return generateBridgeFallback();
        } else {
            return generateGenericFallback();
        }
    }

    /**
     * Clean response from markdown and other formatting
     */
    private String cleanResponse(String response) {
        if (response == null) return "{}";
        
        String cleaned = response.trim();
        
        // Remove markdown code blocks
        cleaned = cleaned.replaceAll("```json\\s*", "");
        cleaned = cleaned.replaceAll("```\\s*$", "");
        
        // Remove any text before the first {
        int firstBrace = cleaned.indexOf('{');
        if (firstBrace > 0) {
            cleaned = cleaned.substring(firstBrace);
        }
        
        // Remove any text after the last }
        int lastBrace = cleaned.lastIndexOf('}');
        if (lastBrace >= 0 && lastBrace < cleaned.length() - 1) {
            cleaned = cleaned.substring(0, lastBrace + 1);
        }
        
        return cleaned;
    }

    /**
     * Extract JSON content using regex patterns
     */
    private String extractJsonFromResponse(String response) {
        // Pattern to match JSON object
        Pattern jsonPattern = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(response);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }    /**
     * Repair common JSON issues
     */
    private String repairJson(String json) {
        if (json == null) return "{}";
        
        String original = json;
        
        // Remove any incomplete elements at the end that might cause parsing issues
        json = removeIncompleteElements(json);
        
        // Fix trailing commas
        json = json.replaceAll(",\\s*([}\\]])", "$1");
        
        // Fix unescaped quotes in strings
        json = json.replaceAll("(?<!\\\\)\"([^\"]*?)(?<!\\\\)\"([^\":,}\\]]*?)\"", "\"$1\\\\\"$2\"");
        
        // Fix unterminated strings by adding closing quote if needed
        if (json.contains("\"") && !isBalancedQuotes(json)) {
            // Find the last unmatched quote and close it
            int lastQuote = json.lastIndexOf('"');
            if (lastQuote > 0) {
                String beforeQuote = json.substring(0, lastQuote);
                if (!isBalancedQuotes(beforeQuote)) {
                    json = json + "\"";
                }
            }
        }
        
        // Ensure proper closure of arrays and objects
        while (countChar(json, '{') > countChar(json, '}')) {
            json += "}";
        }
        
        while (countChar(json, '[') > countChar(json, ']')) {
            json += "]";
        }
        
        // If the repair significantly changed the JSON, log it
        if (json.length() != original.length()) {
            plugin.getLogger().info("Repaired JSON: removed " + (original.length() - json.length()) + " characters");
        }
        
        return json;
    }
    
    /**
     * Remove incomplete JSON elements that could cause parsing failures
     */
    private String removeIncompleteElements(String json) {
        // Look for common patterns of incomplete elements
        
        // Remove incomplete objects that don't have closing braces
        json = json.replaceAll(",\\s*\\{[^}]*$", "");
        
        // Remove incomplete array elements
        json = json.replaceAll(",\\s*\\[[^\\]]*$", "");
        
        // Remove incomplete strings at the end
        json = json.replaceAll(",\\s*\"[^\"]*$", "");
        
        // Remove incomplete property assignments
        json = json.replaceAll(",\\s*\"[^\"]*\"\\s*:\\s*[^,}\\]]*$", "");
        
        // Handle blocks array specifically - if it's incomplete, close it properly
        if (json.contains("\"blocks\"") && json.contains("[")) {
            int blocksStart = json.indexOf("\"blocks\"");
            int arrayStart = json.indexOf("[", blocksStart);
            if (arrayStart > 0) {
                // Count braces and brackets from the blocks array start
                String fromArray = json.substring(arrayStart);
                int openBrackets = countChar(fromArray, '[');
                int closeBrackets = countChar(fromArray, ']');
                int openBraces = countChar(fromArray, '{');
                int closeBraces = countChar(fromArray, '}');
                
                // If we have unmatched elements, try to find a safe truncation point
                if (openBrackets > closeBrackets || openBraces > closeBraces) {
                    // Find the last complete block object
                    int lastCompleteBlock = findLastCompleteBlockIndex(json, arrayStart);
                    if (lastCompleteBlock > arrayStart) {
                        json = json.substring(0, lastCompleteBlock) + "]";
                        // Add closing brace for the main object if needed
                        if (countChar(json, '{') > countChar(json, '}')) {
                            json += "}";
                        }
                    }
                }
            }
        }
        
        return json;
    }
    
    /**
     * Find the index of the last complete block object in the blocks array
     */
    private int findLastCompleteBlockIndex(String json, int arrayStart) {
        int lastGoodIndex = arrayStart + 1; // Start after the opening bracket
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = arrayStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            
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
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // We found a complete block object
                        lastGoodIndex = i + 1;
                    }
                }
            }
        }
        
        return lastGoodIndex;
    }// Helper methods for parsing and generation
    private StructureData.Size parseSize(JsonParser parser) throws Exception {
        StructureData.Size size = new StructureData.Size();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken();
            
            switch (fieldName) {
                case "width" -> size.setWidth(parser.getValueAsInt());
                case "height" -> size.setHeight(parser.getValueAsInt());
                case "depth" -> size.setDepth(parser.getValueAsInt());
            }
        }
        return size;    }

    private boolean isValidStructure(StructureData structure) {
        return structure != null && 
               structure.getBlocks() != null && 
               !structure.getBlocks().isEmpty() &&
               structure.getBlocks().size() > 10; // Minimum reasonable size
    }

    private boolean isBalancedQuotes(String str) {
        int count = 0;
        boolean escaped = false;
        for (char c : str.toCharArray()) {
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            if (c == '"' && !escaped) {
                count++;
            }
            escaped = false;
        }
        return count % 2 == 0;
    }

    private int countChar(String str, char c) {
        return (int) str.chars().filter(ch -> ch == c).count();
    }    private StructureData.Size calculateSize(List<StructureData.Block> blocks) {
        if (blocks.isEmpty()) {
            StructureData.Size size = new StructureData.Size();
            size.setWidth(1);
            size.setHeight(1);
            size.setDepth(1);
            return size;
        }
        
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (StructureData.Block block : blocks) {
            minX = Math.min(minX, block.getX());
            maxX = Math.max(maxX, block.getX());
            minY = Math.min(minY, block.getY());
            maxY = Math.max(maxY, block.getY());
            minZ = Math.min(minZ, block.getZ());
            maxZ = Math.max(maxZ, block.getZ());
        }
        
        StructureData.Size size = new StructureData.Size();
        size.setWidth(maxX - minX + 1);
        size.setHeight(maxY - minY + 1);
        size.setDepth(maxZ - minZ + 1);
        return size;
    }    // Fallback structure generators
    private StructureData generateCastleFallback() {
        StructureData castle = new StructureData();
        castle.setName("Fallback Castle");
        castle.setDescription("Algorithmically generated castle structure");
        
        StructureData.Size size = new StructureData.Size();
        size.setWidth(25);
        size.setHeight(15);
        size.setDepth(25);
        castle.setSize(size);
        
        List<StructureData.Block> blocks = new ArrayList<>();
        
        // Generate castle walls, towers, and courtyard
        generateCastleWalls(blocks, 25, 15, 25);
        generateCastleTowers(blocks, 25, 15, 25);
        generateCastleInterior(blocks, 25, 15, 25);
        
        castle.setBlocks(blocks);
        return castle;
    }

    private StructureData generateHouseFallback() {
        StructureData house = new StructureData();
        house.setName("Fallback House");
        house.setDescription("Algorithmically generated house structure");
        
        StructureData.Size size = new StructureData.Size();
        size.setWidth(12);
        size.setHeight(8);
        size.setDepth(12);
        house.setSize(size);
        
        List<StructureData.Block> blocks = new ArrayList<>();
        generateHouseStructure(blocks, 12, 8, 12);
        
        house.setBlocks(blocks);
        return house;
    }

    private StructureData generateTowerFallback() {
        StructureData tower = new StructureData();
        tower.setName("Fallback Tower");
        tower.setDescription("Algorithmically generated tower structure");
        
        StructureData.Size size = new StructureData.Size();
        size.setWidth(7);
        size.setHeight(20);
        size.setDepth(7);
        tower.setSize(size);
        
        List<StructureData.Block> blocks = new ArrayList<>();
        generateTowerStructure(blocks, 7, 20, 7);
        
        tower.setBlocks(blocks);
        return tower;
    }

    private StructureData generateBridgeFallback() {
        StructureData bridge = new StructureData();
        bridge.setName("Fallback Bridge");
        bridge.setDescription("Algorithmically generated bridge structure");
        
        StructureData.Size size = new StructureData.Size();
        size.setWidth(20);
        size.setHeight(8);
        size.setDepth(5);
        bridge.setSize(size);
        
        List<StructureData.Block> blocks = new ArrayList<>();
        generateBridgeStructure(blocks, 20, 8, 5);
        
        bridge.setBlocks(blocks);
        return bridge;
    }

    private StructureData generateGenericFallback() {
        StructureData structure = new StructureData();
        structure.setName("Fallback Structure");
        structure.setDescription("Algorithmically generated structure");
        
        StructureData.Size size = new StructureData.Size();
        size.setWidth(15);
        size.setHeight(10);
        size.setDepth(15);
        structure.setSize(size);
        
        List<StructureData.Block> blocks = new ArrayList<>();
        generateGenericStructure(blocks, 15, 10, 15);
        
        structure.setBlocks(blocks);
        return structure;
    }    // Structure generation methods (implementations would be quite long, so showing key patterns)
    private void generateCastleWalls(List<StructureData.Block> blocks, int width, int height, int depth) {
        // Generate outer walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Front and back walls
                if (y == 0 || y > 2) { // Leave space for gates
                    blocks.add(new StructureData.Block(x, y, 0, "STONE_BRICKS", ""));
                    blocks.add(new StructureData.Block(x, y, depth-1, "STONE_BRICKS", ""));
                }
                
                // Side walls
                if (x == 0 || x == width-1) {
                    blocks.add(new StructureData.Block(x, y, 0, "STONE_BRICKS", ""));
                    for (int z = 1; z < depth-1; z++) {
                        blocks.add(new StructureData.Block(x, y, z, "STONE_BRICKS", ""));
                    }
                }
            }
        }
    }

    private void generateCastleTowers(List<StructureData.Block> blocks, int width, int height, int depth) {
        // Corner towers
        int towerHeight = height + 5;
        int[][] towers = {{0, 0}, {width-4, 0}, {0, depth-4}, {width-4, depth-4}};
        
        for (int[] tower : towers) {
            for (int x = tower[0]; x < tower[0] + 4; x++) {
                for (int z = tower[1]; z < tower[1] + 4; z++) {
                    for (int y = 0; y < towerHeight; y++) {
                        if (x == tower[0] || x == tower[0] + 3 || z == tower[1] || z == tower[1] + 3) {
                            blocks.add(new StructureData.Block(x, y, z, "STONE_BRICKS", ""));
                        }
                    }
                }
            }
        }
    }

    private void generateCastleInterior(List<StructureData.Block> blocks, int width, int height, int depth) {
        // Add floors, rooms, and details
        for (int x = 2; x < width-2; x++) {
            for (int z = 2; z < depth-2; z++) {
                blocks.add(new StructureData.Block(x, 0, z, "STONE", ""));
                if ((x + z) % 8 == 0 && x < width-4 && z < depth-4) {
                    // Add some pillars
                    for (int y = 1; y < height-1; y++) {
                        blocks.add(new StructureData.Block(x, y, z, "QUARTZ_BLOCK", ""));
                    }
                }
            }
        }
    }

    private void generateHouseStructure(List<StructureData.Block> blocks, int width, int height, int depth) {
        // Foundation
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new StructureData.Block(x, 0, z, "STONE", ""));
            }
        }
        
        // Walls
        for (int x = 0; x < width; x++) {
            for (int y = 1; y < height-1; y++) {
                blocks.add(new StructureData.Block(x, y, 0, "OAK_PLANKS", ""));
                blocks.add(new StructureData.Block(x, y, depth-1, "OAK_PLANKS", ""));
            }
        }
        
        for (int z = 0; z < depth; z++) {
            for (int y = 1; y < height-1; y++) {
                blocks.add(new StructureData.Block(0, y, z, "OAK_PLANKS", ""));
                blocks.add(new StructureData.Block(width-1, y, z, "OAK_PLANKS", ""));
            }
        }
        
        // Roof
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new StructureData.Block(x, height-1, z, "OAK_SLAB", ""));
            }
        }
        
        // Windows
        blocks.add(new StructureData.Block(2, 2, 0, "GLASS", ""));
        blocks.add(new StructureData.Block(width-3, 2, 0, "GLASS", ""));
    }

    private void generateTowerStructure(List<StructureData.Block> blocks, int width, int height, int depth) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    if (x == 0 || x == width-1 || z == 0 || z == depth-1) {
                        blocks.add(new StructureData.Block(x, y, z, y > height * 0.8 ? "STONE_BRICKS" : "STONE", ""));
                    }
                }
            }
        }
    }

    private void generateBridgeStructure(List<StructureData.Block> blocks, int width, int height, int depth) {
        // Bridge deck
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                blocks.add(new StructureData.Block(x, height/2, z, "STONE", ""));
                
                // Railings
                if (z == 0 || z == depth-1) {
                    blocks.add(new StructureData.Block(x, height/2 + 1, z, "STONE_SLAB", ""));
                }
            }
        }
        
        // Support pillars
        for (int x = 3; x < width; x += 6) {
            for (int y = 0; y <= height/2; y++) {
                blocks.add(new StructureData.Block(x, y, depth/2, "STONE_BRICKS", ""));
            }
        }
    }

    private void generateGenericStructure(List<StructureData.Block> blocks, int width, int height, int depth) {
        // Create a pyramid-like structure
        for (int y = 0; y < height; y++) {
            int offset = y / 2;
            for (int x = offset; x < width - offset; x++) {
                for (int z = offset; z < depth - offset; z++) {
                    if (x == offset || x == width - offset - 1 || z == offset || z == depth - offset - 1) {
                        String material = y < height * 0.3 ? "STONE" : 
                                       y < height * 0.6 ? "STONE_BRICKS" : "QUARTZ_BLOCK";
                        blocks.add(new StructureData.Block(x, y, z, material, ""));
                    }
                }
            }
        }
    }
}
