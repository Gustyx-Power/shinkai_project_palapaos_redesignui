# System Update Changelog: WowoOS Shinkai Project (POCO F5 / marble)

Target Device: POCO F5 (marble)  
Android Version: Android 17 (CP2A)  
Package Size: ~2.9 GB  
Compilation Status: Cleanly Prepared (Exit Code 0)  

Document Navigation:
- [English Version (changelogs-en.md)](changelogs-en.md)
- [Indonesian Version (changelogs-id.md)](changelogs-id.md)

---

## Update Summary (Simple & Concise Version)

This update delivers a complete overhaul of the user interface (UI Redesign) inspired by One UI 9, introduces the interactive Dynamic Island feature, enhances the Settings menu, and boosts system performance and hardware audio:

1. Interactive Dynamic Island (POCO F5 Punch-Hole)
   - Charging Indicator: Displays an animated green lightning bolt and live battery percentage for 7.8 seconds when plugged in.
   - Flashlight Indicator: Persistent pill display while the flashlight remains on. Tap once to instantly toggle off the torch.
   - Screen Recording Indicator: Symmetrical pill (~200dp) with a running digital timer matching iOS proportions.
   - Dual-Island (Meta-Ball): When recording and flashlight run simultaneously, recording occupies the main capsule and flashlight forms a circular bubble (meta-ball) beside it.
   - Swipe to Swap: Swipe left or right on the Dynamic Island to seamlessly swap roles between screen recording and flashlight.
   - Floating Expanded Card: Tap the Dynamic Island to open an expanded control panel with stop, pause, or torch extinguish buttons.

2. Redesigned Quick Settings (One UI 9 Style)
   - Single-Pull View (QQS): A clean layout featuring 6 circular toggles at the top, a flicker-free brightness slider in the middle, and 2 large pill buttons (Wi-Fi and Bluetooth) at the bottom.
   - Full Expanded View (Expanded QS): Frosted glass card for quick toggles, integrated media volume slider, sound mode selector (Ring, Vibrate, Silent), and direct power/settings shortcuts.
   - Free 2D Edit Mode: Long-press and drag tiles freely in any direction (horizontal and vertical) with elevation lift effects and an ergonomic floating Done button reachable with one hand.
   - Integrated Music Player: Live music playback controls inside the shade without disrupting overall layout.

3. Settings Menu Enhancements
   - Homepage Quick Dashboard: Real-time status cards to monitor Battery, Storage, RAM, Display, and Sound at a glance.
   - New Notification Panel Button: The Accessibility card on the dashboard has been replaced with a direct Notification shortcut. Accessibility remains intact in the primary settings list.
   - Colored Notification Icons: New option in Notification Settings to show genuine, full-color app logos on status bar icons and shade notification cards instead of monochrome silhouettes.
   - About Phone Dashboard: Shinkai Project visual banner with hardware spec meters (SoC, display, camera) and community contributor credits.

4. System & Performance Improvements
   - Dynamic RAM Expansion (zRAM): True zRAM configuration directly tied to the Linux kernel (Options: Disabled, 2 GB, 4 GB, 8 GB, 16 GB), verifiable in system monitoring tools.
   - Native Hardware Dolby Atmos: Built-in hardware DSP offload with MarbleDolby application (AutoEQ and 10-band equalizer) without requiring root or KernelSU modules.
   - Clean Navigation: Toggles to hide the bottom navigation pill and eliminate empty space below the keyboard.

---

## Detailed Technical Notes (Engineering Sections)

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
5. RAM Expansion is fully dynamic: selecting +8 GB (or any other size) immediately updates `/sys/block/zram0/disksize` at kernel level, reflects accurately in DevCheck and About Phone (e.g. 8 GB + 8 GB), and persists across reboots.
6. Full system compilation finished cleanly without compiler warnings, syntax errors, or SELinux violations.

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
     * `pillTiles`: Tile specifications designated as 2x1 pills (`it.spec.spec`).
     * `gridTiles`: Tile specifications designated as 1x1 circular buttons.
2. **Compact Quick Quick Settings (QQS - Single Pull)**:
   - Replaced the legacy 6-tile circular card with `OneUIBigPillTile` rendering the top 2 configured pill tiles side-by-side.
   - Removed static background wrappers to prevent visual clipping during scene transitions to expanded QS.
3. **Hybrid Layout Hierarchy (Full QS - Double Pull)**:
   - **Top Pill Section**: Houses the first two pill toggles outside the card container using `Row(horizontalArrangement = Arrangement.spacedBy(10.dp))`.
   - **Center Frosted Card (`OneUITileGridCard`)**:
     * Displays `gridTiles` in `GridCells.Fixed(4)` with circular backgrounds (`CircleShape`).
     * Implements collapsed state slicing: displays a maximum of 8 tiles when collapsed (`!isExpanded && gridTiles.size > 8`).
     * Houses an interactive horizontal indicator strip (`Box(40.dp x 4.dp)` with `RoundedCornerShape(2.dp)`) triggering `onClick = { isExpanded = !isExpanded }`, accompanied by `animateContentSize()`.
   - **Audio & Brightness Sliders**:
     * Resolved release glitch in `OneUIBrightnessSlider` by latching `sliderProgress` on `onDragEnd` and avoiding intermediate reverts to previous asynchronous framework values.
     * Integrated `OneUIVolumeSlider` bound to `AudioManager.STREAM_MUSIC`, with volume step tracking and mute toggling.
     * Integrated `OneUISoundModeButton` cycling sequentially through `RINGER_MODE_NORMAL` -> `RINGER_MODE_VIBRATE` -> `RINGER_MODE_SILENT`.
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
     * Reads `persist.vendor.ram_expansion` with unit normalization.
     * When set to `0` or `off`: executes `swapoff /dev/block/zram0` and writes `1` to `/sys/block/zram0/reset`.
     * When set to `> 0`: executes `swapoff` -> `reset` -> writes byte size to `/sys/block/zram0/disksize` -> `mkswap` -> `swapon /dev/block/zram0 -p 32758`.

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
     * `libhwdap.so`: Hardware DAP effect library registered in `audio_effects.xml` under UUID `9d4921da-8225-4f29-aefa-39537a04bcaa`.
     * `libdlbvol.so`: Dolby Volume Listener (DVL) for music, ring, alarm, notification, and voice call streams.
     * `libswspatializer.so`: Dolby Spatial Audio processing.
     * `libswgamedap.so` & `libswvqe.so`: Dolby Game DAP and Voice Quality Enhancement.
   - Codec 2.0 decoders: AC4 (`libcodec2_soft_ac4dec`) and DDP (`libcodec2_soft_ddpdec`) alongside `c2.dolby.*`.
   - Compatibility shim: `libstagefright_foundation-v33.so` provided via `hardware/lineage/compat`.
