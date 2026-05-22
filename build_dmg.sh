#!/bin/bash
# Wrapper : délègue à mkpkg avec le format 'dmg'.
# Nécessite macOS (hdiutil).
exec cargo run --release --bin mkpkg -- --format dmg "$@"

set -e

PROJECT_NAME="genj"
VERSION=$(grep '^version' Cargo.toml | head -1 | sed 's/version = "\(.*\)"/\1/')
ARCH=$(uname -m)
DMG_NAME="${PROJECT_NAME}-${VERSION}-macos-${ARCH}.dmg"
BUILD_DIR="target/package"
STAGING_DIR="$(mktemp -d)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}Building macOS DMG for ${PROJECT_NAME} v${VERSION}...${NC}"

# Ensure we are on macOS
if [ "$(uname -s)" != "Darwin" ]; then
    echo -e "${RED}Error: DMG packaging requires macOS.${NC}"
    exit 1
fi

# Compile in release mode
echo -e "${YELLOW}Compiling Rust project...${NC}"
cargo build --release
echo -e "${GREEN}✓ Compilation successful${NC}"

# Generate template ZIPs
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

# Populate staging directory
echo -e "${YELLOW}Preparing DMG contents...${NC}"
mkdir -p "$STAGING_DIR/bin"
cp target/release/genj "$STAGING_DIR/bin/genj"
chmod 755 "$STAGING_DIR/bin/genj"

mkdir -p "$STAGING_DIR/templates"
if ls "$TEMPLATE_ZIP_DIR"/*.zip &>/dev/null; then
    cp "$TEMPLATE_ZIP_DIR"/*.zip "$STAGING_DIR/templates/"
fi

cp README.md "$STAGING_DIR/"
cp LICENSE "$STAGING_DIR/"

# Copy man pages
mkdir -p "$STAGING_DIR/man/man1" "$STAGING_DIR/man/man5"
[ -f "docs/man/man1/genj.1" ] && cp docs/man/man1/genj.1 "$STAGING_DIR/man/man1/"
[ -f "docs/man/man5/genj-template.5" ] && cp docs/man/man5/genj-template.5 "$STAGING_DIR/man/man5/"

# Create install.sh helper script inside the DMG
cat > "$STAGING_DIR/install.sh" << 'INSTALL_EOF'
#!/bin/bash
# Installer script for genj (macOS)
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INSTALL_BIN="${PREFIX:-/usr/local}/bin"
INSTALL_SHARE="${PREFIX:-/usr/local}/share/genj"
MAN1_DIR="${PREFIX:-/usr/local}/share/man/man1"
MAN5_DIR="${PREFIX:-/usr/local}/share/man/man5"

echo "Installing genj to $INSTALL_BIN ..."
mkdir -p "$INSTALL_BIN" "$INSTALL_SHARE/templates" "$MAN1_DIR" "$MAN5_DIR"

cp "$SCRIPT_DIR/bin/genj" "$INSTALL_BIN/genj"
chmod 755 "$INSTALL_BIN/genj"

if ls "$SCRIPT_DIR/templates/"*.zip &>/dev/null; then
    cp "$SCRIPT_DIR/templates/"*.zip "$INSTALL_SHARE/templates/"
fi

[ -f "$SCRIPT_DIR/man/man1/genj.1" ] && cp "$SCRIPT_DIR/man/man1/genj.1" "$MAN1_DIR/"
[ -f "$SCRIPT_DIR/man/man5/genj-template.5" ] && cp "$SCRIPT_DIR/man/man5/genj-template.5" "$MAN5_DIR/"

echo "✅ genj installed successfully."
echo "Run: genj --help"
INSTALL_EOF
chmod 755 "$STAGING_DIR/install.sh"

echo -e "${GREEN}✓ DMG contents prepared${NC}"

# Build DMG with hdiutil (standard macOS approach)
mkdir -p "$BUILD_DIR"

if command -v create-dmg &>/dev/null; then
    echo -e "${YELLOW}Using create-dmg for a styled DMG...${NC}"
    create-dmg \
        --volname "$PROJECT_NAME $VERSION" \
        --volicon "wix/Product.icns" \
        --window-pos 200 120 \
        --window-size 600 400 \
        --icon-size 100 \
        --app-drop-link 425 120 \
        "$BUILD_DIR/$DMG_NAME" \
        "$STAGING_DIR" 2>/dev/null || true
fi

# Fallback / default: plain hdiutil DMG
if [ ! -f "$BUILD_DIR/$DMG_NAME" ]; then
    echo -e "${YELLOW}Creating DMG with hdiutil...${NC}"
    hdiutil create \
        -volname "${PROJECT_NAME}-${VERSION}" \
        -srcfolder "$STAGING_DIR" \
        -ov \
        -format UDZO \
        "$BUILD_DIR/$DMG_NAME"
fi

rm -rf "$STAGING_DIR"

echo -e "${GREEN}✅ DMG package created: $BUILD_DIR/$DMG_NAME${NC}"
echo ""
echo "Mount and run install.sh inside the DMG to install genj."
echo "Or copy the genj binary manually from the bin/ folder."
