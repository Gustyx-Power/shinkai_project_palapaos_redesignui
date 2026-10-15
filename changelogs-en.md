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
   - Flashlight Indicator: Minimalist compact pill with modern bespoke torch icon and bold "On" status. Tap once to toggle off.
   - Screen Recording Indicator: Compact pill matching flashlight dimensions with pulsing red record dot and running digital timer.
   - Dual-Island (Meta-Ball): When recording and flashlight run simultaneously, the secondary circular meta-ball bubble docks cleanly on the LEFT side of the main capsule with fluid liquid physics swap animations.
   - Floating Expanded Card: Edge-to-edge OriginOS layout with custom video camera badge, audio wave, mic, pencil annotation tools, solid white pause button, and white-ringed stop button.

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

---

## Section XX: Dynamic Island Form-Factor Refinement (Compact Record Pill, Minimalist Flashlight Pill, and Left-Aligned Meta-Ball)

### 1. Requirements & Overview
- **User Request**:
  1. Resize the screen recording capsule to match the compact dimensions of the flashlight pill so it is not overly elongated.
  2. Remove the flashlight label text ("Flashlight" / "Senter") from the compact Dynamic Island pill, showing strictly the flashlight vector logo and "On" status text.
  3. Relocate the secondary meta-ball circular bubble to the LEFT of the main capsule instead of the right.

### 2. Implementation Details
1. **Left-Side Meta-Ball Relocation (`dynamic_island_charging.xml`)**:
   - Reordered `island_meta_ball` FrameLayout to precede `island_capsule` inside `island_root_container`.
   - Adjusted separator margin from `android:layout_marginStart="8dp"` to `android:layout_marginEnd="8dp"`. In dual-activity mode (such as flashlight and screen recording running at the same time), the secondary meta-ball bubble now naturally anchors to the left of the main capsule.
2. **Streamlining Flashlight Pill to Minimal Form**:
   - `dynamic_island_charging.xml`: Removed `island_torch_text` and the 24dp center spacer, leaving only the amber flashlight icon (`16dp`, `#FFD60A`) and bold "On" status label (`island_torch_action_text`, 13sp) with 8dp start margin and 12dp/14dp side padding.
   - `values-in/shinkai_strings.xml`: Updated `dynamic_island_torch_on` from "Menyala" to "On" to guarantee a concise and uniform UI appearance.
3. **Compact Screen Record Pill Form Factor**:
   - `dynamic_island_charging.xml`: Removed `minWidth="200dp"`, the 36dp camera clearance spacer, and the 0dp weight=1 dual-wing container. Replaced them with an inline compact horizontal layout holding the pulsing red indicator dot (`11dp`) and bold digital elapsed timer (`00:00`) with matching 12dp/14dp padding, achieving a compact and visually symmetrical capsule alongside the flashlight pill.
4. **Framework Code Clean-Up (`DynamicIslandChargingView.kt`)**:
   - Removed deprecated `torchTextView` (`R.id.island_torch_text`) field declaration and `findViewById` assignment, keeping code clean with zero orphaned references.

---

## Section XXI: OriginOS-Inspired Dynamic Island Card (Edge-to-Edge Span, Custom Modern Vector Icons, and Fluid Physics Transitions)

### 1. Requirements & Overview
- **User Request**:
  1. Revamp the expanded Dynamic Island screen recording card to mirror OriginOS styling (based on user reference photo).
  2. Widen the card to anchor closely against the left and right edges of the screen (eliminating fixed 326dp width in favor of screen-matching width with slim 10dp margins).
  3. Replace stock Material icon dependencies with bespoke custom-crafted modern vector icons.
  4. Smooth out dismiss and dual-island priority swap transitions for fluid motion.

### 2. Implementation Details
1. **Edge-to-Edge OriginOS Floating Card Architecture (`dynamic_island_expanded_card.xml`)**:
   - Expanded `expanded_island_root` and `expanded_card_container` to `match_parent` with 10dp side padding, creating an edge-to-edge floating surface.
   - Restructured screen recording layout into a two-tier OriginOS arrangement:
     * Top Tier: Red translucent pill badge on the left housing a custom video camera glyph (`16dp`, `#FF3B30`) and bold white elapsed timer (`00:00`), paired with the localized "Screen Recording" header on the right.
     * Bottom Tier: Left cluster contains three frosted circular action discs (System Audio Wave, Microphone, and Annotation Stylus) at 38dp each; right cluster houses an authentic solid white circular Pause button (`42dp`, dual black vertical bars) and a white-ringed Stop disc (`42dp`, 2.2dp white border with centered red square).
   - Flashlight and Charging expanded cards aligned with matching full-width presentation.
2. **Bespoke Vector Drawable Library (`res/drawable`)**:
   - `ic_origin_rec_cam.xml`: Modern video camera with rounded corners and trapezoidal lens.
   - `ic_origin_sound_wave.xml`: 5-bar vertical equalizer sound wave with rounded caps.
   - `ic_origin_mic.xml`: Elegant studio capsule microphone with curved cradle and base.
   - `ic_origin_pencil.xml`: Modern annotation stylus tilted at 45 degrees.
   - `ic_origin_pause.xml`: Dual solid black vertical pause bars.
   - `ic_origin_play.xml`: Solid black play triangle for resuming recording.
   - `ic_origin_stop.xml`: Solid red rounded square (`#FF3B30`).
   - `ic_origin_torch.xml`: Bespoke modern amber flashlight with center switch.
   - `dynamic_island_origin_stop_bg.xml`: Circular dark base with 2.2dp solid white outer ring.
   - `dynamic_island_origin_btn_circle.xml`: Frosted glass semi-translucent circular disc for auxiliary tools.
   - `dynamic_island_origin_record_badge.xml`: Red-tinted rounded pill badge for elapsed timer.
3. **Fluid Transition Mechanics (`DynamicIslandChargingView.kt`)**:
   - **Dismiss Transition**: Anchored animation pivots (`pivotX = displayWidth / 2f`, `pivotY = 0f`) directly to the punch-hole center. Card collapses upward smoothly via `PathInterpolator(0.4f, 0f, 0.2f, 1f)`. Simultaneously, the compact capsule and meta-ball enter via a gentle scale-in (`scaleX 0.75f -> 1.0f`) with elastic overshoot (`OvershootInterpolator`), completely eradicating previous abrupt layout pops.
   - **Dual-Island Swap Transition**: Implemented a 2-phase liquid metaball physics model. When swiped, capsule and meta-ball translate horizontally towards each other (`translationX`), compressing and softening opacity over 130ms, then swap roles and spring back to origin with elastic overshoot (`OvershootInterpolator`), achieving an organic fluid fusion and separation effect.
   - **Recording Interactivity**: Wired pause toggle to freeze/resume elapsed timer progression and bound haptic feedback to all interactive controls.



---

## Section XXII: Punch-Hole Geometry Clearance, Native Android Recording Chip Suppression, and Screen Record Controls on Expanded Island

