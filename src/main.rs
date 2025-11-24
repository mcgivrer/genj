use chrono::prelude::*;
use clap::Parser;
use git2::{Repository};
use serde_json::{Value, json};
use std::fs::File;
use std::fs::{copy, create_dir_all, read_to_string, write};
use std::io::{self, Read};
use std::path::{Path, PathBuf};
use walkdir::WalkDir;
use zip::ZipArchive;

#[derive(Parser, Debug)]
#[command(
    author = "Frédéric Delorme",
    version = "1.2.1",
    about = "This script generates a Java project based on the specified template files.
It creates the necessary directory structure, copies the templates, replaces
placeholders in the templates with the provided values, and generates additional
files such as MANIFEST.MF and README.md."
)]
struct Cli {
    #[arg(short = 'd', long, help = "Path to the template (ZIP or folder)")]
    template: String,
    #[arg(short, long, help = "Destination directory")]
    destination: String,
    #[arg(short = 'n', long = "project_name", default_value = "Demo")]
    project_name: String,
    #[arg(short = 'a', long = "author", default_value = "Unknown Author")]
    author: String,
    #[arg(short = 'e', long = "email", default_value = "email@unknown.local")]
    email: String,
    #[arg(short = 'v', long = "project_version", default_value = "0.0.1")]
    project_version: String,
    #[arg(
        short = 'j',
        long = "java_version",
        help = "JDK version",
        default_value = "25"
    )]
    java: String,
    #[arg(
        short = 'f',
        long = "java_flavor",
        help = "JDK flavor (for sdkman)",
        default_value = "25-zulu"
    )]
    java_flavor: String,
    #[arg(short = 'k', long = "package", default_value = "com.demo")]
    package: String,
    #[arg(short = 'm', long = "mainclass", default_value = "App")]
    mainclass: String,
    #[arg(
        short = 'b',
        long = "build",
        help = "Build tool (maven or gradle)",
        default_value = "maven"
    )]
    build_tool: String,
    #[arg(long = "maven_version", default_value = "3.9.5")]
    maven_version: String,
    #[arg(long = "gradle_version", default_value = "8.5")]
    gradle_version: String,
    #[arg(
        short = 'l',
        long = "vendor_name",
        help = "Vendor name (for manifest)",
        default_value = "Vendor"
    )]
    vendor_name: String,
    #[arg(
        short = 'r',
        long = "remote_git_repository",
        help = "Defini the repository git distant pour ce projet"
    )]
    remote_git: Option<String>,
}

