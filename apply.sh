#!/usr/bin/env bash
#
# Shinkai Project / WowoOS Redesign UI - Installer & Patcher
# Copyright (C) 2026 Gustyx-Power / Shinkai Project
#

set -e

# ANSI Color Codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${CYAN}${BOLD}====================================================${NC}"
echo -e "${CYAN}${BOLD}   Shinkai Project - WowoOS Redesign UI Patcher     ${NC}"
echo -e "${CYAN}${BOLD}====================================================${NC}"

usage() {
    echo -e "\n${BOLD}Usage:${NC}"
    echo -e "  $0 <ROM_ROOT_PATH> [MODE]"
    echo -e ""
    echo -e "${BOLD}MODE Options:${NC}"
    echo -e "  ${GREEN}--ui-only${NC}     : Apply UI redesign only (Settings + SystemUI + Overlays)"
    echo -e "                    ${YELLOW}(Recommended for any non-marble device porting)${NC}"
    echo -e "  ${GREEN}--all${NC}         : Apply everything (UI + Device Tree marble + Build fixes)"
    echo -e "                    ${YELLOW}(Specifically tailored for POCO F5 / marble)${NC}"
    echo -e "  ${GREEN}--device-only${NC} : Apply POCO F5 marble device tree & RAM expansion fixes only"
    echo -e ""
    echo -e "${BOLD}Examples:${NC}"
    echo -e "  $0 /home/user/shinkai --ui-only"
    echo -e "  $0 /home/user/shinkai --all"
    echo -e ""
    exit 1
}

# 1. Parse Arguments
TARGET_DIR="$1"
MODE="$2"

if [ -z "$TARGET_DIR" ]; then
    echo -ne "${YELLOW}Enter your ROM source root directory path: ${NC}"
    read -r TARGET_DIR
fi

# Validate target directory
if [ -z "$TARGET_DIR" ] || [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}[ERROR] Invalid target directory: '$TARGET_DIR'${NC}"
    usage
fi

TARGET_DIR="$(cd "$TARGET_DIR" && pwd)"

# Verify whether the target is an Android source tree
if [ ! -f "$TARGET_DIR/build/envsetup.sh" ] && [ ! -d "$TARGET_DIR/packages" ]; then
    echo -e "${YELLOW}[WARNING] Directory '$TARGET_DIR' does not look like an Android ROM root tree!${NC}"
    echo -ne "${YELLOW}Are you sure you want to proceed? (y/N): ${NC}"
    read -r CONFIRM
    if [[ ! "$CONFIRM" =~ ^[yY]$ ]]; then
        echo -e "${RED}Aborted.${NC}"
        exit 0
    fi
fi

# Interactive mode selection if not specified via CLI arguments
if [ -z "$MODE" ]; then
    echo -e "\n${BOLD}Choose installation mode:${NC}"
    echo -e "  ${CYAN}[1]${NC} UI Only (Settings + SystemUI + Overlays) - ${YELLOW}[For ANY device / Generic porting]${NC}"
    echo -e "  ${CYAN}[2]${NC} Full Setup (UI + Marble Device Tree + Build Fix) - ${YELLOW}[POCO F5 marble only]${NC}"
    echo -e "  ${CYAN}[3]${NC} Marble Device Tree Only (RAM Expansion Fix)     - ${YELLOW}[POCO F5 marble only]${NC}"
    echo -ne "${BOLD}Your choice (1/2/3) [Default 1]: ${NC}"
    read -r CHOICE
    case "$CHOICE" in
        2) MODE="--all" ;;
        3) MODE="--device-only" ;;
        *) MODE="--ui-only" ;;
    esac
fi

