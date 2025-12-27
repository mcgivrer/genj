use std::env;
use std::fs::File;
use std::io::{Write, Read, Seek};
use std::path::{Path, PathBuf};
use zip::write::FileOptions;
use zip::ZipWriter;

fn zip_dir<T: Write + Seek>(
    it: &mut dyn Iterator<Item = PathBuf>,
    prefix: &Path,
    writer: T,
) -> zip::result::ZipResult<()> {
    let mut zip = ZipWriter::new(writer);
    let options = FileOptions::default().compression_method(zip::CompressionMethod::Deflated);
    for path in it {
        let name = path.strip_prefix(prefix).unwrap();
        if path.is_file() {
            zip.start_file(name.to_string_lossy(), options)?;
            let mut f = File::open(&path)?;
            let mut buffer = Vec::new();
            f.read_to_end(&mut buffer)?;
            zip.write_all(&buffer)?;
        } else if path.is_dir() {
            zip.add_directory(name.to_string_lossy(), options)?;
        }
    }
    zip.finish()?;
    Ok(())
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<String> = env::args().collect();
    if args.len() != 3 {
        eprintln!("Usage: {} <src_dir> <dest_zip>", args[0]);
        std::process::exit(1);
    }
    let src_dir = Path::new(&args[1]);
    let dest_zip = &args[2];
    let file = File::create(dest_zip)?;
    let mut paths = Vec::new();
    for entry in walkdir::WalkDir::new(src_dir) {
        let entry = entry?;
        paths.push(entry.path().to_path_buf());
    }
    zip_dir(&mut paths.into_iter(), src_dir, file)?;
    println!("Zipped {} -> {}", src_dir.display(), dest_zip);
    Ok(())
}
