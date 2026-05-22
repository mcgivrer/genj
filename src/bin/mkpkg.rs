//! mkpkg — Universal packaging tool for genj.
//!
//! Reads all configuration from `Cargo.toml` and produces native installers
//! for the current (or targeted) platform with a single command:
//!
//!   cargo run --release --bin mkpkg
//!   cargo run --release --bin mkpkg -- --format zip
//!   cargo run --release --bin mkpkg -- --format all
//!   cargo run --release --bin mkpkg -- --format deb,rpm
//!   cargo run --release --bin mkpkg -- --skip-build
//!
//! Supported formats
//! -----------------
//!   zip  — cross-platform portable archive            (pure Rust, no deps)
//!   deb  — Debian/Ubuntu package                      (requires: cargo-deb)
//!   rpm  — Fedora / RHEL / openSUSE package           (requires: cargo-generate-rpm)
//!   msi  — Windows installer                          (requires: cargo-wix + WiX Toolset 3)
//!   dmg  — macOS disk image                           (requires: hdiutil, macOS only)
//!
//! Default formats per OS are read from [package.metadata.package] in Cargo.toml:
//!
//!   [package.metadata.package]
//!   linux   = ["deb", "rpm", "zip"]
//!   macos   = ["dmg", "zip"]
//!   windows = ["msi", "zip"]

use std::{
    env, fs,
    io::{Read, Write},
    path::{Path, PathBuf},
    process::{self, Command},
};
use walkdir::WalkDir;
use zip::{write::FileOptions, ZipWriter};

// ─────────────────────────────────────────────────────────────────────────────
// Cargo.toml metadata
// ─────────────────────────────────────────────────────────────────────────────

struct Metadata {
    name: String,
    version: String,
}

fn read_metadata() -> Metadata {
    let content = fs::read_to_string("Cargo.toml").expect("Cannot read Cargo.toml");
    let name = toml_string_field(&content, "name", false);
    let version = toml_string_field(&content, "version", false);
    Metadata { name, version }
}

/// Extract a string field from Cargo.toml, stopping before metadata sections.
fn toml_string_field(content: &str, key: &str, in_metadata: bool) -> String {
    let mut inside_meta = false;
    for line in content.lines() {
        let t = line.trim();
        if t.starts_with('[') {
            inside_meta = t.contains("metadata");
        }
        if inside_meta != in_metadata {
            continue;
        }
        if t.starts_with(key) && t.contains('=') {
            if let Some(v) = t.splitn(2, '=').nth(1) {
                return v.trim().trim_matches('"').to_string();
            }
        }
    }
    String::new()
}

/// Parse `[package.metadata.package]` and return the format list for the given OS key.
fn formats_from_cargo_toml(content: &str, os_key: &str) -> Option<Vec<String>> {
    let mut in_section = false;
    for line in content.lines() {
        let t = line.trim();
        if t == "[package.metadata.package]" {
            in_section = true;
            continue;
        }
        if in_section {
            if t.starts_with('[') {
                break;
            }
            if let Some(rest) = t.strip_prefix(os_key) {
                if rest.trim_start().starts_with('=') {
                    let arr_str = rest.trim_start().trim_start_matches('=').trim();
                    let values: Vec<String> = arr_str
                        .trim_matches(|c| c == '[' || c == ']')
                        .split(',')
                        .map(|s| s.trim().trim_matches('"').to_string())
                        .filter(|s| !s.is_empty())
                        .collect();
                    return Some(values);
                }
            }
        }
    }
    None
}

fn os_key() -> &'static str {
    if cfg!(target_os = "linux") {
        "linux"
    } else if cfg!(target_os = "macos") {
        "macos"
    } else {
        "windows"
    }
}

fn default_formats(os: &str) -> Vec<String> {
    match os {
        "linux" => vec!["deb".into(), "rpm".into(), "zip".into()],
        "macos" => vec!["dmg".into(), "zip".into()],
        _ => vec!["msi".into(), "zip".into()],
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CLI argument parsing
// ─────────────────────────────────────────────────────────────────────────────

struct Args {
    formats: Option<Vec<String>>, // None → use Cargo.toml defaults
    skip_build: bool,
}

fn parse_args() -> Args {
    let args: Vec<String> = env::args().collect();
    let mut formats: Option<Vec<String>> = None;
    let mut skip_build = false;
    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--format" | "-f" => {
                i += 1;
                if i < args.len() {
                    let raw = &args[i];
                    if raw == "all" {
                        formats = Some(vec![
                            "zip".into(),
                            "deb".into(),
                            "rpm".into(),
                            "msi".into(),
                            "dmg".into(),
                        ]);
                    } else {
                        formats = Some(
                            raw.split(',')
                                .map(|s| s.trim().to_lowercase())
                                .filter(|s| !s.is_empty())
                                .collect(),
                        );
                    }
                }
            }
            "--skip-build" | "--no-build" => {
                skip_build = true;
            }
            "--help" | "-h" => {
                print_help();
                process::exit(0);
            }
            _ => {}
        }
        i += 1;
    }
    Args { formats, skip_build }
}

