use crate::cli::Cli;
use chrono::Utc;
use serde_json::json;
use std::fs;
use std::io;
use std::path::Path;
use crate::VERSION;

pub fn write_genrc(dest: &Path, cli: &Cli) -> io::Result<()> {
    let genrc = json!({
        "project_name": cli.project_name,
        "author": cli.author,
        "email": cli.email,
        "project_version": cli.project_version,
        "package": cli.package,
        "mainclass": cli.mainclass,
        "java_version": cli.java,
        "java_flavor": cli.java_flavor,
        "build_tool": cli.build_tool,
        "maven_version": cli.maven_version,
        "gradle_version": cli.gradle_version,
        "vendor_name": cli.vendor_name,
        "template": cli.template,
        "remote_git_repository": cli.remote_git,
        "created_at": Utc::now().to_rfc3339(),
        "generated_with": {
            "cmd": "genj",
            "version": VERSION
        }
    });

    let path = dest.join(".genrc");
    if let Some(p) = path.parent() {
        fs::create_dir_all(p)?;
    }
    fs::write(path, serde_json::to_string_pretty(&genrc)?)?;
    Ok(())
}