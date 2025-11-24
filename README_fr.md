# genj

Outil en ligne de commande écrit en Rust pour générer un projet Java à partir d'un template (fichier ZIP ou dossier).

## Principe

Le binaire lit un template (ZIP ou dossier), copie les fichiers vers le répertoire de destination en appliquant des remplacements de variables dans les chemins et le contenu des fichiers. Il gère également la transformation de la variable `${PACKAGE}` en une arborescence de dossiers Java.

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
- `java=<java_flavor>` (ex. : `25-zulu`)
- `maven=<version>` si build Maven, ou `gradle=<version>` si build Gradle

## Installation

Requiert Rust/Cargo. Pour compiler :

```sh
cargo build --release
```

## Options (CLI)

Raccourcis et options disponibles (voir `src/main.rs`) :
- `-t, --template <PATH>` : Chemin du template (ZIP ou dossier) [obligatoire]
- `-d, --destination <DIR>` : Répertoire de destination [obligatoire]
- `-n, --project_name <NAME>` : Nom du projet (par défaut : `Demo`)
- `-a, --author <NAME>` : Auteur (par défaut : `Unknown Author`)
- `-e, --email <EMAIL>` : Email (par défaut : `email@unknown.local`)
- `-v, --project_version <VER>` : Version du projet (par défaut : `0.0.1`)
- `-j, --java_version <VER>` : Version du JDK cible (ex. : `25`) utilisée aussi dans `pom.xml`/`build.gradle` (par défaut : `25`)
- `-f, --java_flavor <LABEL>` : Variante JDK pour sdkman (ex. : `25-zulu`) (par défaut : `25-zulu`)
- `-k, --package <PKG>` : Package Java (par défaut : `com.demo`)
- `-m, --mainclass <CLASS>` : Classe principale (par défaut : `App`)
- `-b, --build <maven|gradle>` : Outil de build (par défaut : `maven`)
- `--maven_version <VER>` : Version Maven pour `.sdkmanrc` (par défaut : `3.9.5`)
- `--gradle_version <VER>` : Version Gradle pour `.sdkmanrc` (par défaut : `8.5`)
- `-l, --vendor_name <NAME>` : Nom du vendeur (utilisable dans les templates) (par défaut : `Vendor`)

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

Des templates d'exemple se trouvent dans `templates/` (ex. : `templates/basic-java`). Vous pouvez les utiliser tels quels ou créer votre propre template (dossier ou ZIP). La structure peut contenir `${PACKAGE}` dans les chemins afin que le générateur crée les sous-dossiers correspondants.

## Fichiers importants

- Configuration Cargo : `Cargo.toml`
- Point d'entrée et logique : `src/main.rs`

## Remarques

- Le programme crée le répertoire de destination `<destination>/<project_name>`.
- Si le template est un fichier ZIP, le script tente d'éliminer un préfixe racine commun présent dans l'archive.
- Les fichiers binaires détectés (non texte) sont copiés tels quels ; seuls les fichiers texte subissent les remplacements.

## Scripts de release et de packaging

Deux scripts utilitaires sont fournis pour produire des artefacts de release et des paquets Debian :

- build-release-full.sh
  - Objectif : compiler une build release et rassembler les artefacts (binaire release, pages man, docs, LICENSE, README, etc.) dans un répertoire de release et une archive (tar.gz).
  - Utilisation :
    ```bash
    chmod +x build-release-full.sh
    ./build-release-full.sh
    ```
  - Résultat : une archive de release (et/ou un répertoire de release) est créée dans le dépôt (voir la sortie du script pour le chemin exact). Utile pour distribuer des binaires précompilés.

- build_deb.sh
  - Objectif : créer un paquet Debian (.deb) qui installe le binaire `genj`, les pages man (sections 1 et 5) et la documentation.
  - Utilisation :
    ```bash
    chmod +x build_deb.sh
    ./build_deb.sh
    ```
  - Résultat : un paquet Debian `genj_<version>_<arch>.deb` est créé à la racine du projet. Installer avec :
    ```bash
    sudo dpkg -i genj_<version>_<arch>.deb
    ```

Rendre les scripts exécutables avant utilisation :

```bash
chmod +x build-release-full.sh build_deb.sh
```

## Générer un paquet Debian (note)

Le script `build_deb.sh` automatise la création du .deb :
1. Compile le projet en mode release
2. Crée la structure du paquet Debian (incluant `/usr/bin` et `/usr/share/man`)
3. Copie et compresse les pages man (`genj.1` et `genj-template.5`) aux emplacements appropriés
4. Crée les scripts de maintenance (postinst, prerm, postrm) et la documentation sous `/usr/share/doc/genj`
5. Construit le paquet `.deb`

**Installer le paquet généré :**

```bash
sudo dpkg -i genj_1.2.2_amd64.deb
```

**Vérification :**

```bash
which genj
man genj
man genj-template
genj --help
```