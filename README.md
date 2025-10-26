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
- `${JAVA}`
- `${VENDOR_NAME}`

## Génération du build et de l'environnement

Selon l'option `--build`, le générateur ajoute automatiquement :
- Maven (`pom.xml`) si `--build maven`
- Gradle (`build.gradle`) si `--build gradle`

Un fichier `.sdkmanrc` est toujours créé avec :
- `java=<java_flavor>` (ex.: `25-zulu`)
- `maven=<version>` si build Maven, ou `gradle=<version>` si build Gradle

## Installation

Requiert Rust/Cargo. Pour compiler :

```sh
cargo build --release
```

## Options (CLI)

Raccourcis et options disponibles (voir `src/main.rs`) :
- `-t, --template <PATH>`: Chemin du template (ZIP ou dossier) [obligatoire]
- `-d, --destination <DIR>`: Répertoire destination [obligatoire]
- `-n, --project_name <NAME>`: Nom du projet (def.: `Demo`)
- `-a, --author <NAME>`: Auteur (def.: `Auteur inconnu`)
- `-e, --email <EMAIL>`: Email (def.: `email@inconnu.local`)
- `-v, --project_version <VER>`: Version du projet (def.: `0.0.1`)
- `-j, --java_version <VER>`: Version du JDK à cibler (ex.: `25`) utilisée aussi dans `pom.xml`/`build.gradle` (def.: `25`)
- `-f, --java_flavor <LABEL>`: Saveur du JDK pour sdkman (ex.: `25-zulu`) (def.: `25-zulu`)
- `-k, --package <PKG>`: Package Java (def.: `com.demo`)
- `-m, --mainclass <CLASS>`: Classe principale (def.: `App`)
- `-b, --build <maven|gradle>`: Outil de build (def.: `maven`)
- `--maven_version <VER>`: Version Maven pour `.sdkmanrc` (def.: `3.9.5`)
- `--gradle_version <VER>`: Version Gradle pour `.sdkmanrc` (def.: `8.5`)
- `-l, --vendor_name <NAME>`: Nom du vendeur (utilisable dans les templates) (def.: `Vendor`)

## Utilisation

Exemples :

- Générer depuis un dossier template (Maven par défaut) :

```sh
cargo run -- \
  --template templates/basic-java \
  --destination ./out \
  --project_name Demo \
  --author "Frédéric Delorme" \
  --email fred@example.com \
  --project_version 0.1.0 \
  --package com.demo \
  --mainclass App
```

- Générer depuis un ZIP :

```sh
cargo run -- --template /chemin/vers/template.zip --destination ./out --project_name Demo
```

- Forcer Gradle et préciser les versions dans `.sdkmanrc` :

```sh
cargo run -- \
  --template templates/basic-java \
  --destination ./out \
  --project_name Demo \
  --build gradle \
  --gradle_version 8.5 \
  --java_flavor 25-zulu
```

- Cibler une version de JDK pour la compilation (utilisée dans `pom.xml` et `build.gradle`) :

```sh
cargo run -- --template templates/basic-java --destination ./out --project_name Demo -j 25
```

## Templates fournis

Des templates d'exemple se trouvent dans `templates/` (ex.: `templates/basic-java`). Vous pouvez les utiliser tels quels ou créer votre propre template (dossier ou ZIP). La structure peut contenir `${PACKAGE}` dans les chemins pour que le générateur crée les sous-dossiers correspondants.

## Fichiers importants

- Configuration Cargo : `Cargo.toml`
- Entrée du programme et logique : `src/main.rs`

## Remarques

- Le programme crée le répertoire de destination `<destination>/<project_name>`.
- Si le template est un ZIP, le script tente d'éliminer un préfixe racine commun présent dans l'archive.
- Les fichiers binaires détectés (non texte) sont copiés tels quels; seuls les fichiers texte subissent les remplacements.