fn print_help() {
    println!(
        "mkpkg — universal packaging tool for genj\n\
         \n\
         USAGE\n\
         \n\
         cargo run --release --bin mkpkg [-- [OPTIONS]]\n\
         \n\
         OPTIONS\n\
         \n\
           --format <fmt>   Comma-separated list of formats: zip,deb,rpm,msi,dmg  (or 'all')\n\
                            Default: read from [package.metadata.package] in Cargo.toml\n\
           --skip-build     Skip 'cargo build --release' (use existing binary)\n\
           --help           Print this message\n\
         \n\
         FORMATS\n\
         \n\
           zip   Portable ZIP archive          (all platforms, no deps)\n\
           deb   Debian/Ubuntu package         (Linux, requires cargo-deb)\n\
           rpm   Fedora/RHEL package           (Linux, requires cargo-generate-rpm)\n\
           msi   Windows MSI installer         (Windows, requires cargo-wix + WiX 3)\n\
           dmg   macOS disk image              (macOS, requires hdiutil)\n"
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

fn run_cmd(program: &str, args: &[&str]) -> bool {
    println!("  → {} {}", program, args.join(" "));
    let status = Command::new(program)
        .args(args)
        .status()
        .unwrap_or_else(|e| {
            eprintln!("  ✗ Failed to run '{}': {}", program, e);
            process::exit(1);
        });
    if !status.success() {
        eprintln!("  ✗ '{}' exited with status: {}", program, status);
        return false;
    }
    true
}

fn cargo_tool_installed(tool: &str) -> bool {
    Command::new("cargo")
        .args([tool, "--version"])
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

fn ensure_cargo_tool(tool: &str, crate_name: &str) -> bool {
    if !cargo_tool_installed(tool) {
        println!("  Installing {} ...", crate_name);
        if !run_cmd("cargo", &["install", crate_name]) {
            eprintln!("  ✗ Could not install {}. Install manually and retry.", crate_name);
            return false;
        }
    }
    true
}

fn platform_suffix() -> &'static str {
    let arch = if cfg!(target_arch = "aarch64") {
        "aarch64"
    } else {
        "x86_64"
    };
    if cfg!(target_os = "windows") {
        return if arch == "aarch64" { "windows-aarch64" } else { "windows-x86_64" };
    }
    if cfg!(target_os = "macos") {
        return if arch == "aarch64" { "macos-aarch64" } else { "macos-x86_64" };
    }
    if arch == "aarch64" { "linux-aarch64" } else { "linux-x86_64" }
}

fn exe_suffix() -> &'static str {
    if cfg!(target_os = "windows") { ".exe" } else { "" }
}

fn find_newest_file(dir: &str, ext: &str) -> Option<PathBuf> {
    let mut entries: Vec<_> = fs::read_dir(dir)
        .ok()?
        .filter_map(|e| e.ok())
        .map(|e| e.path())
        .filter(|p| p.extension().and_then(|x| x.to_str()) == Some(ext))
        .collect();
    entries.sort_by_key(|p| {
        p.metadata()
            .and_then(|m| m.modified())
            .unwrap_or(std::time::SystemTime::UNIX_EPOCH)
    });
    entries.into_iter().last()
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Compile
// ─────────────────────────────────────────────────────────────────────────────

fn step_build() {
    println!("\n[1/3] Compiling genj (release)...");
    if !run_cmd("cargo", &["build", "--release"]) {
        process::exit(1);
    }
    println!("  ✓ Compilation successful");
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — Template ZIPs
// ─────────────────────────────────────────────────────────────────────────────

fn step_templates() {
    println!("\n[2/3] Building template ZIPs...");
    let src = Path::new("templates");
    let dst = Path::new("target/release-templates");
    fs::create_dir_all(dst).expect("Cannot create target/release-templates");

    if !src.is_dir() {
        println!("  ⚠ 'templates/' directory not found — skipping");
        return;
    }

    let mut count = 0;
    for entry in fs::read_dir(src).expect("Cannot read templates/") {
        let entry = entry.expect("IO error reading templates/");
        let path = entry.path();
        if !path.is_dir() {
            continue;
        }
        let name = path.file_name().unwrap().to_string_lossy();
        let zip_path = dst.join(format!("{}.zip", name));
        zip_directory(&path, &zip_path)
            .unwrap_or_else(|e| eprintln!("  ✗ Failed to zip {}: {}", name, e));
        println!("  ✓ {} → {}", name, zip_path.display());
        count += 1;
    }
    println!("  ✓ {} template(s) packaged", count);
}

fn zip_directory(src: &Path, dest: &Path) -> Result<(), Box<dyn std::error::Error>> {
    let file = fs::File::create(dest)?;
    let mut zip = ZipWriter::new(file);
    let options =
        FileOptions::default().compression_method(zip::CompressionMethod::Deflated);

    for entry in WalkDir::new(src).into_iter().filter_map(|e| e.ok()) {
        let path = entry.path();
        let name = path.strip_prefix(src)?;
        if path.is_file() {
            zip.start_file(name.to_string_lossy(), options)?;
            let mut f = fs::File::open(path)?;
            let mut buf = Vec::new();
            f.read_to_end(&mut buf)?;
            zip.write_all(&buf)?;
        } else if path.is_dir() && name != Path::new("") {
            zip.add_directory(name.to_string_lossy(), options)?;
        }
    }
    zip.finish()?;
    Ok(())
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Packaging
// ─────────────────────────────────────────────────────────────────────────────

fn step_package(formats: &[String], meta: &Metadata) {
    println!("\n[3/3] Building packages: {}", formats.join(", "));
    fs::create_dir_all("target/package").expect("Cannot create target/package");

    let mut success = vec![];
    let mut failed = vec![];

    for fmt in formats {
        let ok = match fmt.as_str() {
            "zip" => pkg_zip(meta),
            "deb" => pkg_deb(meta),
            "rpm" => pkg_rpm(meta),
            "msi" => pkg_msi(meta),
            "dmg" => pkg_dmg(meta),
            other => {
                eprintln!("  ✗ Unknown format '{}' — skipped", other);
                false
            }
        };
        if ok {
            success.push(fmt.as_str());
        } else {
            failed.push(fmt.as_str());
        }
    }

    println!("\n─────────────────────────────────────────");
    println!("  Packages in target/package/");
    for path in fs::read_dir("target/package")
        .unwrap()
        .filter_map(|e| e.ok())
        .map(|e| e.path())
        .filter(|p| p.is_file())
    {
        println!("    {}", path.file_name().unwrap().to_string_lossy());
    }
    if !failed.is_empty() {
        eprintln!("\n  ✗ Failed formats: {}", failed.join(", "));
        process::exit(1);
    }
    println!("\n✅ Done — {} format(s) built.", success.len());
}

// ── ZIP ───────────────────────────────────────────────────────────────────────

fn pkg_zip(meta: &Metadata) -> bool {
    println!("\n  [zip] Building portable archive...");
    let filename = format!(
        "target/package/{}-{}-{}.zip",
        meta.name,
        meta.version,
        platform_suffix()
    );
    let exe_path = format!("target/release/{}{}", meta.name, exe_suffix());

    let file = match fs::File::create(&filename) {
        Ok(f) => f,
        Err(e) => { eprintln!("  ✗ Cannot create {}: {}", filename, e); return false; }
    };
    let mut zip = ZipWriter::new(file);
    let opts = FileOptions::default()
        .compression_method(zip::CompressionMethod::Deflated)
        .unix_permissions(0o755);
    let opts_data = opts.clone().unix_permissions(0o644);

    // Binary
    let exe_name = format!("{}{}", meta.name, exe_suffix());
    if let Ok(data) = fs::read(&exe_path) {
        let _ = zip.start_file(&exe_name, opts);
        let _ = zip.write_all(&data);
        println!("    + {}", exe_name);
    } else {
        eprintln!("  ✗ Binary not found at {}", exe_path);
        return false;
    }

    // Docs markdown
    if Path::new("docs").is_dir() {
        for entry in fs::read_dir("docs").unwrap().filter_map(|e| e.ok()) {
            let p = entry.path();
            if p.extension().and_then(|x| x.to_str()) == Some("md") {
                if let Ok(data) = fs::read(&p) {
                    let name = format!("docs/{}", p.file_name().unwrap().to_string_lossy());
                    let _ = zip.start_file(&name, opts_data.clone());
                    let _ = zip.write_all(&data);
                    println!("    + {}", name);
                }
            }
        }
    }

    // Template ZIPs
    let tpl_dir = Path::new("target/release-templates");
    if tpl_dir.is_dir() {
        for entry in fs::read_dir(tpl_dir).unwrap().filter_map(|e| e.ok()) {
            let p = entry.path();
            if p.extension().and_then(|x| x.to_str()) == Some("zip") {
                if let Ok(data) = fs::read(&p) {
                    let name =
                        format!("templates/{}", p.file_name().unwrap().to_string_lossy());
                    let _ = zip.start_file(&name, opts_data.clone());
                    let _ = zip.write_all(&data);
                    println!("    + {}", name);
                }
            }
        }
    }

    let _ = zip.finish();
    println!("  ✓ ZIP → {}", filename);
    true
}

// ── DEB ───────────────────────────────────────────────────────────────────────

fn pkg_deb(_meta: &Metadata) -> bool {
    println!("\n  [deb] Building Debian package (cargo-deb)...");
    if !ensure_cargo_tool("deb", "cargo-deb") {
        return false;
    }
    // --no-build: we already compiled; --output: put .deb in target/package/
    if !run_cmd("cargo", &["deb", "--no-build", "--output", "target/package"]) {
        eprintln!(
            "  Hint: verify [package.metadata.deb] in Cargo.toml and that man pages exist."
        );
        return false;
    }
    println!("  ✓ .deb created in target/package/");
    true
}

// ── RPM ───────────────────────────────────────────────────────────────────────

fn pkg_rpm(_meta: &Metadata) -> bool {
    println!("\n  [rpm] Building RPM package (cargo-generate-rpm)...");
    if !ensure_cargo_tool("generate-rpm", "cargo-generate-rpm") {
        return false;
    }
    if !run_cmd("cargo", &["generate-rpm"]) {
        eprintln!(
            "  Hint: verify [package.metadata.generate-rpm] in Cargo.toml."
        );
        return false;
    }
    // cargo-generate-rpm writes to target/generate-rpm/ — move to target/package/
    if let Some(rpm) = find_newest_file("target/generate-rpm", "rpm") {
        let dest = PathBuf::from("target/package")
            .join(rpm.file_name().unwrap());
        fs::copy(&rpm, &dest).unwrap_or(0);
        println!("  ✓ .rpm → {}", dest.display());
    }
    true
}

// ── MSI ───────────────────────────────────────────────────────────────────────

fn pkg_msi(_meta: &Metadata) -> bool {
    println!("\n  [msi] Building Windows MSI installer (cargo-wix)...");
    if !cfg!(target_os = "windows") {
        eprintln!(
            "  ✗ MSI packaging requires Windows. Use cross-compilation or a CI Windows runner."
        );
        return false;
    }
    if !ensure_cargo_tool("wix", "cargo-wix") {
        return false;
    }
    if !run_cmd("cargo", &["wix", "--no-build", "--nocapture"]) {
        eprintln!(
            "  Hint: WiX Toolset 3.x must be installed: https://wixtoolset.org/releases/"
        );
        return false;
    }
    if let Some(msi) = find_newest_file("target/wix", "msi") {
        let dest = PathBuf::from("target/package")
            .join(msi.file_name().unwrap());
        fs::copy(&msi, &dest).unwrap_or(0);
        println!("  ✓ .msi → {}", dest.display());
    }
    true
}

// ── DMG ───────────────────────────────────────────────────────────────────────

const INSTALL_SH: &str = r#"#!/bin/bash
# Installer for genj (macOS)
# Usage: sudo ./install.sh   or   PREFIX=~/.local ./install.sh
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INSTALL_BIN="${PREFIX:-/usr/local}/bin"
INSTALL_SHARE="${PREFIX:-/usr/local}/share/genj"
MAN1="${PREFIX:-/usr/local}/share/man/man1"
MAN5="${PREFIX:-/usr/local}/share/man/man5"

echo "Installing genj to $INSTALL_BIN ..."
mkdir -p "$INSTALL_BIN" "$INSTALL_SHARE/templates" "$MAN1" "$MAN5"
cp "$SCRIPT_DIR/bin/genj" "$INSTALL_BIN/genj"
chmod 755 "$INSTALL_BIN/genj"
ls "$SCRIPT_DIR/templates/"*.zip &>/dev/null && cp "$SCRIPT_DIR/templates/"*.zip "$INSTALL_SHARE/templates/"
[ -f "$SCRIPT_DIR/man/man1/genj.1" ] && cp "$SCRIPT_DIR/man/man1/genj.1" "$MAN1/"
[ -f "$SCRIPT_DIR/man/man5/genj-template.5" ] && cp "$SCRIPT_DIR/man/man5/genj-template.5" "$MAN5/"
echo "genj installed. Run: genj --help"
"#;

fn pkg_dmg(meta: &Metadata) -> bool {
    println!("\n  [dmg] Building macOS DMG...");
    if !cfg!(target_os = "macos") {
        eprintln!("  ✗ DMG packaging requires macOS.");
        return false;
    }

    let staging = PathBuf::from("target/_dmg_staging");
    // Clean previous staging
    if staging.exists() {
        fs::remove_dir_all(&staging).ok();
    }

    // Populate staging directory
    let bin_dir = staging.join("bin");
    let tpl_dir = staging.join("templates");
    let man1_dir = staging.join("man").join("man1");
    let man5_dir = staging.join("man").join("man5");
    for d in &[&bin_dir, &tpl_dir, &man1_dir, &man5_dir] {
        fs::create_dir_all(d).expect("Cannot create staging dir");
    }

    let src_bin = format!("target/release/{}", meta.name);
    if !Path::new(&src_bin).exists() {
        eprintln!("  ✗ Binary not found at {}", src_bin);
        return false;
    }
    fs::copy(&src_bin, bin_dir.join(&meta.name)).expect("Cannot copy binary");
    // Set executable permission on Unix
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mut perms = fs::metadata(bin_dir.join(&meta.name))
            .unwrap()
            .permissions();
        perms.set_mode(0o755);
        fs::set_permissions(bin_dir.join(&meta.name), perms).unwrap();
    }

    // Template ZIPs
    let tmpl_src = Path::new("target/release-templates");
    if tmpl_src.is_dir() {
        for entry in fs::read_dir(tmpl_src).unwrap().filter_map(|e| e.ok()) {
            let p = entry.path();
            if p.extension().and_then(|x| x.to_str()) == Some("zip") {
                fs::copy(&p, tpl_dir.join(p.file_name().unwrap())).ok();
            }
        }
    }

    // Man pages
    for (src, dst) in [
        ("docs/man/man1/genj.1", man1_dir.join("genj.1")),
        ("docs/man/man5/genj-template.5", man5_dir.join("genj-template.5")),
    ] {
        if Path::new(src).exists() {
            fs::copy(src, &dst).ok();
        }
    }

    // README + LICENSE
    for f in ["README.md", "LICENSE"] {
        if Path::new(f).exists() {
            fs::copy(f, staging.join(f)).ok();
        }
    }

    // install.sh
    let install_path = staging.join("install.sh");
    fs::write(&install_path, INSTALL_SH).expect("Cannot write install.sh");
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mut perms = fs::metadata(&install_path).unwrap().permissions();
        perms.set_mode(0o755);
        fs::set_permissions(&install_path, perms).unwrap();
    }

    // Build DMG
    let arch = if cfg!(target_arch = "aarch64") { "aarch64" } else { "x86_64" };
    let dmg_name = format!(
        "target/package/{}-{}-macos-{}.dmg",
        meta.name, meta.version, arch
    );

    let vol_name = format!("{}-{}", meta.name, meta.version);
    let ok = run_cmd(
        "hdiutil",
        &[
            "create",
            "-volname", &vol_name,
            "-srcfolder", staging.to_str().unwrap(),
            "-ov",
            "-format", "UDZO",
            &dmg_name,
        ],
    );

    fs::remove_dir_all(&staging).ok();

    if ok {
        println!("  ✓ .dmg → {}", dmg_name);
    }
    ok
}

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

fn main() {
    let args = parse_args();
    let meta = read_metadata();
    let cargo_content =
        fs::read_to_string("Cargo.toml").expect("Cannot read Cargo.toml");
    let os = os_key();

    // Determine formats
    let formats = args.formats.unwrap_or_else(|| {
        formats_from_cargo_toml(&cargo_content, os).unwrap_or_else(|| default_formats(os))
    });

    println!(
        "mkpkg  {name} v{ver}  [{os}]  formats: {fmts}",
        name = meta.name,
        ver = meta.version,
        os = os,
        fmts = formats.join(", ")
    );

    // Build
    if !args.skip_build {
        step_build();
    } else {
        println!("\n[1/3] Build skipped (--skip-build)");
    }

    // Templates
    step_templates();

    // Package
    step_package(&formats, &meta);
}
