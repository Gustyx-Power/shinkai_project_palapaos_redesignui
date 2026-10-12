/*
 * Copyright (C) 2026 Wowo OS / Shinkai Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.homepage

import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnResume
import com.android.settingslib.widget.LayoutPreference

class HomepageQuickDashboardPreferenceController(
    context: Context,
    preferenceKey: String
) : BasePreferenceController(context, preferenceKey), LifecycleObserver, OnResume {

    private var layoutPreference: LayoutPreference? = null

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        layoutPreference = screen.findPreference(preferenceKey)
        updateDashboardUI()
    }

    override fun onResume() {
        updateDashboardUI()
    }

    private fun updateDashboardUI() {
        val pref = layoutPreference ?: return

        // 1. Top Device Card
        val deviceName = if (Build.DEVICE.equals("marble", ignoreCase = true)) {
            "POCO F5"
        } else if (!Build.MODEL.isNullOrBlank() && Build.MODEL != "unknown") {
            Build.MODEL
        } else if (!Build.DEVICE.isNullOrBlank()) {
            Build.DEVICE.replaceFirstChar { it.uppercase() }
        } else {
            "Marble"
        }
        pref.findViewById<TextView>(R.id.home_device_title)?.text = deviceName

        val androidVer = if (Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY.isNullOrBlank() || Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY == "unknown" || Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY == "15") {
            "Android 17"
        } else {
            "Android " + Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY
        }
        pref.findViewById<TextView>(R.id.home_device_subtitle)?.setText(R.string.home_device_subtitle)

        // Mini stat Box 1: Versi OS (Android 17)
        pref.findViewById<TextView>(R.id.home_val_warranty)?.text = "Android 17"

        // Mini stat Box 3: RAM & CPU (Dynamically calculate actual device RAM)
        val ramGb = try {
            val am = mContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            if (memInfo.totalMem > 0) {
                val rawGb = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
                when {
                    rawGb > 13.5 -> 16
                    rawGb > 9.5 -> 12
                    rawGb > 6.5 -> 8
                    rawGb > 4.5 -> 6
                    else -> kotlin.math.ceil(rawGb).toInt()
                }
            } else 8
        } catch (e: Exception) {
            8
        }
        pref.findViewById<TextView>(R.id.home_val_ram_cpu)?.text = "${ramGb}GB • 7+ Gen2"

        pref.findViewById<View>(R.id.home_card_device)?.setOnClickListener {
            launchIntent(
                "android.settings.DEVICE_INFO_SETTINGS",
                "com.android.settings.deviceinfo.aboutphone.MyDeviceInfoFragment",
                R.string.about_settings
            )
        }

        // 1b. Search Bar Click Handler
        pref.findViewById<View>(R.id.home_search_bar)?.setOnClickListener {
            try {
                val intent = com.android.settings.overlay.FeatureFactory.featureFactory
                    .searchFeatureProvider
                    .buildSearchIntent(mContext, SettingsEnums.SETTINGS_HOMEPAGE)
                mContext.startActivity(intent)
            } catch (e: Exception) {
                launchIntent("android.settings.APP_SEARCH_SETTINGS")
            }
        }

        // 2. Baterai Card
        try {
            val batteryIntent = mContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 78
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val batteryPct = if (scale > 0 && level >= 0) (level * 100) / scale else 78
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL

            pref.findViewById<TextView>(R.id.home_battery_percent)?.text = "$batteryPct%"
            pref.findViewById<ProgressBar>(R.id.home_battery_progress)?.progress = batteryPct
            pref.findViewById<TextView>(R.id.home_battery_badge)?.setText(if (isCharging) R.string.home_battery_badge_charging else R.string.home_battery_badge_normal)
            pref.findViewById<TextView>(R.id.home_battery_estimate)?.setText(if (isCharging) R.string.home_battery_estimate_charging else R.string.home_battery_estimate_normal)
        } catch (e: Exception) {
            // Ignore
        }

        pref.findViewById<View>(R.id.home_card_battery)?.setOnClickListener {
            launchIntent(
                Intent.ACTION_POWER_USAGE_SUMMARY,
                "com.android.settings.fuelgauge.batteryusage.PowerUsageSummary",
                R.string.power_usage_summary_title
            )
        }

        // 3. Penyimpanan Card (Direct to official Pixel Storage Activity)
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = totalBytes - freeBytes
            val usedPct = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt() else 12

            val usedStr = Formatter.formatShortFileSize(mContext, usedBytes)
            val totalStr = Formatter.formatShortFileSize(mContext, totalBytes)
            val freeStr = Formatter.formatShortFileSize(mContext, freeBytes)

            pref.findViewById<TextView>(R.id.home_storage_percent)?.text = "$usedPct%"
            pref.findViewById<ProgressBar>(R.id.home_storage_progress)?.progress = usedPct
            pref.findViewById<TextView>(R.id.home_storage_badge)?.text = "$usedStr / $totalStr"
            pref.findViewById<TextView>(R.id.home_storage_free)?.text = mContext.getString(R.string.home_storage_free_format, freeStr)
        } catch (e: Exception) {
            // Ignore
        }

        pref.findViewById<View>(R.id.home_card_storage)?.setOnClickListener {
            launchIntent(
                "android.settings.INTERNAL_STORAGE_SETTINGS",
                "com.android.settings.deviceinfo.StorageDashboardFragment",
                R.string.storage_settings
            )
        }

        // 4. Jaringan & Konektivitas
        try {
            val wm = mContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiEnabled = wm?.isWifiEnabled == true
            val connectionInfo = wm?.connectionInfo
            val ssid = connectionInfo?.ssid?.replace("\"", "") ?: ""
            val isConnected = wifiEnabled && ssid.isNotEmpty() && ssid != "<unknown ssid>"

            if (isConnected) {
                pref.findViewById<TextView>(R.id.home_wifi_name)?.text = ssid
                pref.findViewById<TextView>(R.id.home_wifi_status)?.setText(R.string.home_status_connected)
            } else if (wifiEnabled) {
                pref.findViewById<TextView>(R.id.home_wifi_name)?.text = "Wi-Fi"
                pref.findViewById<TextView>(R.id.home_wifi_status)?.setText(R.string.home_status_active)
            } else {
                pref.findViewById<TextView>(R.id.home_wifi_name)?.text = "Wi-Fi"
                pref.findViewById<TextView>(R.id.home_wifi_status)?.setText(R.string.home_status_inactive)
            }
        } catch (e: Exception) {
            // Ignore
        }

        val openNetworkDashboard = {
            try {
                val intent = Intent("android.settings.WIRELESS_SETTINGS").apply {
                    setClassName(mContext.packageName, "com.android.settings.Settings\$NetworkDashboardActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                mContext.startActivity(intent)
            } catch (e: Exception) {
                launchIntent(
                    "android.settings.WIRELESS_SETTINGS",
                    "com.android.settings.network.NetworkDashboardFragment",
                    R.string.network_dashboard_title
                )
            }
        }

        pref.findViewById<View>(R.id.home_card_network_container)?.setOnClickListener { openNetworkDashboard() }
        pref.findViewById<View>(R.id.home_network_header)?.setOnClickListener { openNetworkDashboard() }

        pref.findViewById<View>(R.id.home_btn_wifi)?.setOnClickListener {
            try {
                val intent = Intent("android.settings.WIFI_SETTINGS").apply {
                    setPackage(mContext.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                mContext.startActivity(intent)
            } catch (e: Exception) {
                launchIntent(
                    "android.settings.WIFI_SETTINGS",
                    "com.android.settings.network.NetworkDashboardFragment",
                    R.string.network_dashboard_title
                )
            }
        }

        try {
            val ba = BluetoothAdapter.getDefaultAdapter()
            val btEnabled = ba?.isEnabled == true
            if (btEnabled) {
                pref.findViewById<TextView>(R.id.home_bt_name)?.text = "Bluetooth"
                pref.findViewById<TextView>(R.id.home_bt_status)?.setText(R.string.home_status_active)
            } else {
                pref.findViewById<TextView>(R.id.home_bt_name)?.text = "Bluetooth"
                pref.findViewById<TextView>(R.id.home_bt_status)?.setText(R.string.home_status_inactive)
            }
        } catch (e: Exception) {
            // Ignore
        }

        pref.findViewById<View>(R.id.home_btn_bluetooth)?.setOnClickListener {
            launchIntent(
                "android.settings.BLUETOOTH_SETTINGS",
                "com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment",
                R.string.connected_devices_dashboard_title
            )
        }

        pref.findViewById<View>(R.id.home_btn_cellular)?.setOnClickListener {
            try {
                val intent = Intent("android.settings.NETWORK_OPERATOR_SETTINGS").apply {
                    setClassName(mContext.packageName, "com.android.settings.Settings\$MobileNetworkActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                mContext.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent("android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS").apply {
                        setClassName(mContext.packageName, "com.android.settings.Settings\$MobileNetworkListActivity")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    mContext.startActivity(intent)
                } catch (e2: Exception) {
                    launchIntent(
                        "android.settings.NETWORK_PROVIDER_SETTINGS",
                        "com.android.settings.network.NetworkDashboardFragment",
                        R.string.network_dashboard_title
                    )
                }
            }
        }

        pref.findViewById<View>(R.id.home_btn_hotspot)?.setOnClickListener {
            try {
                val intent = Intent("android.settings.TETHER_SETTINGS").apply {
                    setClassName(mContext.packageName, "com.android.settings.Settings\$TetherSettingsActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                mContext.startActivity(intent)
            } catch (e: Exception) {
                try {
                    SubSettingLauncher(mContext)
                        .setDestination("com.android.settings.network.tether.TetherSettings")
                        .setTitleRes(R.string.tether_settings_title_all)
                        .setSourceMetricsCategory(SettingsEnums.DASHBOARD_SUMMARY)
                        .setIsSecondLayerPage(true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .launch()
                } catch (e2: Exception) {
                    launchIntent("android.settings.TETHER_SETTINGS")
                }
            }
        }

        // 5. Layar & Suara
        val isDark = (mContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val darkText = if (isDark) mContext.getString(R.string.home_theme_dark) else mContext.getString(R.string.home_theme_light)
        val refreshRate = try {
            mContext.display?.refreshRate?.toInt() ?: 120
        } catch (e: Exception) {
            120
        }
        pref.findViewById<TextView>(R.id.home_display_subtitle)?.text = "$darkText • ${refreshRate}Hz"

        pref.findViewById<View>(R.id.home_card_display)?.setOnClickListener {
            launchIntent(
                "android.settings.DISPLAY_SETTINGS",
                "com.android.settings.DisplaySettings",
                R.string.display_settings
            )
        }

        try {
            val am = mContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val ringerText = when (am?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> mContext.getString(R.string.home_ringer_silent)
                AudioManager.RINGER_MODE_VIBRATE -> mContext.getString(R.string.home_ringer_vibrate)
                else -> mContext.getString(R.string.home_ringer_normal)
            }
            val maxVol = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
            val currentVol = am?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
            val volPct = if (maxVol > 0) (currentVol * 100) / maxVol else 0

            pref.findViewById<TextView>(R.id.home_sound_subtitle)?.text = mContext.getString(R.string.home_sound_status_format, volPct, ringerText)
        } catch (e: Exception) {
            // Ignore
        }

        pref.findViewById<View>(R.id.home_card_sound)?.setOnClickListener {
            launchIntent(
                "android.settings.SOUND_SETTINGS",
                "com.android.settings.notification.SoundSettings",
                R.string.sound_settings
            )
        }

        // 6. Aksesibilitas & Sistem
        pref.findViewById<View>(R.id.home_card_accessibility)?.setOnClickListener {
            launchIntent(
                "android.settings.ACCESSIBILITY_SETTINGS",
                "com.android.settings.accessibility.AccessibilitySettings",
                R.string.accessibility_settings
            )
        }

        pref.findViewById<View>(R.id.home_card_system)?.setOnClickListener {
            launchIntent(
                "android.settings.SYSTEM_SETTINGS",
                "com.android.settings.system.SystemDashboardFragment",
                R.string.header_category_system
            )
        }
    }

    private fun launchIntent(action: String, fallbackFragment: String? = null, titleRes: Int = 0) {
        try {
            val intent = Intent(action).apply {
                setPackage(mContext.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(mContext.packageManager) != null) {
                mContext.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            // Fallback below
        }

        if (fallbackFragment != null) {
            try {
                val launcher = SubSettingLauncher(mContext)
                    .setDestination(fallbackFragment)
                    .setSourceMetricsCategory(SettingsEnums.DASHBOARD_SUMMARY)
                    .setIsSecondLayerPage(true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (titleRes != 0) {
                    launcher.setTitleRes(titleRes)
                }
                launcher.launch()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
