# Wrapper de build/packaging pour genj.
# Toute la logique est dans src/bin/mkpkg.rs, pilotée par Cargo.toml.
#
# Usage:
#   .\build-release-full.ps1                      # formats par défaut (msi, zip)
#   .\build-release-full.ps1 --format zip         # ZIP uniquement
#   .\build-release-full.ps1 --format all         # tous les formats
#   .\build-release-full.ps1 --skip-build         # pas de recompilation
cargo run --release --bin mkpkg -- $args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