### 1. Requirements & Overview
- **Background & Issues**:
  1. **Punch-Hole Cutout Camera Obstruction**: On center punch-hole cutout displays (e.g., POCO F5 / `marble`), the compact Dynamic Island capsule for flashlight and screen recording was too narrow. Consequently, the flashlight glyph, "On" status text, pulsing red recording dot, and digital timer were physically obscured by the device front camera cutout.
  2. **Status Bar Chip Duplication (Native Android Recording Pill)**: When Dynamic Island was recording, the native Android red ongoing recording chip adjacent to the status bar clock also displayed simultaneously, creating distracting visual duplication. The native chip must be suppressed whenever Dynamic Island is enabled and only reappear if Dynamic Island is toggled off by the user.
  3. **Expanded Island Control Redesign**: The pause feature lacked native support in the AOSP MediaProjection recording pipeline, and secondary decorative buttons lacked functional utility. All controls other than Stop are replaced with 6 native Android screen recording configuration switches: Record device audio, Record microphone, Show touches, Lower quality (SD), Larger file size limit, and HEVC encoding (which defaults to ON and operates functionally). The Stop button remains accessible and fully functional.

### 2. Implementation Details
1. **Capsule Expansion & Camera Punch-Hole Clearance (`dynamic_island_charging.xml`)**:
   - Expanded minimum capsule dimension (`minWidth="142dp"`) with comfortable edge padding (`paddingStart="14dp"`, `paddingEnd="14dp"`).
   - Implemented a dual-wing symmetrical layout (`minWidth="38dp"` per wing) anchored around center clearance spacers (`island_torch_spacer` and `island_record_spacer`, 38dp each) that span precisely underneath the physical front camera punch hole.
   - Flashlight icon and recording dot anchor neatly in the left wing; "On" label and digital timer (`00:00`) anchor in the right wing, preventing cutout obstruction.
   - Widened charging spacer (`island_spacer`) to `36dp` for uniform clearance across all Dynamic Island modes.
2. **Dynamic Island-Aware Native Recording Chip Suppression (`ScreenRecordChipViewModel.kt` & `ScreenRecordChipViewModelKosmos.kt`)**:
   - Injected `SystemSettingsRepository` into `ScreenRecordChipViewModel` to track `Settings.System.DYNAMIC_ISLAND_CHARGING` reactively via Kotlin StateFlow.
   - Combined flow logic: when Dynamic Island is enabled (`dynamic_island_charging == 1`), `ScreenRecordChipViewModel` emits `OngoingActivityChipModel.Inactive()`, completely hiding the red status bar pill next to the clock.
   - If the user turns off Dynamic Island in Settings (`dynamic_island_charging == 0`), the native Android screen record chip immediately restores its default display behavior.
   - Updated Kosmos test fixture (`ScreenRecordChipViewModelKosmos.kt`) to inject `systemSettingsRepository`.
3. **Expanded Screen Recording Controls with 6 Quick Settings Toggles (`dynamic_island_expanded_card.xml`)**:
   - Removed the unsupported pause button and decorative audio wave/stylus discs.
   - Relocated the solid red OriginOS Stop button (`expanded_record_stop_btn`, 38dp) into the top-right header for direct one-tap recording termination.
   - Integrated 6 stock-style `@style/ScreenRecord.Switch` rows for screen recording options:
     * Record device audio switch (`expanded_record_switch_device_audio`)
     * Record microphone switch (`expanded_record_switch_mic`)
     * Show touches switch (`expanded_record_switch_touches`)
     * Lower quality / SD switch (`expanded_record_switch_low_quality`)
     * Larger file size limit switch (`expanded_record_switch_longer_duration`)
     * HEVC encoding switch (`expanded_record_switch_hevc`, defaults to ON)
4. **Reactive Logic & System Preferences Persistence (`DynamicIslandChargingView.kt`)**:
   - Bound all 6 switches directly to SystemUI persistent `Prefs` storage and `Settings.System.SHOW_TOUCHES`.
   - Defaulted HEVC encoding to active (`Prefs.getInt(context, "screenrecord_use_hevc", 1) == 1`), writing updates to persistent preferences on toggle changes.
   - Connected the Stop button to native recording termination intent (`RecordingService.REQUEST_STOP`) with tactile haptic feedback on all interactions.
5. **Multilingual Localization (`shinkai_strings.xml` & `values-in/shinkai_strings.xml`)**:
   - Added complete string entries for both English and Indonesian for all screen recording options (`screen_record_show_touches_title`, `screen_record_low_quality_title`, `screen_record_longer_duration_title`, `screen_record_hevc_title`).

---

## Section XXIII: Interactive Animated Refresh Rate Preview Card Integration in Display Settings

### 1. Requirements & Overview
- **User Request**:
  - Add an animated refresh rate preview card in Settings > Display > Refresh Rate, showcasing a visual motion fluidity comparison between 60 Hz, 90 Hz, and 120 Hz according to user reference design.
  - Position the preview card at the very top of the page above the radio buttons, featuring sliding puck animations for each refresh tier, with the active selection highlighted in a rounded dark green pill, vibrant mint text, and mint green puck.

### 2. Implementation Details
1. **Interactive Animated Preview View (`RefreshRatePreviewView.java`)**:
   - Engineered a custom `LinearLayout` component that dynamically constructs preview rows matching the hardware-supported refresh rates (`60 Hz`, `90 Hz`, `120 Hz`).
   - Each row features a frequency label on the left and a horizontal sliding lane with a circular puck (`20dp`) on the right.
   - Integrated independent `ValueAnimator` instances per tier with `AccelerateDecelerateInterpolator` in continuous `REVERSE` repeat mode, pacing movement inversely to refresh rates (2200ms for 60Hz, 1500ms for 90Hz, 1100ms for 120Hz) to clearly exhibit differences in motion cadence and fluid frame pacing.
   - The selected tier is encapsulated in a dark green pill container (`14dp` radius, `#0D2E1C`), bold mint green label (`#6BD598`), and glowing mint green puck, while unselected tiers rest in clean neutral styling with grey pucks (`#787E7A`).
   - Horizontal dividers between tiers are intelligently hidden adjacent to the selected pill to maintain seamless visual boundaries.
   - Added interactive touch support: tapping any tier directly inside the preview card switches the active refresh rate, syncs the radio buttons below, and triggers haptic tactile feedback.
   - Handled comprehensive animation lifecycle management (`onAttachedToWindow`, `onDetachedFromWindow`, `onVisibilityChanged`, `onResume`, `onPause`) to pause animators when out of view or screen off, preserving battery life.
2. **Drawable Assets & Preference Layout (`res/drawable` & `res/layout`)**:
   - `refresh_rate_preview_card_bg.xml`: 20dp rounded dark surface card (`#131614`) with subtle outer stroke (`#1E2420`).
   - `refresh_rate_preview_selected_pill_bg.xml`: 14dp rounded dark green pill background (`#0D2E1C`) for active selection.
   - `refresh_rate_preview_puck_grey.xml`: Neutral solid grey circular puck for inactive tiers.
   - `refresh_rate_preview_puck_green.xml`: Vibrant solid mint green circular puck (`#6BD598`) for active tier.
   - `refresh_rate_preview_preference.xml`: Preference container with standardized 16dp horizontal padding.