fn main() -> io::Result<()> {
    let args = Cli::parse();
    let package_val = &args.package;
    let mainclass_val = &args.mainclass;

    let mut dest_path = PathBuf::from(&args.destination);

    dest_path = dest_path.join(&args.project_name);

    let current_year = Utc::now().year().to_string();

//    let remote_git_repo = Some(args.remote_git).as_ref();

    let replacements = [
        ("${PROJECT_NAME}", args.project_name.as_str()),
        ("${AUTHOR_NAME}", args.author.as_str()),
        ("${AUTHOR_EMAIL}", args.email.as_str()),
        ("${PROJECT_VERSION}", args.project_version.as_str()),
        ("${PACKAGE}", package_val.as_str()),
        ("${JAVA}", args.java.as_str()),
        ("${VENDOR_NAME}", args.vendor_name.as_str()),
        ("${MAINCLASS}", mainclass_val.as_str()),
        ("${PROJECT_YEAR}", current_year.as_str()),
//        ("${REMOTE_GIT_REPO}", remote_git_repo.as_deref().as_str()),
    ];

    let template_path = Path::new(&args.template);

    if template_path.is_file() {
        // ZIP extraction
        extract_zip_with_replace(template_path, &dest_path, &replacements)?;
    } else if template_path.is_dir() {
        // Copy directory with replace
        copy_dir_with_replace(template_path, &dest_path, &replacements)?;
    } else {
        eprintln!("Invalid template path");
        std::process::exit(1);
    }

    // Build tool processing
    let build_tool = args.build_tool.to_lowercase();
    if build_tool != "maven" && build_tool != "gradle" {
        eprintln!(
            "Unsupported build tool: {} (possible values: maven, gradle)",
            build_tool
        );
        std::process::exit(1);
    }

    // Add pom.xml or build.gradle file
    if build_tool == "maven" {
        // Ex: simple Maven project
        let pom_path = dest_path.join("pom.xml");
        let pom_content = format!(
            r#"<project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>{}</groupId>
        <artifactId>{}</artifactId>
        <version>{}</version>
        <properties>
            <maven.compiler.target>{}</maven.compiler.target>
            <maven.compiler.source>{}</maven.compiler.source>
        </properties>
        <dependencies>
            <!-- https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone -->
            <dependency>
                <groupId>org.junit.platform</groupId>
                <artifactId>junit-platform-console-standalone</artifactId>
                <version>6.0.0</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </project>"#,
            args.package, args.project_name, args.project_version, args.java, args.java
        );
        write(pom_path, pom_content)?;
    } else if build_tool == "gradle" {
        // Ex: simple Gradle project
        let gradle_path = dest_path.join("build.gradle");
        let gradle_content = format!(
            r#"plugins {{
        id 'java'
    }}
    group '{}'
    version '{}'
    repositories {{
        mavenCentral()
    }}
    dependencies {{}}
    "#,
            args.package, args.project_version
        );
        write(gradle_path, gradle_content)?;
    }

    // Update .sdkman file

    let sdkman_file = dest_path.join(".sdkmanrc");
    let mut sdkman_content = format!("java={}\n", args.java_flavor);

    if build_tool == "maven" {
        sdkman_content.push_str(&format!("maven={}\n", args.maven_version));
    } else if build_tool == "gradle" {
        sdkman_content.push_str(&format!("gradle={}\n", args.gradle_version));
    }
    write(sdkman_file, sdkman_content)?;

    // Configure VSCode and Git
    let vscode_config = VsCodeConfig::new(
        args.project_name.clone(),
        args.mainclass.clone(),
        args.author.clone(),
        args.email.clone(),
        None,                 // or a remote repository URL if necessary
        "1.14.0".to_string(), // JUnit version
    );

    if let Err(e) = vscode_config.setup_project(&dest_path) {
        eprintln!("Error during VSCode/Git configuration: {}", e);
    }

    println!("Java project generated in {}", dest_path.display());
    Ok(())
}

fn replace_package_in_path(
    path_str: &str,
    replacements: &[(&str, &str)],
    package_val: &str,
) -> PathBuf {
    let path_parts: Vec<&str> = path_str.split('/').collect();
    let mut final_path = PathBuf::new();

    for part in path_parts {
        if part == "${PACKAGE}" {
            for seg in package_val.split('.') {
                final_path.push(seg);
            }
        } else {
            let mut replaced = part.to_string();
            for (pat, val) in replacements {
                replaced = replaced.replace(pat, val);
            }
            final_path.push(replaced);
        }
    }
    final_path
}

// Detects if a byte buffer looks like text.
fn is_text_bytes(buf: &[u8]) -> bool {
    // If we find a NUL byte, it's very likely binary
    if buf.iter().any(|&b| b == 0) {
        return false;
    }

    // Try simple UTF-8 validation
    if std::str::from_utf8(buf).is_ok() {
        return true;
    }

    // If not UTF-8, consider as binary
    false
}

// Reads a small prefix of the file to decide if it's text
fn is_text_path(path: &Path) -> io::Result<bool> {
    let mut file = File::open(path)?;
    let mut buf = [0u8; 8192];
    let n = file.read(&mut buf)?;
    Ok(is_text_bytes(&buf[..n]))
}

