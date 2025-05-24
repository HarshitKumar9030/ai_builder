# AI Structure Builder - Server Setup Script
# Run this script to set up your Minecraft server with the plugin

Write-Host "🎮 AI Structure Builder - Server Setup" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

# Create server directory
Write-Host "📁 Creating server directory..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path "server" | Out-Null
New-Item -ItemType Directory -Force -Path "server\plugins" | Out-Null

# Copy plugin JAR
Write-Host "📦 Copying plugin JAR..." -ForegroundColor Yellow
if (Test-Path "target\ai-structure-builder-1.0.0.jar") {
    Copy-Item "target\ai-structure-builder-1.0.0.jar" "server\plugins\"
    Write-Host "✅ Plugin copied successfully!" -ForegroundColor Green
} else {
    Write-Host "❌ Plugin JAR not found! Run 'mvn clean package' first." -ForegroundColor Red
    exit 1
}

# Create start script
Write-Host "🚀 Creating server start script..." -ForegroundColor Yellow
$startScript = @"
@echo off
echo Starting AI Structure Builder Server...
echo.
echo IMPORTANT: 
echo 1. Make sure you have downloaded spigot-1.20.4.jar or paper-1.20.4.jar
echo 2. Place it in this folder
echo 3. Set up your Gemini API key after first run
echo.
pause

REM Check for server jar
if exist "spigot-1.20.4.jar" (
    java -Xmx2G -Xms1G -jar spigot-1.20.4.jar nogui
) else if exist "paper-1.20.4.jar" (
    java -Xmx2G -Xms1G -jar paper-1.20.4.jar nogui
) else (
    echo ERROR: No server JAR found!
    echo Please download spigot-1.20.4.jar or paper-1.20.4.jar
    echo From: https://getbukkit.org/download/spigot
    echo Or: https://papermc.io/downloads/paper
    pause
)
"@

$startScript | Out-File -FilePath "server\start-server.bat" -Encoding ASCII

Write-Host "✅ Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host "1. Download Spigot 1.20.4: https://getbukkit.org/download/spigot" -ForegroundColor White
Write-Host "   OR Paper 1.20.4: https://papermc.io/downloads/paper" -ForegroundColor White
Write-Host "2. Place the downloaded JAR in the 'server' folder" -ForegroundColor White
Write-Host "3. Run 'server\start-server.bat'" -ForegroundColor White
Write-Host "4. Accept EULA (edit eula.txt)" -ForegroundColor White
Write-Host "5. Get your Gemini API key: https://makersuite.google.com/app/apikey" -ForegroundColor White
Write-Host "6. Configure it with: /aiconfig set gemini.api-key YOUR_KEY" -ForegroundColor White
Write-Host ""
Write-Host "🎯 Test your plugin with: /aibuild create a small house" -ForegroundColor Yellow
Write-Host ""
