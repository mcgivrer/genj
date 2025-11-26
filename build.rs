use std::env;

fn main() {
    // Cargo fournit CARGO_CFG_TARGET_OS (e.g. "windows", "linux", "macos")
    let target_os = env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();

    if target_os == "windows" {
        println!("cargo:rustc-link-lib=Advapi32");
        println!("cargo:rustc-link-lib=Secur32");
    }
}