fn extract_zip_with_replace(
    zip_path: &Path,
    dest_path: &Path,
    replacements: &[(&str, &str)],
) -> io::Result<()> {
    let file = File::open(zip_path)?;
    let mut archive = ZipArchive::new(file)?;

    // Collect entry names to avoid multiple mutable borrows
    let entry_names: Vec<String> = (0..archive.len())
        .map(|i| archive.by_index(i).map(|e| e.name().to_string()))
        .collect::<Result<_, _>>()?;

    // Detection of common root prefix
    let common_prefix = entry_names
        .iter()
        .filter_map(|name| name.find('/').map(|pos| &name[..pos + 1]))
        .reduce(|a, b| if a == b { a } else { "" })
        .filter(|s| !s.is_empty());

    for i in 0..archive.len() {
        let mut entry = archive.by_index(i)?;
        let raw_name = entry.name().to_string();

        // Remove common root prefix
        let relative_path = if let Some(prefix) = common_prefix {
            raw_name
                .as_str()
                .strip_prefix(prefix)
                .unwrap_or(raw_name.as_str())
        } else {
            raw_name.as_str()
        };

        // Use replace_package_in_path to handle ${PACKAGE}
        let outpath = replace_package_in_path(
            relative_path,
            replacements,
            replacements
                .iter()
                .find(|(key, _)| *key == "${PACKAGE}")
                .map(|(_, val)| *val)
                .unwrap_or(""),
        );

        let full_path = dest_path.join(outpath);

        if raw_name.ends_with('/') {
            create_dir_all(&full_path)?;
        } else {
            if let Some(parent) = full_path.parent() {
                create_dir_all(parent)?;
            }

            // Read as bytes then detect if it's text
            let mut bytes: Vec<u8> = Vec::new();
            entry.read_to_end(&mut bytes)?;

            if !is_text_bytes(&bytes) {
                // Copy binary files as is
                write(&full_path, &bytes)?;
                continue;
            }

            // Here we assume it's UTF-8
            let content = String::from_utf8(bytes).unwrap_or_default();
            let replaced_content = replacements
                .iter()
                .fold(content, |acc, (pat, val)| acc.replace(pat, val));
            write(&full_path, replaced_content)?;
        }
    }
    Ok(())
}

fn copy_dir_with_replace(
    src_dir: &Path,
    dest_dir: &Path,
    replacements: &[(&str, &str)],
) -> io::Result<()> {
    // Extract the value corresponding to ${PACKAGE} to use in path construction
    let package_val = replacements
        .iter()
        .find(|(key, _)| *key == "${PACKAGE}")
        .map(|(_, val)| *val)
        .unwrap_or("");

    for entry in WalkDir::new(src_dir).into_iter().filter_map(Result::ok) {
        let rel_path = entry.path().strip_prefix(src_dir).unwrap();
        let rel_path_str = rel_path.to_string_lossy();

        // Use replace_package_in_path to handle ${PACKAGE} as subdirectories
        let new_path = replace_package_in_path(&rel_path_str, replacements, package_val);

        let full_dest_path = dest_dir.join(new_path);

        if entry.file_type().is_dir() {
            create_dir_all(&full_dest_path)?;
        } else if entry.file_type().is_file() {
            // Skip binary files
            match is_text_path(entry.path()) {
                Ok(true) => {
                    if let Some(parent) = full_dest_path.parent() {
                        create_dir_all(parent)?;
                    }
                    let content = read_to_string(entry.path())?;
                    let replaced_content = replacements
                        .iter()
                        .fold(content, |acc, (pat, val)| acc.replace(pat, val));
                    write(full_dest_path, replaced_content)?;
                }
                Ok(false) => {
                    // Copy binary files as is
                    if let Some(parent) = full_dest_path.parent() {
                        create_dir_all(parent)?;
                    }
                    match copy(entry.path(), &full_dest_path) {
                        Ok(_) => { /* copied */ }
                        Err(err) => eprintln!(
                            "Failed to copy binary file {}: {}",
                            entry.path().display(),
                            err
                        ),
                    }
                    continue;
                }
                Err(err) => {
                    eprintln!("Error reading file {}: {}", entry.path().display(), err);
                    continue;
                }
            }
        }
    }
    Ok(())
}