3. **Frontend Application (`MarbleDolby`)**:
   - Modern Material 3 UI built with Jetpack Compose (`org.lunaris.dolby`), installed directly as a privileged system app at `system_ext/priv-app/MarbleDolby`.
   - Key Features:
     * 10-band interactive graphical equalizer with live frequency response curve.
     * AutoEQ database integration (hundreds of pre-calibrated headphone/earphone models).
     * Dolby profiles (Dynamic, Movie, Music, Custom).
     * Bass Boost, Dialogue Enhancer, Stereo Widening, and Volume Leveler controls.
     * Quick Settings tile service (`DolbyTileService`) for instant toggle access.
     * Per-app audio profile automation.

### 3. Verification & Compilation Status
- Target `MarbleDolby`: Successfully compiled, DEX-preopted, and verified cleanly (`out/target/product/marble/system_ext/priv-app/MarbleDolby/MarbleDolby.apk`).
- Target `vendor.dolby.hardware.dms@2.0-service`: Linked, verified against ELF rules, and installed cleanly into `/vendor/bin/hw/`.
- Full build check passed with exit code 0 (`#### build completed successfully ####`).

### 4. Bootloop Root-Cause Analysis & Fix
- **Codec2 / Dolby Vision Conflicts Decoupled**: Removed conflicting `vendor.dolby.media.c2@1.0-service` and `dolbycodec2` video/audio services and deactivated `vendor.audio.c2.preferred=true`, deferring media decoding directly to Qualcomm native C2 HALs while reserving Dolby strictly for audio post-processing.
- **Removed Missing Sensor Constraints**: Deactivated `ro.audio.headtracking_enabled=true` and excluded `android.hardware.sensor.dynamic.head_tracker.xml` to avoid AudioService initialization deadlocks on devices without dynamic head trackers.
- **Shifted Init Triggers to post-fs-data**: Moved Dolby data directory creation from `early-boot` to `post-fs-data` in `init.dolby.rc` to support clean flashing with encrypted/formatted `/data`.
- **Early-Boot Persistent ADB Debugging**: Enabled `persist.sys.usb.config=adb` and `ro.adb.secure=0` in `system.prop` and `vendor.prop` to ensure instant USB enumeration and live logcat tracing from the very first seconds of boot.

---

## Section VI: Dedicated Control Center Subpage & Visual Style Selector

### 1. Requirements & Overview
- **Replaced Inline Toggle**: Previously, switching between One UI Control Center and stock Pixel Quick Settings was managed via a single inline switch toggle in Display Settings (`qs_oneui_style`).
- **Dedicated Subpage Experience**: Elevated the selection experience into a dedicated subpage entitled **Pusat kontrol** (Indonesian) / **Control Center** (English), featuring side-by-side visual smartphone mockup previews inspired by modern HyperOS / One UI layouts.

### 2. Implementation Details
1. **Frontend Preference Architecture (`packages/apps/Settings`)**:
   - `display_settings.xml`: Replaced the legacy `SystemSettingSwitchPreference` with a navigation `Preference` (`control_center_settings`) pointing to `com.android.settings.display.ControlCenterSettings`.
   - `ControlCenterPreferenceController.java`: Provides dynamic subtitle summaries on the Display settings menu (displaying `One UI` or `Pixel (Default)` depending on current selection).
   - `SettingsGateway.java`: Registered `ControlCenterSettings` into `ENTRY_FRAGMENTS` to ensure secure SubSettingLauncher fragment navigation without runtime intent violations.
2. **Visual Selector & Interactive Hero Card**:
   - `control_center_settings.xml` & `control_center_layout.xml`: Premium rounded card (24dp radius, `@color/settingslib_materialColorSurfaceBright`) housing two interactive phone mockups side-by-side:
     * **Option 1 (One UI / Separate)**: Custom vector illustration (`ic_cc_preview_oneui.xml`) displaying split Control Center layout with large connectivity cards (Wi-Fi/Bluetooth), dual brightness/volume sliders, and small tile grid, labeled "Separate / One UI" with a radio button.
     * **Option 2 (Pixel / Unified)**: Custom vector illustration (`ic_cc_preview_pixel.xml`) displaying stock merged Quick Settings layout with 2x2 pill buttons, brightness slider, and notification cards, labeled "Unified / Pixel (Default)" with a radio button.
   - **Dynamic Description**: Contextual description text updating instantly upon selection:
     * *One UI*: "Show Control Center and Notification Center separately. Swipe down from the top right of the screen to open Control Center."
     * *Pixel*: "Show Quick Settings and notifications merged in the default Google Pixel layout."
   - **Seamless SystemUI Refresh**: Tapping either option updates `Settings.System ("qs_oneui_style")` and automatically restarts SystemUI for an instantaneous UI transition.
3. **Localization**:
   - Comprehensive string resources added to `shinkai_strings.xml` (EN) and `values-in/shinkai_strings.xml` (ID) for native Indonesian and English experiences.

---

## Section VII: Redesigned One UI Compact Media Player, Carrier Relocation, & Frosted Glass Refinement

