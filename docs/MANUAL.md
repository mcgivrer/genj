# genj(1) - Generate a Java project from a template

## NAME

**genj** - Generate a Java project from a template

## SYNOPSIS

```
genj [--list | --search TERM | [--template TEMPLATE] --destination DESTINATION [OPTIONS]]
```

## DESCRIPTION

**genj** is a command-line tool written in Rust that generates a Java project from a template (ZIP file or folder).
The binary reads a template, copies files to the destination directory while applying variable replacements
in file paths and content. It also handles the transformation of the `${PACKAGE}` variable into a Java folder hierarchy.

Templates can be discovered using the `--list` and `--search` options, or provided directly via [--template](http://_vscodecontentref_/1).

## DISCOVERY COMMANDS

### `--list`

Lists all available templates from system and user template directories with complete metadata.

**Template locations:**
- **System templates:** [templates](http://_vscodecontentref_/2)
- **User templates:** [~/.genj/](http://_vscodecontentref_/3)

Each template displays:
- Name
- Description
- Language
- Version
- Author and contact information
- License
- Tags
- Creation date

**Example:**
```
genj --list
```

### `--search TERM`

Search for templates matching a search term (case-insensitive).

Searches across:
- Template name
- Description
- Language
- Author
- Version
- Contact
- License
- Tags

**Example:**
```
genj --search java
genj --search spring
genj -s gradle
```

## GENERATION OPTIONS

### [-t, --template TEMPLATE](http://_vscodecontentref_/4)

Path to the template (ZIP file or folder).

**Default search paths** (if template name without path is provided):
1. [templates](http://_vscodecontentref_/5)
2. [~/.genj/](http://_vscodecontentref_/6)

If the template is not found in these directories, it is treated as a direct file path.

### `-d, --destination DESTINATION`

Destination directory where the project will be created.

If not specified, the current directory is used. The final project is created as `<destination>/<project_name>`.

## OPTIONAL PARAMETERS

### `-n, --project_name NAME`
Project name (default: `Demo`).

### [-a, --author NAME](http://_vscodecontentref_/7)
Author name (default: `Unknown Author`).

### [-e, --email EMAIL](http://_vscodecontentref_/8)
Author email (default: `email@unknown.local`).

### [-v, --project_version VERSION](http://_vscodecontentref_/9)
Project version (default: `0.0.1`).

### [-j, --java_version VERSION](http://_vscodecontentref_/10)
JDK version to target (e.g., `25`). Also used in `pom.xml` and `build.gradle` (default: `25`).

### [-f, --java_flavor FLAVOR](http://_vscodecontentref_/11)
JDK flavor for sdkman (e.g., `25-zulu`) (default: `25-zulu`).

### [-k, --package PACKAGE](http://_vscodecontentref_/12)
Java package name (default: `com.demo`).

### `-m, --mainclass CLASS`
Main class name (default: `App`).

### [-b, --build TOOL](http://_vscodecontentref_/13)
Build tool to use: `maven` or `gradle` (default: `maven`).

### [--maven_version VERSION](http://_vscodecontentref_/14)
Maven version for `.sdkmanrc` (default: `3.9.5`).

### [--gradle_version VERSION](http://_vscodecontentref_/15)
Gradle version for `.sdkmanrc` (default: `8.5`).

### `-l, --vendor_name NAME`
Vendor name (usable in templates) (default: `Vendor`).

### `-r, --remote_git_repository URL`
Define the remote git repository for this project.

### [--verbose](http://_vscodecontentref_/16)
Enable verbose output for debugging. Prints detailed processing information including:
- File and directory operations
- Variable replacements
- ZIP extraction details
- Git and VSCode setup information

## REPLACEMENT VARIABLES

The following patterns are replaced in files and file names:

| Variable             | Description                                                                                 |
| -------------------- | ------------------------------------------------------------------------------------------- |
| `${PROJECT_NAME}`    | Project name                                                                                |
| `${AUTHOR_NAME}`     | Author name                                                                                 |
| `${AUTHOR_EMAIL}`    | Author email                                                                                |
| `${PROJECT_VERSION}` | Project version                                                                             |
| `${PACKAGE}`         | Java package (transformed into folder hierarchy, e.g., `com.example` becomes `com/example`) |
| `${MAINCLASS}`       | Main class name                                                                             |
| `${PROJECT_YEAR}`    | Current year                                                                                |
| `${JAVA}`            | JDK version                                                                                 |
| `${VENDOR_NAME}`     | Vendor name                                                                                 |

## GENERATED FILES

**genj** automatically generates the following files:

### `pom.xml`
Maven build configuration (if `--build maven`).

### `build.gradle`
Gradle build configuration (if `--build gradle`).

### `.sdkmanrc`
SDKMan configuration file with Java and build tool versions.

### `.genrc`
JSON configuration file documenting the generation parameters and metadata:

```
{
  "project_name": "MyProject",
  "author_name": "John Doe",
  "author_email": "john@example.com",
  "project_version": "1.0.0",
  "java_version": "25",
  "build_tool": "maven",
  "package": "com.example",
  "mainclass": "App",
  "vendor_name": "My Company",
  "template_path": "/path/to/template",
  "created_at": "2025-12-02T10:00:00Z",
  "generated_with": {
    "cmd": "genj",
    "version": "1.3.1"
  }
}
```

### `.vscode/settings.json`
VSCode Java project settings.

### `.vscode/launch.json`
VSCode launch configuration for debugging.

### [.git](http://_vscodecontentref_/17)
Git repository initialized with initial commit.

## TEMPLATE METADATA

Templates can include a [.template](http://_vscodecontentref_/18) metadata file (JSON format) at the root to provide information about the template.

### [.template](http://_vscodecontentref_/19) File Format

```
{
  "name": "Basic Java",
  "version": "1.0.0",
  "language": "Java",
  "description": "Basic Java project template with Maven support",
  "author": "Template Author",
  "contact": "author@example.com",
  "license": "MIT",
  "tags": ["java", "maven", "basic"],
  "created_at": "2025-12-02T10:00:00Z"
}
```

### Metadata Fields

| Field                                       | Description                                      |
| ------------------------------------------- | ------------------------------------------------ |
| [name](http://_vscodecontentref_/20)        | Template name                                    |
| [version](http://_vscodecontentref_/21)     | Template version                                 |
| [language](http://_vscodecontentref_/22)    | Programming language(s) targeted by the template |
| [description](http://_vscodecontentref_/23) | Detailed description of the template             |
| [author](http://_vscodecontentref_/24)      | Template author name                             |
| [contact](http://_vscodecontentref_/25)     | Author contact information (email)               |
| [license](http://_vscodecontentref_/26)     | License of the template                          |
| [tags](http://_vscodecontentref_/27)        | Array of tags for categorization and search      |
| [created_at](http://_vscodecontentref_/28)  | ISO 8601 timestamp of template creation          |

Metadata is displayed when using `--list` and `--search` options, making it easy to discover and evaluate templates.

## EXAMPLES

### List all available templates

```
genj --list
```

### Search for templates by keyword

```
genj --search java
genj --search spring
genj -s gradle
```

### Generate a Maven project from a template folder

```
genj -t templates/basic-java -d ./out \
  -n Demo \
  -a "Frédéric Delorme" \
  -e fred@example.com \
  -v 0.1.0 \
  -k com.demo \
  -m App
```

### Generate a Gradle project from a ZIP template

```
genj -t template.zip -d ./out \
  -n MyProject \
  -b gradle \
  --gradle_version 8.5
```

### Generate with specific JDK version

```
genj -t templates/basic-java -d ./out \
  -n Demo \
  -j 25 \
  -f 25-zulu
```

### Generate with all parameters

```
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
  --vendor_name "My Company" \
  --verbose
```

### Generate with verbose output for debugging

```
genj -t basic-java -d ./out -n Demo --verbose
```

### Generate with remote git repository

```
genj -t template.zip -d ./out -n MyProject \
  -r https://github.com/myuser/myproject.git
```

## TEMPLATE SEARCH PATHS

When a template name (without path separators) is provided with [--template](http://_vscodecontentref_/29), genj searches in the following order:

1. [templates](http://_vscodecontentref_/30)
2. [~/.genj/](http://_vscodecontentref_/31)

If the template is not found in these directories, it is treated as a file path.

## NOTES

- The program creates the destination directory as `<destination>/<project_name>`.
- If the template is a ZIP file, the script attempts to remove a common root prefix present in the archive.
- Binary files detected (non-text) are copied as is; only text files undergo variable replacements.
- The Git repository is automatically initialized with an initial commit.
- Use [--verbose](http://_vscodecontentref_/32) flag to debug template processing issues.

## AUTHOR

Frédéric Delorme

## VERSION

1.3.1

## SEE ALSO

For more information on creating custom templates, see [genj-template(5)](http://_vscodecontentref_/33).
For more information, visit the project repository or consult the [README.md](http://_vscodecontentref_/34) file.