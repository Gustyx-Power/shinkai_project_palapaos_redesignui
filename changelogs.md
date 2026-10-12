# System Update Changelog: WowoOS19Jt-BasedShinkaiProject (marble)

Build Artifact: WowoOS19Jt-BasedShinkaiProject-marble-17.zip  
Package Size: 2.9 GB  
Target Device: POCO F5 (marble)  
Platform Base: Android 17 (CP2A Release Target)  
Build Status: Completed Successfully (Exit Code 0)  

---

## Section I: Meeting Minutes & Executive Summary Version

### 1. Initial Problems & Requirements Discussed
During testing and review sessions, several core interface discrepancies and subsystem regressions were identified and corrected:

1. **Quick Settings (QS) Behavioral Inconsistencies & Visual Bugs**:
   - Media playback cards lacked preview integration during partial shade expansion.
   - Quick settings toggles (such as Flashlight) experienced two-way state synchronization failure (turning on was functional, but clicking again failed to turn off).
   - The brightness slider retained the stock Pixel layout, and dragging the slider caused the entire QS panel UI to disappear until touch release. Additionally, releasing the brightness slider caused a momentary visual flicker/glitch due to an asynchronous value race.
   - The power menu toggle was non-functional, and tapping the Settings shortcut opened Settings without automatically collapsing the notification shade.
   - Header indicators (clock, date, and battery percentage) exhibited duplicate rendering across status bar layers.

2. **One UI 9 Visual Language & Usability Requirements**:
   - Distinct separation of form factors: Pill-shaped toggles (2x1) must sit outside the primary card container.
   - Single-pull Quick Quick Settings (QQS) must display the top configured pill toggles (2x1 side-by-side) instead of an incongruous row of small circular buttons.
   - Smooth transition during double-pull (QQS to QS expanded) without leaving any stuck or lingering frosted card artifact above the header clock.
   - A dedicated frosted glass container must house circular toggles (1x1) arranged in a 4-column grid (holding up to 8 toggles in collapsed mode).
   - An interactive center strip indicator must be present at the bottom of the grid card to expand and reveal additional rows when more than 8 toggles are placed.
   - Introduction of a media volume slider alongside a circular sound mode toggle (Ring, Vibrate, Silent) directly beneath the brightness control.
   - Implementation of an intuitive Edit Mode allowing users to resize toggles between Pill and Circular formats, freely drag-and-drop toggles to any position via long-press gestures, and add or remove items without cluttered arrow buttons.

3. **RAM Expansion (zRAM) Malfunction**:
   - Altering RAM Expansion values in device settings (Off/0GB, 2GB, 8GB, 16GB) had zero effect on the Linux kernel. The kernel zRAM remained permanently locked at 4GB (4.29 GB reported in DevCheck and About Phone), ignoring user preferences across runtime sessions and reboots.

---

### 2. Agreed Architectural Decisions
1. **Hybrid One UI 9 Layout & Fluid Transitions**:
   - Single-pull QQS renders the top 2 pill toggles (Internet and Bluetooth by default) matching the top section of full QS.
   - Eliminate fixed background boxes from QQS so that double-pull expansion transitions cleanly without visual hitching.
   - Partition full quick tiles dynamically into separate layout zones: Top Pill Zone, Center Frosted Card Zone (1x1 quick toggles with expandable strip), Slider & Audio Controls Zone, and Bottom Pill Zone.
   - Provide an in-place Edit Mode engine with visual badges for instant form factor switching and long-press drag-to-reorder gesture support.
   - Integrate native Android AudioManager streams directly into the Quick Settings Compose tree.
   - Suppress duplicate clock and date views across all MotionLayout constraint sets (QQS, QS, and LargeScreen).

2. **Treble-Compliant SELinux Bridging & Persistent Swap Management**:
   - Bridge Android system properties to persistent vendor storage (`persist.vendor.ram_expansion`) at root init level, ensuring values survive reboots and are immediately readable by vendor services.
   - Grant necessary SELinux access (`sysfs_zram`, `swap_block_device`, and `sys_admin` capability) to the vendor execution context.
   - Execute an atomic swap cycle (swapoff, reset, disksize configuration, mkswap, and swapon) to prevent kernel write rejections, with full deactivation when set to 0GB (Off).

