use crate::fs::{is_text_path, write_bytes};
use crate::log::{log_verbose, log_warning};
use std::fs::{self, create_dir_all, read_to_string, copy, File};
use std::io::{self, Read};
use std::path::{Path, PathBuf};
use walkdir::WalkDir;
use zip::ZipArchive;
use serde_json::Value;

/// Main entry: process template path (file or dir)
pub fn process_template(
    template_path: &Path,
    dest_path: &Path,
    replacements: &[(&str, &str)],
    verbose: bool,
) -> io::Result<()> {
    if template_path.is_file() {
        log_verbose("Template detected as ZIP file", verbose);
        extract_zip_with_replace(template_path, dest_path, replacements, verbose)?;
    } else if template_path.is_dir() {
        log_verbose("Template detected as directory", verbose);
        copy_dir_with_replace(template_path, dest_path, replacements, verbose)?;
    } else {
        return Err(io::Error::new(io::ErrorKind::NotFound, "Template path not found"));
    }
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

fn is_text_bytes(buf: &[u8]) -> bool {
    if buf.iter().any(|&b| b == 0) { return false; }
    std::str::from_utf8(buf).is_ok()
}

fn extract_zip_with_replace(
    zip_path: &Path,
    dest_path: &Path,
    replacements: &[(&str, &str)],
    verbose: bool,
) -> io::Result<()> {
    log_verbose(&format!("Opening ZIP file: {}", zip_path.display()), verbose);
    let f = File::open(zip_path)?;
    let mut archive = ZipArchive::new(f)?;

    // collect names
    let entry_names: Vec<String> = (0..archive.len())
        .map(|i| archive.by_index(i).map(|e| e.name().to_string()))
        .collect::<Result<_, _>>()?;

    let common_prefix = entry_names
        .iter()
        .filter_map(|name| name.find('/').map(|pos| &name[..pos + 1]))
        .fold(None::<String>, |acc, p| {
            match acc {
                None => Some(p.to_string()),
                Some(ref a) if a == p => Some(a.clone()),
                _ => Some(String::new()),
            }
        })
        .filter(|s| !s.is_empty());

    if let Some(prefix) = common_prefix.as_ref() {
        log_verbose(&format!("Detected common root prefix: {}", prefix), verbose);
    }

    for i in 0..archive.len() {
        let mut entry = archive.by_index(i)?;
        let raw_name = entry.name().to_string();

        let relative_path = if let Some(prefix) = common_prefix.as_ref() {
            raw_name.as_str().strip_prefix(prefix).unwrap_or(raw_name.as_str())
        } else {
            raw_name.as_str()
        };

        let package_val = replacements
            .iter()
            .find(|(k, _)| *k == "${PACKAGE}")
            .map(|(_, v)| *v)
            .unwrap_or("");

        let outpath = replace_package_in_path(relative_path, replacements, package_val);
        let full_path = dest_path.join(outpath);

        if raw_name.ends_with('/') {
            create_dir_all(&full_path)?;
            log_verbose(&format!("Created directory: {}", full_path.display()), verbose);
            continue;
        }

        if let Some(parent) = full_path.parent() {
            create_dir_all(parent)?;
        }

        let mut bytes: Vec<u8> = Vec::new();
        entry.read_to_end(&mut bytes)?;

        if !is_text_bytes(&bytes) {
            write_bytes(&full_path, &bytes)?;
            log_verbose(&format!("Copied binary file: {}", full_path.display()), verbose);
            continue;
        }

        let content = String::from_utf8(bytes).unwrap_or_default();
        let replaced = replacements.iter().fold(content, |acc, (pat, val)| acc.replace(pat, val));
        std::fs::write(full_path, replaced)?;
        log_verbose(&format!("Extracted and replaced: {}", raw_name), verbose);
    }

    Ok(())
}

fn copy_dir_with_replace(
    src_dir: &Path,
    dest_dir: &Path,
    replacements: &[(&str, &str)],
    verbose: bool,
) -> io::Result<()> {
    log_verbose(&format!("Scanning source directory: {}", src_dir.display()), verbose);

    let package_val = replacements.iter().find(|(k, _)| *k == "${PACKAGE}").map(|(_, v)| *v).unwrap_or("");

    for entry in WalkDir::new(src_dir).into_iter().filter_map(Result::ok) {
        let rel = entry.path().strip_prefix(src_dir).unwrap();
        let rel_str = rel.to_string_lossy();
        let new_path = replace_package_in_path(&rel_str, replacements, package_val);
        let full_dest_path = dest_dir.join(new_path);

        if entry.file_type().is_dir() {
            create_dir_all(&full_dest_path)?;
            log_verbose(&format!("Created directory: {}", full_dest_path.display()), verbose);
            continue;
        }

        if entry.file_type().is_file() {
            match is_text_path(entry.path()) {
                Ok(true) => {
                    if let Some(parent) = full_dest_path.parent() {
                        create_dir_all(parent)?;
                    }
                    let content = read_to_string(entry.path())?;
                    let replaced = replacements.iter().fold(content, |acc, (pat, val)| acc.replace(pat, val));
                    std::fs::write(&full_dest_path, replaced)?;
                    log_verbose(&format!("Copied and replaced: {}", full_dest_path.display()), verbose);
                }
                Ok(false) => {
                    if let Some(parent) = full_dest_path.parent() {
                        create_dir_all(parent)?;
                    }
                    match copy(entry.path(), &full_dest_path) {
                        Ok(_) => { log_verbose(&format!("Copied binary file: {}", full_dest_path.display()), verbose); }
                        Err(err) => { log_warning(&format!("Failed to copy binary file {}: {}", entry.path().display(), err)); }
                    }
                }
                Err(err) => {
                    log_warning(&format!("Error reading file {}: {}", entry.path().display(), err));
                }
            }
        }
    }

    log_verbose("Template copy complete", verbose);
    Ok(())
}

/// Extract .template metadata from a ZIP file
fn extract_template_metadata(zip_path: &Path) -> Option<Value> {
    let f = match File::open(zip_path) {
        Ok(file) => file,
        Err(_) => return None,
    };
    
    let mut archive = match ZipArchive::new(f) {
        Ok(a) => a,
        Err(_) => return None,
    };
    
    // Find .template file in the archive
    for i in 0..archive.len() {
        let mut entry = match archive.by_index(i) {
            Ok(e) => e,
            Err(_) => continue,
        };
        
        if entry.name() == ".template" || entry.name().ends_with("/.template") {
            let mut content = String::new();
            if entry.read_to_string(&mut content).is_ok() {
                if let Ok(metadata) = serde_json::from_str::<Value>(&content) {
                    return Some(metadata);
                }
            }
            break;
        }
    }
    
    None
}

/// Extract .template metadata from a directory
fn extract_template_metadata_from_dir(dir_path: &Path) -> Option<Value> {
    let template_file = dir_path.join(".template");
    if template_file.exists() {
        if let Ok(content) = read_to_string(&template_file) {
            if let Ok(metadata) = serde_json::from_str::<Value>(&content) {
                return Some(metadata);
            }
        }
    }
    None
}

/// List available templates from system and user directories with metadata
pub fn list_available_templates() {
    let system_path = Path::new("/usr/share/genj/templates");
    let home_dir = dirs::home_dir().map(|h| h.join(".genj"));
    
    println!("=== Available Templates ===\n");
    
    // List system templates
    println!("📦 System templates (/usr/share/genj/templates):");
    list_templates_in_dir_with_metadata(system_path, "/usr/share/genj/templates");
    
    // List user templates
    if let Some(user_path) = home_dir {
        println!("\n👤 User templates (~/.genj):");
        let home_path_str = format!("{}/.genj", dirs::home_dir().map(|h| h.display().to_string()).unwrap_or_else(|| "~".to_string()));
        list_templates_in_dir_with_metadata(&user_path, &home_path_str);
    }
    
    println!("\n💡 Usage: genj -t <template_name_or_path> -d <destination> [options]");
    println!("   Or: genj -t /usr/share/genj/templates/basic-java.zip -d ./out -n MyProject");
}

/// Search for templates matching a search term in name and metadata
pub fn search_templates(search_term: &str) {
    let system_path = Path::new("/usr/share/genj/templates");
    let home_dir = dirs::home_dir().map(|h| h.join(".genj"));
    
    let search_lower = search_term.to_lowercase();
    let mut results_found = false;
    
    println!("=== Search Results for: '{}' ===\n", search_term);
    
    // Search system templates
    if system_path.exists() {
        println!("📦 System templates (/usr/share/genj/templates):");
        if search_templates_in_dir(&system_path, &search_lower, "/usr/share/genj/templates") {
            results_found = true;
        }
    }
    
    // Search user templates
    if let Some(user_path) = home_dir {
        if user_path.exists() {
            println!("👤 User templates (~/.genj):");
            let home_path_str = format!("{}/.genj", dirs::home_dir().map(|h| h.display().to_string()).unwrap_or_else(|| "~".to_string()));
            if search_templates_in_dir(&user_path, &search_lower, &home_path_str) {
                results_found = true;
            }
        }
    }
    
    if !results_found {
        println!("  No templates found matching '{}'", search_term);
    }
    
    println!("\n💡 Usage: genj -t <template_name_or_path> -d <destination> [options]");
    println!("   Or: genj -t /usr/share/genj/templates/basic-java.zip -d ./out -n MyProject");
}

/// Search templates in a specific directory
fn search_templates_in_dir(path: &Path, search_term: &str, _display_path: &str) -> bool {
    if !path.exists() {
        return false;
    }
    
    let mut results_found = false;
    
    match fs::read_dir(path) {
        Ok(entries) => {
            let mut templates: Vec<_> = entries
                .filter_map(|e| e.ok())
                .filter_map(|e| {
                    let path = e.path();
                    let name = path.file_name()?.to_string_lossy().to_string();
                    if path.is_file() && name.ends_with(".zip") {
                        Some((name, path, true))
                    } else if path.is_dir() {
                        Some((format!("{}/", name), path, false))
                    } else {
                        None
                    }
                })
                .collect();
            
            templates.sort_by(|a, b| a.0.cmp(&b.0));
            
            for (name, path, is_file) in templates {
                let metadata = if is_file {
                    extract_template_metadata(&path)
                } else {
                    extract_template_metadata_from_dir(&path)
                };
                
                // Check if name matches
                let name_matches = name.to_lowercase().contains(search_term);
                
                // Check if any metadata field matches
                let mut metadata_matches = false;
                if let Some(metadata) = &metadata {
                    let searchable_fields = vec![
                        metadata.get("description").and_then(|v| v.as_str()),
                        metadata.get("language").and_then(|v| v.as_str()),
                        metadata.get("author").and_then(|v| v.as_str()),
                        metadata.get("version").and_then(|v| v.as_str()),
                        metadata.get("contact").and_then(|v| v.as_str()),
                        metadata.get("license").and_then(|v| v.as_str()),
                    ];
                    
                    for field in searchable_fields {
                        if let Some(value) = field {
                            if value.to_lowercase().contains(search_term) {
                                metadata_matches = true;
                                break;
                            }
                        }
                    }
                    
                    // Check tags array
                    if !metadata_matches {
                        if let Some(tags) = metadata.get("tags").and_then(|v| v.as_array()) {
                            for tag in tags {
                                if let Some(tag_str) = tag.as_str() {
                                    if tag_str.to_lowercase().contains(search_term) {
                                        metadata_matches = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Display matching templates
                if name_matches || metadata_matches {
                    println!("\n  📋 Template: {}", name);
                    
                    if let Some(metadata) = metadata {
                        // Display all metadata fields with bold labels
                        if let Some(description) = metadata.get("description").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mDescription:\x1b[0m {}", description);
                        }
                        if let Some(language) = metadata.get("language").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mLanguage:\x1b[0m {}", language);
                        }
                        if let Some(version) = metadata.get("version").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mVersion:\x1b[0m {}", version);
                        }
                        if let Some(author) = metadata.get("author").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mAuthor:\x1b[0m {}", author);
                        }
                        if let Some(contact) = metadata.get("contact").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mContact:\x1b[0m {}", contact);
                        }
                        if let Some(license) = metadata.get("license").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mLicense:\x1b[0m {}", license);
                        }
                        if let Some(tags) = metadata.get("tags").and_then(|v| v.as_array()) {
                            let tag_strs: Vec<_> = tags.iter()
                                .filter_map(|t| t.as_str())
                                .collect();
                            if !tag_strs.is_empty() {
                                println!("     \x1b[1mTags:\x1b[0m {}", tag_strs.join(", "));
                            }
                        }
                        if let Some(created_at) = metadata.get("created_at").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mCreated:\x1b[0m {}", created_at);
                        }
                    } else {
                        println!("     (No metadata available)");
                    }
                    
                    results_found = true;
                }
            }
        }
        Err(_) => {}
    }
    
    results_found
}

fn list_templates_in_dir_with_metadata(path: &Path, _display_path: &str) {
    if !path.exists() {
        println!("  (No templates found - directory does not exist)");
        return;
    }
    
    match fs::read_dir(path) {
        Ok(entries) => {
            let mut templates: Vec<_> = entries
                .filter_map(|e| e.ok())
                .filter_map(|e| {
                    let path = e.path();
                    let name = path.file_name()?.to_string_lossy().to_string();
                    if path.is_file() && name.ends_with(".zip") {
                        Some((name, path, true))
                    } else if path.is_dir() {
                        Some((format!("{}/", name), path, false))
                    } else {
                        None
                    }
                })
                .collect();
            
            templates.sort_by(|a, b| a.0.cmp(&b.0));
            
            if templates.is_empty() {
                println!("  (No templates found)");
            } else {
                for (name, path, is_file) in templates {
                    let metadata = if is_file {
                        extract_template_metadata(&path)
                    } else {
                        extract_template_metadata_from_dir(&path)
                    };
                    
                    println!("\n  📋 Template: {}", name);
                    
                    if let Some(metadata) = metadata {
                        // Display all metadata fields with bold labels
                        if let Some(description) = metadata.get("description").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mDescription:\x1b[0m {}", description);
                        }
                        if let Some(language) = metadata.get("language").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mLanguage:\x1b[0m {}", language);
                        }
                        if let Some(version) = metadata.get("version").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mVersion:\x1b[0m {}", version);
                        }
                        if let Some(author) = metadata.get("author").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mAuthor:\x1b[0m {}", author);
                        }
                        if let Some(contact) = metadata.get("contact").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mContact:\x1b[0m {}", contact);
                        }
                        if let Some(license) = metadata.get("license").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mLicense:\x1b[0m {}", license);
                        }
                        if let Some(tags) = metadata.get("tags").and_then(|v| v.as_array()) {
                            let tag_strs: Vec<_> = tags.iter()
                                .filter_map(|t| t.as_str())
                                .collect();
                            if !tag_strs.is_empty() {
                                println!("     \x1b[1mTags:\x1b[0m {}", tag_strs.join(", "));
                            }
                        }
                        if let Some(created_at) = metadata.get("created_at").and_then(|v| v.as_str()) {
                            println!("     \x1b[1mCreated:\x1b[0m {}", created_at);
                        }
                    } else {
                        println!("     (No metadata available)");
                    }
                }
                println!();
            }
        }
        Err(e) => println!("  Error reading directory: {}", e),
    }
}