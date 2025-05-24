# How to Run Your AI Structure Builder Plugin

## Prerequisites
✅ **Maven is installed** - You have successfully completed this step!
✅ **Plugin JAR is built** - Located at `target/ai-structure-builder-1.0.0.jar`

## Step 1: Download Minecraft Server

Since automated download didn't work, please manually download the server:

1. Go to: https://getbukkit.org/download/spigot
2. Download **Spigot 1.20.4** (the version your plugin is built for)
3. Save it as `spigot-1.20.4.jar` in the `server` folder

**Alternative**: Use Paper (recommended) - https://papermc.io/downloads/paper
- Download Paper 1.20.4 and save as `paper-1.20.4.jar`

## Step 2: Set Up Server Directory

Your server directory should look like this:
```
server/
├── spigot-1.20.4.jar (or paper-1.20.4.jar)
├── plugins/
│   └── ai-structure-builder-1.0.0.jar
├── eula.txt
└── server.properties
```

## Step 3: Install Your Plugin

Copy your plugin JAR to the server plugins folder:
```powershell
# Copy the plugin to server/plugins directory
Copy-Item "target/ai-structure-builder-1.0.0.jar" "server/plugins/"
```

## Step 4: Get Your Google Gemini API Key

**IMPORTANT**: You must configure your Google Gemini API key before the plugin will work:

1. **Get API Key**: Go to https://makersuite.google.com/app/apikey
2. **Create a new API key** and copy it

## Step 5: Start the Server (First Time)

1. Open PowerShell in the `server` directory
2. Accept the EULA first:
```powershell
# Start server once to generate eula.txt
java -jar spigot-1.20.4.jar nogui
```
3. **Stop the server** (type `stop` in console)
4. Edit `eula.txt` and change `eula=false` to `eula=true`

## Step 6: Configure the Plugin

After the first run, the plugin will create its config file. Configure your API key:

**Option 1: Edit config file directly**
Edit `server/plugins/AIStructureBuilder/config.yml`:
```yaml
gemini:
  api-key: "YOUR_ACTUAL_API_KEY_HERE"
  model: "gemini-2.0-flash-exp"
```

**Option 2: Use in-game command**
1. Start the server: `java -Xmx2G -Xms1G -jar spigot-1.20.4.jar nogui`
2. Join the server in Minecraft
3. Use: `/aiconfig set gemini.api-key YOUR_ACTUAL_API_KEY_HERE`

## Step 7: Test Your Plugin

Once the server is running:

1. Connect to your server (localhost:25565)
2. Give yourself operator permissions: `/op YourUsername`
3. Test the plugin commands:
   - `/aibuild help` - Show help
   - `/aibuild create a small house` - Build a house
   - `/aibuild create a castle tower` - Build a tower
   - `/aireload` - Reload plugin config

## Available Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/aibuild <description>` | Build a structure from description | `aibuilder.build` |
| `/aireload` | Reload plugin configuration | `aibuilder.reload` |

## Troubleshooting

### Plugin not loading?
- Check `server/logs/latest.log` for errors
- Ensure you're using Java 17+
- Verify the JAR file is in `server/plugins/`

### API errors?
- Check your Gemini API key is correct
- Ensure you have API credits/quota available
- Check your internet connection

### Plugin won't start (NullPointerException)?
**FIXED IN LATEST VERSION** - If you get this error:
```
java.lang.NullPointerException: Cannot invoke "org.bukkit.configuration.file.FileConfiguration.getString(String, String)" because "this.config" is null
```

**Solution**: 
1. Make sure you're using the latest plugin JAR from `target/ai-structure-builder-1.0.0.jar`
2. Delete old plugin files from `server/plugins/`
3. Restart the server completely
4. The plugin will now create the config file properly

### Build permission errors?
- Make sure you have operator permissions
- Check the plugin permissions in your permissions plugin

## Development Mode

For development/testing:
1. Make changes to your code
2. Run `mvn clean package` to rebuild
3. Copy new JAR to `server/plugins/`
4. Restart server or use `/reload` (not recommended for production)

## Performance Tips

- Use Paper instead of Spigot for better performance
- Adjust `delay-between-blocks` in config for faster/slower building
- Set appropriate `max-size` to prevent lag from huge structures
- Monitor server TPS when building large structures

---

**Your plugin is ready to run! 🎉**

The AI Structure Builder will create amazing Minecraft structures based on your text descriptions using Google's Gemini AI.
