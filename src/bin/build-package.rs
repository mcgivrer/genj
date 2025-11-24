use std::fs;
use std::io::Write;
use std::path::Path;
use zip::ZipWriter;
use zip::write::FileOptions;

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Lire la version depuis Cargo.toml
    let cargo_toml = fs::read_to_string("Cargo.toml")?;
    let version = cargo_toml
        .lines()
        .find(|line| line.starts_with("version"))
        .and_then(|line| line.split('"').nth(1))
        .unwrap_or("unknown");

    let package_name = "genj";
    
    // Déterminer l'extension de l'exécutable
    #[cfg(target_os = "windows")]
    let exe_ext = ".exe";
    #[cfg(not(target_os = "windows"))]
    let exe_ext = "";

    // Construire le nom du fichier ZIP basé sur la plateforme
    let platform = if cfg!(target_os = "windows") {
        "windows-x86_64"
    } else if cfg!(target_os = "macos") {
        "macos-x86_64"
    } else {
        "linux-x86_64"
    };

    let zip_filename = format!("target/package/{}-{}-{}.zip", package_name, version, platform);
    let exe_path = format!("target/release/{}{}", package_name, exe_ext);

    // Créer le répertoire build s'il n'existe pas
    fs::create_dir_all("target/package").expect("Impossible de créer le répertoire build");

    // Créer le fichier ZIP
    let file = fs::File::create(&zip_filename)
        .expect(&format!("Impossible de créer {}", zip_filename));
    let mut zip = ZipWriter::new(file);
    let options = FileOptions::default()
        .compression_method(zip::CompressionMethod::Deflated)
        .unix_permissions(0o755);

    println!("📦 Création du fichier ZIP : {}", zip_filename);

    // Ajouter l'exécutable compilé
    if Path::new(&exe_path).exists() {
        let exe_data = fs::read(&exe_path)
            .expect(&format!("Impossible de lire {}", exe_path));
        
        zip.start_file(format!("{}{}", package_name, exe_ext), options.clone())
            .expect("Erreur lors de l'ajout du fichier EXE au ZIP");
        zip.write_all(&exe_data)
            .expect("Erreur lors de l'écriture du fichier EXE");
        
        println!("  ✓ Ajout de : {}{} ({} bytes)", package_name, exe_ext, exe_data.len());
    } else {
        eprintln!("⚠️  Attention: {} non trouvé à {}", package_name, exe_path);
    }

    // Ajouter les fichiers *.md du répertoire docs
    if Path::new("docs").is_dir() {
        for entry in fs::read_dir("docs").expect("Impossible de lire le répertoire docs") {
            if let Ok(entry) = entry {
                let path = entry.path();
                if path.extension().map_or(false, |ext| ext == "md") {
                    let filename = path.file_name().unwrap().to_string_lossy();
                    let file_content = fs::read(&path)
                        .expect(&format!("Impossible de lire {}", path.display()));
                    
                    zip.start_file(format!("docs/{}", filename), options.clone())
                        .expect(&format!("Erreur lors de l'ajout de {}", filename));
                    zip.write_all(&file_content)
                        .expect(&format!("Erreur lors de l'écriture de {}", filename));
                    
                    println!("  ✓ Ajout de : docs/{} ({} bytes)", filename, file_content.len());
                }
            }
        }
    } else {
        eprintln!("⚠️  Attention: Le répertoire docs n'existe pas");
    }

    zip.finish().expect("Erreur lors de la finalisation du ZIP");
    println!("✅ Fichier ZIP créé avec succès: {}", zip_filename);
    println!("   Version: {}", version);
    println!("   Plateforme: {}", platform);
    
    Ok(())
}
