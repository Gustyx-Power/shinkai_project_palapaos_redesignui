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
    echo -e "\n${BOLD}Penggunaan:${NC}"
    echo -e "  $0 <ROM_ROOT_PATH> [MODE]"
    echo -e ""
    echo -e "${BOLD}Opsi MODE:${NC}"
    echo -e "  ${GREEN}--ui-only${NC}     : Hanya terapkan UI (Settings + SystemUI + Overlays)"
    echo -e "                    ${YELLOW}(Cocok untuk perangkat selain POCO F5 / marble)${NC}"
    echo -e "  ${GREEN}--all${NC}         : Terapkan semuanya (UI + Device Tree marble + Build fixes)"
    echo -e "                    ${YELLOW}(Khusus untuk POCO F5 / marble)${NC}"
    echo -e "  ${GREEN}--device-only${NC} : Hanya terapkan Device Tree marble & perbaikan RAM expansion"
    echo -e ""
    echo -e "${BOLD}Contoh:${NC}"
    echo -e "  $0 /home/user/shinkai --ui-only"
    echo -e "  $0 /home/user/shinkai --all"
    echo -e ""
    exit 1
}

# 1. Parse Argumen
TARGET_DIR="$1"
MODE="$2"

if [ -z "$TARGET_DIR" ]; then
    echo -ne "${YELLOW}Masukkan path root source ROM Anda: ${NC}"
    read -r TARGET_DIR
fi

# Validasi target direktori
if [ -z "$TARGET_DIR" ] || [ ! -d "$TARGET_DIR" ]; then
    echo -e "${RED}[ERROR] Direktori target tidak valid: '$TARGET_DIR'${NC}"
    usage
fi

TARGET_DIR="$(cd "$TARGET_DIR" && pwd)"

# Validasi apakah target benar-benar source tree Android
if [ ! -f "$TARGET_DIR/build/envsetup.sh" ] && [ ! -d "$TARGET_DIR/packages" ]; then
    echo -e "${YELLOW}[PERINGATAN] Direktori '$TARGET_DIR' sepertinya bukan root source tree Android!${NC}"
    echo -ne "${YELLOW}Apakah Anda yakin ingin melanjutkan? (y/N): ${NC}"
    read -r CONFIRM
    if [[ ! "$CONFIRM" =~ ^[yY]$ ]]; then
        echo -e "${RED}Dibatalkan.${NC}"
        exit 0
    fi
fi

# Jika mode tidak dispesifikasikan, tanyakan secara interaktif
if [ -z "$MODE" ]; then
    echo -e "\n${BOLD}Pilih mode instalasi yang diinginkan:${NC}"
    echo -e "  ${CYAN}[1]${NC} UI Saja (Settings + SystemUI + Overlays) - ${YELLOW}[Untuk Semua Device]${NC}"
    echo -e "  ${CYAN}[2]${NC} Semua (UI + Device Marble + Build Fix)   - ${YELLOW}[Khusus POCO F5 marble]${NC}"
    echo -e "  ${CYAN}[3]${NC} Device Marble Saja (Fix RAM Expansion)   - ${YELLOW}[Khusus POCO F5 marble]${NC}"
    echo -ne "${BOLD}Pilihan Anda (1/2/3) [Default 1]: ${NC}"
    read -r CHOICE
    case "$CHOICE" in
        2) MODE="--all" ;;
        3) MODE="--device-only" ;;
        *) MODE="--ui-only" ;;
    esac
fi

apply_ui() {
    echo -e "\n${BLUE}[1/2] Menerapkan Settings Redesign (Homepage, About Phone, RAM Expansion UI)...${NC}"
    mkdir -p "$TARGET_DIR/packages/apps/Settings"
    cp -rf "$SCRIPT_DIR/packages/apps/Settings/"* "$TARGET_DIR/packages/apps/Settings/"
    echo -e "  ${GREEN}✓ packages/apps/Settings berhasil disalin!${NC}"

    echo -e "\n${BLUE}[2/2] Menerapkan OneUI SystemUI (QQS 1-tarikan, Free 2D Drag Edit, Overlays)...${NC}"
    mkdir -p "$TARGET_DIR/frameworks/base/packages"
    cp -rf "$SCRIPT_DIR/frameworks/base/packages/"* "$TARGET_DIR/frameworks/base/packages/"
    echo -e "  ${GREEN}✓ frameworks/base/packages/ berhasil disalin!${NC}"
}

apply_device() {
    echo -e "\n${BLUE}[+] Menerapkan Device Tree Marble (RAM Expansion kernel fix, SELinux, Init RC)...${NC}"
    mkdir -p "$TARGET_DIR/device/xiaomi/marble"
    cp -rf "$SCRIPT_DIR/device/xiaomi/marble/"* "$TARGET_DIR/device/xiaomi/marble/"
    echo -e "  ${GREEN}✓ device/xiaomi/marble/ berhasil disalin!${NC}"
}

apply_build_fixes() {
    echo -e "\n${BLUE}[+] Menerapkan Build Fix & Script (Soong memory limit & temp.sh)...${NC}"
    if [ -d "$SCRIPT_DIR/build/blueprint" ]; then
        mkdir -p "$TARGET_DIR/build/blueprint"
        cp -rf "$SCRIPT_DIR/build/blueprint/"* "$TARGET_DIR/build/blueprint/"
        echo -e "  ${GREEN}✓ build/blueprint/ berhasil disalin!${NC}"
    fi

    if [ -f "$SCRIPT_DIR/temp.sh" ]; then
        cp -f "$SCRIPT_DIR/temp.sh" "$TARGET_DIR/temp.sh"
        chmod +x "$TARGET_DIR/temp.sh"
        echo -e "  ${GREEN}✓ temp.sh berhasil disalin!${NC}"
    fi
}

echo -e "\n${CYAN}Memulai instalasi ke: ${BOLD}$TARGET_DIR${NC}"
echo -e "${CYAN}Mode: ${BOLD}$MODE${NC}"

case "$MODE" in
    --ui-only)
        apply_ui
        echo -e "\n${GREEN}${BOLD}=== SUKSES: UI Redesign Berhasil Diterapkan ke Perangkat Anda! ===${NC}"
        echo -e "Catatan: Device tree tidak disentuh, sehingga aman untuk device apa pun."
        ;;
    --device-only)
        apply_device
        echo -e "\n${GREEN}${BOLD}=== SUKSES: Device Tree Marble Berhasil Diterapkan! ===${NC}"
        ;;
    --all)
        apply_ui
        apply_device
        apply_build_fixes
        echo -e "\n${GREEN}${BOLD}=== SUKSES: Seluruh Komponen Berhasil Diterapkan! ===${NC}"
        echo -e "Anda sekarang dapat menjalankan build via:"
        echo -e "  ${CYAN}cd $TARGET_DIR && ./temp.sh${NC}"
        ;;
    *)
        echo -e "${RED}[ERROR] Mode tidak dikenali: '$MODE'${NC}"
        usage
        ;;
esac

echo -e "\n${CYAN}Selesai! Terima kasih telah menggunakan Shinkai Project Redesign UI.${NC}\n"
