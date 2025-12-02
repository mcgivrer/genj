use std::fs;
use std::io::Write;
use std::path::Path;
use zip::ZipWriter;
use zip::write::FileOptions;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Read version from Cargo.toml
    let cargo_toml = fs::read_to_string("Cargo.toml")?;
    let version = cargo_toml
        .lines()
        .find(|line| line.starts_with("version"))
        .and_then(|line| line.split('"').nth(1))
        .unwrap_or("unknown");

    let package_name = "genj";
    
    // Determine executable extension
    #[cfg(target_os = "windows")]
    let exe_ext = ".exe";
    #[cfg(not(target_os = "windows"))]
    let exe_ext = "";

    // Build ZIP filename based on platform
    let platform = if cfg!(target_os = "windows") {
        "windows-x86_64"
    } else if cfg!(target_os = "macos") {
        "macos-x86_64"
    } else {
        "linux-x86_64"
    };

    let zip_filename = format!("target/package/{}-{}-{}.zip", package_name, version, platform);
    let exe_path = format!("target/release/{}{}", package_name, exe_ext);

    // Create build directory if it doesn't exist
    fs::create_dir_all("target/package").expect("Failed to create build directory");

    // Create ZIP file
    let file = fs::File::create(&zip_filename)
        .expect(&format!("Failed to create {}", zip_filename));
    let mut zip = ZipWriter::new(file);
    let options = FileOptions::default()
        .compression_method(zip::CompressionMethod::Deflated)
        .unix_permissions(0o755);

    println!("📦 Creating ZIP file: {}", zip_filename);

    // Add compiled executable
    if Path::new(&exe_path).exists() {
        let exe_data = fs::read(&exe_path)
            .expect(&format!("Failed to read {}", exe_path));
        
        zip.start_file(format!("{}{}", package_name, exe_ext), options.clone())
            .expect("Error adding EXE file to ZIP");
        zip.write_all(&exe_data)
            .expect("Error writing EXE file");
        
        println!("  ✓ Added: {}{} ({} bytes)", package_name, exe_ext, exe_data.len());
    } else {
        eprintln!("⚠️  Warning: {} not found at {}", package_name, exe_path);
    }

    // Add *.md files from docs directory
    if Path::new("docs").is_dir() {
        for entry in fs::read_dir("docs").expect("Failed to read docs directory") {
            if let Ok(entry) = entry {
                let path = entry.path();
                if path.extension().map_or(false, |ext| ext == "md") {
                    let filename = path.file_name().unwrap().to_string_lossy();
                    let file_content = fs::read(&path)
                        .expect(&format!("Failed to read {}", path.display()));
                    
                    zip.start_file(format!("docs/{}", filename), options.clone())
                        .expect(&format!("Error adding {}", filename));
                    zip.write_all(&file_content)
                        .expect(&format!("Error writing {}", filename));
                    
                    println!("  ✓ Added: docs/{} ({} bytes)", filename, file_content.len());
                }
            }
        }
    } else {
        eprintln!("⚠️  Warning: docs directory not found");
    }

    // Add template ZIPs from release-templates directory
    if Path::new("target/release-templates").is_dir() {
        println!("📦 Adding templates...");
        for entry in fs::read_dir("target/release-templates").expect("Failed to read release-templates directory") {
            if let Ok(entry) = entry {
                let path = entry.path();
                if path.is_file() && path.extension().map_or(false, |ext| ext == "zip") {
                    let filename = path.file_name().unwrap().to_string_lossy();
                    let file_content = fs::read(&path)
                        .expect(&format!("Failed to read {}", path.display()));
                    
                    zip.start_file(format!("templates/{}", filename), options.clone())
                        .expect(&format!("Error adding {}", filename));
                    zip.write_all(&file_content)
                        .expect(&format!("Error writing {}", filename));
                    
                    println!("  ✓ Added: templates/{} ({} bytes)", filename, file_content.len());
                }
            }
        }
    } else {
        eprintln!("⚠️  Warning: release-templates directory not found");
    }

    zip.finish().expect("Error finalizing ZIP");
    println!("✅ ZIP file created successfully: {}", zip_filename);
    println!("   Version: {}", version);
    println!("   Platform: {}", platform);
    println!("   Structure:");
    println!("   ├── genj{}", exe_ext);
    println!("   ├── docs/");
    println!("   │   ├── MANUAL.md");
    println!("   │   ├── TEMPLATES.md");
    println!("   │   └── ...");
    println!("   └── templates/");
    println!("       ├── basic-java.zip");
    println!("       ├── game-fps.zip");
    println!("       └── ...");
    
    Ok(())
}