// Structure for VSCode configurations
#[derive(Debug)]
struct VsCodeConfig {
    project_name: String,
    main_class: String,
    git_author_name: String,
    git_author_email: String,
    remote_repo_url: Option<String>,
    standalone_junit_version: String,
}

impl VsCodeConfig {
    fn new(
        project_name: String,
        main_class: String,
        git_author_name: String,
        git_author_email: String,
        remote_repo_url: Option<String>,
        standalone_junit_version: String,
    ) -> Self {
        Self {
            project_name,
            main_class,
            git_author_name,
            git_author_email,
            remote_repo_url,
            standalone_junit_version,
        }
    }

    fn generate_settings_json(&self) -> Value {
        json!({
            "java.format.settings.url": ".vscode/java-formatter.xml",
            "java.project.sourcePaths": [
                "src/main/java",
                "src/main/resources",
                "src/test/java",
                "src/test/resources"
            ],
            "java.project.encoding": "warning",
            "java.project.referencedLibraries": [
                format!("libs/junit-platform-console-standalone-{}.jar", self.standalone_junit_version)
            ],
            "java.project.outputPath": "target/classes"
        })
    }

    fn generate_launch_json(&self) -> Value {
        json!({
            "version": "0.2.0",
            "configurations": [
                {
                    "type": "java",
                    "name": "App",
                    "request": "launch",
                    "mainClass": format!("{}_{}", self.main_class,"com.demo.App"),
                    "projectName": format!("{}_{}", self.project_name,"DemoGenJ")
                },
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
                    "projectName": format!("{}_{}", self.project_name, "53c24221")
                }
            ]
        })
    }

    fn init_git_repository(&self, project_path: &Path) -> io::Result<()> {
        // Initialize Git repository
        let repo = Repository::init(project_path)
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

        let mut config = repo
            .config()
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

        // Set default branch (e.g., "main")
        config
            .set_str("init.defaultBranch", "main")
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
        // Configure Git user
        config
            .set_str("user.name", &self.git_author_name)
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
        config
            .set_str("user.email", &self.git_author_email)
            .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

        // Add all files and make the first commit
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
            &format!("Create Project {}", self.project_name),
            &tree,
            &[],
        )
        .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

        // Configure remote repository if URL is provided
        if let Some(url) = &self.remote_repo_url {
            repo.remote("origin", url)
                .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

            // Push to remote repository
            let mut remote = repo
                .find_remote("origin")
                .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;

            remote
                .push(&["refs/heads/main:refs/heads/main"], None)
                .map_err(|e| io::Error::new(io::ErrorKind::Other, e.message().to_string()))?;
        }

        Ok(())
    }

    fn setup_vscode_config(&self, project_path: &Path) -> io::Result<()> {
        let vscode_dir = project_path.join(".vscode");
        create_dir_all(&vscode_dir)?;

        // Generate settings.json
        let settings_json = self.generate_settings_json();
        let settings_path = vscode_dir.join("settings.json");
        write(
            &settings_path,
            serde_json::to_string_pretty(&settings_json)?,
        )?;

        // Generate launch.json
        let launch_json = self.generate_launch_json();
        let launch_path = vscode_dir.join("launch.json");
        write(&launch_path, serde_json::to_string_pretty(&launch_json)?)?;

        Ok(())
    }

    fn setup_project(&self, project_path: &Path) -> io::Result<()> {
        // Create VSCode directories and configure
        self.setup_vscode_config(project_path)?;

        // Initialize Git repository and configure
        self.init_git_repository(project_path)?;

        Ok(())
    }
}