3. **Fragment Integration (`RefreshRateSettings.java`)**:
   - Attached `LayoutPreference` with top priority order (`setOrder(-100)`) and disabled `orderingAsAdded`, guaranteeing the preview card always anchors at the very top above all candidate radio buttons.
   - Synchronized radio button selections (`setDefaultKey`) with real-time visual updates on `RefreshRatePreviewView`.
   - Wired bidirectional touch events from the preview card directly into `onRadioButtonConfirmed(key)`.

---

## Section XXIV: Rollback Dynamic Island Screen Recording Design to Pure OriginOS Aesthetic

### 1. Requirements & Overview
- **User Request**:
  - Roll back the integration of 6 AOSP screen recording setting toggles on the expanded Dynamic Island card, as they compromised and deviated from the clean visual concept.
  - Restore the pure OriginOS design style for screen recording, keeping it elegant, minimal, and authentic.
  - Return the compact pill to a sleek form factor and restore the expanded card layout featuring the camera badge, recording timer, pause/resume button, white-bordered stop button, and the 3 circular tool buttons on the bottom row.

### 2. Implementation Details
1. **Restoration of OriginOS Expanded Card Layout (`dynamic_island_expanded_card.xml`)**:
   - Removed the 6 AOSP recording setting toggle rows that overcrowded the expanded card.
   - Restored the authentic 2-row OriginOS layout:
     * Top Row: OriginOS camera badge with a pulsing red recording dot and live elapsed timer on the left; Pause/Resume button (solid white circle with black bars) and Stop button (white outer ring container with a red square center) on the right.
     * Bottom Row: Three 36dp circular tool buttons with subtle translucent dark backgrounds (`#1AFFFFFF`) featuring modern bespoke vector icons (microphone, touch/stylus indicator, and audio sound wave).
   - Maintained proportional expanded card height (92dp) with 28dp corner curvature (`dynamic_island_expanded_bg.xml`) and edge-to-edge margins aligning cleanly with the screen borders.
2. **Restoration of Compact Capsule Layout (`dynamic_island_charging.xml`)**:
   - Reverted the primary compact capsule to its refined, compact dimensions (`paddingStart="12dp"`, `paddingEnd="14dp"`), matching the minimalist flashlight pill without artificial excess width.
   - Preserved the organic liquid metaball positioning on the left side in accordance with user preferences.
3. **Logic Cleanup & State Handling (`DynamicIslandChargingView.kt`)**:
   - Removed AOSP setting switch listeners and restored OriginOS interactive control states.
   - Re-enabled pause state management (`isRecordPaused`) with accumulated pause duration calculation (`pausedAccumulatedMs`) so the elapsed timer remains strictly synchronous upon resuming.
   - Retained the multi-layered 5-tier stop dispatch mechanism to reliably stop screen recording.
   - Retained native status bar chip suppression so redundant recording chips remain hidden when Dynamic Island is active.
4. **Asset & String Cleanup**:
   - Removed unused setting icon drawables (`ic_hevc.xml`, `ic_sr_quality.xml`, `ic_storage.xml`, `ic_touch.xml`).
   - Cleaned up obsolete string entries in `shinkai_strings.xml` and `values-in/shinkai_strings.xml`, keeping resources lightweight and uncluttered.
---

## Section XXV: Proportional Capsule Geometry and Simplified OriginOS Screen Record Card

### 1. Requirements & Overview
- **User Request**:
  - The compact pill was previously perceived as too small and cramped. The user requested expanding the capsule to a well-proportioned size (~120dp) with a center punch-hole clearance spacer to prevent camera obstruction.
  - The pause button and auxiliary controls were non-functional in the AOSP MediaRecorder pipeline, behaving only as dummy visual placeholders. The user chose to simplify the expanded OriginOS card to show exclusively functional controls (prominent Stop button, live timer, and recording status), eliminating all non-working dummy buttons.

### 2. Implementation Details
1. **Proportional Capsule Geometry (~120dp) (`dynamic_island_charging.xml`)**:
   - Enforced a minimum width of `minWidth="120dp"` on both flashlight (`island_torch_content`) and screen recording (`island_record_content`) pill containers.
   - Structured a symmetrical dual-wing layout: left wing (34dp) centering the icon or record dot, center spacer (32dp) clearing the physical punch-hole camera cutout, and right wing (34dp) centering the action text or live timer with balanced 12dp horizontal padding.
   - Standardized the charging spacer (`island_spacer`) to 32dp for uniform geometry across all island states.
2. **Simplified OriginOS Expanded Record Card (`dynamic_island_expanded_card.xml`)**:
   - Removed all non-functional auxiliary rows and dummy pause controls (sound wave, microphone, pencil/draw, and pause buttons).
   - Reorganized the expanded view into a single focused horizontal row matching the clean aesthetic of the flashlight and charging cards:
     * Left: 48dp circular OriginOS camera badge (`dynamic_island_origin_record_badge.xml`) with the red camera icon (`ic_origin_rec_cam`) and pulsating record dot (`ic_dynamic_island_record_dot`).
     * Middle: Title label ("Screen Recording") and elapsed duration subtext with live running timer (`expanded_record_timer`).
     * Right: 48dp signature OriginOS circular Stop button (`dynamic_island_origin_stop_bg`) with a crisp white border ring and red square (`ic_origin_stop`) providing instant haptic stop dispatch.
3. **Framework Controller Streamlining (`DynamicIslandChargingView.kt`)**:
   - Removed all listeners and state tracking variables for the dummy pause feature (`isRecordPaused`, `pausedAccumulatedMs`, `pauseStartTimestamp`) and auxiliary dummy tools.
   - Wired the Stop button directly to `stopScreenRecording()` with haptic click feedback and smooth collapse animation.
   - Streamlined timer calculation directly from `recordingStartTimestamp` via `SystemClock.elapsedRealtime()`.
4. **Cleanup of Deprecated Assets**:
   - Removed 7 unused drawable files that were dedicated to dummy controls: `dynamic_island_origin_btn_circle.xml`, `dynamic_island_origin_pause_bg.xml`, `ic_origin_pause.xml`, `ic_origin_play.xml`, `ic_origin_pencil.xml`, `ic_origin_mic.xml`, and `ic_origin_sound_wave.xml`.

---

## Section XXVI: OriginOS Native Status Bar Evasion Physics and Fluid Spring Transitions

### 1. Requirements & Overview
- **User Request**:
  - The expansion and dismiss animations of the Dynamic Island card previously felt too static, fast, and abrupt (simply overlaying the status bar directly).
  - On authentic OriginOS devices, the status bar exhibits a deeply organic and fluid physical behavior:
    * When the island expands, the left-side status bar content (clock and notification icons) smoothly glides away to the left (`translationX < 0` with fade out), while the right-side status bar content (battery, cellular signal, and Wi-Fi) smoothly glides away to the right (`translationX > 0` with fade out).
    * When the island collapses (dismisses), both status bar sides glide gracefully back into their original resting positions.
  - Soften the physical interpolation curves and extend the transition durations to reproduce the authentic, silky-smooth liquid spring physics of OriginOS.