---

### 3. Implementation Outcomes
1. The Quick Settings panel now adheres strictly to the One UI 9 visual aesthetics:
   - Single-pull (QQS) renders:
     * Header Clock and Date with action shortcuts (Edit, Power, Settings) seamlessly aligned with QS.
     * Row 1: 1x6 circular quick toggle buttons (1x1 format).
     * Row 2: Custom One UI pill brightness slider (matching QS slider exactly, eliminating Pixel slider).
     * Row 3: 2x1 large pill buttons (Wi-Fi and Bluetooth side-by-side).
   - Double-pull expands smoothly into full QS without stuck cards or flickering.
   - The brightness slider updates with zero hitching or release flicker.
   - Instant SystemUI restart upon toggling QS style (Stock Pixel vs OneUI) in Settings.
2. Edit Mode allows fluid 2D drag-and-drop movements anywhere (even spinning in circles and dragging to the top edge) with elevation feedback, zero drop bugs, and instant Pill vs 1x1 size toggles.
3. RAM Expansion (zRAM) now incorporates the FurLC ZRAM method:
   - Sets exact byte sizes (`8589934592` for 8GB, `2147483648` for 2GB, `17179869184` for 16GB, 0 for Off).
   - Graceful `swapoff`, 5-iteration retry reset loop on `/sys/block/zram0/reset` to eliminate `-EBUSY` kernel denials.
   - Sets 8 multi-streams and `lz4` compression algorithm, followed by `mkswap`, `swapon`, and swappiness 100.
   - Immediately verifiable in `/proc/swaps`, DevCheck, and Kernel Manager.
4. About Phone / Firmware Version:
   - Dedicated row for **Versi Shinkai** (`ro.shinkai.version`).
   - Dedicated row for **Versi Wowo OS** with value **Embegeh-1T**.
4. RAM Expansion is fully dynamic: selecting +8 GB (or any other size) immediately updates `/sys/block/zram0/disksize` at kernel level, reflects accurately in DevCheck and About Phone (e.g. 8 GB + 8 GB), and persists across reboots.
5. Full system compilation finished cleanly without compiler warnings, syntax errors, or SELinux violations.

---

## Section II: Technical & Architectural Changelog (Developer Version)

### 1. SystemUI & Jetpack Compose Architecture

#### Affected Components:
- `OneUIQuickSettingsLayout.kt`
- `QSFragmentCompose.kt`
- `ShadeHeaderController.kt`

#### Technical Implementation Details:
1. **Dynamic Tile Partitioning & State Persistence**:
   - Integrated `rememberPillTileSpecs()` and `setTilePillMode()` backed by `Settings.System.putString(..., "qs_oneui_pill_tiles", ...)`.
   - Incoming tile streams are dynamically partitioned into:
     - `pillTiles`: Tile specifications designated as 2x1 pills (`it.spec.spec`).
     - `gridTiles`: Tile specifications designated as 1x1 circular buttons.
2. **Compact Quick Quick Settings (QQS - Single Pull)**:
   - Replaced the legacy 6-tile circular card with `OneUIBigPillTile` rendering the top 2 configured pill tiles side-by-side.
   - Removed static background wrappers to prevent visual clipping during scene transitions to expanded QS.
3. **Hybrid Layout Hierarchy (Full QS - Double Pull)**:
   - **Top Pill Section**: Houses the first two pill toggles outside the card container using `Row(horizontalArrangement = Arrangement.spacedBy(10.dp))`.
   - **Center Frosted Card (`OneUITileGridCard`)**:
     - Displays `gridTiles` in `GridCells.Fixed(4)` with circular backgrounds (`CircleShape`).
     - Implements collapsed state slicing: displays a maximum of 8 tiles when collapsed (`!isExpanded && gridTiles.size > 8`).
     - Houses an interactive horizontal indicator strip (`Box(40.dp x 4.dp)` with `RoundedCornerShape(2.dp)`) triggering `onClick = { isExpanded = !isExpanded }`, accompanied by `animateContentSize()`.
   - **Audio & Brightness Sliders**:
     - Resolved release glitch in `OneUIBrightnessSlider` by latching `sliderProgress` on `onDragEnd` and avoiding intermediate reverts to previous asynchronous framework values.
     - Integrated `OneUIVolumeSlider` bound to `AudioManager.STREAM_MUSIC`, with volume step tracking and mute toggling.
     - Integrated `OneUISoundModeButton` cycling sequentially through `RINGER_MODE_NORMAL` -> `RINGER_MODE_VIBRATE` -> `RINGER_MODE_SILENT`.
   - **Bottom Pill Section**: Renders remaining pill toggles positioned beneath the Media Player Card.
