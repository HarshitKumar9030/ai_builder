# AI Structure Builder

A powerful Minecraft Spigot plugin that uses Google's Gemini 2.0 Flash AI to build amazing structures from simple text descriptions.

## 🌟 Features

- **AI-Powered Building**: Describe what you want, and let AI build it for you
- **Google Gemini 2.0 Flash Integration**: Uses the latest and fastest AI model
- **Async Operations**: Non-blocking AI calls to prevent server lag
- **Configurable Safety**: Size limits and material restrictions
- **Permission System**: Fine-grained control over who can use features
- **Real-time Building**: Watch structures appear block by block
- **Material Safety**: Prevents dangerous blocks like TNT or lava

## 🚀 Quick Start

### 1. Installation

**Prerequisites:**
- Java 17 or higher
- Maven 3.6+ (for building from source)
- Spigot/Paper server 1.20.4+

**Option A: Build from Source**
1. Ensure Java 17+ and Maven are installed
2. Run the provided `setup.bat` script or:
   ```bash
   mvn clean package
   ```
3. Copy `target/ai-structure-builder-1.0.0.jar` to your server's `plugins/` folder
4. Get a Google Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)

### 2. Configuration

```bash
# Set your Gemini API key
/aiconfig set gemini.api-key YOUR_API_KEY_HERE

# Optional: Adjust AI creativity (0.0 = deterministic, 1.0 = very creative)
/aiconfig set gemini.temperature 0.7

# Optional: Set maximum structure size
/aiconfig set building.max-structure-size 100
```

### 3. Start Building

```bash
# Build a simple house
/aibuild a small wooden house with a red roof

# Build a castle
/aibuild a medieval stone castle with towers and walls

# Build a modern structure
/aibuild a modern glass skyscraper 30 blocks tall

# Build infrastructure
/aibuild a stone bridge spanning 20 blocks
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/aibuild <description>` | `aibuilder.build` | Build a structure using AI |
| `/aiconfig set <key> <value>` | `aibuilder.admin` | Set configuration values |
| `/aiconfig get <key>` | `aibuilder.admin` | Get configuration values |
| `/aihelp` | `aibuilder.help` | Show help information |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `aibuilder.*` | OP | All permissions |
| `aibuilder.build` | true | Build structures with AI |
| `aibuilder.admin` | OP | Configure the plugin |
| `aibuilder.help` | true | View help |

## Configuration

The plugin creates a `config.yml` file with the following key settings:

### Gemini AI Settings
```yaml
gemini:
  api-key: "YOUR_GEMINI_API_KEY_HERE"
  model: "gemini-2.0-flash-exp"
  max-tokens: 4000
  temperature: 0.7
```

### Building Settings
```yaml
building:
  max-structure-size: 100
  require-confirmation: true
  confirmation-threshold: 50
```

### Performance Settings
```yaml
performance:
  blocks-per-tick: 10
  build-delay: 2
  async-building: true
```

## Examples

### Simple Structures
- `/aibuild a wooden cabin`
- `/aibuild a stone tower`
- `/aibuild a small garden`

### Complex Structures
- `/aibuild a medieval castle with moat and drawbridge`
- `/aibuild a modern city hall with glass facade`
- `/aibuild a fantasy wizard tower with spiral stairs`

### Infrastructure
- `/aibuild a railway bridge 50 blocks long`
- `/aibuild a harbor with docks and lighthouse`
- `/aibuild a mountain tunnel entrance`

## Building Tips

1. **Be Specific**: More detailed descriptions yield better results
2. **Set Context**: Mention the style, materials, and size you want
3. **Use Minecraft Terms**: Reference familiar blocks and structures
4. **Start Small**: Test with simple structures before complex ones

## API Integration

This plugin uses Google's Gemini 2.0 Flash API for structure generation. You need:

1. A Google account
2. Access to Google AI Studio
3. A free API key (with usage limits)

Get your API key at: https://makersuite.google.com/app/apikey

## Development

### Building from Source

```bash
# Clone the repository
git clone <repository-url>

# Build with Maven
mvn clean package

# The plugin jar will be in target/
```

### Dependencies

- Java 17+
- Spigot API 1.20.4
- Google Generative AI SDK
- OkHttp for HTTP requests
- Gson for JSON processing

## Support

- **Wiki**: Check the wiki for detailed guides
- **Issues**: Report bugs on the GitHub issues page
- **Discord**: Join our Discord for community support

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Changelog

### Version 1.0.0
- Initial release
- Basic AI structure generation
- Configuration system
- Command interface
- Performance optimizations
