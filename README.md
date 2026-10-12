# Shinkai Project - WowoOS Redesign UI (Settings, SystemUI & Device Tree)

This repository contains the full source files (XML layouts, drawables, assets, Kotlin/Java code, device tree scripts, SELinux policies, and build scripts) for the custom UI redesign implemented for Shinkai Project / WowoOS on Android 17 (marble - POCO F5).

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
  - Settings page allowing user configuration (Off/0 GB, 2 GB, 4 GB, 8 GB, 16 GB).
  - Runtime disksize reallocation (`/sys/block/zram0/disksize` via swap reinitialization).
  - Accurate system-wide zRAM reflection verified in DevCheck, Kernel Manager, and About Phone.
- **Display & Gesture Navigation Additions**:
  - One UI Quick Settings layout toggle switch in Display Settings.
  - Gesture navigation options to hide navbar pill and IME keyboard bottom spacing.

### 2. SystemUI One UI 9 Redesign (`frameworks/base/packages/SystemUI`)
- **One UI 9 Quick Settings Compose Architecture**:
  - Complete Jetpack Compose implementation in `OneUIQuickSettingsLayout.kt`.
  - Top status bar alignment without Pixel header duplicate clock/date.
  - Cellular carrier and network indicators positioned cleanly.
- **QQS (Quick Quick Settings) Single-Swipe View (1 Tarikan)**:
  - **Baris 1 (Atas)**: 1x6 circular quick tile buttons (bukan pill), lengkap dengan animasi active/inactive dan klik/long-click.
  - **Baris 2 (Tengah)**: Slidebar brightness responsif dengan gesture exclusion yang presisi.
  - **Baris 3 (Bawah)**: 2x1 big pill buttons untuk Wi-Fi dan Bluetooth.
  - Live media player terintegrasi saat memutar lagu.
  - Transisi mulus (`sharedElement` & `GridAnchor`) antara 1 tarikan (QQS) dan 2 tarikan (QS).
- **Brightness & Media Controls**:
  - Seamless brightness slider dengan zero-flicker selama dan setelah dragging.
  - Integrated volume slider dan Sound mode selector (Ring / Vibrate / Silent).
  - Media playback container di bawah kontrol slider.
- **Edit Quick Settings One UI**:
  - Sticky header bar dengan tombol Title, Subtitle, Reset, dan **Done** (tidak hilang/terpotong saat scroll).
  - Floating pill **Done** di bagian bawah layar untuk kemudahan jangkauan satu tangan.
  - **Free 2D Drag & Drop**: Tile dapat digeser bebas secara horizontal maupun vertikal antar baris dan kolom.
  - Visual feedback saat drag: Scale 1.15x, shadow elevation 16dp, zIndex mengambang, serta scroll layar dinonaktifkan sementara selama drag.
  - Badge `-` untuk menghapus dari tile aktif dan `+` untuk menambahkan dari daftar yang tersedia.

### 3. Device Tree & RAM Expansion Fix (`device/xiaomi/marble`)
- Skrip `/vendor/bin/ram_expansion.sh` dan `/vendor/bin/init.kernel.post_boot.sh` menggunakan aritmatika POSIX standar `$((ramExpansion * 1024))` yang kompatibel dengan `/vendor/bin/sh` (toybox).
- Penanganan mode `0` / `off` dengan pemutusan swap (`swapoff`) dan reset node zRAM.
- Init service `vendor.ram_expansion` di `init.marble.rc` yang terpicu secara instan saat properti `persist.vendor.ram_expansion` atau `persist.sys.ram_expansion` berubah.
- Sepolicy (`system_app.te`, `vendor_qti_init_shell.te`, `property.te`, `property_contexts`, `file_contexts`) untuk perizinan penuh tanpa SELinux denial.

### 4. Build Scripts & Environment (`build/`, `temp.sh`)
- `temp.sh`: Skrip build ROM non-trunking (target rilis `cp2a`) dengan proteksi memori cgroup (`GOMEMLIMIT=10GiB`, `GOMAXPROCS=8`, dan `-j32`).
- `build/blueprint/bootstrap/bootstrap.go`: Pengendalian alokasi memori Soong Blueprint agar tidak memicu OOM killer di server bersama.

---

## Repository Structure

```
.
|-- README.md
|-- changelogs.md
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

## How to Apply to Source Tree

Copy the full directories directly into your ROM source tree root:

```bash
# Dari root repositori ini:
cp -rf packages/apps/Settings/* /path/to/rom/packages/apps/Settings/
cp -rf frameworks/base/packages/* /path/to/rom/frameworks/base/packages/
cp -rf device/xiaomi/marble/* /path/to/rom/device/xiaomi/marble/
cp -rf build/blueprint/* /path/to/rom/build/blueprint/
cp -f temp.sh /path/to/rom/
```

---

## Changelog Details

Lihat [changelogs.md](changelogs.md) untuk detail perubahan teknik dan riwayat perbaikan lengkap.