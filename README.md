# Shinkai Project - WowoOS Redesign UI (Settings & SystemUI)

This repository contains the full source files, assets, and patch files for the custom UI redesign implemented for Shinkai Project / WowoOS on Android 16 (marble).

---

## Overview of Components

### 1. Settings Redesign (`packages/apps/Settings`)
- **Homepage Quick Dashboard**:
  - Top header greeting and device identity badge.
  - Interactive status cards for Battery, Storage, RAM, Display, and Sound.
  - Dynamic resource meters and system shortcut integration.
- **About Phone Dashboard**:
  - Hero banner with customized device illustration and Shinkai Project branding.
  - Real-time spec cards: SoC, CPU architecture, Display refresh rate, Cameras, Android version.
  - Maintainer identity and Indonesian community badges.
  - Dedicated Device Status dashboard sub-fragment.
- **Dynamic RAM Expansion (zRAM)**:
  - Settings page allowing user configuration (Off, 2 GB, 4 GB, 8 GB, 16 GB).
  - Runtime disksize reallocation (`/sys/block/zram0/disksize` via swap reinitialization).
  - Accurate system-wide zRAM reflection verified in DevCheck and About Phone.
- **Display & Gesture Navigation Additions**:
  - One UI Quick Settings layout toggle switch in Display Settings.
  - Gesture navigation options to hide navbar pill and IME keyboard bottom spacing.

### 2. SystemUI One UI 9 Redesign (`frameworks/base/packages/SystemUI`)
- **One UI 9 Quick Settings Compose Architecture**:
  - Complete Jetpack Compose implementation in `OneUIQuickSettingsLayout.kt`.
  - Top status bar alignment without Pixel header duplicate clock/date.
  - Cellular carrier and network indicators positioned cleanly.
- **QQS (Quick Quick Settings) Single-Swipe View**:
  - Dual-pill compact layout on initial shade pull-down.
  - Smooth expansion transition without card clipping or sticking artifacts.
- **Brightness & Media Controls**:
  - Seamless brightness slider with zero flicker during and after dragging.
  - Integrated volume slider and Sound mode selector (Ring / Vibrate / Silent).
  - Dedicated media playback container below brightness controls.
- **Tile Reordering & Grid Customization**:
  - Edit mode supporting drag-and-drop tile rearrangement.
  - Expandable grid view supporting 8+ quick settings toggles with scrollable pages.

---

## Repository Structure

```
.
|-- README.md
|-- changelogs.md
|-- patches/
|   |-- 0001-Settings-redesign.patch
|   `-- 0002-SystemUI-OneUI-redesign.patch
|-- frameworks/
|   `-- base/
|       `-- packages/
|           `-- SystemUI/
|               |-- compose/features/src/com/android/systemui/qs/ui/composable/
|               |   |-- OneUIQuickSettingsLayout.kt
|               |   `-- QuickSettingsShadeOverlay.kt
|               `-- src/com/android/systemui/
|                   |-- qs/composefragment/QSFragmentCompose.kt
|                   `-- shade/ShadeHeaderController.kt
`-- packages/
    `-- apps/
        `-- Settings/
            |-- res/
            `-- src/
```

---

## How to Apply to Source Tree

### Method 1: Applying via Git Patch (Recommended)

From your ROM source tree root:

1. **Apply Settings Patch**:
   ```bash
   cd packages/apps/Settings
   git apply --check ../../../patches/0001-Settings-redesign.patch
   git apply ../../../patches/0001-Settings-redesign.patch
   cd ../../..
   ```

2. **Apply SystemUI Patch**:
   ```bash
   cd frameworks/base
   git apply --check ../../patches/0002-SystemUI-OneUI-redesign.patch
   git apply ../../patches/0002-SystemUI-OneUI-redesign.patch
   cd ../..
   ```

### Method 2: Direct File Copy

From the root of this repository:
```bash
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/SystemUI/* /path/to/rom/frameworks/base/packages/SystemUI/
```

---

## Changelog Details

See [changelogs.md](changelogs.md) for full engineering details and release notes.