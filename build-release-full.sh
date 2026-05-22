#!/bin/bash
# Wrapper de build/packaging pour genj.
# Toute la logique est dans src/bin/mkpkg.rs, pilotée par Cargo.toml.
#
# Usage:
#   ./build-release-full.sh                  # formats par défaut pour l'OS courant
#   ./build-release-full.sh --format zip     # ZIP uniquement
#   ./build-release-full.sh --format all     # tous les formats
#   ./build-release-full.sh --skip-build     # pas de recompilation
set -e
cargo run --release --bin mkpkg -- "$@"
