use std::fs;
use std::path::Path;
use std::io::Write;
use zip::ZipWriter;
use zip::write::FileOptions;
use std::fs::File;

fn main() {
    // Lire la version depuis Cargo.toml
    let cargo_toml = fs::read_to_string("Cargo.toml").expect("Impossible de lire Cargo.toml");
    let version = cargo_toml
        .lines()
        .find(|line| line.starts_with("version"))
        .and_then(|line| line.split('"').nth(1))
        .unwrap_or("unknown");

    let package_name = "genj";
    let zip_filename = format!("build/{}-{}-windows-x86_64.zip", package_name, version);
    
    // Créer le répertoire build s'il n'existe pas
    fs::create_dir_all("build").expect("Impossible de créer le répertoire build");

    // Créer le fichier ZIP
    let file = File::create(&zip_filename).expect(&format!("Impossible de créer {}", zip_filename));
    let mut zip = ZipWriter::new(file);
    let options = FileOptions::default().compression_method(zip::CompressionMethod::Deflated);

    // Ajouter l'exécutable compilé
    #[cfg(target_os = "windows")]
    let exe_path = format!("target/release/{}.exe", package_name);
    #[cfg(not(target_os = "windows"))]
    let exe_path = format!("target/release/{}", package_name);

    if Path::new(&exe_path).exists() {
        let exe_data = fs::read(&exe_path).expect(&format!("Impossible de lire {}", exe_path));
        zip.start_file(format!("{}.exe", package_name), options.clone())
            .expect("Erreur lors de l'ajout du fichier EXE au ZIP");
        zip.write_all(&exe_data).expect("Erreur lors de l'écriture du fichier EXE");
    } else {
        eprintln!("Attention: {} non trouvé", exe_path);
    }

    // Ajouter les fichiers *.md du répertoire docs
    if Path::new("docs").is_dir() {
        for entry in fs::read_dir("docs").expect("Impossible de lire le répertoire docs") {
            if let Ok(entry) = entry {
                let path = entry.path();
                if path.extension().map_or(false, |ext| ext == "md") {
                    let filename = path.file_name().unwrap().to_string_lossy();
                    let file_content = fs::read(&path).expect(&format!("Impossible de lire {}", path.display()));
                    zip.start_file(format!("docs/{}", filename), options.clone())
                        .expect(&format!("Erreur lors de l'ajout de {}", filename));
                    zip.write_all(&file_content).expect(&format!("Erreur lors de l'écriture de {}", filename));
                }
            }
        }
    }

    zip.finish().expect("Erreur lors de la finalisation du ZIP");
    println!("✓ Fichier ZIP créé: {}", zip_filename);
}