### 2. Implementation Details
1. **Status Bar Side Binding (`DynamicIslandChargingView.kt`)**:
   - Wired dynamic hooks into the native AOSP status bar view hierarchy within `PhoneStatusBarView`:
     * Left / Start Side: `status_bar_start_side_content` (housing the clock and notification icon area).
     * Right / End Side: `status_bar_end_side_content` (housing network traffic, battery, cellular, and Wi-Fi system icons).
   - Implemented `ensureStatusBarSideViews()` to guarantee references are safely resolved upon window attachment and interaction.
2. **Status Bar Evasion Animation on Expand (`animateStatusBarSidesOnExpand`)**:
   - Translated the left-side container 56dp outward to the left (`translationX = -56dp`) with a gentle fade out (`alpha = 0f`) over 420ms using an acceleration curve `PathInterpolator(0.32f, 0.0f, 0.12f, 1.0f)`.
   - Translated the right-side container 56dp outward to the right (`translationX = +56dp`) with a gentle fade out (`alpha = 0f`) over 420ms.
   - This creates a realistic physical illusion of the blossoming Dynamic Island expanding outward and displacing status bar elements toward the screen edges.
3. **Status Bar Restoration Animation on Collapse (`animateStatusBarSidesOnCollapse`)**:
   - Restored the left-side container from -56dp back to its baseline position (`translationX = 0f`) and full opacity (`alpha = 1f`) over 440ms using a gentle spring deceleration curve `PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f)`.
   - Restored the right-side container from +56dp back to its baseline position (`translationX = 0f`) with full opacity (`alpha = 1f`) synchronously.
   - Added `resetStatusBarSides()` to `onDetachedFromWindow()` and fallback error paths to guarantee system status bar icons are never left in a hidden or offset state.
4. **Refined Fluid Physics for Expanded Card & Pill Transitions**:
   - Softened card expansion (`expandedCardContainer`) with a 440ms duration (previously 320ms) using an organic liquid curve `PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f)` blooming from the top punch-hole cutout (`pivotY = 0f`, scaling from 0.32x to 1.0x).
   - Softened card collapse with a 380ms duration (previously 240ms) using suction curve `PathInterpolator(0.38f, 0.0f, 0.2f, 1.0f)`.
   - Extended compact pill (`capsuleView`) and liquid metaball (`metaBallView`) return transitions from 260ms to 400ms with smooth spring damping.
---

## Section XXVII: Enhanced Liquid Squash & Stretch Physics and Smooth Gesture Detection on Meta-Ball

### 1. Requirements & Overview
- **User Request**:
  - The swap/displacement animation between the primary capsule and the meta-ball previously felt too brief (130ms) with minimal travel distance (12dp), appearing like a rigid jump rather than organic liquid.
  - The user requested enhancing the meta-ball swipe/drag animation to embody the living liquid physics of OriginOS.
  - Integrate liquid volume-preserving squash & stretch, full displacement merge into the capsule (~38dp), tactile haptic click feedback upon fusion, and sensitive casual drag/scroll gesture detection.

### 2. Implementation Details
1. **Responsive Drag & Swipe Gesture Handling (`onScroll` & `onFling`)**:
   - Extended gesture detectors on both the main capsule (`gestureDetector`) and the meta-ball bubble (`metaGestureDetector`) to handle continuous horizontal drag events via `onScroll`.
   - Enabled priority swapping upon crossing a sensitive 16dp-18dp horizontal drag threshold, allowing casual slow drags to trigger the swap seamlessly without requiring a rapid fling.
   - Added `isSwappingDualIsland` state lock to ensure clean atomic execution without erratic multi-triggers.
2. **Phase 1: Magnetic Attraction, Liquid Squash & Stretch, and Fusion**:
   - Propelled the meta-ball bubble 38dp across the status bar gap directly into the primary capsule over 190ms with an acceleration curve `PathInterpolator(0.35f, 0.0f, 0.15f, 1.0f)`.
   - Applied realistic fluid droplet deformation: the meta-ball stretches horizontally along its motion vector (`scaleX = 1.36x`) while squashing vertically (`scaleY = 0.70x`), simulating a liquid droplet drawn by magnetic surface tension.
   - The primary capsule reciprocates by gliding outward (-14dp) and compressing (`scaleX = 0.92x`, `scaleY = 1.06x`) upon absorbing the incoming droplet.
3. **Tactile Haptic Click at Fusion Point**:
   - Dispatched a crisp tactile haptic pulse (`HapticFeedbackConstants.CONTEXT_CLICK`) at the exact millisecond of droplet coalescence, providing immediate physical confirmation in hand.
4. **Phase 2: Secondary Droplet Ejection & Elastic Spring Recovery**:
   - The new secondary droplet bubble emerges from the capsule toward the left with vertical elastic wobble (`scaleX = 0.72x`, `scaleY = 1.28x`) from -22dp.
   - Settled the bubble back into neutral resting geometry over 400ms using an organic spring curve `OvershootInterpolator(1.35f)`.
   - Rebounded the primary capsule back to its resting coordinates and scale over 380ms with fluid deceleration `PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f)`.
5. **Tactile Tap Animation on Meta-Ball (`onMetaBallClicked`)**:
   - Added an organic squash-and-rebound ripple when the meta-ball is tapped (squashing to 0.85x over 100ms and springing back to 1.0x with `OvershootInterpolator(1.4f)` over 220ms) accompanied by instantaneous haptic click feedback.


---

## Section XXVIII: Material 3 Expressive Power Menu Redesign (Pixel 9 Pro XL) and Enhanced Power Menu Advanced Reboot

### 1. Requirements & Overview
- **User Request**:
  - Completely redesign the System Power Menu (GlobalActions) following the provided Material 3 Expressive mockup for Pixel 9 Pro XL with pixel-perfect precision.
  - UI components implemented:
    * Top Capsule Tag: Material 3 Expressive • Pixel 9 Pro XL and Header: Menu Daya Sistem.
    * Power Off Slider: Crimson red rounded pill Geser untuk Mematikan Daya with subtitle Tetap terlacak oleh Find My Device and a circular blush pink puck draggable/tappable to power down.
    * Three Squircle Cards Row: Mulai Ulang (pastel mint green card), Lockdown (dark olive card), and Darurat (dark teal card).
    * Quick Controls Bottom Card: Dark container Kontrol Cepat Rumah & Dompet with Pixel VIP tag housing Home Controls (Lampu Kamar / Menyala • 80%) and Google Wallet (Google Wallet / Visa •••• 4821).
  - No Dummy Functions: Every button and action is wired to actual Android system APIs and Intents without placeholder behaviors.
  - Advanced Reboot Integration: Full advanced reboot support including System, Recovery Mode, Bootloader/Fastboot, and SystemUI restart.
  - Settings Integration: Add a switch preference named Enhanced Power Menu directly under Aktifkan mulai ulang lanjutan in the Power button settings menu, with broadcast trigger to cleanly restart SystemUI.

