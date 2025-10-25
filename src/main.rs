use clap::Parser;
use chrono::prelude::*;
use std::fs::File;
use std::fs::{create_dir_all, read_to_string, write, copy};
use std::io::{self, Read};
use std::path::{Path, PathBuf};
use walkdir::WalkDir;
use zip::ZipArchive;

#[derive(Parser, Debug)]
#[command(
    author = "Frédéric Delorme",
    version = "1.0",
    about = "This script generates a Java project based on the specified template files.
It creates the necessary directory structure, copies the templates, replaces
placeholders in the templates with the provided values, and generates additional
files such as MANIFEST.MF and README.md."
)]
struct Cli {
    #[arg(short, long, help = "Chemin du template (ZIP ou dossier)")]
    template: String,
    #[arg(short, long, help = "Répertoire destination")]
    destination: String,
    #[arg(short = 'n', long = "project_name", default_value = "Demo")]
    project_name: String,
    #[arg(short = 'a', long = "author", default_value = "Auteur inconnu")]
    author: String,
    #[arg(short = 'e', long = "email", default_value = "email@inconnu.local")]
    email: String,
    #[arg(short = 'v', long = "project_version", default_value = "0.0.1")]
    project_version: String,
    #[arg(short = 'j', long = "java_version", help = "la version du JDK", default_value="25")]
    java: String,
    #[arg(short = 'f', long = "java_flavor", help="La saveur du JDK (pour sdkman)", default_value="25-zulu")]
    java_flavor: String,
    #[arg(short = 'k', long = "package", default_value = "com.demo")]
    package: String,
    #[arg(short = 'm', long = "mainclass", default_value = "App")]
    mainclass: String,
     #[arg(short = 'b', long = "build", help = "Outil de construction (maven ou gradle)", default_value = "maven")]
    build_tool: String,
    #[arg(long = "maven_version", default_value = "3.9.5")]
    maven_version: String,
    #[arg(long = "gradle_version", default_value = "8.5")]
    gradle_version: String,
    #[arg(short = 'l', long = "vendor_name", help="Le nom du vendeur (pour le manifest)", default_value = "Vendor")]
    vendor_name: String,
}

fn main() -> io::Result<()> {
    let args = Cli::parse();
    let package_val = &args.package;
    let mainclass_val = &args.mainclass;

    let mut dest_path = PathBuf::from(&args.destination);

    dest_path = dest_path.join(&args.project_name);
    
    let current_year = Utc::now().year().to_string();
    
    let replacements = [
        ("${PROJECT_NAME}", args.project_name.as_str()),
        ("${AUTHOR_NAME}", args.author.as_str()),
        ("${AUTHOR_EMAIL}", args.email.as_str()),
        ("${PROJECT_VERSION}", args.project_version.as_str()),
        ("${PACKAGE}", package_val.as_str()),
        ("${JAVA}", args.java.as_str()),
        ("${VENDOR_NAME}", args.vendor_name.as_str()),
        ("${MAINCLASS}", mainclass_val.as_str()),
        ("${PROJECT_YEAR}", current_year.as_str()),
    ];

    let template_path = Path::new(&args.template);

    if template_path.is_file() {
        // Zip extraction
        extract_zip_with_replace(template_path, &dest_path, &replacements)?;
    } else if template_path.is_dir() {
        // Copy directory with replace
        copy_dir_with_replace(template_path, &dest_path, &replacements)?;
    } else {
        eprintln!("Chemin template invalide");
        std::process::exit(1);
    }

    // Traitement du build tool
    let build_tool = args.build_tool.to_lowercase();
    if build_tool != "maven" && build_tool != "gradle" {
        eprintln!("Outil de build non supporté : {} (valeurs possibles : maven, gradle)", build_tool);
        std::process::exit(1);
    }

    // Ajout du fichier pom.xml ou build.gradle
    if build_tool == "maven" {
        // Ex : projet simple Maven
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
        <dependencies>
            <!-- https://mvnrepository.com/artifact/org.junit.platform/junit-platform-console-standalone -->
            <dependency>
                <groupId>org.junit.platform</groupId>
                <artifactId>junit-platform-console-standalone</artifactId>
                <version>6.0.0</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </project>"#,
            args.package,
            args.project_name,
            args.project_version,
            args.java,
            args.java
        );
        write(pom_path, pom_content)?;
    } else if build_tool == "gradle" {
        // Ex : projet simple Gradle
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
    dependencies {{}}
    "#,
            args.package,
            args.project_version
        );
        write(gradle_path, gradle_content)?;
    }

    // Mise à jour du fichier .sdkman
    
    let sdkman_file = dest_path.join(".sdkmanrc");
    let mut sdkman_content = format!("java={}\n", args.java_flavor);

    if build_tool == "maven" {
        sdkman_content.push_str(&format!("maven={}\n", args.maven_version));
    } else if build_tool == "gradle" {
        sdkman_content.push_str(&format!("gradle={}\n", args.gradle_version));
    }
    write(sdkman_file, sdkman_content)?;


    println!("Projet Java généré dans {}", dest_path.display());
    Ok(())
}