4. **Intuitive Drag-to-Reorder Edit Mode (`OneUIEditMode`)**:
   - Embedded `detectDragGesturesAfterLongPress` into active tile items with dynamic index displacement calculation (`dx`, `dy` thresholds).
   - Added visual lifting effects (`graphicsLayer { scaleX = 1.18f; scaleY = 1.18f; shadowElevation = 12f }`).
   - Cleaned up obsolete reordering arrow buttons `<` and `>`, leaving a minimal, modern interface with interactive size and removal badges.
5. **Header De-duplication**:
   - Updated `loadConstraints()` in `ShadeHeaderController.kt` to apply `applyOneUiConstraints()` across all three MotionLayout states: `QQS_HEADER_CONSTRAINT`, `QS_HEADER_CONSTRAINT`, and `LARGE_SCREEN_HEADER_CONSTRAINT`.
   - Suppressed stock Pixel `clock` and `date` views (`GONE`, `alpha = 0f`) and aligned `carrier_group` to parent start.

---

### 2. RAM Expansion Subsystem (Kernel, Init, SELinux, & Android Treble)

#### Affected Components:
- `ram_expansion.sh`
- `init.kernel.post_boot.sh`
- `init.marble.rc`
- `device.mk`
- `property.te`
- `property_contexts`
- `vendor_qti_init_shell.te`
- `RamExpansionSettings.kt`
- `RamExpansionPreferenceController.kt`

#### Root Cause Analysis:
1. **Property Volatility & Isolation**: `vendor.ram_expansion` was non-persistent and erased upon reboot. Vendor shell processes were blocked by SELinux from reading `persist.sys.*`. Post-boot scripts defaulted to `4096MB`.
2. **Missing SELinux Capabilities**: `vendor_qti_init_shell` lacked `sysfs_zram` read/write permissions and `CAP_SYS_ADMIN`, causing `swapoff`, `reset`, and `swapon` invocations to fail silently with permission denied errors.

#### Architectural Resolution:
1. **Persistent Vendor Property Registration**:
   - Declared `vendor_public_prop(vendor_ram_expansion_prop)` in `property.te`.
   - Mapped `persist.vendor.ram_expansion` and `vendor.ram_expansion` to `vendor_ram_expansion_prop:s0` in `property_contexts`.
2. **Init Root Synchronization**:
   - Added triggers in `init.marble.rc` on `persist.sys.ram_expansion=*`, `sys.boot_completed=1`, and `post-fs-data` to synchronize values to `persist.vendor.ram_expansion`.
   - Registered `/vendor/bin/ram_expansion.sh` under `vendor.ram_expansion` service with `seclabel u:r:vendor_qti_init_shell:s0`.
3. **SELinux Policy Access**:
   - Labeled `ram_expansion.sh` as `vendor_qti_init_shell_exec:s0` in `file_contexts`.
   - Granted `sysfs_zram:dir`, `sysfs_zram:file`, `swap_block_device:blk_file`, and `self:capability sys_admin` to `vendor_qti_init_shell`.
4. **Settings App Integration**:
   - Updated `RamExpansionSettings.kt` and `RamExpansionPreferenceController.kt` to read and synchronize directly with `persist.vendor.ram_expansion`.
5. **Atomic Swap Reset Routine**:
   - In `ram_expansion.sh` and `init.kernel.post_boot.sh`:
     - Reads `persist.vendor.ram_expansion` with unit normalization.
     - When set to `0` or `off`: executes `swapoff /dev/block/zram0` and writes `1` to `/sys/block/zram0/reset`.
     - When set to `> 0`: executes `swapoff` -> `reset` -> writes byte size to `/sys/block/zram0/disksize` -> `mkswap` -> `swapon /dev/block/zram0 -p 32758`.