### 1. Requirements & Overview
- **Refined Frosted Glass Palette**: The initial frosted glass palette in One UI Quick Settings was overly milky-white (22%–54% white opacity), washing out the background and obscuring system wallpaper blurs. Refined to deep translucent smoky glass with luminous rims.
- **Relocated Carrier Text**: In One UI Control Center mode, the carrier name was awkwardly positioned above the battery indicator inside the status bar header. Moved to the top-left directly above the digital clock in `OneUIHeaderClockRow`.
- **Compact One UI Media Player Card & QQS Integration**:
  * Added media player card to QQS (1-pull) positioned directly below the 2x1 pill tiles (Wi-Fi & Bluetooth).
  * Redesigned the bulky Pixel UMO layout into a compact (~130dp) Samsung One UI-style card featuring album art background, top header row, dynamic cyan wave seekbar/timeline, and bottom playback controls.
  * Ensured shared-element animated transition between QQS (1-pull) and QS (2-pull) for fluid shade expansion.
  * Preserved stock Google Pixel QS media player untouched when One UI mode is disabled.

### 2. Implementation Details

1. **Frosted Glass Blur Palette Refinement (`OneUIQuickSettingsLayout.kt`)**:
   - Replaced flat milky-white containers with deep smoky translucent glass tokens:
     * Dark Mode: `cardBg = Color(0x3514171E)`, luminous glass border `cardBorder = Color(0x24FFFFFF)`, `pillInactiveBg = Color(0x26FFFFFF)`.
     * Light Mode: `cardBg = Color(0x66FFFFFF)`, `cardBorder = Color(0x24000000)`, `pillInactiveBg = Color(0x14000000)`.
   - Updated slider track backgrounds in `OneUIBrightnessSlider` and `OneUIVolumeSlider` to `Color(0x26FFFFFF)` to allow native wallpaper blur to shine through with rich depth and clarity.
   - Synchronized palette tokens across Quick Settings, QQS, and Tile Edit Mode (`OneUIEditMode`).

2. **Carrier Relocation Above Clock (`ShadeHeaderController.kt` & `OneUIQuickSettingsLayout.kt`)**:
   - `ShadeHeaderController.kt`: In `applyOneUiConstraints` and `updatePosition`, set `carrier_group` and `mShadeCarrierGroup` to `View.GONE` with `alpha = 0f` when `qs_oneui_style` is active.
   - `OneUIQuickSettingsLayout.kt`:
     * Created `rememberCarrierName()` composable observing `TelephonyManager` and `SubscriptionManager` (via `activeSubscriptionInfoList` and broadcast listeners).
     * Placed `carrierText` in `OneUIHeaderClockRow` in a `Column` directly above the 28sp digital clock time with `12sp, FontWeight.Medium, color = textSecondary`.

3. **Compact One UI Media Player Card (`OneUICompactMediaPlayer`)**:
   - **Form Factor**: Compact ~130dp height with 26dp rounded corner radius (`RoundedCornerShape(26.dp)`), glass border, and cropped album artwork background with vertical gradient scrim (`Brush.verticalGradient(listOf(Color(0x9905070A), Color(0xCC05070A)))`).
   - **Top Header Row**:
     * Left: "This phone" device chip with rounded glass pill (`Color(0x38FFFFFF)`).
     * Center/Left: Active app icon (via `PackageManager`) + single-line marquee/ellipsis text for `Title • Artist`.
     * Right: Circular output switcher button (26dp, `ic_speaker_on`) that triggers `com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG`.
   - **Wave Seekbar (`OneUIWaveSeekbar`)**:
     * Elapsed time on left (`mm:ss`) and duration on right (`mm:ss`).
     * Liquid cyan wave curve (`Color(0xFF29B6F6)`) along the played track with cubic bezier crests/troughs and gradient fill underneath.
     * Scrubber thumb at progress position (cyan outer circle with white center dot).
     * Unplayed track with smooth rounded grey line (`Color(0x40FFFFFF)`).
     * Interactive drag and tap gestures for seeking via `activeController.transportControls.seekTo()`.
   - **Bottom Media Controls**:
     * Shuffle button (`ic_oneui_media_shuffle`).
     * Previous track button (`ic_media_prev`).
     * Large circular frosted glass play/pause button (42dp, `ic_oneui_media_play` / `ic_oneui_media_pause`).
     * Next track button (`ic_media_next`).
     * Favorite / check button (`ic_oneui_media_check`).
   - **Transition & QQS Placement**:
     * In QQS: Placed directly below the 2x1 pills (Wi-Fi & Bluetooth).
     * In QS: Placed at Card 5 below volume/sound mode sliders.
     * Both wrapped in `Modifier.element(OneUIElements.MediaPlayer)` in `SceneTransitionLayout` for buttery smooth expansion animations between 1-pull and 2-pull states.
   - **Vector Assets Added**:
     * `packages/SystemUI/res/drawable/ic_oneui_media_play.xml`
     * `packages/SystemUI/res/drawable/ic_oneui_media_pause.xml`
     * `packages/SystemUI/res/drawable/ic_oneui_media_shuffle.xml`
     * `packages/SystemUI/res/drawable/ic_oneui_media_check.xml`

---

## Section VIII: One UI Media Player Geometry Refinement & QS Header Status Icon Clipping Fix

### 1. Requirements & Overview
- **Centered Track Info**: Previously, `Title • Artist` and app icon were bunched leftward against the "This phone" device chip. Centered directly between the device chip and the audio output switcher.
- **Circular Frosted Prev & Next Controls**: In One UI, playback controls sit on matching frosted glass circular backings. Added circular frosted backgrounds to both previous and next buttons, enlarged control diameters, and tightened button spacing.
- **Authentic Liquid Wave Hill Seekbar**: Replaced rapid sinusoidal squiggles with Samsung's signature organic liquid wave hill curve (`Color(0xFF00C7FF)` to `Color(0xFF008FD6)` gradient fill beneath), cyan scrubber thumb with white center dot, and unplayed straight track line (`Color(0x45FFFFFF)`).
- **Separated Seekbar & Timestamps**: Extracted timestamps into a dedicated row directly below the wave seekbar for maximum horizontal seekbar span and cleaner visual rhythm.
- **Card Rounded Corners**: Increased media card curvature from 26dp to 32dp (`RoundedCornerShape(32.dp)`), accurately matching Samsung One UI card radius.
- **Status Bar Icon Drowning / Clipping Fix in Full QS**: In full QS (2-pull), battery, Wi-Fi, and signal icons were getting displaced and clipped out of view at the top of the screen (while appearing normally in QQS). Resolved by re-anchoring constraints to the status bar guideline and locking MotionLayout progress to 0 in One UI mode.

