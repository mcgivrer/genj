use clap::Parser;

pub const VERSION: &str = env!("CARGO_PKG_VERSION");

#[derive(Parser, Debug, Clone)]
#[command(
    author = "Frédéric Delorme",
    version = VERSION,
    about = "This script generates a Java project based on the specified template files.
It creates the necessary directory structure, copies the templates, replaces
placeholders in the templates with the provided values, and generates additional
files such as MANIFEST.MF and README.md."
)]
pub struct Cli {
    #[arg(
        short = 't',
        long = "template",
        help = "Path to the template (ZIP or folder). Default search paths: /usr/share/genj/templates, ~/.genj/"
    )]
    pub template: Option<String>,
    #[arg(short = 'd', long = "destination", help = "Destination directory (default: current directory)")]
    pub destination: Option<String>,
    #[arg(short = 'n', long = "project_name", default_value = "Demo")]
    pub project_name: String,
    #[arg(short = 'a', long = "author", default_value = "Unknown Author")]
    pub author: String,
    #[arg(short = 'e', long = "email", default_value = "email@unknown.local")]
    pub email: String,
    #[arg(short = 'v', long = "project_version", default_value = "0.0.1")]
    pub project_version: String,
    #[arg(short = 'j', long = "java_version", help = "JDK version", default_value = "25")]
    pub java: String,
    #[arg(short = 'f', long = "java_flavor", help = "JDK flavor (for sdkman)", default_value = "25-zulu")]
    pub java_flavor: String,
    #[arg(short = 'k', long = "package", default_value = "com.demo")]
    pub package: String,
    #[arg(short = 'm', long = "mainclass", default_value = "App")]
    pub mainclass: String,
    #[arg(short = 'b', long = "build", help = "Build tool (maven or gradle)", default_value = "maven")]
    pub build_tool: String,
    #[arg(long = "maven_version", default_value = "3.9.5")]
    pub maven_version: String,
    #[arg(long = "gradle_version", default_value = "8.5")]
    pub gradle_version: String,
    #[arg(short = 'l', long = "vendor_name", default_value = "Vendor")]
    pub vendor_name: String,
    #[arg(short = 'r', long = "remote_git_repository", help = "Define the remote git repository for this project")]
    pub remote_git: Option<String>,
    #[arg(long = "verbose", help = "Enable verbose output for debugging", action = clap::ArgAction::SetTrue)]
    pub verbose: bool,
    #[arg(long = "list", help = "List available templates in /usr/share/genj/templates and ~/.genj/", action = clap::ArgAction::SetTrue)]
    pub list: bool,
    #[arg(short = 's', long = "search", help = "Search for templates by name or metadata (description, tags, language, author)")]
    pub search: Option<String>,
}

impl Cli {
    /// Provide an inherent parse() method delegating to clap's Parser implementation.
    pub fn parse() -> Self {
        <Self as clap::Parser>::parse()
    }
}
