# Shinkai Project - PalapaOS Redesign UI (POCO F5 & Generic Android 17)

This repository contains the complete user interface source code, XML layouts, drawable assets, kernel scripts, and system configurations for the One UI 9-inspired redesign and interactive Dynamic Island implemented for Shinkai Project / PalapaOS (Majapahit Edition) on Android 17 (POCO F5 / marble and generic AOSP devices).

---

## Overview of Components & Features

### 1. Interactive Dynamic Island
- **POCO F5 Punch-Hole Alignment**: Geometrically tailored for the centered camera cutout of POCO F5 (`device/xiaomi/marble`).
- **Spring Overshoot Physics**: Smooth horizontal expansion and two-phase elastic recoil when expanding and dismissing.
- **Three Core System Events**:
  - **Charging**: Real-time battery percentage indicator with an animated neon green lightning bolt (7.8s presentation duration).
  - **Flashlight**: Persistent pill display while the flashlight is active. Tapping toggles off the torch instantly.
  - **Screen Recording**: Symmetrical capsule (~200dp) with a pulsing red indicator dot on the left, a 36dp camera clearance spacer in the center, and a running elapsed digital timer on the right.
- **Dual-Island Meta-Ball Architecture**: When screen recording and flashlight run concurrently, recording takes the main capsule and the flashlight becomes a secondary circular bubble (meta-ball) to the right.
- **Swipe-to-Swap Priority**: Horizontal swipe gesture smoothly swaps roles between the main capsule and the meta-ball.
- **Expanded Island Card**: Tapping the compact pill opens a floating expanded control card with quick action buttons (Stop, Pause, Toggle Flashlight) and a large digital readout.

### 2. Quick Settings One UI 9 (Jetpack Compose)
- **Single-Pull View (QQS / 1-Pull)**:
  - Row 1: 6 circular quick setting buttons (1x1 format).
  - Row 2: Smooth brightness slider without touch flicker.
  - Row 3: 2 large pill buttons (2x1 format) for Wi-Fi and Bluetooth.
  - Live media player integration when audio is actively playing.
- **Expanded Shade View (Expanded QS / 2-Pull)**:
  - Frosted glass container hosting circular quick tiles.
  - Integrated media volume slider and sound mode selector (Ring, Vibrate, Silent).
  - Clean placement for power menu and settings shortcuts.
- **Free 2D Tile Reordering (Edit Mode)**:
  - Long-press and drag tiles freely in any direction (horizontal or vertical).
  - Floating 'Done' button at the bottom center of the screen for one-handed reachability.

### 3. Settings Redesign
- **Homepage Quick Dashboard**:
  - Interactive resource monitor cards for Battery, Storage, RAM, Display, and Sound.
  - Notification Panel Button: Direct quick access to Notification Settings on the homepage dashboard.
  - Accessibility Menu: Preserved in the primary settings list under the support category.
- **Colored Notification Icons**:
  - Toggle in Notification Settings to display authentic, full-color application logos across status bar icons and shade notification cards instead of monochrome silhouettes.
- **About Phone Dashboard**:
  - Shinkai Project visual banner with real-time hardware spec meters (SoC, CPU architecture, display refresh rate, cameras) and community credits.

### 4. System, Kernel & Audio Optimizations
- **Dynamic RAM Expansion (zRAM)**:
  - Real Linux kernel zRAM reallocation directly configurable via Settings (Options: Disabled/0 GB, 2 GB, 4 GB, 8 GB, 16 GB).
- **Native Hardware Dolby Atmos**:
  - Hardware DSP offload with built-in MarbleDolby control application (AutoEQ and 10-band equalizer) without requiring root or KernelSU modules.
- **Clean Navigation Customizations**:
  - Toggles to hide the bottom navigation pill and remove bottom keyboard insets.

---

## Repository Structure

```
.
|-- README.md
|-- changelogs.md
|-- changelogs-en.md
|-- changelogs-id.md
|-- apply.sh
|-- temp.sh
|-- build/
|   `-- blueprint/bootstrap/
|-- device/
|   `-- xiaomi/
|       `-- marble/
|           |-- rootdir/bin/ram_expansion.sh
|           |-- rootdir/bin/init.kernel.post_boot.sh
|           |-- rootdir/etc/init.marble.rc
|           `-- sepolicy/vendor/
|-- frameworks/
|   `-- base/
|       `-- packages/
|           |-- SystemUI/
|           |   |-- compose/features/src/com/android/systemui/qs/ui/composable/
|           |   `-- src/com/android/systemui/
|           `-- overlays/
`-- packages/
    `-- apps/
        `-- Settings/
            |-- res/
            `-- src/
```

---

## Installation Guide

### Method 1: Automated Script (apply.sh)

Use the provided `apply.sh` script to apply the changes automatically:

1. **Navigate to the repository directory**:
   ```bash
   cd shinkai_project_wowoos_redesignui
   chmod +x apply.sh
   ```

2. **Run according to your target**:

   - **A. For Any Other Device (UI Redesign Only)**:  
     Use this mode when porting the interface to devices other than POCO F5 to ensure your device tree is preserved:
     ```bash
     ./apply.sh /path/to/rom --ui-only
     ```

   - **B. For POCO F5 (marble) Full Setup**:  
     Applies UI Redesign, Marble Device Tree, Kernel zRAM fix, and SELinux policies:
     ```bash
     ./apply.sh /path/to/rom --all
     ```

   - **C. Interactive Mode**:  
     Run without arguments to select options interactively:
     ```bash
     ./apply.sh
     ```

---

### Method 2: Manual Installation

If you prefer copying files manually:

#### A. UI Redesign Only (Compatible with Any Device):
```bash
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/
```

#### B. Full Setup for POCO F5 (marble):
```bash
# 1. Interface Redesign
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/

# 2. Marble Device Tree (RAM Expansion & SELinux)
cp -rf device/xiaomi/marble/* /path/to/rom/device/xiaomi/marble/

# 3. Build Script
cp -f temp.sh /path/to/rom/
```

---

## Compilation Guide

Once files are synchronized into your ROM directory:

```bash
cd /path/to/rom

# 1. Initialize Android build environment
source build/envsetup.sh

# 2. Select build target
breakfast marble # or lunch your specific device target

# 3. Compile full ROM
./temp.sh # or m shinkai -j$(nproc)

# Alternative: Compile SystemUI independently for quick testing
m SystemUI -j$(nproc)
```

---

## Changelog & Release Notes

Detailed technical notes and changelogs are available in:
- English: [changelogs-en.md](changelogs-en.md)
- Indonesian: [changelogs-id.md](changelogs-id.md)