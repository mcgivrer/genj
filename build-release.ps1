# Script de build release pour genj
# Ce script compile le projet en mode release et crée un ZIP avec l'exécutable et la documentation

param(
    [string]$ConfigPath = "Cargo.toml"
)

# Lire la version depuis Cargo.toml
$cargoContent = Get-Content $ConfigPath -Raw
$versionMatch = [regex]::Match($cargoContent, 'version\s*=\s*"([^"]+)"')
if ($versionMatch.Success) {
    $version = $versionMatch.Groups[1].Value
} else {
    Write-Error "Impossible de trouver la version dans $ConfigPath"
    exit 1
}

$packageName = "genj"
$platform = "windows-x86_64"
$zipFilename = "build/$packageName-$version-$platform.zip"

Write-Host "🔨 Compilation en mode release..."
Write-Host "Version: $version"
Write-Host "Nom du ZIP: $zipFilename"

# Créer le répertoire build s'il n'existe pas
if (-not (Test-Path "build")) {
    New-Item -ItemType Directory -Path "build" | Out-Null
    Write-Host "📁 Répertoire 'build' créé"
}

# Compiler en mode release
$exePath = "target/release/$packageName.exe"

# Vérifier si cargo est disponible
$cargoBin = Get-Command cargo -ErrorAction SilentlyContinue
if (-not $cargoBin) {
    Write-Error "❌ Cargo n'est pas installé ou pas dans le PATH"
    Write-Host "Téléchargez Rust depuis: https://www.rust-lang.org/tools/install"
    exit 1
}

Write-Host "🚀 Compilation en cours..."
cargo build --release
if ($LASTEXITCODE -ne 0) {
    Write-Error "❌ La compilation a échoué"
    exit 1
}

# Vérifier que l'exécutable a été créé
if (-not (Test-Path $exePath)) {
    Write-Error "❌ L'exécutable $exePath n'a pas été créé"
    exit 1
}

# Créer le ZIP
Write-Host "📦 Création du fichier ZIP..."
$zip = New-Object System.IO.Compression.ZipFile

# Supprimer le fichier ZIP s'il existe
if (Test-Path $zipFilename) {
    Remove-Item $zipFilename
}

# Créer le ZIP avec SharpZipLib ou la méthode native
$tempDir = New-Item -ItemType Directory -Path "build/temp_$([guid]::NewGuid().ToString())" -Force
try {
    # Copier l'exécutable
    Copy-Item $exePath "$tempDir/$packageName.exe"
    
    # Copier les fichiers .md du répertoire docs
    if (Test-Path "docs") {
        $docsDir = "$tempDir/docs"
        New-Item -ItemType Directory -Path $docsDir -Force | Out-Null
        Get-ChildItem "docs" -Filter "*.md" -File | ForEach-Object {
            Copy-Item $_.FullName "$docsDir/$($_.Name)"
        }
    }
    
    # Créer le ZIP en compressant le répertoire temporaire
    Compress-Archive -Path "$tempDir/*" -DestinationPath $zipFilename -Force
    
    Write-Host "✅ Fichier ZIP créé avec succès: $zipFilename"
    
    # Afficher le contenu du ZIP
    Write-Host ""
    Write-Host "📋 Contenu du ZIP:"
    Add-Type -Assembly System.IO.Compression.FileSystem
    $zipFile = [System.IO.Compression.ZipFile]::OpenRead($zipFilename)
    $zipFile.Entries | ForEach-Object {
        Write-Host "  - $($_.FullName) ($($_.Length) bytes)"
    }
    $zipFile.Dispose()
    
} finally {
    # Nettoyer le répertoire temporaire
    if (Test-Path $tempDir) {
        Remove-Item $tempDir -Recurse -Force
    }
}

Write-Host "✨ Build terminé!"