### 2. Implementation Details

1. **SystemUI QS Header Status Bar Clipping Fix (`ShadeHeaderController.kt`)**:
   - `applyOneUiConstraints`: Cleared conflicting top/bottom/start/end anchors for `R.id.shade_header_system_icons` and `R.id.privacy_container`, reconnecting them directly to `PARENT_ID` top, `R.id.qqs_header_bottom_guideline` bottom, and `R.id.end_guide` end.
   - `updatePosition`: In One UI mode (`isOneUi == true`), locked `header.progress = 0f` instead of applying `qsExpandedFraction`. This prevents the MotionLayout transition from displacing system icons upwards when pulling down full QS, ensuring battery, Wi-Fi, and cell status remain perfectly positioned at standard status bar height at all times.

2. **One UI Compact Media Player Geometry & Seekbar (`OneUIQuickSettingsLayout.kt`)**:
   - **Curvature & Dimensions**: Updated media card outer shell to `RoundedCornerShape(32.dp)` with height `138.dp`.
   - **Centered Header**: Wrapped `"This phone"` on the left, an expanded `Row` with `Modifier.weight(1f)` and `Arrangement.Center` in the middle holding the app icon and `Title • Artist`, and the 28dp circular output switcher on the right.
   - **Liquid Wave Hill (`OneUIWaveSeekbar`)**:
     * Implemented smooth cubic bezier crest and valley curve (`cubicTo`) creating the signature organic liquid wave crest spanning the played portion.
     * Rendered vibrant vertical gradient liquid fill beneath the wave (`Color(0xFF00C7FF)` down to `Color(0xFF008FD6)`).
     * Rendered unplayed portion as a straight 3.5dp rounded line (`Color(0x45FFFFFF)`).
     * Rendered 7dp cyan thumb circle with 2.8dp white inner dot at progress position.
   - **Dedicated Timestamp Row**: Positioned `01:20` and `04:23` timestamps directly beneath the wave seekbar in a separate row with `11.sp` font size and `Arrangement.SpaceBetween`.
   - **Playback Button Trio**:
     * Previous track: `42.dp` circular frosted glass container (`Color(0x38FFFFFF)` background, `1.dp` border `Color(0x28FFFFFF)`).
     * Play/Pause: `50.dp` large circular frosted glass container (`Color(0x48FFFFFF)` background, `1.dp` border `Color(0x35FFFFFF)`).
     * Next track: `42.dp` circular frosted glass container (`Color(0x38FFFFFF)` background, `1.dp` border `Color(0x28FFFFFF)`).
     * Spacing tightened to `Arrangement.spacedBy(10.dp)`.
     * Outer auxiliary controls: 36dp shuffle on left and 34dp favorite check on right.

---

## Section IX: Dedicated Dynamic "Restart System UI" Action in Control Center Settings

### 1. Requirements & Overview
- **Eliminated Forceful Auto-Restarts**: Previously, toggling between One UI and Pixel QS styles triggered an immediate SystemUI process kill on click, which caused an abrupt screen flicker.
- **Dynamic Action Card**: Introduced a dedicated Material 3 action card (`cc_restart_card`) beneath the hero preview card that dynamically appears whenever the user switches between One UI and Pixel QS options.
- **Clear User Guidance**: Displays an explicit description stating that restarting SystemUI is optional, but recommended for full layout and aesthetic synchronization ("Restarting System UI is optional but recommended to apply all layout changes perfectly.").
- **Material 3 Action Button**: Features a prominent styled button ("Restart System UI") with custom vector icon (`ic_restart_systemui`) allowing the user to trigger the restart at their own discretion.

### 2. Implementation Details
1. **Frontend Layout (`control_center_layout.xml`)**:
   - Added `cc_restart_card` (rounded 24dp MaterialCardView, matching hero card surface color).
   - Contains header icon (`@drawable/ic_restart_systemui`), title (`control_center_restart_title`), descriptive summary (`control_center_restart_desc`), and filled action button (`cc_restart_button`).
   - Default visibility set to `android:visibility="gone"`.
2. **Dynamic Flow Controller (`ControlCenterSettings.kt`)**:
   - On fragment resume, initializes `cc_restart_card` visibility to `View.GONE`.
   - When tapping either One UI or Pixel option, saves the setting and immediately reveals `cc_restart_card` (`View.VISIBLE`).
   - Clicking `cc_restart_button` invokes `OneUIQSStylePreferenceController.restartSystemUI(ctx)`.
3. **Assets & Localization**:
   - Vector icon: `ic_restart_systemui.xml` (standard Material refresh/restart glyph).
   - String definitions added in English (`shinkai_strings.xml`) and Indonesian (`values-in/shinkai_strings.xml`).

---

## Section X: Dynamic Island Charging Capsule for POCO F5 (marble) [Experimental]

### 1. Requirements & Overview
- **Punch-Hole Integrated Dynamic Island**: Implemented a sleek Dynamic Island charging pill tailored for the centered camera punch-hole cutout of POCO F5 (`device/xiaomi/marble`, cutout: `M 0,0 H -28 V 94 H 28 V 0 H 0 Z`).
- **Charging-Triggered Animation**: Activates exclusively when charging commences (cable plugged in), smoothly expanding from the center camera notch into a capsule status bar pill.
- **Visual Design Matching Reference**:
  * Capsule Shell: Pitch black (`#000000`) with rounded ends (100dp corner radius, stadium pill shape, height 35dp), subtle translucent rim (`#24FFFFFF`), and z-order layered directly above status bar contents.
  * Left Info: Localized status text ("Charging" in EN, "Mengisi daya" in ID) in clean sans-serif white typography (13.5sp).
  * Right Info: Real-time battery percentage (e.g. "29%") accompanied by an animated neon green lightning bolt vector icon (`#00E676`).
  * Elastic Expansion & Auto-Retract: Scales organically from the punch hole center outwards with `OvershootInterpolator(1.18f)`, holds for 4.5 seconds to display charging status clearly, and retracts back into the camera cutout via `AccelerateInterpolator(1.4f)`.
