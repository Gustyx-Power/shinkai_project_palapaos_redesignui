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
* **Play Integrity GMS Integration**:
  * Built-in official certified device fingerprint from Google Pixel Fold (Android 17, `felix_beta`) in `config_verifiedDeviceSpecs`.
  * Guarantees passing Play Integrity API (`MEETS_BASIC_INTEGRITY` and `MEETS_DEVICE_INTEGRITY`).
* **Kernel Command Line Filter (`/proc/cmdline`)**:
  * Seamless kernel filter (`cmdline.c`) rewriting `androidboot.verifiedbootstate=orange` to `green`, `androidboot.flash.locked=0` to `1`, and `androidboot.vbmeta.device_state=unlocked` to `locked` for direct OS-level readers.

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
| **Key Attestation & Bootloader Lock** | `custom_config.xml`<br>`KeyProviderManager.java`<br>`KeyboxDataPreference.java`<br>`cmdline.c` | Google Root Keybox ECDSA integration, RKP support, Pixel Fold PIF specs, and `/proc/cmdline` filter |

---
*Palapa OS - Triwulan Edition | Shinkai Project (2026)*
