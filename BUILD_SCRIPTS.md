# 📦 Build & Packaging

Tout le packaging est piloté par un **unique binaire Rust** (`src/bin/mkpkg.rs`) dont la configuration se trouve dans `Cargo.toml`.

## Commande unique

```bash
cargo run --release --bin mkpkg
```

Les formats construits par défaut dépendent de l'OS courant, tels que définis dans `Cargo.toml` :

```toml
[package.metadata.package]
linux   = ["deb", "rpm", "zip"]
macos   = ["dmg", "zip"]
windows = ["msi", "zip"]
```

## Options

```
--format <fmt>   Formats à construire : zip, deb, rpm, msi, dmg  (ou 'all', ou liste csv)
--skip-build     Ne pas recompiler (utiliser le binaire déjà présent dans target/release/)
--help           Aide
```

### Exemples

```bash
# Formats par défaut de l'OS
cargo run --release --bin mkpkg

# ZIP seul (toutes plateformes)
cargo run --release --bin mkpkg -- --format zip

# DEB + RPM sans recompiler
cargo run --release --bin mkpkg -- --format deb,rpm --skip-build

# Tous les formats
cargo run --release --bin mkpkg -- --format all
```

## Scripts wrapper (compatibilité)

Les anciens scripts sont conservés comme alias d'une ligne :

| Script | Équivalent |
|---|---|
| `./build-release-full.sh [opts]` | `cargo run --release --bin mkpkg -- [opts]` |
| `.\build-release-full.ps1 [opts]` | `cargo run --release --bin mkpkg -- [opts]` |
| `./build_deb.sh` | `cargo run --release --bin mkpkg -- --format deb` |
| `./build_rpm.sh` | `cargo run --release --bin mkpkg -- --format rpm` |
| `./build_dmg.sh` | `cargo run --release --bin mkpkg -- --format dmg` |

## Formats produits

Tous les fichiers sont créés dans `target/package/` :

