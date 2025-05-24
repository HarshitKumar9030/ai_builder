# Development Guide

## Setting Up the Development Environment

### Prerequisites
1. **Java 17+**: Download from [Adoptium](https://adoptium.net/)
2. **Maven 3.6+**: Download from [Apache Maven](https://maven.apache.org/download.cgi)
3. **VS Code**: With Java extensions (already configured)

### Quick Setup
1. Run `setup.bat` (Windows) or follow manual steps below
2. Ensure Java and Maven are in your PATH
3. Open the project in VS Code

### Manual Setup
```bash
# Compile the project
mvn clean compile

# Build the plugin JAR
mvn clean package

# The plugin will be in target/ai-structure-builder-1.0.0.jar
```

## Project Structure
```
ai_builder/
├── src/
│   ├── main/
│   │   ├── java/com/aibuilder/
│   │   │   ├── AIStructureBuilder.java      # Main plugin class
│   │   │   ├── commands/                    # Command handlers
│   │   │   ├── manager/                     # Core managers
│   │   │   ├── model/                       # Data models
│   │   │   └── util/                        # Utility classes
│   │   └── resources/
│   │       ├── plugin.yml                   # Plugin configuration
│   │       └── config.yml                   # Default config
│   └── test/                                # Unit tests
├── pom.xml                                  # Maven configuration
└── README.md                                # Project documentation
```

## Key Components

### Managers
- **ConfigManager**: Handles plugin configuration
- **AIManager**: Manages Gemini API interactions
- **BuildManager**: Handles structure building

### Commands
- **AIBuildCommand**: `/aibuild` - Build structures with AI
- **AIConfigCommand**: `/aiconfig` - Configure the plugin
- **AIHelpCommand**: `/aihelp` - Show help information

### Models
- **StructureData**: Represents AI-generated structure data
- **BuildInstruction**: Individual block placement instruction

## Development Workflow

### Making Changes
1. Edit the source files in `src/main/java/`
2. Test with `mvn test`
3. Build with `mvn package`
4. Test on a Spigot server

### Adding New Features
1. Create new classes in appropriate packages
2. Update commands/managers as needed
3. Add configuration options if required
4. Write unit tests
5. Update documentation

### Testing
```bash
# Run unit tests
mvn test

# Run specific test
mvn test -Dtest=StructureDataTest

# Build and test
mvn clean package
```

## Configuration

### API Setup
1. Get Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Set in config: `/aiconfig set gemini.api-key YOUR_KEY`

### Performance Tuning
- `blocks-per-tick`: Higher = faster building but more lag
- `build-delay`: Lower = faster but more resource intensive
- `max-structure-size`: Safety limit for structure size

## Troubleshooting

### Common Issues
1. **Maven not found**: Install Maven and add to PATH
2. **Java version**: Ensure Java 17+ is installed
3. **API errors**: Check API key and internet connection
4. **Build failures**: Check dependencies and Java version

### Debug Mode
Enable debug logging in config:
```yaml
logging:
  debug: true
  log-ai-requests: true
```

## Contributing
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## VS Code Extensions

### Recommended Extensions
- **Extension Pack for Java** (already installed)
- **Maven for Java** (already installed)
- **XML** (installed)
- **YAML** (installed)
- **Lombok Annotations Support** (recommended)

### VS Code Settings
The project includes pre-configured tasks for building and running.
