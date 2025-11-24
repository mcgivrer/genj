#!/bin/bash

# Script de build release pour genj
# Ce script compile le projet en mode release et crée un ZIP avec l'exécutable et la documentation

CARGO_TOML="Cargo.toml"

# Lire la version depuis Cargo.toml
VERSION=$(grep '^version' "$CARGO_TOML" | head -1 | sed 's/.*"\([^"]*\)".*/\1/')

if [ -z "$VERSION" ]; then
    echo "❌ Impossible de trouver la version dans $CARGO_TOML"
    exit 1
fi

PACKAGE_NAME="genj"
PLATFORM=$(uname -s)

case "$PLATFORM" in
    Linux)
        PLATFORM_STR="linux-x86_64"
        EXE_EXT=""
        ;;
    Darwin)
        PLATFORM_STR="macos-x86_64"
        EXE_EXT=""
        ;;
    MINGW*|MSYS*)
        PLATFORM_STR="windows-x86_64"
        EXE_EXT=".exe"
        ;;
    *)
        PLATFORM_STR="unknown"
        EXE_EXT=""
        ;;
esac

ZIP_FILENAME="build/$PACKAGE_NAME-$VERSION-$PLATFORM_STR.zip"
EXE_PATH="target/release/$PACKAGE_NAME$EXE_EXT"

echo "🔨 Compilation en mode release..."
echo "Version: $VERSION"
echo "Plateforme: $PLATFORM_STR"
echo "Nom du ZIP: $ZIP_FILENAME"

# Créer le répertoire build s'il n'existe pas
mkdir -p build
echo "📁 Répertoire 'build' prêt"

# Vérifier si cargo est disponible
if ! command -v cargo &> /dev/null; then
    echo "❌ Cargo n'est pas installé ou pas dans le PATH"
    echo "Téléchargez Rust depuis: https://www.rust-lang.org/tools/install"
    exit 1
fi

# Compiler en mode release
echo "🚀 Compilation en cours..."
cargo build --release

if [ $? -ne 0 ]; then
    echo "❌ La compilation a échoué"
    exit 1
fi

# Vérifier que l'exécutable a été créé
if [ ! -f "$EXE_PATH" ]; then
    echo "❌ L'exécutable $EXE_PATH n'a pas été créé"
    exit 1
fi

# Créer le répertoire temporaire pour le ZIP
TEMP_DIR="build/temp_$$"
mkdir -p "$TEMP_DIR"

echo "📦 Création du fichier ZIP..."

# Copier l'exécutable
cp "$EXE_PATH" "$TEMP_DIR/$PACKAGE_NAME$EXE_EXT"

# Copier les fichiers .md du répertoire docs
if [ -d "docs" ]; then
    mkdir -p "$TEMP_DIR/docs"
    find docs -maxdepth 1 -name "*.md" -type f -exec cp {} "$TEMP_DIR/docs/" \;
fi

# Créer le ZIP
cd "$TEMP_DIR"
zip -r "../../$ZIP_FILENAME" . > /dev/null
cd - > /dev/null

# Nettoyer le répertoire temporaire
rm -rf "$TEMP_DIR"

echo "✅ Fichier ZIP créé avec succès: $ZIP_FILENAME"

# Afficher le contenu du ZIP
echo ""
echo "📋 Contenu du ZIP:"
unzip -l "$ZIP_FILENAME" | tail -n +4 | head -n -2 | awk '{print "  - " $4 " (" $1 " bytes)"}'

echo "✨ Build terminé!"
