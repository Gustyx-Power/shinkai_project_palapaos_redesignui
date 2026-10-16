# System Update Changelog: Palapa OS (Triwulan Edition)
Target Device: POCO F5 (marble) / AOSP Generic  
Android Version: Android 17 (CP2A)  
Branch: `palapa-triwulan`  
Release Status: Stable & Verified  

Document Navigation:
- [English Version (changelogs-en.md)](changelogs-en.md)
- [Indonesian Version (changelogs-id.md)](changelogs-id.md)

---

## Update Summary (Triwulan Edition)

The new **`palapa-triwulan`** branch introduces a comprehensive system interface overhaul inspired by **Samsung One UI 9**, a complete redesign of the Battery settings page, enhanced Quick Settings / Control Center, an interactive punch-hole Dynamic Island, and deep kernel-level memory optimizations:

---

### 1. Total Battery Settings UI Redesign (Samsung One UI 9 Style)
The Battery settings screen (`Settings -> Battery`) has been completely overhauled to adopt authentic Samsung One UI 9 design principles while preserving 100% of the existing battery management features and system controls:

* **Complete Elimination of Pixel Remnants**:
  * Fully removed all stock Google Pixel header elements (`battery_header` UsageProgressBarPreference and `battery_header_text` subtitle) and suppressed duplicate `battery_usage_summary` entries.
  * Replaced flat lists with One UI grouped rounded card containers (corner radius 26dp) adhering strictly to dynamic Material You Monet palette tokens (`@color/settingslib_materialColorSurfaceContainer`).
  * Hardened controller lifecycle and null-safety guards in `PowerUsageSummary` for `BatteryHeaderTextPreferenceController` and `BatteryTipPreferenceController`, paired with invisible preference placeholders to completely eliminate NullPointerException crashes during battery loading.

* **Hero Overview Header**:
  * Large and prominent bold percentage indicator (`81%`) on the left.
  * Real-time estimated discharge time on the right, baseline-aligned.
  * Full rounded capsule pill progress bar (radius 100dp) in vibrant One UI Green (`#2ED158`) with an elegant Monet surface container track.
  * Dynamic full-charge duration or time remaining until full charge subtitle paired with an interactive info button `(i)`.

* **Unified Interactive Battery Usage Card (Real Data)**:
  * Contained within a sleek One UI rounded card fully adaptive to AMOLED dark mode and Material You Monet dynamics.
  * **Interactive Dropdown Selector**:
    * **Daily Usage Mode**: Displays a 7-day vertical pill bar chart rendered with Monet primary tint (`@color/settingslib_materialColorPrimary`), dashed average baseline (`--- Avg.`), Y-axis scale (`300` and `0 (%)`), and a circular badge highlighting the current day based on device locale. Bar heights are dynamically calculated from real past 7 days foreground application usage.
    * **Since Last Charge Mode**: Displays charging history subtitles (*"Last charged to 100%"*, *"Stopped charging..."*), a smooth Bezier area curve with translucent Monet blue gradient, start badge `⚡100`, current level pin bubble indicator, power-saving projection trajectory, and battery level legends.
  * **Real Application Consumption Breakdown (No Dummy Data)**:
    * Subheader label *"Battery usage: X%"*.
    * Displays genuine installed applications actively consuming power as reported by `BatteryStatsManager` and `UsageStatsManager`, with authentic application icons, labels, precise consumption percentages, and fine hairline dividers.
    * Centered footer action button *"View details"* to launch advanced battery history (`PowerUsageAdvanced`).