- **User Settings Toggle**: Integrated a dedicated toggle card in the Control Center subpage (`ControlCenterSettings`) with instant runtime persistence (`dynamic_island_charging`, default enabled).
- **Interactive Controls & Testing**: Tapping the capsule dismisses it immediately. Supports broadcast intent `com.android.systemui.action.TEST_DYNAMIC_ISLAND` for automated and live manual testing.

### 2. Implementation Details
1. **Status Bar Hierarchy & Custom View (`SystemUI`)**:
   - `status_bar.xml`: Placed `DynamicIslandChargingView` (`@+id/dynamic_island_charging`) inside `PhoneStatusBarView` with `layout_gravity="center_horizontal|center_vertical"`, aligning directly over the POCO F5 centered camera cutout.
   - `DynamicIslandChargingView.kt`: Custom `FrameLayout` listening to sticky `Intent.ACTION_BATTERY_CHANGED`. Detects charging state transitions (`!wasPluggedIn && isPluggedIn`), measures display metrics, and manages fluid `AnimatorSet` spring animations.
   - `dynamic_island_charging.xml` & `dynamic_island_pill_bg.xml`: Dedicated capsule layout and stadium pill drawable.
   - `ic_dynamic_island_bolt.xml`: Custom neon green (`#00E676`) 16dp lightning bolt vector asset.
2. **Settings Integration (`packages/apps/Settings`)**:
   - `control_center_layout.xml`: Added `cc_dynamic_island_card` with `MaterialSwitch` (`@+id/cc_dynamic_island_switch`).
   - `ControlCenterSettings.kt`: Bound switch state to `Settings.System.getInt("dynamic_island_charging", 1)`.
   - String localization in `shinkai_strings.xml` and `values-in/shinkai_strings.xml`.

---

## Section XI: Android Setup (OOBE) Boot Stability & Receiver Security Fix

### 1. Requirements & Overview
- **Issue Diagnosis**: After a clean flash or data format, SystemUI was crash-looping during the Android Setup Wizard (OOBE), causing the screen to flicker and triggering Android Rescue Party (ARP) reboot to recovery.
- **Root Cause 1**: Registering a broadcast receiver in `DynamicIslandChargingView` with a non-protected intent (`ACTION_TEST_DYNAMIC_ISLAND`) without the mandatory Android 14+ `Context.RECEIVER_EXPORTED` flag threw an uncaught `java.lang.SecurityException` on the main thread when attaching to the window.
- **Root Cause 2**: Triggering status bar animations and querying early-boot SettingsProvider before the device was provisioned (`DEVICE_PROVISIONED == 0`) during initial setup.
- **Resolution**:
  * Added `Context.RECEIVER_EXPORTED` flag to broadcast receiver registration.
  * Added `Settings.Global.DEVICE_PROVISIONED` check so Dynamic Island remains strictly dormant during Setup Wizard (OOBE).
  * Changed default setting to `0` (opt-in experimental feature via Control Center settings).
  * Wrapped all lifecycle callbacks (`onFinishInflate`, `onAttachedToWindow`, `onDetachedFromWindow`, `triggerChargingIsland`, `dismissIsland`, `onReceive`) and `Settings.System.getInt` calls in `ShadeHeaderController.kt` in comprehensive `try-catch` blocks to ensure SystemUI can never crash under any circumstances.

---

## Section XII: Control Center Settings Crash Fix (NullPointerException in SwitchCompat.makeLayout)

### 1. Requirements & Overview
- **Issue Diagnosis**: Opening **Control Center** (Pusat kontrol) in Settings resulted in an immediate crash:
  ```
  java.lang.NullPointerException: Attempt to invoke interface method 'int java.lang.CharSequence.length()' on a null object reference
      at android.text.StaticLayout.<init>(StaticLayout.java:654)
      at androidx.appcompat.widget.SwitchCompat.makeLayout(SwitchCompat.java:986)
      at androidx.appcompat.widget.SwitchCompat.onMeasure(SwitchCompat.java:907)
  ```
- **Root Cause**: In `control_center_layout.xml`, `MaterialSwitch` extends `androidx.appcompat.widget.SwitchCompat`. During layout measurement (`onMeasure`), when `mShowText` is active or inherited from the theme and `textOn`/`textOff` are null, `SwitchCompat.makeLayout` passes `null` to `StaticLayout`, throwing a `NullPointerException` on `CharSequence.length()`.
- **Resolution**:
  * Replaced `MaterialSwitch` in `control_center_layout.xml` with native platform `Switch` (`android.widget.Switch`) with explicit `android:showText="false"`, `android:textOn=""`, and `android:textOff=""`.
  * Updated `ControlCenterSettings.kt` to bind to `android.widget.CompoundButton`, which works universally for all switch components.
  * Wrapped the entire Dynamic Island switch binding in `ControlCenterSettings.kt` inside a `try-catch` block for guaranteed resilience.

---

## Section XIII: Definitive SystemUI Setup Wizard Crash Loop & Receiver Isolation Fix

### 1. Requirements & Overview
- **Issue Diagnosis**: DropBox crash logs (`system_app_crash`) revealed that SystemUI was crash-looping with:
  ```
  java.lang.SecurityException: com.android.systemui: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts
      at android.app.ContextWrapper.registerReceiver(ContextWrapper.java:793)
      at com.android.systemui.statusbar.phone.DynamicIslandChargingView.onAttachedToWindow
  ```
