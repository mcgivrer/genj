# genj

A command-line tool written in Rust to generate a Java project from a template (ZIP file or folder).

## Principle

The binary reads a template (ZIP or folder), copies files to the destination directory while applying variable replacements in file paths and content. It also handles the transformation of the `${PACKAGE}` variable into a Java folder hierarchy.

The main behavior is implemented in [`main`](src/main.rs) via the [`Cli`](src/main.rs) configuration structure and the [`extract_zip_with_replace`](src/main.rs), [`copy_dir_with_replace`](src/main.rs), and [`replace_package_in_path`](src/main.rs) functions.

## Available replacement variables

The following patterns are replaced in files and file names:
- `${PROJECT_NAME}`
- `${AUTHOR_NAME}`
- `${AUTHOR_EMAIL}`
- `${PROJECT_VERSION}`
- `${PACKAGE}` (transformed into folder hierarchy, e.g., `com.example`)
- `${MAINCLASS}`
- `${PROJECT_YEAR}`
- `${JAVA}`
- `${VENDOR_NAME}`

## Build and environment generation

Depending on the `--build` option, the generator automatically adds:
- Maven (`pom.xml`) if `--build maven`
- Gradle (`build.gradle`) if `--build gradle`

A `.sdkmanrc` file is always created with:
- `java=<java_flavor>` (e.g., `25-zulu`)
- `maven=<version>` if Maven build, or `gradle=<version>` if Gradle build

## Installation

Requires Rust/Cargo. To compile:

```sh
cargo build --release
```

## Options (CLI)

Available shortcuts and options (see `src/main.rs`):
- `-t, --template <PATH>`: Path to the template (ZIP or folder) [required]
- `-d, --destination <DIR>`: Destination directory [required]
- `-n, --project_name <NAME>`: Project name (default: `Demo`)
- `-a, --author <NAME>`: Author (default: `Unknown Author`)
- `-e, --email <EMAIL>`: Email (default: `email@unknown.local`)
- `-v, --project_version <VER>`: Project version (default: `0.0.1`)
- `-j, --java_version <VER>`: JDK version to target (e.g., `25`) also used in `pom.xml`/`build.gradle` (default: `25`)
- `-f, --java_flavor <LABEL>`: JDK flavor for sdkman (e.g., `25-zulu`) (default: `25-zulu`)
- `-k, --package <PKG>`: Java package (default: `com.demo`)
- `-m, --mainclass <CLASS>`: Main class (default: `App`)
- `-b, --build <maven|gradle>`: Build tool (default: `maven`)
- `--maven_version <VER>`: Maven version for `.sdkmanrc` (default: `3.9.5`)
- `--gradle_version <VER>`: Gradle version for `.sdkmanrc` (default: `8.5`)
- `-l, --vendor_name <NAME>`: Vendor name (usable in templates) (default: `Vendor`)

## Usage

Examples:

- Generate from a template folder (Maven by default):

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

- Generate from a ZIP:

```sh
cargo run -- --template /path/to/template.zip --destination ./out --project_name Demo
```

- Force Gradle and specify versions in `.sdkmanrc`:

```sh
cargo run -- \
  --template templates/basic-java \
  --destination ./out \
  --project_name Demo \
  --build gradle \
  --gradle_version 8.5 \
  --java_flavor 25-zulu
```

- Target a specific JDK version for compilation (used in `pom.xml` and `build.gradle`):

```sh
cargo run -- --template templates/basic-java --destination ./out --project_name Demo -j 25
```

## Provided templates

Example templates can be found in `templates/` (e.g., `templates/basic-java`). You can use them as is or create your own template (folder or ZIP). The structure can contain `${PACKAGE}` in paths so that the generator creates the corresponding subfolders.

## Important files

- Cargo configuration: `Cargo.toml`
- Program entry point and logic: `src/main.rs`

## Notes

- The program creates the destination directory `<destination>/<project_name>`.
- If the template is a ZIP, the script attempts to remove a common root prefix present in the archive.
- Binary files detected (non-text) are copied as is; only text files undergo replacements.

## Generate Debian package

Rendre le script exécutable :

```bash
chmod +x build_deb.sh
```

Puis pour générer le package Debian :

```bash
./build_deb.sh
```

Le script va :
1. Compiler le projet Rust en mode release
2. Créer la structure Debian standard
3. Copier le binaire compilé dans `/usr/bin/`
4. Copier et compresser la page man dans `/usr/share/man/man1/`
5. Créer les scripts de maintenance (postinst, prerm, postrm)
6. Générer les fichiers de documentation (changelog, copyright)
7. Construire le package `.deb`

**Installation du package généré :**

```bash
sudo dpkg -i genj_1.0.4_amd64.deb
```

**Vérification :**

```bash
which genj
man genj
genj --help