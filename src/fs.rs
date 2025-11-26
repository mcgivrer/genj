use std::fs::{File, create_dir_all, write};
use std::io::{self, Read};
use std::path::Path;

pub fn is_text_bytes(buf: &[u8]) -> bool {
    if buf.iter().any(|&b| b == 0) { return false; }
    std::str::from_utf8(buf).is_ok()
}

pub fn is_text_path(path: &Path) -> io::Result<bool> {
    let mut f = File::open(path)?;
    let mut buf = [0u8; 8192];
    let n = f.read(&mut buf)?;
    Ok(is_text_bytes(&buf[..n]))
}

pub fn create_parent_dir(path: &Path) -> io::Result<()> {
    if let Some(p) = path.parent() {
        create_dir_all(p)?;
    }
    Ok(())
}

pub fn write_bytes(path: &Path, data: &[u8]) -> io::Result<()> {
    create_parent_dir(path)?;
    write(path, data)?;
    Ok(())
}