- **Root Cause**:
  * In `DynamicIslandChargingView.kt`, the receiver was registered inside `onAttachedToWindow()` without checking whether the device was provisioned (`DEVICE_PROVISIONED == 1`).
  * The `IntentFilter` previously registered for both `Intent.ACTION_BATTERY_CHANGED` (protected system broadcast) and `ACTION_TEST_DYNAMIC_ISLAND` (custom non-protected broadcast). On Android 14+, mixing protected and non-protected actions causes `BroadcastController` to enforce explicit export flags and throw `SecurityException`.
  * In `ShadeHeaderController.kt`, `updateCarrierGroupPadding()` was calling `Settings.System.getInt("qs_oneui_style")` without a `try-catch` guard.
  * In `OneUIQuickSettingsLayout.kt`, `rememberIsOneUiStyle()` was reading `Settings.System.getInt` directly during Compose initialization without early boot fault tolerance.
- **Resolution**:
  * In `DynamicIslandChargingView.kt`, added an immediate guard at the beginning of `onAttachedToWindow()`: if `DEVICE_PROVISIONED != 1`, set `visibility = View.GONE` and return immediately without registering any receiver or touching the window.
  * Stripped `ACTION_TEST_DYNAMIC_ISLAND` from `batteryReceiver` so the receiver listens strictly to `Intent.ACTION_BATTERY_CHANGED` using `Context.RECEIVER_NOT_EXPORTED`.
  * Wrapped `Settings.System.getInt` in `ShadeHeaderController.kt:updateCarrierGroupPadding()` and `OneUIQuickSettingsLayout.kt` inside `try-catch` blocks.

---

## Section XIV: Bulletproof "Restart System UI" Implementation from Settings

### 1. Requirements & Overview
- **Issue Diagnosis**: The "Restart System UI" button in the Control Center subpage (`ControlCenterSettings`) was not successfully restarting SystemUI when clicked, whereas the "Restart System UI" action in the Power Menu (Advanced Reboot) worked properly.
- **Root Cause**:
  * The Power Menu executes inside `com.android.systemui` and can directly call `Process.killProcess(Process.myPid())`.
  * `com.android.settings` runs as an external process and lacks POSIX `CAP_KILL` capabilities to send arbitrary `SIGKILL` to another process directly via `Process.killProcess(proc.pid)`.
  * Calling `ActivityManager.getService().killApplicationProcess("com.android.systemui", -1)` failed because `-1` did not match SystemUI's actual package UID in `ProcessList`.
  * In SystemUI, `SysuiRestartReceiver` had `android:exported="false"` and crashed if `intent.getData()` was null.
- **Resolution**:
  * In `SysuiRestartReceiver.java`, made `onReceive()` null-safe for `intent.getData()` so that it can be invoked with standard broadcast intents, cleanly triggering `Process.killProcess(Process.myPid())` from within SystemUI.
  * In SystemUI's `AndroidManifest.xml`, exported `SysuiRestartReceiver` protected by `android.permission.STATUS_BAR_SERVICE`.
  * In Settings' `AndroidManifest.xml`, added `STATUS_BAR_SERVICE` and `KILL_UID` permissions.
  * In `OneUIQSStylePreferenceController.java`, implemented a triple-redundant restart pipeline:
    1. Send `com.android.systemui.action.RESTART` broadcast to `com.android.systemui` as `UserHandle.ALL`.
    2. Fallback: Call `ActivityManager.getService().killApplicationProcess` using the resolved package UID from `PackageManager`.
    3. Fallback: Call `ActivityManager.getService().killUid` with `evenPersistent = true`.
  * In `ControlCenterSettings.kt`, automatically display the "Restart System UI" card when the Dynamic Island toggle is flipped.

---

## Section XV: Dynamic Island Capsule Display Duration Tuning

### 1. Requirements & Overview
- **User Request**: The Dynamic Island charging capsule was auto-retracting too fast (previously 4.5 seconds).
- **Adjustment**: Extended the display and hold time to **7.8 seconds** (`7800L`) in `DynamicIslandChargingView.kt` so the charging status, real-time percentage, and pulsating green lightning bolt remain clearly visible before smoothly retracting back into the camera punch-hole cutout.

---

## Section XVI: Full Dynamic Island Evolution – Spring Physics, Meta-Ball Dual Island, and Expanded Floating Controls

### 1. Requirements & Overview
- **User Request**:
  1. Transform the Dynamic Island into a comprehensive, true Dynamic Island system beyond just charging.
  2. Implement iOS-style fluid **Spring & Overshoot physics**:
     - Expansion: Horizontal expansion left and right from the POCO F5 centered camera punch-hole with bouncy spring overshoot.
     - Closing: Two-phase elastic recoil (slight horizontal rubber stretch before rapidly collapsing into the punch-hole).
  3. Support three core events:
     - **Charging**: Ephemeral 7.8s hold with live battery percentage and green neon pulsing bolt.
     - **Flashlight (Senter)**: Persistent live activity while torch is ON.
     - **Screen Recording (Record)**: Persistent live activity while recording is active, featuring a running live timestamp timer (`00:00`, `02:31`).
  4. **Dual-Island Meta-Ball Architecture**:
     - When Flashlight and Screen Recording run simultaneously, Screen Recording takes priority in the **Main Capsule** (displaying the running timer) and Flashlight becomes the **Meta-Ball** (separate circular bubble on the right side).
     - **Horizontal Swipe Gesture**: Swiping left or right smoothly swaps priorities between the main capsule and the meta-ball with spring animations.
  5. **Interactive Expanded Floating Island Card (Tap to Expand)**:
     - Tapping the compact island capsule springs open a floating expanded card beneath the punch-hole (`WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL`).
     - Screen Recording Expanded: Circular Stop button with red accent, Pause button, and large digital clock font for running elapsed time under golden uppercase `TIME ELAPSED` / `WAKTU BERJALAN`.
     - Flashlight Expanded: Circular glowing amber button with power icon, status `ACTIVE`, and one-tap toggle to extinguish torch.
     - Outside touch detection (`FLAG_WATCH_OUTSIDE_TOUCH`) cleanly collapses the card back into the compact punch-hole pill.

