<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# AI Structure Builder - Copilot Instructions

This is a Minecraft Spigot plugin project that integrates with Google's Gemini 2.0 Flash API to build structures using AI.

## Project Overview
- **Language**: Java 17
- **Framework**: Spigot API 1.20.4
- **Build Tool**: Maven
- **AI Integration**: Google Gemini 2.0 Flash API
- **Architecture**: Manager-based pattern with separate concerns

## Key Components
1. **AIStructureBuilder**: Main plugin class
2. **ConfigManager**: Handles configuration and settings
3. **AIManager**: Manages Google Gemini API interactions
4. **BuildManager**: Handles structure building in Minecraft
5. **Commands**: Command handlers for player interactions

## Coding Guidelines
- Use Lombok annotations for reducing boilerplate code
- Follow Bukkit/Spigot best practices for event handling and scheduling
- Implement proper async handling for API calls to prevent server lag
- Use ChatColor for message formatting
- Implement proper error handling and logging
- Follow Maven project structure conventions

## API Integration
- The plugin uses HTTP requests to communicate with Google Gemini API
- API responses are parsed as JSON and converted to BuildInstruction objects
- All AI operations should be asynchronous to prevent blocking the main thread

## Minecraft-Specific Considerations
- All block placement should be done on the main thread
- Use Bukkit scheduler for timed operations
- Implement proper permission checking
- Follow Minecraft coordinate system (x, y, z)
- Use Material enum for block types

## Dependencies
- Spigot API (provided scope)
- OkHttp3 for HTTP requests
- Gson for JSON processing
- Lombok for code generation
- JUnit for testing

When suggesting code changes:
1. Maintain the existing architecture patterns
2. Ensure thread safety for async operations
3. Use appropriate Bukkit/Spigot APIs
4. Follow the established error handling patterns
5. Consider performance implications for Minecraft servers
