#!/bin/bash

# Script de build complet pour genj
# Compile en mode release et crée le package ZIP

echo "🔨 Build genj en mode release avec package ZIP..."

# Compiler en mode release
echo "🚀 Compilation en cours..."
cargo build --release

if [ $? -ne 0 ]; then
    echo "❌ La compilation a échoué"
    exit 1
fi

echo "✓ Compilation réussie"

# Exécuter le binaire de création de package
echo "📦 Création du package ZIP..."
cargo run --release --bin build-package

if [ $? -ne 0 ]; then
    echo "❌ La création du package a échoué"
    exit 1
fi

echo "✨ Build terminé avec succès !"

# Sur Linux, lancer le script de création du paquet .deb si présent
OS_NAME="$(uname -s)"
if [ "$OS_NAME" = "Linux" ]; then
    if [ -x ./build_deb.sh ]; then
        echo "📦 Création du paquet .deb (Linux)..."
        ./build_deb.sh
        if [ $? -ne 0 ]; then
            echo "❌ La création du paquet .deb a échoué"
            exit 1
        fi
        echo "✅ Paquet .deb créé avec succès"
    else
        echo "⚠️  build_deb.sh introuvable ou non exécutable — saut de la création du .deb"
    fi
else
    echo "ℹ️  Système détecté: $OS_NAME — création du .deb uniquement sur Linux."
fi