### 2. Files Modified & Added
- `frameworks/base/packages/SystemUI/src/com/android/systemui/Dependency.java`: Added `@Inject Lazy<FlashlightController>` and `@Inject Lazy<ScreenRecordUxController>` to `mProviders` for safe controller discovery.
- `frameworks/base/packages/SystemUI/src/com/android/systemui/statusbar/phone/DynamicIslandChargingView.kt`: Re-architected with state machine, spring overshoot animations, live timer ticker runnable, dual-island swipe gesture detector, and expanded window manager card lifecycle.
- `frameworks/base/packages/SystemUI/res/layout/dynamic_island_charging.xml`: Rebuilt with root horizontal container, `island_capsule` (with charging, torch, and record views), and `island_meta_ball` circular bubble.
- `frameworks/base/packages/SystemUI/res/layout/dynamic_island_expanded_card.xml`: Created expanded floating card layout with circular action buttons and large digital readout.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_circle_bg.xml`: Oval pure black background with subtle white border for meta-ball.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_expanded_bg.xml`: 28dp rounded rectangle pure black background for expanded card.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_btn_bg.xml`: Circular ripple button background for stop and action controls.
- `frameworks/base/packages/SystemUI/res/drawable/dynamic_island_btn_amber_bg.xml`: Circular glowing amber button background for torch and pause controls.
- `frameworks/base/packages/SystemUI/res/drawable/ic_dynamic_island_record_dot.xml`: Pure red pulsing dot for screen recording indicator.
- `frameworks/base/packages/SystemUI/res/drawable/ic_dynamic_island_stop.xml`: Square stop vector asset for recording dismissal.
- `frameworks/base/packages/SystemUI/res/values/shinkai_strings.xml` & `res/values-in/shinkai_strings.xml`: English and Indonesian strings for torch and recording labels.

---

## Section XVII: Home Settings Notification Dashboard Card & Colored Notification Icons

### 1. Requirements & Overview
- **User Request**:
  1. In the Home Settings Quick Dashboard (`homepage_quick_dashboard.xml`), replace the **Accessibility** card with a dedicated **Notification** panel card.
  2. Inside Notification Settings (`ConfigureNotificationSettings`), add a new feature toggle: **Colored Notification Icons** (`notification_colored_icons`).
  3. "Colored Notification Icons": display the real, full-color application logos (`pm.getApplicationIcon(packageName)`) for notifications in the status bar and shade cards instead of monochrome/tinted silhouettes (as popularized by Evolution X, crDroid, and Iconify).

### 2. Implementation Details
1. **Home Settings Quick Dashboard Panel Replacement (`packages/apps/Settings`)**:
   - `homepage_quick_dashboard.xml`: Replaced `home_card_accessibility` with `home_card_notification` featuring a warm orange icon container (`#FB923C`), `@drawable/ic_notifications_filled`, title `@string/configure_notification_settings`, and subtitle `@string/home_notification_sub`.
   - `HomepageQuickDashboardPreferenceController.kt`: Bound `home_card_notification` click listener to launch `Settings.ACTION_NOTIFICATION_SETTINGS` / `ConfigureNotificationSettings`.
2. **Settings Preference Controller & XML Integration**:
   - `ColoredNotificationIconsPreferenceController.java`: Created `TogglePreferenceController` controlling `Settings.System.notification_colored_icons`.
   - `configure_notification_settings.xml`: Added `<SwitchPreferenceCompat>` with key `notification_colored_icons` at order 0 under `general_notifications_category`.
   - `res/values/shinkai_strings.xml` & `res/values-in/shinkai_strings.xml`: Added localization strings for English and Indonesian.
3. **SystemUI Real-Time Colored Notification Icon Rendering (`frameworks/base`)**:
   - `StatusBarIconView.java`:
     * In `loadDrawable`: If `isNotification()` and `notification_colored_icons == 1`, loads and returns the authentic launcher application icon via `pm.getApplicationIcon(packageName)`.
     * In `updateIconColor`: Bypasses matrix color tinting and grayscaling when the screen is active, ensuring authentic app brand colors remain untouched.
     * In `setNotification`: Immediately updates drawable and color state when a notification is attached.
   - `NotificationHeaderViewWrapper.java`:
     * In `onContentUpdated`: Replaces monochromatic header small icon with the full-color app logo (`mIcon.setImageDrawable(appIcon)`), clears color filters, resets background, and normalizes padding for pristine display in shade and lockscreen cards.
   - `IconManager.kt`:
     * In `attach()`: Registered a `ContentObserver` on `Settings.System.getUriFor("notification_colored_icons")` that dynamically re-renders all existing notification icons via `updateIconsSafe(entry)` in real time when toggled, without requiring a SystemUI reboot.

---

## Section XVIII: Restore Accessibility to Home Settings List & Dynamic Island Dual-Slot / Expanded Card Fixes

### 1. Requirements & Overview
- **User Request**:
  1. Return Accessibility (`top_level_accessibility`) back to the standard Home Settings list (since the quick dashboard panel was replaced with Notification).
  2. Fix Dynamic Island bug in Photo 1: Tapping the island (flashlight or screen record) caused an empty/blank black expanded rounded card to appear without any buttons or indicators.
  3. Fix Dynamic Island bug in Photo 2: When both flashlight (senter) and screen recording were active simultaneously, the secondary circular meta-ball bubble failed to appear beside the main capsule.
  4. Fix Dynamic Island screen record capsule length: Redesign to be symmetrical ala iOS (~200dp) with pulsing red dot on the left, center clearance spacer over the punch hole, and digital timer + record icon on the right.

### 2. Implementation Details
1. **Home Settings Accessibility Restoration (`packages/apps/Settings`)**:
   - `top_level_settings.xml`: Re-added `<com.android.settings.widget.HomepagePreference>` with key `top_level_accessibility`, icon `@drawable/ic_settings_accessibility_filled`, and controller `TopLevelAccessibilityPreferenceController` inside `top_level_support_category` at order `-20`.
   - `top_level_settings_expressive.xml`: Re-added `top_level_accessibility` with icon `@drawable/ic_homepage_accessibility` inside `top_level_support_category`.
