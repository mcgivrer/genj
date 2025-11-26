use crate::cli::Cli;
use crate::log::{log_success, log_verbose};
use git2::Repository;
use serde_json::json;
use std::fs::{create_dir_all, write};
use std::io;
use std::path::Path;

pub fn setup_vscode_and_git(dest: &Path, cli: &Cli) -> io::Result<()> {
    // VSCode config
    log_verbose("Creating VSCode configuration", cli.verbose);
    let vscode_dir = dest.join(".vscode");
    create_dir_all(&vscode_dir)?;

    let settings = json!({
        "java.format.settings.url": ".vscode/java-formatter.xml",
        "java.project.sourcePaths": ["src/main/java","src/main/resources","src/test/java","src/test/resources"],
        "java.project.encoding": "warning",
        "java.project.outputPath": "target/classes"
    });
    write(
        vscode_dir.join("settings.json"),
        serde_json::to_string_pretty(&settings)?,
    )?;
    log_success(".vscode/settings.json created");

    let launch = json!({
        "version": "0.2.0",
        "configurations": [
            {
                "type": "java",
                "name": "Run",
                "request": "launch",
                "mainClass": cli.mainclass,
                "projectName": cli.project_name
            }
        ]
    });
    write(
        vscode_dir.join("launch.json"),
        serde_json::to_string_pretty(&launch)?,
    )?;
    log_success(".vscode/launch.json created");

    // Initialize Git
    log_verbose("Initializing Git repository", cli.verbose);
    let repo = Repository::init(dest)
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

    let mut config = repo
        .config()
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    config
        .set_str("init.defaultBranch", "main")
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    config
        .set_str("user.name", &cli.author)
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    config
        .set_str("user.email", &cli.email)
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

    let mut index = repo
        .index()
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    index
        .add_all(["."].iter(), git2::IndexAddOption::DEFAULT, None)
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    index
        .write()
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

    let tree_id = index
        .write_tree()
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    let tree = repo
        .find_tree(tree_id)
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    let sig = repo
        .signature()
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
    repo.commit(
        Some("HEAD"),
        &sig,
        &sig,
        &format!("Create Project {}", cli.project_name),
        &tree,
        &[],
    )
    .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

    log_success("Git repository initialized with initial commit");

    if let Some(url) = &cli.remote_git {
        log_verbose(
            &format!("Configuring remote repository: {}", url),
            cli.verbose,
        );
        repo.remote("origin", url)
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
        let mut remote = repo
            .find_remote("origin")
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
        remote
            .push(&["refs/heads/main:refs/heads/main"], None)
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
        log_success("Pushed to remote repository");
    }

    Ok(())
}
