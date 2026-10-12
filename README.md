# Shinkai Project - WowoOS Redesign UI (Settings, SystemUI & Device Tree)

This repository contains the complete source code, XML layouts, drawables, assets, device tree scripts, and SELinux policies for the One UI-inspired interface redesign implemented for Shinkai Project / WowoOS on Android 17 (POCO F5 / marble & generic devices).

---

## Overview of Components

### 1. Settings Redesign (`packages/apps/Settings`)
- **Homepage Quick Dashboard**:
  - Greeting header with custom maintainer and device status badge.
  - Interactive status cards for Battery, Storage, RAM, Display, and Sound.
  - Real-time resource meters and direct shortcut routing.
- **About Phone Dashboard**:
  - Hero banner with customized device illustration and Shinkai Project branding.
  - Real-time hardware spec cards: SoC, CPU architecture, Display refresh rate, Cameras, Android version.
  - Maintainer identity and Indonesian community credits.
  - Dedicated Device Status dashboard sub-fragment.
- **Dynamic RAM Expansion (zRAM)**:
  - Settings page allowing user configuration (Off/0 GB, 2 GB, 4 GB, 8 GB, 16 GB).
  - Runtime disksize reallocation (`/sys/block/zram0/disksize` via swap reinitialization).
  - Verified real-time reflection across the system (DevCheck, Kernel Manager, and Settings).
- **Display & Gesture Navigation Additions**:
  - One UI Quick Settings layout toggle switch in Display Settings.
  - Gesture navigation options to hide navbar pill and IME keyboard bottom spacing.

### 2. SystemUI One UI 9 Redesign (`frameworks/base/packages/SystemUI`)
- **One UI 9 Quick Settings Compose Architecture**:
  - Complete Jetpack Compose implementation in `OneUIQuickSettingsLayout.kt`.
  - Top status bar alignment without Pixel header duplicate clock/date views.
  - Clean placement for cellular carrier and network status indicators.
- **QQS (Quick Quick Settings) Single-Pull View (1-Pull)**:
  - **Row 1 (Top)**: 1x6 circular quick setting buttons (not pills), featuring active/inactive state animations and click/long-click interactions.
  - **Row 2 (Middle)**: Responsively integrated brightness slider wrapped in `systemGestureExclusionInShade`.
  - **Row 3 (Bottom)**: 2x1 large pill buttons (`OneUIBigPillTile`) for Wi-Fi and Bluetooth.
  - Live media player integration in single-pull shade when audio is active.
  - Symmetrical reverse scene transitions (`quickQuickSettingsToQuickSettings` and symmetrical `toEditMode`) ensuring fluid morphing between 1-pull and 2-pull shades.
- **Brightness & Media Controls**:
  - Seamless brightness slider with zero flicker during and after dragging.
  - Integrated volume slider and Sound mode selector (Ring / Vibrate / Silent).
  - Dedicated media playback container below brightness controls.
- **Edit Quick Settings One UI**:
  - Sticky header bar with Title, Subtitle, Reset, and **Done** buttons (never scrolls offscreen).
  - Floating pill **Done** button at the bottom center of the screen for one-handed reachability.
  - **Free 2D Drag & Drop**: Freely drag and reorder tiles horizontally and vertically across rows and columns.
  - Smooth visual feedback during drag: Scale 1.15x, shadow elevation 16dp, elevated zIndex, and automatic disablement of page scrolling during drag gestures.
  - Quick tap actions on badges (`-` to remove active tiles, `+` to add from available pool).

### 3. Device Tree & RAM Expansion Fix (`device/xiaomi/marble`)
- POSIX-compliant arithmetic `$((ramExpansion * 1024))` in `/vendor/bin/ram_expansion.sh` and `/vendor/bin/init.kernel.post_boot.sh` (resolving toybox `/vendor/bin/sh` syntax errors).
- Clean handling of `0` / `off` modes by executing `swapoff /dev/block/zram0` and resetting the zRAM node.
- Dedicated `vendor.ram_expansion` service in `init.marble.rc` triggered automatically upon property changes (`persist.vendor.ram_expansion` or `persist.sys.ram_expansion`).
- Complete SELinux policies (`system_app.te`, `vendor_qti_init_shell.te`, `property.te`, `property_contexts`, `file_contexts`) granting unblocked permission to configure zRAM.

