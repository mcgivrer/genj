#!/bin/bash

# Script de build complet pour genj
# Compile en mode release, crée les templates ZIP, et génère le package Debian

set -e

echo "🔨 Build genj en mode release avec package ZIP..."

# Compiler en mode release
echo "🚀 Compilation en cours..."
cargo build --release
echo "✓ Compilation réussie"

# Générer les archives ZIP pour chaque template
echo "📦 Génération des templates ZIP..."
TEMPLATE_SRC_DIR="templates"
TEMPLATE_ZIP_DIR="target/release-templates"
mkdir -p "$TEMPLATE_ZIP_DIR"

for dir in "$TEMPLATE_SRC_DIR"/*/; do
    template_name=$(basename "$dir")
    zip_file="$TEMPLATE_ZIP_DIR/${template_name}.zip"
    echo "  - $template_name -> $zip_file"
    (cd "$dir" && zip -r "../../$zip_file" .)
done

echo "✓ Templates ZIP générés dans $TEMPLATE_ZIP_DIR"

# Exécuter le binaire de création de package (optionnel)
if [ -f "src/bin/build-package.rs" ] || [ -f "src/bin/build-package/main.rs" ]; then
    echo "📦 Création du package ZIP (binaire build-package)..."
    cargo run --release --bin build-package
    echo "✨ Package ZIP généré par le binaire build-package"
fi

# Sur Linux, lancer le script de création du paquet .deb si présent
OS_NAME="$(uname -s)"
if [ "$OS_NAME" = "Linux" ]; then
    if [ -x ./build_deb.sh ]; then
        echo "📦 Création du paquet .deb (Linux)..."
        # Passer le dossier des templates ZIP à build_deb.sh
        TEMPLATE_INSTALL_DIR="usr/share/genj/templates"
        export GENJ_TEMPLATE_ZIP_DIR="$TEMPLATE_ZIP_DIR"
        export GENJ_TEMPLATE_INSTALL_DIR="$TEMPLATE_INSTALL_DIR"
        ./build_deb.sh
        echo "✅ Paquet .deb créé avec succès"
        echo "Les templates ZIP seront installés dans /$TEMPLATE_INSTALL_DIR"
        echo "Le chemin de recherche par défaut pour les templates est : /$TEMPLATE_INSTALL_DIR"
        echo "Vous pouvez aussi stocker vos propres templates dans ~/.genj/ dans votre répertoire home."
    else
        echo "⚠️  build_deb.sh introuvable ou non exécutable — saut de la création du .deb"
    fi
else
    echo "ℹ️  Système détecté: $OS_NAME — création du .deb uniquement sur Linux."
fi
