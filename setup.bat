@echo off
echo Setting up AI Structure Builder Development Environment...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    echo Download from: https://adoptium.net/
    pause
    exit /b 1
)

REM Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven
    echo Download from: https://maven.apache.org/download.cgi
    echo.
    echo Quick setup:
    echo 1. Download Maven binary zip
    echo 2. Extract to C:\Program Files\Apache\maven
    echo 3. Add C:\Program Files\Apache\maven\bin to your PATH
    echo 4. Restart command prompt and try again
    pause
    exit /b 1
)

echo Java and Maven are properly installed!
echo.

echo Compiling project...
call mvn clean compile
if %errorlevel% neq 0 (
    echo Build failed! Check the output above for errors.
    pause
    exit /b 1
)

echo.
echo Building plugin JAR...
call mvn package
if %errorlevel% neq 0 (
    echo Package failed! Check the output above for errors.
    pause
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS! Plugin built successfully!
echo.
echo Plugin JAR location: target\ai-structure-builder-1.0.0.jar
echo.
echo Next steps:
echo 1. Copy the JAR file to your Spigot server's plugins folder
echo 2. Start your server
echo 3. Get a Gemini API key from https://makersuite.google.com/app/apikey
echo 4. Set it with: /aiconfig set gemini.api-key YOUR_KEY
echo 5. Start building with: /aibuild your description
echo.
echo ========================================
pause
