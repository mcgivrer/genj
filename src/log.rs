pub fn log_verbose(msg: &str, verbose: bool) {
    if verbose {
        println!("[VERBOSE] {}", msg);
    }
}
pub fn log_info(msg: &str) {
    println!("[INFO] {}", msg);
}
pub fn log_success(msg: &str) {
    println!("[✓] {}", msg);
}
pub fn log_warning(msg: &str) {
    eprintln!("[⚠] {}", msg);
}