### 2. Implementation Details
1. **Material 3 Expressive Layout & Vector Assets**:
   - Created primary layout global_actions_enhanced_menu.xml faithfully replicating the Pixel 9 Pro XL layout architecture.
   - Crafted 18 custom vector drawables and ripple backgrounds:
     * power_menu_tag_pill_bg.xml: Dark green pill capsule with subtle border stroke.
     * power_menu_slider_bg.xml: Crimson red #78000C pill background for shutdown slider track.
     * power_menu_slider_puck_bg.xml: Cream/blush pink circular puck with touch ripple feedback.
     * power_menu_card_restart_bg.xml: Pastel mint #9EE4B4 squircle with 26dp rounded corners.
     * power_menu_card_lockdown_bg.xml: Dark olive #2B3830 squircle with 26dp rounded corners.
     * power_menu_card_emergency_bg.xml: Deep teal #1E3F45 squircle with 26dp rounded corners.
     * power_menu_card_quick_controls_bg.xml: Deep dark slate #181E1B container for quick affordances.
     * power_menu_control_home_bg.xml & power_menu_control_wallet_bg.xml: Specialized tile backgrounds for smart home and wallet.
     * Clean vector icons: ic_power_menu_power.xml, ic_power_menu_restart.xml, ic_power_menu_lockdown.xml, ic_power_menu_emergency.xml, ic_power_menu_home.xml, ic_power_menu_wallet.xml, and ic_power_menu_quick_controls.xml.
2. **Tactile Power Slider Gesture Physics**:
   - Developed dynamic touch and drag physics for the power off slider puck:
     * Calculates maximum travel distance (maxSlide) dynamically from track width and paddings.
     * Seamlessly tracks leftward finger movement with 1.08x touch scaling and smooth label text alpha fading.
     * Passing 42% of travel distance triggers confirmation haptic feedback (HapticFeedbackConstants.CONFIRM), snaps puck to the left, and invokes mWindowManagerFuncs.shutdown().
     * Tapping without dragging plays tactile feedback and animates the full slide sequence before powering off.
     * Releasing before threshold triggers an organic bounce back using OvershootInterpolator(1.2f).
3. **In-Place Advanced Reboot Container**:
   - Connected Mulai Ulang card: if advanced reboot is enabled, seamlessly swaps power menu content with the Advanced Reboot panel via a fluid cross-fade transition:
     * System Reboot: Dispatches mWindowManagerFuncs.reboot(false, null).
     * Recovery Mode: Dispatches mWindowManagerFuncs.reboot(false, PowerManager.REBOOT_RECOVERY).
     * Bootloader Mode: Dispatches mWindowManagerFuncs.reboot(false, PowerManager.REBOOT_BOOTLOADER).
     * Restart SystemUI: Dismisses dialog and immediately restarts the SystemUI process via Process.killProcess(Process.myPid()).
     * Cancel/Back Button: Instantly animates back to the main power menu.
   - If advanced reboot is disabled, Mulai Ulang executes a standard reboot, with long-press support for safe mode boot.
4. **Real Platform Wiring for Lockdown, Emergency, Home Controls, and Wallet**:
   - Lockdown: Locks the device immediately and enforces credential entry via mLockPatternUtils.requireCredentialEntry() and WindowManagerGlobal.getWindowManagerService().lockNow().
   - Emergency: Dispatches emergency intent via mTelecomManager.createLaunchEmergencyDialerIntent() or EmergencyAffordanceManager.
   - Quick Home Controls: Launches Android native device controls com.android.systemui.controls.ui.ControlsActivity.
   - Google Wallet: Launches Google Wallet package (com.google.android.apps.walletnfcrel) with fallback to WalletActivity or NFC payment settings.
5. **Settings Configuration & Provider Registration**:
   - Declared Settings.System.ENHANCED_POWER_MENU constant in Settings.java.
   - Added boolean validator in SystemSettingsValidators.java and backup inclusion in SystemSettings.java.
   - Added switch preference enhanced_power_menu in packages/apps/Settings/res/xml/power_menu_settings.xml directly under advanced_reboot.
   - Implemented preference listener in PowerMenuSettings.java that automatically broadcasts SysuiRestartReceiver.ACTION when toggled.
   - Fully localized all strings in Indonesian (res/values-in/shinkai_strings.xml) and English (res/values/shinkai_strings.xml).


---

## Section XXIX: Wowo OS Power Menu Update: Wowo OS Tag Rebranding and Functional Flashlight & Airplane Mode Quick Controls

### 1. Requirements & Overview
- **User Request**:
  - Remove the "Pixel VIP" tag on the bottom quick controls card and rebrand it to "Wowo OS".
  - Replace rarely used Home Controls and Google Wallet tiles with:
    * Flashlight (Senter): 100% functionally wired to hardware flash control via FlashlightController, including live state synchronization (On / Off) and dynamic background state tinting.
    * Airplane Mode (Mode Pesawat): 100% functionally wired to system radio toggling via ConnectivityManager and Settings.Global.AIRPLANE_MODE_ON, including live state synchronization (On / Off) and dynamic background state tinting.
  - Update group title label to "Quick System Controls".
  - Strictly maintain zero dummy features and zero emojis across all documentation.

### 2. Implementation Details
1. **Resource Strings (Indonesian & English)**:
   - Updated frameworks/base/packages/SystemUI/res/values/shinkai_strings.xml:
     * power_menu_vip_tag: "Wowo OS"
     * power_menu_quick_controls: "Quick System Controls"
     * power_menu_torch: "Flashlight", power_menu_torch_on: "On", power_menu_torch_off: "Off"
     * power_menu_airplane: "Airplane Mode", power_menu_airplane_on: "On", power_menu_airplane_off: "Off"
   - Updated frameworks/base/packages/SystemUI/res/values-in/shinkai_strings.xml:
     * power_menu_vip_tag: "Wowo OS"
     * power_menu_quick_controls: "Kontrol Cepat Sistem"
     * power_menu_torch: "Senter", power_menu_torch_on: "Nyala", power_menu_torch_off: "Mati"
     * power_menu_airplane: "Mode Pesawat", power_menu_airplane_on: "Aktif", power_menu_airplane_off: "Nonaktif"
2. **Vector Icons & State-Aware Drawables**:
   - ic_power_menu_torch.xml: Precision vector flashlight silhouette with bright amber/yellow #FFD60A reflector.
   - ic_power_menu_airplane.xml: Aerodynamic airplane vector icon in cyan/teal #80D2DC.
   - power_menu_control_torch_on_bg.xml: Interactive ripple drawable with dark olive/amber tint #2E3812 and stroke #4D5E1F for active flashlight state.
   - power_menu_control_airplane_on_bg.xml: Interactive ripple drawable with deep teal tint #163A42 and stroke #245964 for active airplane mode state.
   - power_menu_control_off_bg.xml: Interactive ripple drawable with neutral dark slate #28322D with 18dp rounded corners for idle state.
