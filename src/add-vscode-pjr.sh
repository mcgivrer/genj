#!/bin/bash
export PROJECT_NAME=$1
export GIT_AUTHOR_NAME=$2
export GIT_AUTHOR_EMAIL=$3
export REMOTE_REPO_URL=$4
export STANDALONE_JUNIT_VERSION=1.14.0

# integrate VSCode support
mkdir -p "${PROJECT_NAME}/.vscode";\
cat <<EOL > "${PROJECT_NAME}/.vscode/settings.json"
{
    "java.format.settings.url": ".vscode/java-formatter.xml",
    "java.project.sourcePaths": [
        "src/main/java",
        "src/main/resources",
        "src/test/java",
        "src/test/resources"
    ],
    "java.project.encoding": "warning",
    "java.project.referencedLibraries": [
        "libs/junit-platform-console-standalone-${STANDALONE_JUNIT_VERSION}.jar"
    ],
    "java.project.outputPath": "target/classes"
}
EOL

cat <<EOL > "${PROJECT_NAME}/.vscode/launch.json"
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Current File",
            "request": "launch",
            "mainClass": "${file}"
        },
        {
            "type": "java",
            "name": "App",
            "request": "launch",
            "mainClass": "App",
            "projectName": "${PROJECT_NAME}_53c24221"
        }
    ]
}
EOL

## create git repository
git init -b main --quiet "${PROJECT_NAME}"/
git -C "${PROJECT_NAME}" config user.name "${GIT_AUTHOR_NAME}"
git -C "${PROJECT_NAME}" config user.email "${GIT_AUTHOR_EMAIL}"
git -C "${PROJECT_NAME}" add .
git -C "${PROJECT_NAME}" commit -m "Create Project ${PROJECT_NAME}"

# Pousser vers le dépôt distant si l'URL est fournie
if [[ -n "${REMOTE_REPO_URL}" ]]; then
    git -C "${PROJECT_NAME}" remote add origin "${REMOTE_REPO_URL}"
    git -C "${PROJECT_NAME}" push -f origin main
fi