* **Dedicated "Battery information" Page**:
  * Direct access via the header info icon `(i)` or the Battery Information list entry, with the proper action bar title (`< Battery information`).
  * Unified One UI card displaying genuine hardware metrics:
    1. **Battery status**: Charging status (*Not charging*, *Charging*, *Full*).
    2. **Battery level**: Current percentage.
    3. **Battery health**: Genuine battery health percentage (e.g. `100% (Good)` or `98% (Good)`) computed from design capacity ratio and sysfs SOH.
    4. **Cycle count**: Battery charge cycle count (e.g. `142 cycles`) retrieved from battery intent or sysfs kernel interfaces.
    5. **Battery capacity**: Typical capacity (`5000 mAh (typical)`) dynamically resolved from hardware and device PowerProfile.
    6. **Battery temperature**: Real-time battery temperature in Celsius (`31.5 °C`).
    7. **Battery voltage**: Real-time battery terminal voltage in Volts (`4.12 V`).
  * Official IEC 61960 laboratory testing standard disclaimer paragraph below the card.

* **100% Feature Preservation**:
  * All previous system preferences remain intact within neatly organized One UI card groups:
    * **Power Management**: *Battery Saver* and *Adaptive Battery (Smart Battery)*.
    * **Charging Settings**: *Lineage Health Charging Control* and *Charging Speed*.
    * **Battery Info & Display**: *Status Bar Battery Percentage*, *Lockscreen Charging Info*, and *Battery Information*.

---

### 2. Control Center & Quick Settings (One UI QS Style)
* **Single-Pull Quick Settings (QQS)**:
  * Segregated layout with 6 circular quick toggles on top, a flicker-free brightness slider in the center, and 2 large capsule buttons (Wi-Fi & Bluetooth) at the bottom.
* **Expanded Control Center**:
  * Frosted glass card containers, integrated media volume slider, sound mode selector (Ring, Vibrate, Silent), and direct power/settings shortcuts.
* **Free 2D Tile Edit Mode**:
  * Freely drag quick setting tiles in any direction (horizontal and vertical) with smooth elevation lift effects and an ergonomic floating "Done" button reachable with one hand.

---

### 3. Interactive Dynamic Island (Punch-Hole Camera)
* **Charging Indicator**: Displays an animated green lightning bolt and live battery percentage for 7.8 seconds when plugged in.
* **Flashlight Indicator**: Minimalist compact pill with a bespoke torch icon and bold "On" status. Tap once to toggle off.
* **Screen Recording Indicator**: Compact pill with a pulsing red record dot and running digital timer.
* **Dual-Island (Meta-Ball Fluid Physics)**: When recording and flashlight run simultaneously, a secondary circular meta-ball docks cleanly on the left side with fluid liquid physics swap animations.

---

### 4. Settings Dashboard Enhancements
* **Homepage Quick Dashboard**: Real-time status cards to monitor Battery, Storage, RAM, Display, and Sound at a glance.
* **Colored Notification Icons**: Option in Notification Settings to show genuine, full-color app logos on status bar icons and shade notification cards.
* **About Phone Palapa OS Dashboard**: Shinkai Project visual banner with hardware spec meters (SoC, display, camera) and contributor credits.

---

### 5. Performance & System Tweaks
* **Dynamic RAM Expansion (zRAM)**: True Linux kernel-integrated zRAM configuration (Options: Off, +2 GB, +4 GB, +8 GB, +16 GB).
* **Gesture Navigation Tweaks**: Dedicated toggles to hide the gesture navigation pill and remove the bottom IME gap under the keyboard.
* **Enhanced Power Menu**: Material 3 Expressive power menu with quick controls and advanced reboot (Recovery / Bootloader).

---

### 6. Security, Key Attestation & Locked Bootloader Spoofing
* **Hardware-Backed Key Attestation Spoofing**:
  * Native integration of authentic Google Root CA (`Key Attestation CA1`) keybox into system framework configuration (`custom_config.xml` & `KeyProviderManager`).
  * Full support for modern ECDSA-only RKP keyboxes with preserved sequential numerical certificate chain ordering (`CERT_1 .. CERT_4`).
  * Key Attestation applications (Nikolay Elenkov / Key Attestation Demo) reliably report **Device locked: true** and **Verified Boot State: Verified (Green)**.