fn replace_package_in_path(path_str: &str, replacements: &[(&str, &str)], package_val: &str) -> PathBuf {
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

// Détecte si un buffer d'octets ressemble à du texte.
fn is_text_bytes(buf: &[u8]) -> bool {
    // Si on trouve un octet NUL, c'est très probablement binaire
    if buf.iter().any(|&b| b == 0) {
        return false;
    }

    // Essayer une vérification UTF-8 simple
    if std::str::from_utf8(buf).is_ok() {
        return true;
    }

    // Si pas UTF-8, considérer comme binaire
    false
}

// Lit un petit préfixe du fichier pour décider si c'est du texte
fn is_text_path(path: &Path) -> io::Result<bool> {
    let mut file = File::open(path)?;
    let mut buf = [0u8; 8192];
    let n = file.read(&mut buf)?;
    Ok(is_text_bytes(&buf[..n]))
}

fn extract_zip_with_replace(
    zip_path: &Path,
    dest_path: &Path,
    replacements: &[(&str, &str)],
) -> io::Result<()> {
    let file = File::open(zip_path)?;
    let mut archive = ZipArchive::new(file)?;

    // Collecte des noms d’entrées pour éviter plusieurs emprunts mutables
    let entry_names: Vec<String> = (0..archive.len())
        .map(|i| archive.by_index(i).map(|e| e.name().to_string()))
        .collect::<Result<_, _>>()?;

    // Détection du préfixe racine commun
    let common_prefix = entry_names.iter()
        .filter_map(|name| name.find('/').map(|pos| &name[..pos + 1]))
        .reduce(|a, b| if a == b { a } else { "" })
        .filter(|s| !s.is_empty());

    for i in 0..archive.len() {
    let mut entry = archive.by_index(i)?;
    let raw_name = entry.name().to_string();

        // Suppression du préfixe racine commun
        let relative_path = if let Some(prefix) = common_prefix {
            raw_name.as_str().strip_prefix(prefix).unwrap_or(raw_name.as_str())
        } else {
            raw_name.as_str()
        };

        // Utilisation de replace_package_in_path pour gérer ${PACKAGE}
        let outpath = replace_package_in_path(relative_path, replacements, 
                         replacements.iter()
                          .find(|(key, _)| *key == "${PACKAGE}")
                          .map(|(_, val)| *val)
                          .unwrap_or(""));

        let full_path = dest_path.join(outpath);

        if raw_name.ends_with('/') {
            create_dir_all(&full_path)?;
        } else {
            if let Some(parent) = full_path.parent() {
                create_dir_all(parent)?;
            }

            // Lire en bytes puis détecter si c'est texte
            let mut bytes: Vec<u8> = Vec::new();
            entry.read_to_end(&mut bytes)?;

            if !is_text_bytes(&bytes) {
                // Copier les fichiers binaires tels quels
                write(&full_path, &bytes)?;
                continue;
            }

            // Ici on suppose que c'est UTF-8
            let content = String::from_utf8(bytes).unwrap_or_default();
            let replaced_content = replacements.iter().fold(content, |acc, (pat, val)| acc.replace(pat, val));
            write(&full_path, replaced_content)?;
        }
    }
    Ok(())
}

fn copy_dir_with_replace(
    src_dir: &Path,
    dest_dir: &Path,
    replacements: &[(&str, &str)],
) -> io::Result<()> {
    // Extraire la valeur correspondant à ${PACKAGE} pour l'utiliser dans la construction des chemins
    let package_val = replacements.iter()
        .find(|(key, _)| *key == "${PACKAGE}")
        .map(|(_, val)| *val)
        .unwrap_or("");

    for entry in WalkDir::new(src_dir).into_iter().filter_map(Result::ok) {
        let rel_path = entry.path().strip_prefix(src_dir).unwrap();
        let rel_path_str = rel_path.to_string_lossy();

        // Utilisation de replace_package_in_path pour gérer ${PACKAGE} comme des sous-dossiers
        let new_path = replace_package_in_path(&rel_path_str, replacements, package_val);

        let full_dest_path = dest_dir.join(new_path);

        if entry.file_type().is_dir() {
            create_dir_all(&full_dest_path)?;
        } else if entry.file_type().is_file() {
            // Sauter les fichiers binaires
            match is_text_path(entry.path()) {
                Ok(true) => {
                    if let Some(parent) = full_dest_path.parent() {
                        create_dir_all(parent)?;
                    }
                    let content = read_to_string(entry.path())?;
                    let replaced_content = replacements.iter().fold(content, |acc, (pat, val)| acc.replace(pat, val));
                    write(full_dest_path, replaced_content)?;
                }
                Ok(false) => {
                    // Copier les fichiers binaires tels quels
                    if let Some(parent) = full_dest_path.parent() {
                        create_dir_all(parent)?;
                    }
                    match copy(entry.path(), &full_dest_path) {
                        Ok(_) => { /* copié */ }
                        Err(err) => eprintln!("Failed to copy binary file {}: {}", entry.path().display(), err),
                    }
                    continue;
                }
                Err(err) => {
                    eprintln!("Error reading file {}: {}", entry.path().display(), err);
                    continue;
                }
            }
        }
    }
    Ok(())
}