---

### 3. Component Modification Summary

| Component Name | Scope | Summary of Modifications |
|---|---|---|
| `OneUIQuickSettingsLayout.kt` | UI / Feature | 2-pill QQS layout, flicker-free brightness slider, drag-to-reorder edit mode |
| `QSFragmentCompose.kt` | Architecture | One UI compose fragment bridge and view model integration |
| `ShadeHeaderController.kt` | Bugfix | Eliminated duplicate clock/date across QQS, QS, and LargeScreen constraint sets |
| `RamExpansionSettings.kt` | Settings / UI | Persistent vendor property write and state tracking |
| `RamExpansionPreferenceController.kt` | Settings / Core | Preference summary binding to persistent property |
| `ram_expansion.sh` | Daemon / Subsystem | Runtime swapoff, zRAM reset, and dynamic disksize allocation script |
| `init.kernel.post_boot.sh` | Kernel / Boot | Post-boot zRAM configuration utilizing persistent vendor property |
| `init.marble.rc` | Init / Triggers | Property synchronization triggers, service registration, and seclabel mapping |
| `device.mk` | Build Rules | Registered `ram_expansion.sh` into vendor `PRODUCT_COPY_FILES` |
| `file_contexts` | SELinux Context | Context mapping for `ram_expansion.sh` |
| `property.te` | SELinux Policy | Declared `vendor_ram_expansion_prop` public vendor property |
| `property_contexts` | SELinux Context | Context mapping for `persist.vendor.ram_expansion` |
| `vendor_qti_init_shell.te` | Security Access | Granted zram, swap block device, and sys_admin capability |
| `device.mk` (dolby) | Device Tree / Config | Enabled `hardware/dolby/dolby.mk` product inheritance |
| `BoardConfig.mk` (dolby) | VINTF / Build | Restored Dolby HAL & C2 manifests and framework matrix |
| `vendor.dolby.hardware.dms@2.0-service` | Vendor HAL | Native DMS HIDL daemon for Dolby effect management |
| `MarbleDolby` | UI / Application | Compose-based Dolby Atmos control app with AutoEQ & QS tile |

---

## Section IV: Latest Refinements & User Feedback Additions

### 1. OneUI QQS 1-Tarikan Layout Overhaul
- **Row 1 (Top)**: 1x6 circular quick setting buttons (`OneUIQQSCircularTileItem`), distinct from pill buttons, complete with active/inactive state animations and click/long-click interactions.
- **Row 2 (Middle)**: Responsively integrated brightness slider wrapped in `systemGestureExclusionInShade`.
- **Row 3 (Bottom)**: 2x1 large pill buttons (`OneUIBigPillTile`) for Wi-Fi and Bluetooth.
- Live media player integration in single-pull shade when audio is active.
- Symmetrical reverse scene transitions (`quickQuickSettingsToQuickSettings` and symmetrical `toEditMode`) ensuring fluid morphing between 1-pull and 2-pull shades.

### 2. Edit Quick Settings OneUI Experience
- **Sticky Done Controls**: Dual Done buttons—one fixed in the top sticky header bar (prevents scrolling offscreen) and an ergonomic floating Done pill at the bottom center of the screen for one-handed operation.
- **Free 2D Drag & Drop**: Removed `currentIndex` key dependency from `pointerInput` to ensure continuous gesture tracking without resets upon item reordering. Calculates relative 2D column delta (X axis) and row delta (Y axis) dynamically based on rendered tile boundaries.
- **Visual Feedback**: Elevated tile scale (1.15x), shadow elevation (16dp), zIndex layering (100f), and automatic disablement of page scrolling during drag gestures.
- **Action Badges**: Quick tap actions on badges (`-` to remove active tiles, `+` to add from available pool).