* **Play Integrity GMS Integration & Play Store Certification**:
  * Built-in official certified device fingerprint from Google Pixel Fold (Android 17, `felix_beta`) in `config_verifiedDeviceSpecs` enriched with `TYPE:user`, `TAGS:release-keys`, and `INCREMENTAL`.
  * Updated `PropImitationHooks` to spoof all `com.google.android.gms` processes (including CheckinService), ensuring Google Play Store officially reports **"Device is certified"**.
  * Prioritized key attestation blocking for DroidGuard callers (`isCallerPlayIntegrity()`) prior to keybox lookup, compelling Play Integrity to fall back to software evaluation and successfully acquire **`MEETS_DEVICE_INTEGRITY`**.
* **Dynamic Anti-Revoked Resilience & Spoof Option UI Integration**:
  * **On-Device Automatic PIF Generator (`Auto PIF Generator`)**: Added an automated PIF profile generation and update preference right inside the Settings *Spoof Option* menu. Users can fetch live, unrevoked Pixel Canary/Beta builds directly from Google Flash Station or Developer Builds without requiring root access or terminal commands.
  * **One-Tap Play Store & GMS Data/Cache Cleaner**: Added an option to wipe cache and storage for Google Play Store and Google Play Services directly from Settings UI utilizing system-privileged `clearApplicationUserData`, prompting immediate re-registration of device certification.
  * Multi-tier PIF resolution: `Settings.Secure.PIF_DATA`, local file storage (`/data/system/pif.json` or `/data/adb/pif.json`), OTA `FETCHED_PIF`, and built-in ROM fallback.
  * Multi-tier Keybox resolution: `Settings.Secure.KEYBOX_DATA`, local file storage (`/data/system/keybox.xml` or `/data/adb/tricky_store/keybox.xml`), and built-in ROM keychain.
  * Integrated `/system/bin/autopif` command-line utility with `fetch`, `clear`, `reset`, and `status` actions.
* **Google Account Login & GSF Token Reset Fix**:
  * Strictly scoped `sIsGms` to background DroidGuard (`com.google.android.gms.unstable`), preventing the Google account login UI process (`com.google.android.gms.ui` / `MinuteMaidActivity`) from prematurely terminating itself during authentication.
  * Resolved the *"Device needs to be reset to factory settings"* Play Protect certification prompt by incorporating *Google Services Framework* (`com.google.android.gsf`) into the Play Store/GMS cache cleaning routine, forcing generation of a fresh certified GSF Checkin ID without requiring a factory reset.
* **Kernel Command Line Filter (`/proc/cmdline`)**:
  * Seamless kernel filter (`cmdline.c`) rewriting `androidboot.verifiedbootstate=orange` to `green`, `androidboot.flash.locked=0` to `1`, and `androidboot.vbmeta.device_state=unlocked` to `locked` for direct OS-level readers.

* **Hardware Key Attestation Enhancement & "Attest Key" Fix**:
  * Removed the restrictive `if (attestationKey == null)` guard in `KeyStoreSecurityLevel.java`, ensuring asymmetric key attestation requests in Key Attestation app ("Attest key" mode) are consistently intercepted by `KeyboxImitationHooks`. This resolves the anomaly where the bootloader was previously reported as *Bootloader is Unlocked* in that mode.
* **Guaranteed Play Store Certification & Framework Signature Spoofing (DEVICE_INTEGRITY)**:
  * Prevented public keybox leakage to background DroidGuard (`com.google.android.gms.unstable`), avoiding Google CRL revocation flags and forcing DroidGuard into a clean software attestation fallback.
  * Injected official Google Pixel framework release key (`GOOGLE_RELEASE_SIGNATURE`) into `"android"` package queries specifically inside `com.google.android.gms.unstable` via dynamic `PackageInfo.CREATOR` hooking, guaranteeing Google Play Integrity passes **`MEETS_DEVICE_INTEGRITY`** and Google Play Store certifies the device as **"Device is certified"**.
