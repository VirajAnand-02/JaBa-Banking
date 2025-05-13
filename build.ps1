# Build.ps1

# === Configurable Paths ===
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$srcDir       = Join-Path $projectRoot 'src'
$webContent   = Join-Path $projectRoot 'WebContent'
$buildDir     = Join-Path $projectRoot 'build'
$tomcatHome   = 'E:\programming\JaBa\apache-tomcat-11.0.6'
$appName      = 'JaBa-Banking' # Ensure this matches your desired deployment name

# --- Dependency Paths ---
$servletApi   = Join-Path $tomcatHome 'lib\servlet-api.jar'
$webInfLibSrc = Join-Path $webContent 'WEB-INF\lib' # Source Lib directory
$sqliteJdbcJar = Join-Path $webInfLibSrc 'sqlite-jdbc-3.49.1.0.jar' # Specific JAR needed for compilation
# $jsonLibJar = Join-Path $webInfLibSrc 'json-20231013.jar' # Specific JAR needed for compilation

# === 1. Clean previous build ===
if (Test-Path $buildDir) {
    Write-Host "Cleaning old build..."
    Remove-Item $buildDir -Recurse -Force
}
New-Item $buildDir -ItemType Directory | Out-Null

# === 2. Prepare WEB-INF structure ===
$webInfDir    = Join-Path $buildDir 'WEB-INF'
$classesDir   = Join-Path $webInfDir 'classes'
$webInfLibDest = Join-Path $webInfDir 'lib' # Destination Lib directory in build
New-Item $webInfDir    -ItemType Directory -Force | Out-Null
New-Item $classesDir   -ItemType Directory -Force | Out-Null
# Lib dir will be created later if needed during copy

# === 3. Compile Java sources ===
Write-Host "Compiling Java sources..."

# --- Build Classpath ---
# Start with Servlet API
$compileClasspath = $servletApi
# Add SQLite JDBC Driver if it exists
if (Test-Path $sqliteJdbcJar) {
    Write-Host "Adding SQLite JDBC driver to classpath: $sqliteJdbcJar"
    $compileClasspath += ";$sqliteJdbcJar" # Use semicolon for classpath separator on Windows
} else {
    Write-Warning "SQLite JDBC JAR not found at $sqliteJdbcJar. Compilation might fail if DB classes are used."
    # Decide if you want to 'throw' an error here or just warn
}
# Add other JARs from $webInfLibSrc to the classpath if needed:
Get-ChildItem -Path $webInfLibSrc -Filter *.jar | ForEach-Object { $compileClasspath += ";$($_.FullName)" }


# Get all Java files first
$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName

# Check if any java files were found
if ($javaFiles.Count -eq 0) {
    Write-Warning "No Java source files found in $srcDir"
} else {
    Write-Host "Using Classpath: $compileClasspath"
    # Compile all files in one go
    & javac `
        -d $classesDir `
        -cp $compileClasspath `
        $javaFiles # Pass the collection/array of full file paths

    if ($LASTEXITCODE -ne 0) {
        # The error message from javac should already be visible in the console
        throw "Java compilation failed. See messages above."
    }
    Write-Host "Compilation succeeded."
}

# === 4. Copy web.xml into WEB-INF ===
$sourceWebXml = Join-Path $webContent 'WEB-INF\web.xml'
if (-Not (Test-Path $sourceWebXml)) {
    throw "Cannot find web.xml at $sourceWebXml"
}
Write-Host "Copying web.xml..."
Copy-Item -Path $sourceWebXml -Destination $webInfDir

# === 5. Copy library JARs into WEB-INF/lib ===
if (Test-Path $webInfLibSrc) {
    Write-Host "Copying libraries from $webInfLibSrc to $webInfLibDest..."
    # Ensure destination lib directory exists
    if (-not (Test-Path $webInfLibDest)) {
        New-Item $webInfLibDest -ItemType Directory -Force | Out-Null
    }
    Copy-Item -Path (Join-Path $webInfLibSrc '*') -Destination $webInfLibDest -Recurse -Force
} else {
    Write-Host "No source library directory found at $webInfLibSrc, skipping library copy."
}

# === 6. Copy remaining web resources ===
Write-Host "Copying static WebContent (excluding WEB-INF)..."
Get-ChildItem -Path $webContent -Recurse | Where-Object {
    # exclude the WEB-INF folder itself and its contents entirely
    $_.FullName -notlike "$webContent\WEB-INF*"
} | ForEach-Object {
    $relativePath = $_.FullName.Substring($webContent.Length+1)
    $destPath     = Join-Path $buildDir $relativePath
    if ($_.PSIsContainer) {
        # Ensure parent directory exists before creating child directory
        $parentDir = Split-Path $destPath -Parent
        if (-not (Test-Path $parentDir)) {
           New-Item $parentDir -ItemType Directory -Force | Out-Null
        }
        New-Item $destPath -ItemType Directory -Force | Out-Null
    } else {
         # Ensure parent directory exists before copying file
        $parentDir = Split-Path $destPath -Parent
        if (-not (Test-Path $parentDir)) {
           New-Item $parentDir -ItemType Directory -Force | Out-Null
        }
        Copy-Item -Path $_.FullName -Destination $destPath -Force
    }
}

# === 7. Deploy to Tomcat ===
$deployDir = Join-Path $tomcatHome "webapps\$appName"
$warFile   = Join-Path $tomcatHome "webapps\$appName.war" # Tomcat might auto-create this from the dir

# 7a) Remove any existing exploded app or WAR
if (Test-Path $deployDir) {
    Write-Host "Removing existing deployment directory: $deployDir"
    Remove-Item $deployDir -Recurse -Force -ErrorAction Stop
}

if (Test-Path $warFile) {
    Write-Host "Removing existing WAR file: $warFile"
    Remove-Item $warFile -Force -ErrorAction Stop
}

# 7b) Recreate the empty deployment directory
Write-Host "Creating deployment directory: $deployDir"
New-Item -Path $deployDir -ItemType Directory -Force | Out-Null

# 7c) Copy everything from build into webapps/YourAppName
Write-Host "Copying build output to Tomcat webapps..."
Copy-Item `
    -Path (Join-Path $buildDir '*') `
    -Destination $deployDir `
    -Recurse `
    -Force `
    -ErrorAction Stop

# === 8. Clean the src folder of any .class files ===
Write-Host "Cleaning .class files from source directory..."
$classFiles = Get-ChildItem -Path $srcDir -Recurse -Filter '*.class' -File
if ($classFiles.Count -gt 0) {
    Write-Host "Found $($classFiles.Count) .class files to clean..."
    foreach ($file in $classFiles) {
        Remove-Item $file.FullName -Force
        Write-Verbose "Removed: $($file.FullName)"
    }
    Write-Host "Source directory cleaned of all class files."
} else {
    Write-Host "No .class files found in source directory."
}

Write-Host "Deployment complete. Restart Tomcat if it was running to pick up changes."