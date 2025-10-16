# genj

Outil en ligne de commande écrit en Rust pour générer un projet Java à partir d'un template (fichier ZIP ou dossier).

## Principe

Le binaire lit un template (ZIP ou dossier), copie les fichiers vers le répertoire de destination en appliquant des remplacements de variables dans les chemins et le contenu des fichiers. Il gère aussi la transformation de la variable `${PACKAGE}` en arborescence de dossiers Java.

Le comportement principal est implémenté dans [`main`](src/main.rs) via la structure de configuration [`Cli`](src/main.rs) et les fonctions [`extract_zip_with_replace`](src/main.rs), [`copy_dir_with_replace`](src/main.rs) et [`replace_package_in_path`](src/main.rs).

## Variables de remplacement disponibles

Les motifs suivants sont remplacés dans les fichiers et les noms de fichiers :
- `${PROJECT_NAME}`
- `${AUTHOR_NAME}`
- `${AUTHOR_EMAIL}`
- `${PROJECT_VERSION}`
- `${PACKAGE}` (transformé en arborescence de dossiers, ex. `com.example`)
- `${MAINCLASS}`
- `${PROJECT_YEAR}`

## Installation

Requiert Rust/Cargo. Pour compiler :

```sh
cargo build --release
```
## Utilisation

Exemples :

* Générer depuis un dossier template :

```sh
cargo run -- --template templates/basic-java --destination ./out --project_name Demo --author "Frédéric Delorme" --email fred@example.com --project_version 0.1.0 --package com.demo --mainclass App
```

* Générer depuis un ZIP :

```sh
cargo run -- --template /chemin/vers/template.zip --destination ./out --project_name Demo
```

* Spécifier une version Java (génère un fichier .sdkman contenant java=<version> dans le projet) :

```sh
cargo run -- --template templates/basic-java --destination ./out --project_name Demo -j 17.0.6-tem
```
## Templates fournis

Un template d'exemple se trouve dans le répertoire templates/basic-java. Vous pouvez l'utiliser tel quel ou créer votre propre template (dossier ou ZIP). La structure peut contenir ${PACKAGE} dans les chemins pour que le générateur crée les sous-dossiers correspondants.

## Fichiers importants

- Configuration Cargo : Cargo.toml
- Entrée du programme et logique : src/main.rs

## Remarques

Le programme crée le répertoire de destination <destination>/<project_name>.
Si le template est un ZIP, le script tente d'éliminer un préfixe racine commun présent dans l'archive.