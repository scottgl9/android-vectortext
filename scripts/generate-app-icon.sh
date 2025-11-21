#!/bin/bash
#
# Generate 512x512 PNG app icon from SVG
#
# This script converts the app-icon.svg to a 512x512 PNG file
# suitable for Google Play Console and other marketing materials.
#
# Requirements:
#   - rsvg-convert (librsvg2-bin package)
#
# Usage:
#   ./scripts/generate-app-icon.sh
#

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
SVG_FILE="$PROJECT_ROOT/app-icon.svg"
OUTPUT_FILE="$PROJECT_ROOT/app-icon-512x512.png"

echo "=================================================="
echo "  Vertext App Icon Generator"
echo "=================================================="
echo ""

# Check if rsvg-convert is installed
if ! command -v rsvg-convert &> /dev/null; then
    echo -e "${RED}Error: rsvg-convert not found${NC}"
    echo "Please install librsvg2-bin:"
    echo "  sudo apt-get install librsvg2-bin"
    exit 1
fi

# Check if SVG file exists
if [ ! -f "$SVG_FILE" ]; then
    echo -e "${RED}Error: app-icon.svg not found at $SVG_FILE${NC}"
    exit 1
fi

echo -e "${YELLOW}Input:${NC}  $SVG_FILE"
echo -e "${YELLOW}Output:${NC} $OUTPUT_FILE"
echo ""

# Generate 512x512 PNG
echo "Generating 512x512 PNG..."
rsvg-convert -w 512 -h 512 "$SVG_FILE" -o "$OUTPUT_FILE"

if [ $? -eq 0 ] && [ -f "$OUTPUT_FILE" ]; then
    FILE_SIZE=$(du -h "$OUTPUT_FILE" | cut -f1)
    echo -e "${GREEN}✓ Successfully generated app-icon-512x512.png (${FILE_SIZE})${NC}"
    echo ""
    echo "Icon ready for:"
    echo "  - Google Play Console"
    echo "  - GitHub repository"
    echo "  - Marketing materials"
    echo "  - Website/documentation"
else
    echo -e "${RED}✗ Failed to generate PNG${NC}"
    exit 1
fi

echo ""
echo "=================================================="
echo "  Done!"
echo "=================================================="
