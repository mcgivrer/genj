fn main() {
    println!("cargo:rustc-link-lib=Advapi32");
    println!("cargo:rustc-link-lib=Secur32");
}