2. **Dynamic Island Expanded Card Fix (`frameworks/base/packages/SystemUI`)**:
   - `DynamicIslandChargingView.kt`:
     * Fixed critical initialization order in `showExpandedCard()`: Extracted `populateExpandedCard(slot)`, and guaranteed `isExpandedCardShowing = true` before making child layouts visible. Previously, `updateExpandedCardIfShowing()` aborted early via `if (!isExpandedCardShowing) return`, leaving all three sub-views (`expandedRecordContent`, `expandedTorchContent`, `expandedChargingContent`) set to `View.GONE` and producing an empty black box.
     * Hidden compact pill views (`capsuleView` and `metaBallView`) while the expanded floating card is displayed, smoothly restoring them upon dismissal.
     * Improved `stopScreenRecording()` using `PendingIntent.getService(...)` with `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` fallback.
   - `dynamic_island_expanded_card.xml`:
     * Changed `expanded_card_container` from `LinearLayout` to `FrameLayout` with `android:layout_gravity="center_vertical"` on all child layouts to eliminate horizontal measurement collapses and ensure clean full-width presentation.
3. **Dynamic Island Dual-Slot Meta-Ball & Flashlight Detection Fix**:
   - `DynamicIslandChargingView.kt`:
     * Fixed meta-ball visibility bug: Dismissal animation previously omitted `alpha = 1.0f` reset in `withEndAction`, leaving the meta-ball permanently transparent at `alpha = 0f` once dismissed. Now explicitly guarantees `alpha = 1.0f`, `scaleX = 1.0f`, and `scaleY = 1.0f` whenever `metaSlot != IslandSlot.NONE`.
     * Added triple-redundancy flashlight state tracking:
       1. `FlashlightController.addCallback(flashlightListener)`
       2. Direct `CameraManager.registerTorchCallback(cameraTorchCallback, mainHandler)` for all camera IDs
       3. `ContentObserver` on `Settings.Secure.getUriFor(Settings.Secure.FLASHLIGHT_ENABLED)`
       4. Direct synchronous evaluation in `refreshIslandState()`: `isTorchOn || controllerTorch || secureTorch`.
     * Fixed `GestureDetector` touch handling by overriding `onDown(e: MotionEvent): Boolean = true` and `onSingleTapUp(e: MotionEvent): Boolean` to guarantee instant responsiveness without tap lag.
   - `status_bar.xml`:
     * Added `android:clipChildren="false"` and `android:clipToPadding="false"` to `PhoneStatusBarView` to ensure multi-bubble islands never get clipped at the status bar boundaries.
4. **Dynamic Island Screen Recording Pill Length & Symmetry (iOS Style 200dp)**:
   - `dynamic_island_charging.xml`:
     * Redesigned `island_record_content` with `minWidth="200dp"`, eliminating the cramped/short appearance and matching the balanced width of the charging capsule.
     * Symmetrical dual-wing architecture: Left wing hosts the pulsing red indicator dot (`gravity="start"`), center features a 36dp clearance spacer positioned directly across the camera punch-hole, and right wing houses the bold red digital timer (`00:00`) and record icon (`gravity="end"`). This ensures the punch-hole never occludes content and maintains iOS Dynamic Island symmetry.

---

## Section XIX: Dynamic Island Settings Nomenclature Tuning & Screen Record Multi-Layer Stop Dispatch

### 1. Requirements & Overview
- **User Request**:
  1. The title in Settings still read "Dynamic Island charging", whereas flashlight and screen recording activities are already supported.
  2. String localizations were inconsistent across Indonesian and English (such as the elapsed time label previously displaying countdown text).
  3. Bug during screen recording: pressing the square stop button only dismissed the island while background recording continued running. Tapping pause also failed to pause and immediately closed the island.

### 2. Implementation Details
1. **Settings Nomenclature & Description Updates (`packages/apps/Settings`)**:
   - `shinkai_strings.xml` (EN): Updated `dynamic_island_title` to "Dynamic Island" and `dynamic_island_summary` to "Interactive punch-hole capsule for charging, flashlight, and screen recording (Experimental)".
   - `values-in/shinkai_strings.xml` (ID): Updated `dynamic_island_title` to "Dynamic Island" and `dynamic_island_summary` to "Kapsul interaktif notch kamera untuk pengisian daya, senter, dan perekam layar (Eksperimental)".
2. **SystemUI Localization Harmonization (`frameworks/base/packages/SystemUI`)**:
   - `values-in/shinkai_strings.xml`: Corrected `dynamic_island_record_elapsed` to "Waktu Berjalan" (replacing misleading countdown string) and aligned `dynamic_island_record_label` to "Perekam Layar".
   - `values/shinkai_strings.xml`: Aligned `dynamic_island_record_label` to "Screen Recording".
3. **Multi-Layer Screen Recording Stop Dispatch**:
   - `DynamicIslandChargingView.kt`: Resolved the root cause where `RecordingController.stopRecording()` prematurely flipped `isRecording` state to false while silently dropping stop execution when `mStop` was null. Implemented a 5-layer redundant stop pipeline:
     * Layer 1: Directly triggers the active recording notification's stop `actionIntent` (`channelId == "screen_record"`).
     * Layer 2: Dispatches `ScreenRecordingService.ACTION_STOP` with `STOP_HOST_APP` to Android 15/17 flow-based `ScreenRecordingService`.
     * Layer 3: Dispatches `STOP_FROM_NOTIF` with `currentUserId` to legacy `RecordingService`.
     * Layer 4: Dispatches interactive `PendingIntent` with `BroadcastOptions` to bypass background execution limitations.
     * Layer 5: Fallback dispatch via `screenRecordController?.stopRecording(...)`.
4. **Expanded Card Streamlining (iOS Authentic Form Factor)**:
   - `dynamic_island_expanded_card.xml`: Eliminated the non-functional pause button (unsupported by AOSP MediaProjection architecture) and enlarged the red Stop button (44dp with 18dp square icon), matching authentic iOS Dynamic Island design.
