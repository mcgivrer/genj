# genj(1) - Generate a Java project from a template

## NAME

**genj** - Generate a Java project from a template

## SYNOPSIS

```
genj -t TEMPLATE -d DESTINATION [OPTIONS]
```

## DESCRIPTION

**genj** is a command-line tool written in Rust that generates a Java project from a template (ZIP file or folder).
The binary reads a template, copies files to the destination directory while applying variable replacements
in file paths and content. It also handles the transformation of the `${PACKAGE}` variable into a Java folder hierarchy.

## REQUIRED OPTIONS

### `-t, --template TEMPLATE`
Path to the template (ZIP file or folder).

### `-d, --destination DESTINATION`
Destination directory where the project will be created.

## OPTIONAL OPTIONS

### `-n, --project_name NAME`
Project name (default: `Demo`).

### `-a, --author NAME`
Author name (default: `Unknown Author`).

### `-e, --email EMAIL`
Author email (default: `email@unknown.local`).

### `-v, --project_version VERSION`
Project version (default: `0.0.1`).

### `-j, --java_version VERSION`
JDK version to target (e.g., `25`). Also used in `pom.xml` and `build.gradle` (default: `25`).

### `-f, --java_flavor FLAVOR`
JDK flavor for sdkman (e.g., `25-zulu`) (default: `25-zulu`).

### `-k, --package PACKAGE`
Java package name (default: `com.demo`).

### `-m, --mainclass CLASS`
Main class name (default: `App`).

### `-b, --build TOOL`
Build tool to use: `maven` or `gradle` (default: `maven`).

### `--maven_version VERSION`
Maven version for `.sdkmanrc` (default: `3.9.5`).

### `--gradle_version VERSION`
Gradle version for `.sdkmanrc` (default: `8.5`).

### `-l, --vendor_name NAME`
Vendor name (usable in templates) (default: `Vendor`).

## REPLACEMENT VARIABLES

The following patterns are replaced in files and file names:

| Variable | Description |
|----------|-------------|
| `${PROJECT_NAME}` | Project name |
| `${AUTHOR_NAME}` | Author name |
| `${AUTHOR_EMAIL}` | Author email |
| `${PROJECT_VERSION}` | Project version |
| `${PACKAGE}` | Java package (transformed into folder hierarchy, e.g., `com.example` becomes `com/example`) |
| `${MAINCLASS}` | Main class name |
| `${PROJECT_YEAR}` | Current year |
| `${JAVA}` | JDK version |
| `${VENDOR_NAME}` | Vendor name |

## GENERATED FILES

**genj** automatically generates the following files:

### `pom.xml`
Maven build configuration (if `--build maven`).

### `build.gradle`
Gradle build configuration (if `--build gradle`).

### `.sdkmanrc`
SDKMan configuration file with Java and build tool versions.

### `.vscode/settings.json`
VSCode Java project settings.

### `.vscode/launch.json`
VSCode launch configuration for debugging.

### `.git/`
Git repository initialized with initial commit.

## EXAMPLES

### Generate a Maven project from a template folder

```bash
genj -t templates/basic-java -d ./out -n Demo \
  -a "Frédéric Delorme" -e fred@example.com \
  -v 0.1.0 -k com.demo -m App
```

### Generate a Gradle project from a ZIP template

```bash
genj -t template.zip -d ./out -n MyProject \
  -b gradle --gradle_version 8.5
```

### Generate with specific JDK version

```bash
genj -t templates/basic-java -d ./out -n Demo \
  -j 25 -f 25-zulu
```

### Generate with all parameters

```bash
genj \
  --template templates/basic-java \
  --destination ./projects \
  --project_name MyApp \
  --author "John Doe" \
  --email john@example.com \
  --project_version 1.0.0 \
  --java_version 21 \
  --java_flavor 21-zulu \
  --package com.company.app \
  --mainclass MyApp \
  --build gradle \
  --gradle_version 8.7 \
  --vendor_name "My Company"
```

## NOTES

- The program creates the destination directory as `<destination>/<project_name>`.
- If the template is a ZIP file, the script attempts to remove a common root prefix present in the archive.
- Binary files detected (non-text) are copied as is; only text files undergo variable replacements.
- The Git repository is automatically initialized with an initial commit.

## AUTHOR

Frédéric Delorme

## VERSION

1.2.0

## SEE ALSO

For more information, visit the project repository or consult the README.md file.