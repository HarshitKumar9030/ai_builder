# Maven Installation Guide for Windows

## Method 1: Download and Install Manually (Recommended)

### Step 1: Download Maven
1. Go to [Apache Maven Download Page](https://maven.apache.org/download.cgi)
2. Download the **Binary zip archive** (e.g., `apache-maven-3.9.6-bin.zip`)
3. Save it to your Downloads folder

### Step 2: Extract Maven
1. Create a directory: `C:\Program Files\Apache\Maven`
2. Extract the downloaded zip file to this location
3. You should have: `C:\Program Files\Apache\Maven\apache-maven-3.9.6\`

### Step 3: Set Environment Variables
1. **Open System Properties:**
   - Press `Win + X` and select "System"
   - Click "Advanced system settings"
   - Click "Environment Variables"

2. **Set MAVEN_HOME:**
   - In "System variables", click "New"
   - Variable name: `MAVEN_HOME`
   - Variable value: `C:\Program Files\Apache\Maven\apache-maven-3.9.6`
   - Click "OK"

3. **Update PATH:**
   - In "System variables", find and select "Path"
   - Click "Edit"
   - Click "New"
   - Add: `%MAVEN_HOME%\bin`
   - Click "OK" on all dialogs

### Step 4: Verify Installation
1. **Open a NEW Command Prompt or PowerShell**
2. Run: `mvn -version`
3. You should see output like:
   ```
   Apache Maven 3.9.6
   Maven home: C:\Program Files\Apache\Maven\apache-maven-3.9.6
   Java version: 17.0.x
   ```

## Method 2: Using Chocolatey Package Manager

### Step 1: Install Chocolatey (if not already installed)
1. Open PowerShell as Administrator
2. Run:
   ```powershell
   Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
   ```

### Step 2: Install Maven
```powershell
choco install maven
```

### Step 3: Verify
```powershell
mvn -version
```

## Method 3: Using Scoop Package Manager

### Step 1: Install Scoop (if not already installed)
```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex
```

### Step 2: Install Maven
```powershell
scoop install maven
```

## Troubleshooting

### Issue: "mvn is not recognized"
**Solutions:**
1. **Restart your terminal/IDE** after installation
2. **Check PATH variable** includes `%MAVEN_HOME%\bin`
3. **Verify MAVEN_HOME** points to correct directory
4. **Use full path temporarily**: `"C:\Program Files\Apache\Maven\apache-maven-3.9.6\bin\mvn" -version`

### Issue: "JAVA_HOME not set"
**Solution:**
1. Set JAVA_HOME environment variable:
   - Variable name: `JAVA_HOME`
   - Variable value: Path to your JDK (e.g., `C:\Program Files\Eclipse Adoptium\jdk-17.0.x`)
2. Add to PATH: `%JAVA_HOME%\bin`

### Issue: Permission Denied
**Solution:**
1. Run PowerShell/Command Prompt as Administrator
2. Or install Maven in your user directory instead of Program Files

## Quick Test Commands

After installation, test Maven with these commands:

```bash
# Check Maven version
mvn -version

# Check if Maven can download dependencies (in your project folder)
mvn clean compile

# Build your project
mvn clean package

# Run tests
mvn test
```

## IDE Integration

### VS Code
1. Install "Extension Pack for Java" (includes Maven support)
2. Reload VS Code after Maven installation
3. Open Command Palette (`Ctrl+Shift+P`)
4. Run: "Java: Reload Projects"

### IntelliJ IDEA
1. File → Settings → Build Tools → Maven
2. Set Maven home directory to your installation path
3. Apply and restart IDE

## Next Steps for Your Project

Once Maven is installed:

1. **Navigate to your project:**
   ```bash
   cd "C:\Users\Harshit\Desktop\ai_builder"
   ```

2. **Compile the project:**
   ```bash
   mvn clean compile
   ```

3. **Build the plugin JAR:**
   ```bash
   mvn clean package
   ```

4. **The plugin will be created at:**
   ```
   target\ai-structure-builder-1.0.0.jar
   ```

## Common Maven Commands for Your Project

```bash
# Clean and compile
mvn clean compile

# Build plugin JAR with dependencies
mvn clean package

# Run unit tests
mvn test

# Install dependencies only
mvn dependency:resolve

# Clean build artifacts
mvn clean

# Skip tests during build
mvn clean package -DskipTests
```

## Environment Variable Summary

After complete installation, you should have:

```
JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.x.x
MAVEN_HOME = C:\Program Files\Apache\Maven\apache-maven-3.9.6
PATH = %JAVA_HOME%\bin;%MAVEN_HOME%\bin;... (other paths)
```

**Important:** Always restart your terminal/VS Code after setting environment variables!