* **Global Native Release-Keys Enforcement Across All Build Partitions**:
  * Configured `BUILD_KEYS := release-keys` and `BUILD_VERSION_TAGS := release-keys` globally across `vendor/custom/config/common.mk`, `shinkai_marble.mk`, and `build/make/core/config.mk`.
  * Ensured all partition `build.prop` outputs across `system`, `vendor`, `product`, `system_ext`, and `odm` natively emit `ro.*.build.tags=release-keys` and `BuildTag=release-keys` during compilation without relying purely on runtime spoofing.

---

### 5. Play Integrity, TrickyStore & TEESimulator Integrated Suite in Settings
Comprehensively transformed **Settings -> System -> Spoof Option** into a centralized, on-device attestation management dashboard uniting TrickyStore, PIF profile selector, and TEE Simulator capabilities without requiring Magisk or KernelSU root modules:

* **Material Header Card with Real-Time Diagnostics**:
  * Hero card featuring glowing security shield icon, status badge, active fingerprint summary, and 3 real-time status chips:
    * **Play Integrity**: `MEETS_DEVICE` (Green)
    * **Keybox State**: `ACTIVE` (Cyan)
    * **Bootloader State**: `LOCKED` (Green)
* **PIF Profile Catalog & Selector (PIF Suite)**:
  * Integrated a curated list of certified device presets with 1-tap switching:
    * Google Pixel 8 Pro (`husky`), Pixel 7 Pro (`cheetah`), Pixel 6a (`bluejay`)
    * Google Pixel 5 (`redfin`) with Initial API 30 (bypassing mandatory RKP requirements)
    * POCO F5 5G (`marble` HyperOS Global Stock)
    * Xiaomi 13 Pro (`nuwa` Global Stock)
    * Nothing Phone (2) (`Pong`)
    * ASUS ROG Phone 6 (`AI2201` Initial API 31)
  * Instant application writing to `Settings.Secure.PIF_DATA`, `/data/system/pif.json`, and automatic GMS refresh.
  * Preserved online OTA / Flash Station automated PIF fetcher and custom `pif.json` file loader.
* **TrickyStore Suite (Keybox & Root of Trust)**:
  * Real-time Keybox inspection displaying loaded cryptographic algorithm (EC / RSA) and validity.
  * **Custom Verified Boot Hash (`verified_boot_hash`)**: User-configurable 64-character SHA-256 boot hash spoofed directly into ASN.1 `KeyDescription` root of trust extension, replicating TrickyStore's `boot_hash`.
  * **Target Application Filter**: Configurable target modes (Recommended vs All Applications) and support for `/data/system/target.txt`.
* **TEESimulator Suite (Attestation Behavior & Patch Alignment)**:
  * **Enforce RKP Attestation Toggle (`tee_rkp_only`)**: Allows users to toggle RKP enforcement on/off to pass Strong integrity with Pixel 6-9 profiles on unlocked bootloaders.
  * **Auto Security Patch Synchronization (`tee_auto_spl`)**: Automatically matches simulated TEE boot and vendor patch levels with the active certified PIF profile date, eliminating patch mismatches.
  * **Simulated Security Level**: Toggle between TEE (Trusted Execution Environment) and StrongBox Keymaster.
* **Maintenance & Direct Testing**:
  * One-tap Play Store, GMS, and GSF data & cache cleaner with automatic package restarts.
  * Direct intent launcher to Key Attestation test applications.

### 5. Play Integrity Strong & Device Certification Parity (TrickyStore OSS Parity)
* **GMS Hardware Keybox Attestation Unblocked**:
  * Removed restrictive `isGms()` checks in `KeyboxImitationHooks.java`, allowing `com.google.android.gms` (DroidGuard worker) to receive genuine hardware Keybox certificate chains (`deviceLocked = true`, `verifiedBootState = 0`).
  * Re-ordered attestation handling in `PropImitationHooks.java`: Keystore attestation blocking (`UnsupportedOperationException`) is now bypassed whenever a Keybox is available, ensuring GMS successfully completes hardware attestation instead of failing to basic integrity.