3. **Layout Integration (global_actions_enhanced_menu.xml)**:
   - Replaced top-right tag text with @string/power_menu_vip_tag (Wowo OS).
   - Replaced left control tile with @+id/btn_torch featuring @drawable/ic_power_menu_torch, @string/power_menu_torch, and @+id/txt_torch_status.
   - Replaced right control tile with @+id/btn_airplane featuring @drawable/ic_power_menu_airplane, @string/power_menu_airplane, and @+id/txt_airplane_status.
4. **Real Platform Control Logic & Lifecycle Management (GlobalActionsDialogLite.java)**:
   - **FlashlightController Integration**:
     * Retrieved FlashlightController instance via Dependency.get(FlashlightController.class).
     * Initialized live hardware state on dialog creation (flashlightController.isEnabled()), applying appropriate background drawables and status text "On" (#FFD60A) or "Off" (#A6B0AA).
     * Added real-time FlashlightListener callback to keep UI synchronized even when toggled from outside.
     * Handled clicks with tactile haptics (HapticFeedbackConstants.CONTEXT_CLICK), flipping state via flashlightController.setFlashlight(!isEnabled) and instantaneously refreshing the tile without dismissing the dialog.
   - **ConnectivityManager & System Settings Integration**:
     * Queried real airplane mode state via Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1.
     * Bound active teal theme #163A42 with "On" (#80D2DC) or inactive #28322D with "Off" (#A6B0AA).
     * Registered ContentObserver on Settings.Global.AIRPLANE_MODE_ON URI to observe changes from Quick Settings, Settings, or background services.
     * Handled tile clicks with tactile haptics, invoking ConnectivityManager.setAirplaneMode(!isAirplaneOn) and broadcasting ACTION_AIRPLANE_MODE_CHANGED.
   - **Lifecycle Teardown on onStop**:
     * Cleaned up mTorchListener from FlashlightController.
     * Unregistered mAirplaneContentObserver from ContentResolver to prevent memory leaks.


---

## Section XXX: Default System Wallpaper Integration: Kopdes Ontop

### 1. Requirements & Overview
- **User Request**:
  - Configure the kopdes_ontop image in the root project as the out-of-the-box default wallpaper across the ROM.
  - Ensure the wallpaper is rendered in full sharpness and aspect ratio tailored for the device display (POCO F5 marble, 1080x2400).

### 2. Implementation Details
1. **Image Processing & Format Conversion**:
   - Ingested source image kopdes_ontop.jpg from the project root and saved kopdes_ontop.png directly in the project root.
   - Rescaled and fitted the image to standard 1080 x 2400 resolution using Lanczos resampling (ImageOps.fit), preserving vertical composition and aspect ratio without distortion.
2. **Framework & RRO Default Wallpaper Replacement**:
   - Replaced core Android framework-res wallpapers:
     * frameworks/base/core/res/res/drawable-nodpi/default_wallpaper.png
     * frameworks/base/core/res/res/drawable-sw600dp-nodpi/default_wallpaper.png
     * frameworks/base/core/res/res/drawable-sw720dp-nodpi/default_wallpaper.png
   - Replaced Runtime Resource Overlay assets in FrameworkOverlayCustom:
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-hdpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-xhdpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-xxhdpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-xxxhdpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-nodpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-sw600dp-nodpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/drawable-sw720dp-nodpi/default_wallpaper.png
3. **Installer Script Enhancement (apply.sh)**:
   - Updated apply_ui() in apply.sh to copy frameworks/base/core and vendor/custom overlay assets to target source trees.


---

## Section XXXI: Kokopdes Custom Bootanimation & Bootaudio Integration and Pure Local Build Optimization (Non-RBE)

### 1. Requirements & Overview
- **User Request**:
  - Extract and integrate the custom bootanimation and bootaudio from kokopdes cool_modules.zip into the ROM.
  - Disable RBE (export USE_RBE=false) in the build script temp.sh to prevent reproxy port 8081 dial timeouts and unlock all 96 server CPU cores for pure local compilation.

### 2. Implementation Details
1. **Bootanimation & Bootaudio Extraction (1080p)**:
   - Extracted common/bootanimation.zip (1080x1920, 30 FPS, uncompressed Store Type 0 format per Android BootAnimation parser specification) and common/bootaudio.mp3 from kokopdes cool_modules.zip.
   - Replaced target prebuilt animation archives in vendor/custom/bootanimation/:
     * bootanimation_1080.zip
     * bootanimation_1440.zip
     * bootanimation_720.zip
   - Integrated audio file vendor/custom/bootanimation/bootaudio.mp3.
2. **Prebuilt Media Declaration & Product Packaging**:
   - Declared bootaudio_pixelos prebuilt_media module in vendor/custom/bootanimation/Android.bp installing bootaudio.mp3 into /product/media/bootaudio.mp3.
   - Added bootaudio_pixelos to PRODUCT_PACKAGES in vendor/custom/config/common.mk.
3. **Build Script Optimization (temp.sh)**:
   - Added export USE_RBE=false prior to m shinkai -j$(nproc) to bypass remote build execution dependencies and compile purely using local hardware across 96 parallel cores.

---

## Section XXXII: Power Menu Device Tag Rebranding (POCO F5) and Dynamic Material You Monet Color Adaptation

### 1. Requirements & Overview
- **User Request**:
  - Correct the top capsule pill tag in the Power Menu: replace "Material 3 Expressive • Pixel 9 Pro XL" with "Material 3 Expressive • POCO F5" to accurately reflect the target device (POCO F5 / marble).
  - Transition the Power Menu color scheme from static solid green shades (#042D1C, #9EE4B4, #2B3830, etc.) to Android Material You dynamic Monet color tokens, enabling the menu to organically harmonize with the user's active wallpaper palette.

### 2. Implementation Details
1. **Device Identity String Update**:
   - Updated power_menu_tag_pill string resource across:
     * frameworks/base/packages/SystemUI/res/values/shinkai_strings.xml
     * frameworks/base/packages/SystemUI/res/values-in/shinkai_strings.xml
   - New value: "Material 3 Expressive • POCO F5".
2. **Power Menu Drawables Refactored to Dynamic Monet Tokens**:
   - Replaced all hardcoded green hex colors with official Android Material You system palette resources (@android:color/system_*):
     * power_menu_tag_pill_bg.xml: Uses solid @android:color/system_accent1_900 with stroke @android:color/system_accent1_700.
     * power_menu_card_restart_bg.xml: Uses hero accent fill @android:color/system_accent1_200 with ripple @android:color/system_accent1_100.
     * power_menu_card_lockdown_bg.xml: Uses neutral surface container @android:color/system_neutral1_800 with border @android:color/system_neutral2_800.
     * power_menu_card_emergency_bg.xml: Uses tertiary accent @android:color/system_accent3_800 with border @android:color/system_accent3_700.
     * power_menu_card_quick_controls_bg.xml: Uses container background @android:color/system_neutral1_900 with subtle outline @android:color/system_neutral2_800.
     * power_menu_control_off_bg.xml: Uses inactive tile background @android:color/system_neutral1_800 with outline @android:color/system_neutral2_800.
     * power_menu_control_torch_on_bg.xml: Uses active primary accent @android:color/system_accent1_800 with border @android:color/system_accent1_600.
     * power_menu_control_airplane_on_bg.xml: Uses active secondary accent @android:color/system_accent2_800 with border @android:color/system_accent2_600.
     * power_menu_advanced_item_bg.xml: Uses surface container @android:color/system_neutral1_800 with border @android:color/system_neutral2_800.
3. **Vector Drawable & Layout Refinements**:
   - Standardized vector drawable base fillColors (ic_power_menu_restart, ic_power_menu_emergency, ic_power_menu_torch, ic_power_menu_airplane, ic_power_menu_quick_controls) to pure white (#FFFFFFFF) to allow seamless dynamic tinting.
   - Updated global_actions_enhanced_menu.xml and global_actions_advanced_reboot_dialog.xml:
     * Top pill tag: Text styled with @android:color/system_accent1_200.
     * Restart Card: High-contrast on-accent text and icon tint @android:color/system_accent1_900 over system_accent1_200.
     * Lockdown Card: Text and icon styled with @android:color/system_neutral1_50.
     * Emergency Card: Text and icon styled with @android:color/system_accent3_200.
     * Header & VIP Tag: Styled with @android:color/system_neutral1_100 and @android:color/system_accent1_200.
4. **Dynamic Status Color Runtime Logic in GlobalActionsDialogLite.java**:
   - Replaced hardcoded status hex values with dynamic context.getColor() Monet references:
     * Torch (Flashlight): Active tint android.R.color.system_accent1_200; inactive tint system_neutral2_300 / system_neutral1_100.
     * Airplane Mode: Active tint android.R.color.system_accent2_200; inactive tint system_neutral2_300 / system_neutral1_100.

---

## Section XXXIII: Comprehensive Rebranding to PalapaOS, Default Wallpaper Update, New Bootanimation, and Palapa Satellite Logo Integration

### 1. Requirements & Overview
- **User Request**:
  - Comprehensive rebranding from WowoOS / Wowo OS to PalapaOS / Palapa OS across the ROM codebase and user-facing interfaces.
  - Update system default wallpaper from kopdes_ontop to palapa_os.jpg.
  - Replace product bootanimation with the gemini.zip archive located in the root project directory.
  - Remove the Kopdes edition card (and kopdes logo) from the Android version info interface (shinkai_logo.xml).
  - Update the Wowo OS version row to Palapa OS version with version code changing from Embegeh-1T to Majapahit.
  - Replace all Kopdes logos across the interface with the provided Palapa Satellite SVG (gold Palapa cylinder body, cyan solar panel arrays, gold letter-P satellite orbit, signal emitter antenna).

### 2. Implementation Details
1. **Operating System Identity Rebranding**:
   - Updated product build properties in vendor/custom/config/version.mk:
     * Added ro.palapa.edition=Majapahit Edition, ro.palapa.os.name=Palapa OS, ro.palapa.os.version=Majapahit.
     * Updated ro.wowo.edition=Majapahit Edition, ro.wowo.os.name=Palapa OS, ro.wowo.os.version=Majapahit for full backward compatibility.
   - Updated OTA target package filename in vendor/lineage/build/tasks/bacon.mk to PalapaOS-Majapahit-$(TARGET_DEVICE)-17.zip.
   - Updated power_menu_vip_tag in SystemUI strings (values & values-in) to "Palapa OS".
   - Updated installer banner and script metadata in apply.sh to Shinkai Project / PalapaOS Redesign UI.
2. **Default Wallpaper Replacement (palapa_os.jpg)**:
   - Converted palapa_os.jpg to clean 1080 x 2400 pixel PNG format utilizing high-precision Lanczos resampling (ImageOps.fit).
   - Deployed updated default_wallpaper.png across all system destinations:
     * frameworks/base/core/res/res/drawable-nodpi/default_wallpaper.png
     * frameworks/base/core/res/res/drawable-sw600dp-nodpi/default_wallpaper.png
     * frameworks/base/core/res/res/drawable-sw720dp-nodpi/default_wallpaper.png
     * vendor/custom/overlay/rro_packages/FrameworkOverlayCustom/res/ (hdpi, xhdpi, xxhdpi, xxxhdpi, nodpi, sw600dp, sw720dp).
3. **Gemini Bootanimation Deployment (gemini.zip)**:
   - Extracted uncompressed bootanimation.zip (1080x498, 30 FPS, Store Type 0 specification) from gemini.zip.
   - Replaced animation prebuilts in vendor/custom/bootanimation/:
     * bootanimation_1080.zip
     * bootanimation_1440.zip
     * bootanimation_720.zip
4. **Android Version Info & About Phone Dashboard Updates**:
   - Deleted wowo_stat_edition_card (Kopdes edition card) in packages/apps/Settings/res/layout/shinkai_logo.xml, leaving a clean balanced two-card layout (Android Version and Device).
   - Updated About Phone banner in about_phone_dashboard.xml from "Wowo OS 19jt" to "Palapa OS" and subtitle to "Majapahit Edition • Shinkai Project".
   - Updated version row in shinkai_logo.xml from "Embegeh-1T" to "Majapahit".
   - Updated preference controllers AboutPhoneDashboardPreferenceController.kt, ShinkaiLogoPreference.kt, and ShinkaiVersionPreference.kt to dynamically reference Palapa OS and Majapahit Edition.
5. **Palapa Satellite Logo Rendering & Integration**:
   - Rendered the Palapa Satellite SVG into high-resolution transparent raster assets (512 x 512 pixels, 16-bit RGBA) using ImageMagick.
   - Replaced packages/apps/Settings/res/drawable-nodpi/kopdes.png and added palapa_logo.png with the new Palapa Satellite emblem.
---

## Section XXXIV: Complete Purge of Legacy Internal Identifiers (wowo_ to palapa_), XML/Kotlin Resource Refactoring, and README Documentation Synchronization

### 1. Requirements & Overview
- **User Request**:
  - Rename all internal identifiers (resource IDs, string names, color tokens, drawable filenames) from legacy prefix wowo_ to palapa_ across Settings and SystemUI codebases.
  - Update changelogs-id, changelogs-en, and README.md documentation to reflect the official PalapaOS (Majapahit Edition) identity.
  - Ensure all changelog files strictly adhere to the zero emojis policy.

### 2. Implementation Details
1. **Interface Drawable Asset Refactoring (res/drawable)**:
   - Renamed 14 visual asset files from prefix wowo_ to palapa_:
     * wowo_badge_edition_bg.xml -> palapa_badge_edition_bg.xml
     * wowo_badge_sub_bg.xml -> palapa_badge_sub_bg.xml
     * wowo_banner_card_background.xml -> palapa_banner_card_background.xml
     * wowo_card_flag_border.xml -> palapa_card_flag_border.xml
     * wowo_divider.xml -> palapa_divider.xml
     * wowo_hashtag_badge_bg.xml -> palapa_hashtag_badge_bg.xml
     * wowo_icon_box_bg.xml -> palapa_icon_box_bg.xml
     * wowo_logo_box_bg.xml -> palapa_logo_box_bg.xml
     * wowo_mini_indonesia_flag.xml -> palapa_mini_indonesia_flag.xml
     * wowo_shinkai_credit_bg.xml -> palapa_shinkai_credit_bg.xml
     * wowo_shinkai_icon_circle_bg.xml -> palapa_shinkai_icon_circle_bg.xml
     * wowo_stat_box_bg.xml -> palapa_stat_box_bg.xml
     * ic_wowo_maintainer_device.xml -> ic_palapa_maintainer_device.xml
     * ic_wowo_maintainer_source.xml -> ic_palapa_maintainer_source.xml
   - Updated internal color references inside these drawable files to the palapa_ scheme (e.g. @color/palapa_banner_bg).
2. **Layout IDs and XML View Hierarchy Refactoring (res/layout/shinkai_logo.xml & settings_homepage_container.xml)**:
   - Renamed all view component IDs from wowo_* to palapa_*:
     * palapa_hero_card, palapa_header_flipper, palapa_banner_os_title, palapa_banner_edition_badge, palapa_banner_official_badge
     * palapa_stats_row, palapa_stat_android_card, palapa_stat_android_version, palapa_stat_device_card, palapa_stat_device_name, palapa_shinkai_credit_bar
     * palapa_chunk_system, palapa_row_android_version, palapa_val_android_version, palapa_row_shinkai_version, palapa_val_shinkai_version
     * palapa_row_palapa_version, palapa_val_palapa_version, palapa_row_security_patch, palapa_val_security_patch
     * palapa_row_maintainer_source, palapa_val_maintainer_source, palapa_row_maintainer, palapa_val_maintainer
     * palapa_chunk_hardware, palapa_row_kernel, palapa_val_kernel, palapa_row_baseband, palapa_val_baseband
     * palapa_chunk_build, palapa_row_build_number, palapa_val_build_number
   - Updated homepage glance string reference to @string/home_glance_palapa_edition.
3. **String Resources and Color Token Harmonization (res/values & res/values-in & res/values-night)**:
   - Updated all string resource keys in shinkai_strings.xml:
     * palapa_os_title, palapa_os_version, palapa_edition_badge_text, palapa_version_edition, palapa_official_badge
     * palapa_stat_os_label, palapa_stat_edition_label, palapa_stat_device_label, palapa_system_specs_title, palapa_copied_toast
     * palapa_based_shinkai_project, palapa_shinkai_hashtag, home_glance_palapa_edition, palapa_chunk_system_title, palapa_chunk_hardware_title
   - Updated all color tokens in colors.xml and values-night/colors.xml:
     * palapa_banner_bg, palapa_card_stroke, palapa_logo_container_bg, palapa_logo_container_stroke
     * palapa_edition_badge_bg, palapa_edition_badge_stroke, palapa_chip_bg, palapa_stat_box_bg, palapa_divider_color
4. **Kotlin & Java Source Controllers Refactoring**:
   - ShinkaiLogoPreference.kt: Bound all view lookups to R.id.palapa_* and string resources to R.string.palapa_*.
   - ShinkaiVersionPreference.kt: Pointed primary system property reading to KEY_PALAPA_EDITION (ro.palapa.edition) with a safe backwards-compatible fallback to KEY_LEGACY_EDITION (ro.wowo.edition).
   - AboutPhoneDashboardPreferenceController.kt: Updated runtime display logic to present Palapa OS and Majapahit Edition.
   - Refactored copyright header notices across 58 UI controller files to Copyright (C) 2026 Palapa OS / Shinkai Project.
5. **Documentation and README.md Synchronization**:
   - Synchronized README.md header and feature overview to Shinkai Project - PalapaOS Redesign UI (POCO F5 & Generic Android 17).
   - Synchronized all engineering and technical entries across changelogs.md, changelogs-id.md, and changelogs-en.md under strict zero emoji validation.
---

## Section XXXV: Implementation of DT2W (Double Tap to Wake) and DT2S (Double Tap to Sleep) Features

### 1. Requirements & Overview
- **User Request**:
  - Add and enable DT2W (Double Tap to Wake) and DT2S (Double Tap to Sleep) features for PalapaOS (POCO F5 / `marble`).
  - Ensure end-to-end integration across touchscreen kernel drivers, Power HAL, AOSP framework, SystemUI, and Display Settings.
  - Ensure all changelog files strictly adhere to the zero-emoji requirement.

### 2. Subsystem Analysis & Hardware Architecture
1. **POCO F5 (`marble`) Kernel & Sysfs Support**:
   - Sysfs nodes `/sys/class/touch/touch_dev/gesture_double_tap_enabled` and `gesture_double_tap_state` are configured in `init.marble.rc` with system permissions.
   - Vendor double tap sensor is monitored by `NonUiNotifier.cpp` using the sensor type `org.lineageos.sensor.double_tap`.
2. **Double Tap to Wake (DT2W)**:
   - `PowerManagerService` dispatches `Mode::DOUBLE_TAP_TO_WAKE` to the Power HAL when `config_supportDoubleTapWake` is set to true.
   - `TapToWakePreferenceController.java` in Settings manages `Settings.Secure.DOUBLE_TAP_TO_WAKE`.
3. **Double Tap to Sleep (DT2S)**:
   - `QQSGestureListener.kt` listens for double-taps on the status bar / QQS and lockscreen to trigger `powerManager.goToSleep()`.
   - `NotificationPanelViewController.java` detects double taps and routes them to power sleep when `Settings.System.DOUBLE_TAP_SLEEP_GESTURE` is enabled.
   - `config_supportDoubleTapSleep` and `config_dt2sGestureEnabledByDefault` configure the feature out of the box.

### 3. Implementation Details & File Modifications
1. **Device Tree & Framework Overlays (`device/xiaomi/marble`)**:
   - `overlay/xiaomi/Frameworks/res/values/config.xml` & `overlay/marble/Frameworks/res/values/config.xml`:
     * Added `<bool name="config_supportDoubleTapWake">true</bool>` to enable DT2W in the framework and Display Settings.
     * Added `<bool name="config_supportDoubleTapSleep">true</bool>` to declare system-level DT2S support.
     * Added `<bool name="config_dt2sGestureEnabledByDefault">true</bool>` to enable the DT2S gesture by default.
2. **SettingsProvider Defaults (`frameworks/base/packages/SettingsProvider`)**:
   - `res/values/defaults.xml`: Set `def_double_tap_to_sleep` to `true` to ensure consistent defaults.
3. **Display Settings UI (`packages/apps/Settings`)**:
   - `res/xml/display_settings.xml`:
     * "Tap to wake" (`tap_to_wake`) is enabled dynamically based on `config_supportDoubleTapWake`.
     * Set `android:defaultValue="true"` for `double_tap_sleep_gesture` ("Tap to sleep").
4. **Redesign UI Repository Synchronization (`shinkai_project_palapaos_redesignui`)**:
   - Synchronized all updated overlay and Settings XML configurations to the PalapaOS redesign UI repository.
   - Updated changelog files with verified zero emojis.
