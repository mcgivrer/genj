pub mod cli;
pub mod log;
pub mod fs;
pub mod template;
pub mod genrc;
pub mod vscode_git;

pub const VERSION: &str = env!("CARGO_PKG_VERSION");

use crate::cli::Cli;
use crate::log::{log_info, log_verbose, log_success, log_warning};
use crate::template::process_template;
use crate::genrc::write_genrc;
use crate::vscode_git::setup_vscode_and_git;
use chrono::prelude::*;
use std::io;
use std::path::PathBuf;

pub fn run(cli: Cli) -> io::Result<()> {
    if cli.verbose {
        println!("=== genj - Java Project Generator ===");
        println!("Version: {}", VERSION);
        println!("Verbose mode enabled");
        println!();
    }

    let mut dest_path = PathBuf::from(&cli.destination);
    dest_path.push(&cli.project_name);

    log_verbose(&format!("Destination path will be: {}", dest_path.display()), cli.verbose);

    // Replacements array
    let current_year = Utc::now().year().to_string();
    let replacements = [
        ("${PROJECT_NAME}", cli.project_name.as_str()),
        ("${AUTHOR_NAME}", cli.author.as_str()),
        ("${AUTHOR_EMAIL}", cli.email.as_str()),
        ("${PROJECT_VERSION}", cli.project_version.as_str()),
        ("${PACKAGE}", cli.package.as_str()),
        ("${JAVA}", cli.java.as_str()),
        ("${VENDOR_NAME}", cli.vendor_name.as_str()),
        ("${MAINCLASS}", cli.mainclass.as_str()),
        ("${PROJECT_YEAR}", current_year.as_str()),
    ];

    log_info(&format!("Reading template from: {}", cli.template));

    let template_path = std::path::Path::new(&cli.template);
    process_template(template_path, &dest_path, &replacements, cli.verbose)?;

    // Build files (.pom / build.gradle) and .sdkmanrc
    let build_tool = cli.build_tool.to_lowercase();
    if build_tool != "maven" && build_tool != "gradle" {
        log_warning(&format!("Unsupported build tool: {} (possible values: maven, gradle)", build_tool));
        std::process::exit(1);
    }
    log_info(&format!("Using build tool: {}", build_tool));

    if build_tool == "maven" {
        log_verbose("Generating pom.xml", cli.verbose);
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
</project>"#,
            cli.package, cli.project_name, cli.project_version, cli.java, cli.java
        );
        std::fs::create_dir_all(pom_path.parent().unwrap_or(&dest_path))?;
        std::fs::write(pom_path, pom_content)?;
        log_success("pom.xml generated");
    } else {
        log_verbose("Generating build.gradle", cli.verbose);
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
"#,
            cli.package, cli.project_version
        );
        std::fs::create_dir_all(gradle_path.parent().unwrap_or(&dest_path))?;
        std::fs::write(gradle_path, gradle_content)?;
        log_success("build.gradle generated");
    }

    // .sdkmanrc
    log_verbose("Generating .sdkmanrc", cli.verbose);
    let sdkman_file = dest_path.join(".sdkmanrc");
    let mut sdkman_content = format!("java={}\n", cli.java_flavor);
    if build_tool == "maven" {
        sdkman_content.push_str(&format!("maven={}\n", cli.maven_version));
    } else {
        sdkman_content.push_str(&format!("gradle={}\n", cli.gradle_version));
    }
    std::fs::create_dir_all(sdkman_file.parent().unwrap_or(&dest_path))?;
    std::fs::write(sdkman_file, sdkman_content)?;
    log_success(".sdkmanrc generated");

    // .genrc
    log_verbose("Generating .genrc", cli.verbose);
    write_genrc(&dest_path, &cli)?;
    log_success(".genrc configuration file generated");

    // VSCode + Git
    log_info("Configuring VSCode and Git repository...");
    if let Err(e) = setup_vscode_and_git(&dest_path, &cli) {
        log_warning(&format!("Error during VSCode/Git configuration: {}", e));
    }

    log_success(&format!("Java project '{}' generated successfully in {}", cli.project_name, dest_path.display()));

    if cli.verbose {
        println!();
        println!("=== Generation Summary ===");
        println!("Project Name: {}", cli.project_name);
        println!("Package: {}", cli.package);
        println!("Build Tool: {}", build_tool);
        println!("Java Version: {}", cli.java);
        println!("Location: {}", dest_path.display());
    }

    Ok(())
}