#!/bin/bash

set -e

# Configuration
PROJECT_NAME="genj"
VERSION="1.2.2"
MAINTAINER="Frédéric Delorme <fredericDOTdelormeATgmailDOTcom>"
DESCRIPTION="Generate a Java project from a template as a ZIP file or folder. \
Please, look at https://github.com/mcgivrer/genj for details."
ARCHITECTURE="amd64"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Building Debian package for ${PROJECT_NAME}...${NC}"

# Create temporary build directory
BUILD_DIR=./package/
trap "rm -rf $BUILD_DIR" EXIT

echo -e "${YELLOW}Build directory: $BUILD_DIR${NC}"

# Create debian package structure
DEBIAN_DIR="$BUILD_DIR/${PROJECT_NAME}_${VERSION}_${ARCHITECTURE}"
mkdir -p "$DEBIAN_DIR"

# Create DEBIAN metadata directory
DEBIAN_META="$DEBIAN_DIR/DEBIAN"
mkdir -p "$DEBIAN_META"

# Create binary directory
mkdir -p "$DEBIAN_DIR/usr/bin"

# Create man directories
mkdir -p "$DEBIAN_DIR/usr/share/man/man1"
mkdir -p "$DEBIAN_DIR/usr/share/man/man5"

# Compile the Rust project
echo -e "${YELLOW}Compiling Rust project...${NC}"
cargo build --release

# Copy compiled binary
echo -e "${YELLOW}Copying binary...${NC}"
cp target/release/genj "$DEBIAN_DIR/usr/bin/genj"
chmod 755 "$DEBIAN_DIR/usr/bin/genj"

# Copy man pages (man1)
echo -e "${YELLOW}Copying man pages (section 1)...${NC}"
if [ -f "docs/man/man1/genj.1" ]; then
    cp docs/man/man1/genj.1 "$DEBIAN_DIR/usr/share/man/man1/genj.1"
    gzip -9 "$DEBIAN_DIR/usr/share/man/man1/genj.1"
else
    echo -e "${RED}Error: docs/man/man1/genj.1 man file not found${NC}"
    exit 1
fi

# Copy man pages (man5)
echo -e "${YELLOW}Copying man pages (section 5)...${NC}"
if [ -f "docs/man/man5/genj-template.5" ]; then
    cp docs/man/man5/genj-template.5 "$DEBIAN_DIR/usr/share/man/man5/genj-template.5"
    gzip -9 "$DEBIAN_DIR/usr/share/man/man5/genj-template.5"
else
    echo -e "${YELLOW}Warning: docs/man/man5/genj-template.5 man file not found (optional)${NC}"
fi

# Create control file
echo -e "${YELLOW}Creating control file...${NC}"
cat > "$DEBIAN_META/control" << EOF
Package: $PROJECT_NAME
Version: $VERSION
Architecture: $ARCHITECTURE
Maintainer: $MAINTAINER
Homepage: https://github.com/mcgivrer/genj
License: MIT
Description: $DESCRIPTION
 Generate a Java project from a template (ZIP file or folder).
 .
 Key Features:
  * Template support: ZIP files or directory structures
  * Variable replacement: \${PROJECT_NAME}, \${AUTHOR_NAME}, \${PACKAGE}, etc.
  * Build tool generation: Maven (pom.xml) or Gradle (build.gradle)
  * Development environment: VSCode configuration and Git setup
  * SDK management: .sdkmanrc file for SDKMan integration
 .
 This tool is designed for Java developers who need a quick and automated
 way to scaffold new projects with consistent structures and configurations.
 .
 Licensed under the MIT License - See /usr/share/doc/$PROJECT_NAME/copyright
 for full license details.
EOF
# copy metaino file
echo -e "${YELLOW}Copying metainfo file...${NC}"
mkdir -p "$DEBIAN_DIR/usr/share/metainfo"
cp ./docs/$PROJECT_NAME.metainfo.xml "$DEBIAN_DIR/usr/share/metainfo/$PROJECT_NAME.metainfo.xml"

# Create postinst script (run after installation)
echo -e "${YELLOW}Creating postinst script...${NC}"
cat > "$DEBIAN_META/postinst" << 'EOF'
#!/bin/bash
set -e

