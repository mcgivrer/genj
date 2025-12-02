use genj::cli::Cli;
use genj::run;
use genj::template::{list_available_templates, search_templates};

fn main() {
    let cli = Cli::parse();
    
    // Handle --list option
    if cli.list {
        list_available_templates();
        return;
    }

    // Handle --search option
    if let Some(search_term) = cli.search.as_ref() {
        search_templates(search_term);
        return;
    }

    // Validate required options for generation
    if cli.template.is_none() {
        eprintln!("Error: --template is required (unless using --list or --search)");
        eprintln!("Use 'genj --list' to see available templates");
        eprintln!("Use 'genj --search <term>' to search for templates");
        std::process::exit(1);
    }

    if let Err(e) = run(cli) {
        eprintln!("Error: {}", e);
        std::process::exit(1);
    }
}
