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
The Battery settings screen (`Settings -> Battery`) has been completely overhauled to adopt Samsung One UI 9 design principles while preserving 100% of the existing battery management features and system controls:

* **Hero Overview Header**:
  * Large and prominent bold percentage indicator (`81%`) on the left.
  * Real-time estimated discharge time (`8 h 35 m left`) on the right, baseline-aligned.
  * Full rounded capsule pill progress bar (radius 100dp) in vibrant One UI Green (`#2ED158`) with an elegant dark charcoal track.
  * Dynamic full-charge duration subtitle (*"A full charge will last about..."*) paired with an interactive info button `(i)`.

* **Unified Interactive Battery Usage Card**:
  * Contained within a sleek One UI dark card (corner radius 26dp) fully adaptive to AMOLED dark mode and Material You Monet dynamics.
  * **Interactive Dropdown Selector**:
    * **Daily Usage Mode**: Displays a 7-day vertical pill bar chart in One UI Blue (`#2979FF`), a dashed average baseline (`--- 195 Avg.`), Y-axis scale (`300` and `0 (%)`), and a circular badge highlighting the current day (`M`).
    * **Since Last Charge Mode**: Displays charging history subtitles (*"Last charged to 100%"*, *"Stopped charging..."*), a smooth area curve with a translucent blue gradient, start badge `⚡100`, current level pin bubble `81%`, power-saving projection trajectory, and battery level legends.
  * **Application Consumption Breakdown**:
    * Subheader label *"Battery usage: X%"*.
    * Mini breakdown showing top 3 battery-consuming apps with squircle app icons, application labels, accurate percentage metrics, and subtle dividers.
    * Centered footer action button *"View details"* to launch advanced battery history (`PowerUsageAdvanced`).

* **Dedicated "Battery information" Page**:
  * Direct access via the header info icon `(i)` or the Battery Information list entry.
  * Unified One UI card displaying:
    1. **Battery status**: Charging status (*Not charging*, *Charging*, *Full*).
    2. **Battery level**: Current percentage.
    3. **Battery capacity**: Typical capacity (`4500 mAh (typical)`) dynamically resolved from hardware and device PowerProfile.
  * Official IEC 61960 laboratory testing standard disclaimer paragraph below the card.

* **100% Feature Preservation**:
  * All previous system preferences remain intact within neatly organized card groups: *Battery Saver*, *Smart Battery / Adaptive Battery*, *Lineage Health Charging Control*, *Charging Speed*, *Status Bar Battery Percentage*, and *Lockscreen Charging Info*.

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

## Technical Details & Modified Files

| Component | Related Files | Description |
|---|---|---|
| **Battery UI Header** | `OneUIBatteryHeaderPreference.kt`<br>`oneui_battery_header_card.xml` | Large percentage, time left estimate, and One UI pill progress bar |
| **Interactive Chart** | `OneUIBatteryChartView.kt` | Custom canvas view rendering 7-day vertical pill bars and gradient area curves |
| **Usage Card** | `OneUIBatteryUsageCardPreference.kt`<br>`OneUIBatteryUsageCardController.kt`<br>`oneui_battery_usage_card.xml` | Main card uniting dropdown filter, chart view, and top 3 apps list |
| **Battery Info** | `OneUIBatteryInfoFragment.kt`<br>`oneui_battery_info_fragment.xml`<br>`SettingsGateway.java` | Dedicated sub-page for IEC 61960 battery capacity and hardware status |
| **Unified Settings** | `power_usage_summary.xml` | Restructured preference layout organizing all power features into card groups |
| **Theme & Strings** | `colors.xml`, `values-night/colors.xml`<br>`shinkai_strings.xml`, `values-in/` | AMOLED / Monet dynamic color palettes and comprehensive EN/ID translations |

---
*Palapa OS - Triwulan Edition | Shinkai Project (2026)*