apply_ui() {
    echo -e "\n${BLUE}[1/2] Applying Settings Redesign (Homepage, About Phone, RAM Expansion UI)...${NC}"
    mkdir -p "$TARGET_DIR/packages/apps/Settings"
    cp -rf "$SCRIPT_DIR/packages/apps/Settings/"* "$TARGET_DIR/packages/apps/Settings/"
    echo -e "  ${GREEN}✓ packages/apps/Settings successfully copied!${NC}"

    echo -e "\n${BLUE}[2/2] Applying OneUI SystemUI (QQS 1-pull, Free 2D Drag Edit, Overlays)...${NC}"
    mkdir -p "$TARGET_DIR/frameworks/base/packages"
    cp -rf "$SCRIPT_DIR/frameworks/base/packages/"* "$TARGET_DIR/frameworks/base/packages/"
    echo -e "  ${GREEN}✓ frameworks/base/packages/ successfully copied!${NC}"
}

apply_device() {
    echo -e "\n${BLUE}[+] Applying Marble Device Tree & System Init (RAM Expansion FurLC fix, SELinux, Init RC)...${NC}"
    mkdir -p "$TARGET_DIR/device/xiaomi/marble"
    cp -rf "$SCRIPT_DIR/device/xiaomi/marble/"* "$TARGET_DIR/device/xiaomi/marble/"
    echo -e "  ${GREEN}✓ device/xiaomi/marble/ successfully copied!${NC}"

    if [ -f "$SCRIPT_DIR/system/core/rootdir/init.rc" ]; then
        mkdir -p "$TARGET_DIR/system/core/rootdir"
        cp -f "$SCRIPT_DIR/system/core/rootdir/init.rc" "$TARGET_DIR/system/core/rootdir/init.rc"
        echo -e "  ${GREEN}✓ system/core/rootdir/init.rc successfully updated!${NC}"
    fi
}

apply_build_fixes() {
    echo -e "\n${BLUE}[+] Applying Build Fixes & Scripts (Soong memory limit & temp.sh)...${NC}"
    if [ -d "$SCRIPT_DIR/build/blueprint" ]; then
        mkdir -p "$TARGET_DIR/build/blueprint"
        cp -rf "$SCRIPT_DIR/build/blueprint/"* "$TARGET_DIR/build/blueprint/"
        echo -e "  ${GREEN}✓ build/blueprint/ successfully copied!${NC}"
    fi

    if [ -f "$SCRIPT_DIR/temp.sh" ]; then
        cp -f "$SCRIPT_DIR/temp.sh" "$TARGET_DIR/temp.sh"
        chmod +x "$TARGET_DIR/temp.sh"
        echo -e "  ${GREEN}✓ temp.sh successfully copied!${NC}"
    fi
}

echo -e "\n${CYAN}Starting installation to: ${BOLD}$TARGET_DIR${NC}"
echo -e "${CYAN}Target Mode: ${BOLD}$MODE${NC}"

case "$MODE" in
    --ui-only)
        apply_ui
        echo -e "\n${GREEN}${BOLD}=== SUCCESS: UI Redesign Applied Successfully to Your Source Tree! ===${NC}"
        echo -e "${YELLOW}${BOLD}[IMPORTANT for Non-Shinkai ROMs & Generic Devices]:${NC}"
        echo -e "  1. Device tree was untouched (100% safe for your device)."
        echo -e "  2. If using another ROM (LineageOS, PixelOS, crDroid, etc.), build via your ROM's"
        echo -e "     standard command (e.g. 'm bacon' or test 'm SystemUI -j\$(nproc)'). Do NOT use 'm shinkai'."
        echo -e "  3. If your ROM has a custom settings dashboard, check 'top_level_settings.xml' and"
        echo -e "     'SettingsGateway.java' to preserve custom preference categories."
        ;;
    --device-only)
        apply_device
        echo -e "\n${GREEN}${BOLD}=== SUCCESS: Marble Device Tree Applied Successfully! ===${NC}"
        ;;
    --all)
        apply_ui
        apply_device
        apply_build_fixes
        echo -e "\n${GREEN}${BOLD}=== SUCCESS: All Components Applied Successfully! ===${NC}"
        echo -e "You can now start building via:"
        echo -e "  ${CYAN}cd $TARGET_DIR && ./temp.sh${NC}"
        ;;
    *)
        echo -e "${RED}[ERROR] Unrecognized mode: '$MODE'${NC}"
        usage
        ;;
esac

echo -e "\n${CYAN}Done! Thank you for using Shinkai Project Redesign UI.${NC}\n"