| Format | Fichier | Outil requis |
|---|---|---|
| `zip` | `genj-X.Y.Z-<os>-x86_64.zip` | *(aucun — pur Rust)* |
| `deb` | `genj_X.Y.Z_amd64.deb` | `cargo install cargo-deb` |
| `rpm` | `genj-X.Y.Z-1.x86_64.rpm` | `cargo install cargo-generate-rpm` |
| `msi` | `genj-X.Y.Z-x86_64.msi` | `cargo install cargo-wix` + [WiX 3](https://wixtoolset.org/releases/) |
| `dmg` | `genj-X.Y.Z-macos-x86_64.dmg` | `hdiutil` (intégré macOS) |

> `cargo-deb` et `cargo-generate-rpm` sont installés automatiquement s'ils sont absents.

## Configuration dans Cargo.toml

| Section | Usage |
|---|---|
| `[package.metadata.package]` | Formats par défaut par OS |
| `[package.metadata.deb]` | Assets, dépendances, scripts post-install DEB |
| `[package.metadata.generate-rpm]` | Assets et dépendances RPM |
| `wix/main.wxs` | Template WiX pour le MSI Windows |


## Vue d'ensemble des formats produits

| Plateforme | Format | Outil requis |
|---|---|---|
| Windows | `.msi` (installeur natif) | [WiX Toolset 3.x](https://wixtoolset.org/releases/) + `cargo-wix` |
| Windows | `.zip` (archive portable) | *(aucun)* |
| Linux Debian/Ubuntu | `.deb` | `dpkg-deb` (intégré à Debian) |
| Linux Fedora/RHEL | `.rpm` | `cargo-generate-rpm` |
| macOS | `.dmg` | `hdiutil` (intégré à macOS) |

---

## 🪟 Windows

### Build complet (ZIP + MSI)

```powershell
.\build-release-full.ps1
```

Le script :
1. Compile en mode release (`cargo build --release`)
2. Crée un ZIP portable dans `target/package/`
3. Génère un **MSI** via `cargo-wix` dans `target/package/`

> **Prérequis MSI** : [WiX Toolset 3.x](https://wixtoolset.org/releases/) doit être installé
> et ses binaires (`candle.exe`, `light.exe`) dans le `PATH`.
> `cargo-wix` sera installé automatiquement si absent.

### ZIP seulement (sans MSI)

```powershell
.\build-release-full.ps1 -SkipMsi
```

### Depuis CMD

```cmd
powershell -ExecutionPolicy Bypass -File ".\build-release-full.ps1"
```

### MSI seul (si déjà compilé)

```powershell
cargo install cargo-wix   # une seule fois
cargo wix
```

---

## 🐧 Linux

### Build complet (DEB + RPM)

```bash
chmod +x build-release-full.sh
./build-release-full.sh
```

Le script détecte Linux et enchaîne automatiquement la création du `.deb` puis du `.rpm`.

### Paquet Debian/Ubuntu (`.deb`) seul

```bash
chmod +x build_deb.sh
./build_deb.sh
```

Installe dans :
- `/usr/bin/genj`
- `/usr/share/genj/templates/` (templates ZIP)
- `/usr/share/man/man1/genj.1.gz`
- `/usr/share/man/man5/genj-template.5.gz`

```bash
sudo dpkg -i target/package/genj_*.deb
```

### Paquet RPM — Fedora / RHEL / openSUSE (`.rpm`) seul

```bash
chmod +x build_rpm.sh
./build_rpm.sh
```

> `cargo-generate-rpm` sera installé automatiquement si absent.

```bash
sudo rpm -ivh target/package/genj-*.rpm
# ou
sudo dnf install target/package/genj-*.rpm
```

---

## 🍎 macOS

### Build complet (DMG)

```bash
chmod +x build-release-full.sh
./build-release-full.sh
```

Le script détecte macOS et crée automatiquement le `.dmg`.

### DMG seul

```bash
chmod +x build_dmg.sh
./build_dmg.sh
```

Le DMG contient :
- `bin/genj` (exécutable)
- `templates/*.zip` (templates)
- `man/` (pages de manuel)
- `install.sh` (script d'installation dans `/usr/local`)
- `README.md` et `LICENSE`

Utilisation après montage :
```bash
# Installation dans /usr/local/bin (par défaut)
sudo /Volumes/genj-*/install.sh

# Ou avec un préfixe personnalisé
PREFIX=~/.local /Volumes/genj-*/install.sh
```

> **Option** : Pour un DMG stylisé avec fond et icônes :
> ```bash
> brew install create-dmg
> ./build_dmg.sh
> ```

---

## 📋 Résultats

Tous les installeurs produits se trouvent dans `target/package/` :

| Fichier | Plateforme |
|---|---|
| `genj-X.Y.Z-windows-x86_64.zip` | Windows (portable) |
| `genj-X.Y.Z-x86_64.msi` | Windows (installeur MSI) |
| `genj_X.Y.Z_amd64.deb` | Debian / Ubuntu |
| `genj-X.Y.Z-1.x86_64.rpm` | Fedora / RHEL / openSUSE |
| `genj-X.Y.Z-macos-x86_64.dmg` | macOS |

---

## 🐛 Dépannage

### `cargo-wix` : `candle.exe` introuvable
Installez [WiX Toolset 3.x](https://wixtoolset.org/releases/) et ajoutez son dossier au `PATH` Windows.

### `cargo-generate-rpm` : erreur de build
Assurez-vous que le projet est compilé en mode release avant (`cargo build --release`).

### DMG macOS : `hdiutil: create failed`
Vérifiez les permissions sur le répertoire `target/package/` et que suffisamment d'espace disque est disponible.

### Erreur: "Cargo n'est pas installé"
Installez Rust depuis https://www.rust-lang.org/tools/install puis redémarrez votre terminal.