### 3. POSIX-Compliant zRAM Expansion Controller
- Replaced bash-only `let` commands with POSIX standard arithmetic `$((ramExpansion * 1024))` in `/vendor/bin/sh` (toybox ash).
- Full support for `0` and `off` modes with proper `swapoff` execution and zRAM device resets.
- Multi-stage property synchronization in `init.marble.rc` on `boot`, `post-fs-data`, and property change triggers to guarantee persistence across reboots.

---

## Section V: Native Hardware Dolby Atmos & MarbleDolby Integration

### 1. Requirements & System Overview
- **Hardware Dolby Support Verification**: Confirmed that POCO F5 (`marble`) hardware (Snapdragon 7+ Gen 2 / SM7475) natively supports Qualcomm Hexagon DSP hardware offload for Dolby Audio Processing (DAP).
- **Decoupling from Third-Party Root Modules**: Replaced the previous reliance on external KernelSU/Magisk modules (OnePlus 9 Pro Dolby Atmos + OPlus Core), which required intrusive overlayfs modules (`mountify`), non-native ColorOS frameworks (`oplus-framework-res`), and aggressive runtime daemon killing (`killall audioserver`/camera services) on AOSP systems.
- **Full In-Tree Integration**: Integrated the complete hardware Dolby Atmos subsystem directly into the ROM build tree via `hardware/dolby` (marble-indonesia `audio-fixed` branch).

### 2. Architectural & Build Implementation Details
1. **Device Tree Integration (`device/xiaomi/marble`)**:
   - `device.mk`: Re-enabled inheritance for `hardware/dolby/dolby.mk` (`$(call inherit-product, hardware/dolby/dolby.mk)`).
   - `BoardConfig.mk`: Restored VINTF HAL manifests (`vendor.dolby.hardware.dms@2.0-service.xml`, `vendor.dolby.media.c2.xml`) and the framework compatibility matrix (`dolby_framework_matrix.xml`).
   - `PRODUCT_SOONG_NAMESPACES`: Registered `hardware/dolby` into Soong namespaces cleanly.
2. **HAL Daemon & Hardware Offload (`hardware/dolby`)**:
   - `vendor.dolby.hardware.dms@2.0-service`: Dolby Mobile Service HIDL HAL running as a native vendor daemon with dedicated `init.dolby.rc`.
   - SoundFX & DSP Offload Libraries:
     - `libhwdap.so`: Hardware DAP effect library registered in `audio_effects.xml` under UUID `9d4921da-8225-4f29-aefa-39537a04bcaa`.
     - `libdlbvol.so`: Dolby Volume Listener (DVL) for music, ring, alarm, notification, and voice call streams.
     - `libswspatializer.so`: Dolby Spatial Audio processing.
     - `libswgamedap.so` & `libswvqe.so`: Dolby Game DAP and Voice Quality Enhancement.
   - Codec 2.0 decoders: AC4 (`libcodec2_soft_ac4dec`) and DDP (`libcodec2_soft_ddpdec`) alongside `c2.dolby.*`.
   - Compatibility shim: `libstagefright_foundation-v33.so` provided via `hardware/lineage/compat`.
3. **Frontend Application (`MarbleDolby`)**:
   - Modern Material 3 UI built with Jetpack Compose (`org.lunaris.dolby`), installed directly as a privileged system app at `system_ext/priv-app/MarbleDolby`.
   - Key Features:
     - 10-band interactive graphical equalizer with live frequency response curve.
     - AutoEQ database integration (hundreds of pre-calibrated headphone/earphone models).
     - Dolby profiles (Dynamic, Movie, Music, Custom).
     - Bass Boost, Dialogue Enhancer, Stereo Widening, and Volume Leveler controls.
     - Quick Settings tile service (`DolbyTileService`) for instant toggle access.
     - Per-app audio profile automation.

### 3. Verification & Compilation Status
- Target `MarbleDolby`: Successfully compiled, DEX-preopted, and verified cleanly (`out/target/product/marble/system_ext/priv-app/MarbleDolby/MarbleDolby.apk`).
- Target `vendor.dolby.hardware.dms@2.0-service`: Linked, verified against ELF rules, and installed cleanly into `/vendor/bin/hw/`.
- Full build check passed with exit code 0 (`#### build completed successfully ####`).


