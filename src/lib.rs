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
use std::path::{PathBuf, Path};

/// Resolve template path from CLI option or default search paths
fn resolve_template_path(template_opt: &Option<String>) -> io::Result<PathBuf> {
    match template_opt {
        Some(t) => {
            let path = Path::new(t);
            if path.exists() {
                Ok(path.to_path_buf())
            } else {
                Err(io::Error::new(
                    io::ErrorKind::NotFound,
                    format!("Template not found: {}", t),
                ))
            }
        }
        None => Err(io::Error::new(
            io::ErrorKind::InvalidInput,
            "Template is required (use --list to see available templates)",
        )),
    }
}

/// Resolve destination path from CLI option or use current directory
fn resolve_destination_path(destination_opt: &Option<String>) -> PathBuf {
    match destination_opt {
        Some(d) => PathBuf::from(d),
        None => PathBuf::from("."),
    }
}

pub fn run(cli: Cli) -> io::Result<()> {
    if cli.verbose {
        println!("=== genj - Java Project Generator ===");
        println!("Version: {}", VERSION);
        println!("Verbose mode enabled");
        println!();
    }

    // Resolve template path
    let template_path = resolve_template_path(&cli.template)?;
    let mut dest_path = resolve_destination_path(&cli.destination);
    dest_path.push(&cli.project_name);

    log_verbose(&format!("Template: {}", template_path.display()), cli.verbose);
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

    log_info(&format!("Reading template from: {}", template_path.display()));
    process_template(&template_path, &dest_path, &replacements, cli.verbose)?;

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
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.4.1</version>
        <configuration>
          <archive>
            <manifest>
              <mainClass>{}.{}</mainClass>
            </manifest>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>"#,
            cli.package, cli.project_name, cli.project_version, cli.java, cli.java, cli.package, cli.mainclass  
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