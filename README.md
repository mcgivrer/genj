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

## Release and packaging scripts

Two helper scripts are provided to produce release artifacts and Debian packages:

- build-release-full.sh
  - Purpose: compile a release build and collect artifacts (release binary, man pages, docs, LICENSE, README, etc.) into a release directory and an archive (tar.gz).
  - Usage:
    ```bash
    chmod +x build-release-full.sh
    ./build-release-full.sh
    ```
  - Result: a release archive (and/or release directory) is produced in the repository (see script output for exact path). Use this to distribute prebuilt binaries.

- build_deb.sh
  - Purpose: build a Debian package (.deb) that installs the `genj` binary and man pages (section 1 and section 5), plus documentation.
  - Usage:
    ```bash
    chmod +x build_deb.sh
    ./build_deb.sh
    ```
  - Result: a Debian package `genj_<version>_<arch>.deb` is created in the project root. Install with:
    ```bash
    sudo dpkg -i genj_<version>_<arch>.deb
    ```

Make both scripts executable before running:

```bash
chmod +x build-release-full.sh build_deb.sh
```

## Generate Debian package (legacy note)

The project includes `build_deb.sh` which automates packaging into a .deb. The script:
1. Compiles the project in release mode
2. Creates the Debian package layout (including `/usr/bin` and `/usr/share/man`)
3. Copies and compresses man pages (genj.1 and genj-template.5) into appropriate sections
4. Creates maintainer scripts (postinst, prerm, postrm) and documentation under `/usr/share/doc/genj`
5. Builds the .deb

**Installing the generated package:**

```bash
sudo dpkg -i genj_1.2.2_amd64.deb
```

**Verification:**

```bash
which genj
man genj
man genj-template
genj --help
```