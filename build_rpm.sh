#!/bin/bash
# Wrapper : délègue à mkpkg avec le format 'rpm'.
# Toute la configuration est dans [package.metadata.generate-rpm] de Cargo.toml.
exec cargo run --release --bin mkpkg -- --format rpm "$@"

set -e

PROJECT_NAME="genj"
VERSION=$(grep '^version' Cargo.toml | head -1 | sed 's/version = "\(.*\)"/\1/')

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Building RPM package for ${PROJECT_NAME} v${VERSION}...${NC}"

# Check cargo-generate-rpm is installed
if ! command -v cargo-generate-rpm &>/dev/null; then
    echo -e "${YELLOW}Installing cargo-generate-rpm...${NC}"
    cargo install cargo-generate-rpm
fi

# Compile in release mode
echo -e "${YELLOW}Compiling Rust project...${NC}"
cargo build --release
echo -e "${GREEN}✓ Compilation successful${NC}"

# Generate template ZIPs (required by package metadata)
echo -e "${YELLOW}Generating template ZIPs...${NC}"
TEMPLATE_SRC_DIR="templates"
TEMPLATE_ZIP_DIR="target/release-templates"
mkdir -p "$TEMPLATE_ZIP_DIR"

for dir in "$TEMPLATE_SRC_DIR"/*/; do
    template_name=$(basename "$dir")
    zip_file="$TEMPLATE_ZIP_DIR/${template_name}.zip"
    echo "  - $template_name -> $zip_file"
    cargo run --release --bin zip-template -- "$dir" "$zip_file"
done
echo -e "${GREEN}✓ Template ZIPs generated${NC}"

# Compress man pages in a temporary location
echo -e "${YELLOW}Compressing man pages...${NC}"
MANDIR_TMP="$(mktemp -d)"
cp docs/man/man1/genj.1 "$MANDIR_TMP/genj.1"
gzip -9 "$MANDIR_TMP/genj.1"
cp "$MANDIR_TMP/genj.1.gz" docs/man/man1/genj.1.gz

if [ -f "docs/man/man5/genj-template.5" ]; then
    cp docs/man/man5/genj-template.5 "$MANDIR_TMP/genj-template.5"
    gzip -9 "$MANDIR_TMP/genj-template.5"
    cp "$MANDIR_TMP/genj-template.5.gz" docs/man/man5/genj-template.5.gz
fi
rm -rf "$MANDIR_TMP"
echo -e "${GREEN}✓ Man pages compressed${NC}"

# Generate RPM
echo -e "${YELLOW}Generating RPM package...${NC}"
cargo generate-rpm

RPM_FILE=$(find target/generate-rpm -name "*.rpm" | head -1)
if [ -n "$RPM_FILE" ]; then
    mkdir -p target/package
    cp "$RPM_FILE" target/package/
    FINAL=$(basename "$RPM_FILE")
    echo -e "${GREEN}✅ RPM package created: target/package/${FINAL}${NC}"
    echo ""
    echo "Install with:  sudo rpm -ivh target/package/${FINAL}"
    echo "Or:            sudo dnf install target/package/${FINAL}"
else
    echo -e "${RED}Error: RPM file not found in target/generate-rpm/${NC}"
    exit 1
fi

# Clean up temporary compressed man pages
rm -f docs/man/man1/genj.1.gz docs/man/man5/genj-template.5.gz
