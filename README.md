# crDroid 13.0 - PalapaOS Redesign UI (Redmi Note 13 5G / gold & Generic Android 17)

This repository contains the user interface source code, XML layouts, and drawable assets for the **PalapaOS Redesign UI (Majapahit Edition)** and interactive **Dynamic Island**, adapted specifically for **crDroid 13.0 (Android 17)** on **Xiaomi Redmi Note 13 5G / Poco X6 Neo 5G (`gold`, MediaTek Dimensity 6080)** and generic crDroid 13.0 devices.

---

## Overview of Components & Features

### 1. Interactive Dynamic Island
- **Centered Punch-Hole Alignment**: Tailored for centered camera cutout displays.
- **Spring Overshoot Physics**: Smooth horizontal expansion and two-phase elastic recoil when expanding and dismissing.
- **Three Core System Events**:
  - **Charging**: Real-time battery percentage indicator with an animated neon green lightning bolt.
  - **Flashlight**: Persistent pill display while the flashlight is active. Tapping toggles off the torch instantly.
  - **Screen Recording**: Symmetrical capsule with a pulsing red indicator dot and running elapsed digital timer.
- **Dual-Island Meta-Ball Architecture & Expanded Island Card**.

### 2. Quick Settings One UI 9 Style (Jetpack Compose)
- **Single-Pull View (QQS / 1-Pull)**: 6 circular quick setting buttons, smooth brightness slider, and 2 large pill buttons for Wi-Fi and Bluetooth.
- **Expanded Shade View (Expanded QS / 2-Pull)**: Frosted glass container hosting circular quick tiles, integrated media volume slider, and sound mode selector.
- **Free 2D Tile Reordering (Edit Mode)**.

### 3. Settings Redesign (Adapted for crDroid 13.0 & Redmi Note 13 5G)
- **Homepage Quick Dashboard**:
  - Interactive resource monitor cards for Battery, Storage, RAM & CPU (`Dimensity 6080` / `D6080`), Display, and Sound.
  - Custom crDroid 13.0 greeting banner (`#crDroidOnTop!`).
- **About Phone & Android Version Dashboard**:
  - PalapaOS Majapahit Edition • crDroid 13.0 banner with hardware specs for **Redmi Note 13 5G (`gold`)** and **MediaTek Dimensity 6080 Octa-core**.

---

## Installation Guide (For crDroid 13.0)

```bash
cp -rf packages/apps/Settings/* /path/to/crDroid/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/crDroid/frameworks/base/packages/
```

Then compile with:
```bash
cd /path/to/crDroid
source build/envsetup.sh
brunch gold
```

---

## Credits & Acknowledgments
- **Original PalapaOS UI Redesign**: [Gustyx-Power/shinkai_project_palapaos_redesignui](https://github.com/Gustyx-Power/shinkai_project_palapaos_redesignui) by **Gustyx-Power**, **SkyyVerse**, and **kylieeXD**.
- **ROM Base**: [crDroid Android 13.0 (Android 17)](https://github.com/crdroidandroid/android).
- **Adapted & Maintained for Redmi Note 13 5G (`gold`) by**: **Asnalstan** ([danislascarcrit-max](https://github.com/danislascarcrit-max)).