* **System Properties Consistency for GMS**:
  * Integrated `PropImitationHooks.getSystemProperty` in `SystemProperties.java` (`get`, `getInt`, `getBoolean`) to return spoofed build properties, security patch levels, and locked boot state for GMS and Google Play Store processes, preventing native-to-Java property divergence.
* **Refined Spoof Option Header Card**:
  * Polished Material 3 card container with 26dp rounded squircle corners, subtle Monet outline stroke, and emerald security shield header.
  * Responsive 3-metric status row displaying real-time Play Integrity (`STRONG` / `DEVICE`), Hardware TEE (`HARDWARE`), and Bootloader state (`LOCKED`).
  * Enhanced Target Profile box with active profile indicator and formatted monospace fingerprint display.

---

## Technical Details & Modified Files

| Component | Related Files | Description |
|---|---|---|
| **Battery UI Header** | `OneUIBatteryHeaderPreference.kt`<br>`oneui_battery_header_card.xml` | Large percentage, time left estimate, and One UI pill progress bar |
| **Interactive Chart** | `OneUIBatteryChartView.kt` | Custom canvas view rendering 7-day vertical pill bars and gradient area curves with Monet adaptation |
| **Usage Card** | `OneUIBatteryUsageCardPreference.kt`<br>`OneUIBatteryUsageCardController.kt`<br>`oneui_battery_usage_card.xml` | Main card uniting dropdown filter, chart view, and real app breakdown via `BatteryStatsManager` |
| **Battery Info** | `OneUIBatteryInfoFragment.kt`<br>`oneui_battery_info_fragment.xml`<br>`SettingsGateway.java` | Dedicated sub-page for battery health %, cycle count, temp, voltage, and IEC 61960 capacity |
| **Unified Settings** | `power_usage_summary.xml`<br>`PowerUsageSummary.java`<br>`PowerUsageSummaryScreen.kt` | Restructured preference layout organizing all power features into card groups and eliminating Pixel headers |
| **Theme & Strings** | `colors.xml`, `values-night/colors.xml`<br>`shinkai_strings.xml`, `values-in/` | Dynamic Material You Monet palettes (`surfaceContainer`, `primary`) and comprehensive EN/ID translations |
| **Key Attestation & Bootloader Lock** | `custom_config.xml`<br>`KeyProviderManager.java`<br>`KeyboxDataPreference.java`<br>`KeyStoreSecurityLevel.java`<br>`cmdline.c` | Google Root Keybox ECDSA integration, RKP support, Pixel Fold PIF specs, "Attest key" bypass fix, and `/proc/cmdline` filter |
| **Play Integrity & Play Store Certification** | `PropImitationHooks.java`<br>`KeyboxImitationHooks.java`<br>`KeyProviderManager.java`<br>`SystemProperties.java`<br>`custom_config.xml`<br>`common.mk`, `autopif` | GMS hardware Keybox unblocking, property virtualization in `SystemProperties`, DroidGuard attestation bypass with Keybox, framework Pixel release signature spoofing, and Auto-PIF CLI |
| **Spoof Option & Suite** | `AutoPifHelper.java`<br>`SpoofOptionFragment.java`<br>`SpoofOptionController.java`<br>`spoof_option_fragment.xml`<br>`spoof_option_header_card.xml`<br>`Settings.java`<br>`KeyboxChainGenerator.java`<br>`SecureSettingsValidators.java` | Centralized Play Integrity, TrickyStore & TEESimulator control center with redesigned Material card, PIF presets catalog, dynamic chip indicators, and verified boot hash spoofing |

---
*Palapa OS - Tri-Monthly Edition | Shinkai Project (2026)*