# Compress man pages if not already done
if [ -f /usr/share/man/man1/genj.1 ]; then
    gzip -9f /usr/share/man/man1/genj.1
fi

if [ -f /usr/share/man/man5/genj-template.5 ]; then
    gzip -9f /usr/share/man/man5/genj-template.5
fi

# Update man database
if command -v mandb &> /dev/null; then
    mandb -q
fi

echo "genj has been installed successfully."
echo "View the manual with: man genj"
echo "View the template guide with: man genj-template"
EOF

chmod 755 "$DEBIAN_META/postinst"

# Create prerm script (run before removal)
echo -e "${YELLOW}Creating prerm script...${NC}"
cat > "$DEBIAN_META/prerm" << 'EOF'
#!/bin/bash
set -e
echo "Removing genj..."
EOF

chmod 755 "$DEBIAN_META/prerm"

# Create postrm script (run after removal)
echo -e "${YELLOW}Creating postrm script...${NC}"
cat > "$DEBIAN_META/postrm" << 'EOF'
#!/bin/bash
set -e

# Update man database
if command -v mandb &> /dev/null; then
    mandb -q
fi
EOF

chmod 755 "$DEBIAN_META/postrm"

# Create changelog
echo -e "${YELLOW}Creating changelog...${NC}"
mkdir -p "$DEBIAN_DIR/usr/share/doc/$PROJECT_NAME"
cat > "$DEBIAN_DIR/usr/share/doc/$PROJECT_NAME/changelog.Debian" << EOF
${PROJECT_NAME} (${VERSION}) stable; urgency=medium

  * Version $VERSION release
  * Added comprehensive man page documentation (genj.1 and genj-template.5)
  * Supports Maven and Gradle builds
  * Automatic Git repository initialization
  * VSCode configuration generation
  * SDKMan environment file creation
  * Template creation guide

 -- ${MAINTAINER}  $(date -R)
EOF

# Create copyright file
cat > "$DEBIAN_DIR/usr/share/doc/$PROJECT_NAME/copyright" << EOF
Format: https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: genj
Upstream-Contact: Frédéric Delorme

Files: *
Copyright: 2025 Frédéric Delorme
License: MIT

License: MIT
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 .
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
EOF

# Gzip changelog
gzip -9 "$DEBIAN_DIR/usr/share/doc/$PROJECT_NAME/changelog.Debian"

# Calculate installed size
echo -e "${YELLOW}Calculating installed size...${NC}"
INSTALLED_SIZE=$(du -s "$DEBIAN_DIR" | cut -f1)

# Update control file with installed size
sed -i "1s/^/Installed-Size: $INSTALLED_SIZE\n/" "$DEBIAN_META/control"

# Build the .deb package
echo -e "${YELLOW}Building .deb package...${NC}"
DEB_PACKAGE="${PROJECT_NAME}_${VERSION}_${ARCHITECTURE}.deb"
dpkg-deb --build "$DEBIAN_DIR" "$DEB_PACKAGE"

# Move deb to current directory
mkdir -p ./target/package
mv "$DEB_PACKAGE" ./target/package/

echo -e "${GREEN}✓ Debian package created successfully: $DEB_PACKAGE${NC}"
echo -e "${GREEN}✓ Installation size: ${INSTALLED_SIZE} KB${NC}"

# Display package info
echo -e "\n${YELLOW}Package information:${NC}"
dpkg-deb -I "./target/package/$DEB_PACKAGE"

# Optional: Display contents
echo -e "\n${YELLOW}Package contents:${NC}"
dpkg-deb -c "./target/package/$DEB_PACKAGE"

echo -e "\n${YELLOW}To install the package:${NC}"
echo -e "  ${GREEN}sudo dpkg -i ./target/package/$DEB_PACKAGE${NC}"
echo -e "\n${YELLOW}To uninstall:${NC}"
echo -e "  ${GREEN}sudo apt remove ./target/package/$PROJECT_NAME${NC}"
echo -e "\n${YELLOW}After installation, consult the documentation:${NC}"
echo -e "  ${GREEN}man genj${NC}"
echo -e "  ${GREEN}man genj-template${NC}"