use genj::cli::Cli;
use genj::run;
use genj::template::list_available_templates;

fn main() {
    let cli = Cli::parse();
    
    // Handle --list option
    if cli.list {
        list_available_templates();
        return;
    }

    // Validate required options for generation
    if cli.template.is_none() {
        eprintln!("Error: --template is required (unless using --list)");
        eprintln!("Use 'genj --list' to see available templates");
        std::process::exit(1);
    }

    if let Err(e) = run(cli) {
        eprintln!("Error: {}", e);
        std::process::exit(1);
    }
}