### 4. Build Scripts & Environment (`build/`, `temp.sh`)
- `temp.sh`: Non-trunking build script (release target `cp2a`) with memory-safe limits (`GOMEMLIMIT=10GiB`, `GOMAXPROCS=8`, and `-j32`).
- `build/blueprint/bootstrap/bootstrap.go`: Hardened Soong Blueprint memory limits to prevent shared server cgroup OOM kills.

---

## Repository Structure

```
.
|-- README.md
|-- changelogs.md
|-- apply.sh
|-- temp.sh
|-- build/
|   `-- blueprint/bootstrap/
|       |-- bootstrap.go
|       `-- command.go
|-- device/
|   `-- xiaomi/
|       `-- marble/
|           |-- rootdir/bin/ram_expansion.sh
|           |-- rootdir/bin/init.kernel.post_boot.sh
|           |-- rootdir/etc/init.marble.rc
|           |-- sepolicy/vendor/
|           |-- device.mk
|           `-- ...
|-- frameworks/
|   `-- base/
|       `-- packages/
|           |-- SystemUI/
|           |   |-- compose/features/src/com/android/systemui/qs/ui/composable/
|           |   |   |-- OneUIQuickSettingsLayout.kt
|           |   |   `-- QuickSettingsShadeOverlay.kt
|           |   `-- src/com/android/systemui/
|           |       |-- qs/composefragment/QSFragmentCompose.kt
|           |       `-- shade/ShadeHeaderController.kt
|           `-- overlays/
|               |-- Android.bp
|               `-- NavigationBarModeGesturalHiddenBar*
`-- packages/
    `-- apps/
        `-- Settings/
            |-- res/
            `-- src/
```

---

## Installation Guide

### Method 1: Automated Script (`apply.sh`) [Recommended]

This repository includes a smart, flexible automated setup script [`apply.sh`](apply.sh):

1. **Clone this repository**:
   ```bash
   git clone https://github.com/Gustyx-Power/shinkai_project_wowoos_redesignui.git
   cd shinkai_project_wowoos_redesignui
   chmod +x apply.sh
   ```

2. **Run according to your target**:

   - **A. For Any Other Device (UI Redesign Only)**:  
     If you are porting the One UI Settings & SystemUI redesign to any phone other than POCO F5 (e.g. Pixel, Xiaomi, Realme, etc.) without touching or modifying your device tree:
     ```bash
     ./apply.sh /path/to/rom --ui-only
     ```

   - **B. For POCO F5 (marble) Full Setup**:  
     Applies UI Redesign + Marble Device Tree + Kernel zRAM Fix + Build Memory Fixes:
     ```bash
     ./apply.sh /path/to/rom --all
     ```

   - **C. Interactive Mode**:  
     Simply execute without flags, and the script will prompt you for the ROM path and mode:
     ```bash
     ./apply.sh
     ```

---

### Method 2: Manual Installation (Step-by-Step Copy)

If you prefer copying the files manually:

#### A. UI Redesign Only (Compatible with ANY Device):
```bash
# From the root of this repository:
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/
```
*(Your device tree remains completely intact).*

#### B. Full Setup specifically for POCO F5 / marble:
```bash
# From the root of this repository:
# 1. UI Redesign
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/

# 2. Marble Device Tree (RAM Expansion Kernel Fix & SELinux)
cp -rf device/xiaomi/marble/* /path/to/rom/device/xiaomi/marble/

# 3. Build Memory Fix (Prevents OOM Killer on Shared Servers) & Build Script
cp -rf build/blueprint/* /path/to/rom/build/blueprint/
cp -f temp.sh /path/to/rom/
```

---

## How to Build

Once the files are applied to your ROM tree:

```bash
cd /path/to/rom

# Setup environment
source build/envsetup.sh

# Select target device
breakfast marble # or lunch your specific device target

# Build full ROM
./temp.sh # or m shinkai -j32

# Or quickly compile and test SystemUI only:
m SystemUI -j32
```

---

## Changelog Details

See [changelogs.md](changelogs.md) for full engineering details and release notes.