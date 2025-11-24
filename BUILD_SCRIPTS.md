# 📦 Scripts de Build

Ce répertoire contient des scripts pour compiler et packager `genj`.

## 🪟 Windows

### Avec PowerShell

```powershell
# Exécuter le script de build
.\build-release.ps1

# Ou avec paramètres
.\build-release.ps1 -ConfigPath "Cargo.toml"
```

Le script va :
1. Lire la version depuis `Cargo.toml`
2. Compiler le projet en mode release avec `cargo build --release`
3. Créer un fichier ZIP : `build/genj-X.Y.Z-windows-x86_64.zip`
4. Le ZIP contient :
   - `genj.exe` (l'exécutable compilé)
   - `docs/*.md` (tous les fichiers Markdown de documentation)

### Depuis CMD ou autre terminal

Si vous préférez utiliser CMD, vous pouvez aussi lancer PowerShell directement :

```cmd
powershell -ExecutionPolicy Bypass -File ".\build-release.ps1"
```

## 🐧 Linux / macOS

### Avec Bash

```bash
chmod +x build-release.sh
./build-release.sh
```

Le script va :
1. Détecter automatiquement votre plateforme
2. Compiler le projet en mode release
3. Créer un fichier ZIP avec le nom approprié
4. Lister le contenu du ZIP généré

## 📋 Résultats

Après exécution, vous trouverez le fichier ZIP dans le répertoire `build/` :
- Windows : `build/genj-X.Y.Z-windows-x86_64.zip`
- Linux : `build/genj-X.Y.Z-linux-x86_64.zip`
- macOS : `build/genj-X.Y.Z-macos-x86_64.zip`

## 🐛 Dépannage

### Erreur: "Cargo n'est pas installé"
- Installez Rust depuis https://www.rust-lang.org/tools/install
- Redémarrez votre terminal après l'installation

### Erreur: "L'exécutable n'a pas été créé"
- Vérifiez que le build s'est déroulé sans erreurs
- Assurez-vous d'avoir les dépendances nécessaires
- Consultez les logs du build

## 📝 Notes

- Le script crée automatiquement le répertoire `build/` s'il n'existe pas
- Les fichiers `.md` sont inclus depuis le répertoire `docs/`
- La version est lue automatiquement depuis `Cargo.toml`
- Le ZIP est compressé (Deflate)
