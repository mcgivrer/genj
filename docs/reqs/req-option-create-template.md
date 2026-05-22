# UC100 - Create a project Template

## UC101. Create a new template as a directory

With the option `--create-template`, genj create a new directorywith the .genrc file and a default README.

```bash
genj --create-template \
--name my-new-template \
--language "Java" \
--author "Frédéric Delorme" \
--email frederic.delorme@gmail.com \
--tags "basic,java" \
--description "A new Template for genj project."
```

this command will create a new directory named `my-new-template` with a single file `.template`:

The `.template` file content:

```json
{
    "name": "my-new-template",
    "version": "1.0.0",
    "language": "Java",
    "tags": [
        "basic",
        "java"
    ],
    "description": "A new Template for genj project.",
    "author": "Frédéric Delorme",
    "contact": "frederic.delorme@gmail.com",
    "created_at": "2025-12-02T21:56:00Z",
    "license": "MIT"
}
```

> [!NOTE]
> If no version attribute is defined, use `1.0.0` as version value.
> If no License type is defined, use `MIT` as License value

## UC102. Convert an existing projet into a template

The option `--convert-to-template` will covert an exoisrting directory projest into a template by copying its structure and generating a `.template`

The template will be created into `~/.genj/templates`.

Example :

You execute the `genj` command into that following java project folder

```
MyProject
 |_ src
 |  |_ main
 |  |_ test
 |_ README.md
 |_ LICENSE
 |_ pom.xml
```

The command line :

```bash
genj --convert-to-template \
--name my-new-template \
--language "Java" \
--build "maven"
--author "Frédéric Delorme" \
--email frederic.delorme@gmail.com \
--tags "basic,java,game" \
--description "A converted project as genj template." \
--license "MIT"
```

The generated `.template` file will be:

```json
{
    "name": "my-new-template",
    "version": "1.0.0",
    "language": "Java",
    "tags": [
        "basic",
        "java",
        "game"
    ],
    "description": "A converted project as genj template.",
    "author": "Frédéric Delorme",
    "contact": "frederic.delorme@gmail.com",
    "created_at": "2025-12-02T21:56:00Z",
    "license": "MIT"
}
```

As the `--build` parameter has been set as "maven", the `pom.xml` file will be parsed to replace the following maven project values, to be prepared as a `pom.xml` template file with placeholders:

Artifact naming :

- group name (groupId) : `${PACKAGE}`
- project name (artifactId) : `${PROJECT_NAME}`
- project version (version) : `${PROJECT_VERSION}`

Other parameters:

- main class (if exists) : `${MAINCLASS}`
- contributor name (if exists) : `${AUTHOR_NAME}`
- contributor email (if exists) : `${AUTHOR_EMAIL}`
- contributor org (if exists): `${VENDOR_NAME}`

> [!IMPORTANT]
> The user converting that project will be responsible for replacing relevant placeholders like `${PACKAGE}` and `${MAIN_CLASS}`  in the source code to ensure proper functionality and adaptability of the generated template.
> The `${PACKAGE}` placeholder will be used to rename and replace root package in the directory and in all the source code.

The create/converted templates are intialized into the `~/.genj/templates` folder.

## UC103. Promote a user template as a global template

When you are satisfied by the creaed/converted template, you can promote it.

Just execute the following command:

```bash
genj --promote my-new-template
```

the package will be copied to `/usr/share/genj/templates` path, if it does not exists with the same version.

> [!WARNING]
> Only one version at a time can be stored as a promoted template.
