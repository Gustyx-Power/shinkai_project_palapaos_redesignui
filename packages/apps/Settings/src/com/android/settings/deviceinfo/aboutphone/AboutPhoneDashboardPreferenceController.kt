/*
 * Copyright (C) 2026 Palapa OS / Shinkai Project
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

package com.android.settings.deviceinfo.aboutphone

import android.app.Activity
import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.SystemProperties
import android.provider.Settings
import android.text.InputFilter
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceScreen
import com.android.internal.app.PlatLogoActivity
import com.android.internal.os.PowerProfile
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.deviceinfo.DeviceNamePreferenceController
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnResume
import com.android.settingslib.widget.LayoutPreference
import java.io.File
import kotlin.math.roundToInt

class AboutPhoneDashboardPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), LifecycleObserver, OnResume {

    private var mLayoutPreference: LayoutPreference? = null

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mLayoutPreference = screen.findPreference(preferenceKey)
        mLayoutPreference?.let { populateViews(it) }
    }

    override fun onResume() {
        mLayoutPreference?.let { populateViews(it) }
    }

    companion object {
        const val KEY = "about_phone_dashboard"
        private val hits = LongArray(3)

        @JvmStatic
        fun populateViews(layoutPreference: LayoutPreference) {
            updateDashboardUI(layoutPreference.context, layoutPreference)
        }

        fun updateDashboardUI(context: Context, layoutPreference: LayoutPreference) {
            // 1. Marketing Model Name (e.g. POCO F5 / iQOO style)
            val marketingName = if (Build.DEVICE.equals("marble", ignoreCase = true)) {
                "POCO F5"
            } else if (!Build.MODEL.isNullOrBlank() && Build.MODEL != "unknown") {
                Build.MODEL
            } else {
                "POCO F5"
            }
            layoutPreference.findViewById<TextView>(R.id.about_iqoo_marketing_name)?.text = marketingName

            // 2. Device Name (Nama perangkat)
            var deviceName = Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            if (deviceName.isNullOrBlank()) {
                deviceName = if (Build.DEVICE.equals("marble", ignoreCase = true)) {
                    "Gustyx POCO F5"
                } else {
                    marketingName
                }
            }
            layoutPreference.findViewById<TextView>(R.id.about_spec_device_name_value)?.text = deviceName

            // 3. Processor (Prosesor)
            val processor = if (Build.DEVICE.equals("marble", ignoreCase = true) ||
                SystemProperties.get("ro.board.platform", "").contains("kalama", ignoreCase = true) ||
                SystemProperties.get("ro.board.platform", "").contains("sm7475", ignoreCase = true)) {
                "2.91 GHz Qualcomm® Snapdragon® 7+ Gen 2 Mobile Platform Octa-core"
            } else {
                val soc = SystemProperties.get("ro.soc.model", "")
                if (soc.isNotBlank()) {
                    "Qualcomm® $soc Octa-core"
                } else if (!Build.HARDWARE.isNullOrBlank() && Build.HARDWARE != "unknown") {
                    Build.HARDWARE
                } else {
                    "Qualcomm® Snapdragon® Octa-core"
                }
            }
            layoutPreference.findViewById<TextView>(R.id.about_spec_processor_value)?.text = processor

            // 4. Storage (Penyimpanan telepon)
            try {
                val dataPath = Environment.getDataDirectory()
                val stat = StatFs(dataPath.path)
                val totalBytes = stat.totalBytes
                val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val roundedStorage = when {
                    totalGb > 380 -> "512 GB"
                    totalGb > 190 -> "256 GB"
                    totalGb > 90 -> "128 GB"
                    totalGb > 45 -> "64 GB"
                    else -> "${totalGb.roundToInt()} GB"
                }
                layoutPreference.findViewById<TextView>(R.id.about_spec_storage_value)?.text = roundedStorage
            } catch (e: Exception) {
                layoutPreference.findViewById<TextView>(R.id.about_spec_storage_value)?.text = "256 GB"
            }

            // 5. RAM (Physical RAM e.g. 8/12/16 GB + zRAM)
            try {
                val actMan = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actMan?.getMemoryInfo(memInfo)
                val rawGb = if (memInfo.totalMem > 0) memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0) else 7.5
                val physicalRamGb = when {
                    rawGb > 13.5 -> 16
                    rawGb > 9.5 -> 12
                    rawGb > 6.5 -> 8
                    rawGb > 4.5 -> 6
                    else -> kotlin.math.ceil(rawGb).toInt()
                }

                // Detect zRAM (Swap)
                val zramGb = try {
                    val zramFile = File("/sys/block/zram0/disksize")
                    if (zramFile.exists()) {
                        val bytes = zramFile.readText().trim().toLongOrNull() ?: 0L
                        (bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)).roundToInt()
                    } else {
                        var swapKb = 0L
                        File("/proc/meminfo").forEachLine { line ->
                            if (line.startsWith("SwapTotal:")) {
                                swapKb = line.substringAfter(":").trim().substringBefore(" ").trim().toLongOrNull() ?: 0L
                            }
                        }
                        (swapKb.toDouble() / (1024.0 * 1024.0)).roundToInt()
                    }
                } catch (e: Exception) {
                    7
                }

                val ramText = if (zramGb > 0) "$physicalRamGb GB + $zramGb GB" else "$physicalRamGb GB"
                layoutPreference.findViewById<TextView>(R.id.about_spec_ram_value)?.text = ramText
            } catch (e: Exception) {
                layoutPreference.findViewById<TextView>(R.id.about_spec_ram_value)?.text = "8 GB + 7 GB"
            }

            // 6. Battery Capacity (Kapasitas baterai)
            try {
                val profile = PowerProfile(context)
                val capacity = profile.batteryCapacity.roundToInt()
                val capacityText = if (capacity > 0) "$capacity mAh" else "5000 mAh"
                layoutPreference.findViewById<TextView>(R.id.about_spec_battery_value)?.text = capacityText
            } catch (e: Exception) {
                layoutPreference.findViewById<TextView>(R.id.about_spec_battery_value)?.text = "5000 mAh"
            }

            // 7. Footer: Android Version (Android 17)
            val androidVer = if (!Build.VERSION.RELEASE.isNullOrBlank()) {
                "Android ${Build.VERSION.RELEASE}"
            } else {
                "Android 17"
            }
            layoutPreference.findViewById<TextView>(R.id.about_footer_android_text)?.text = androidVer

            // Banner Title
            val palapaEdition = SystemProperties.get("ro.palapa.edition", SystemProperties.get("ro.wowo.edition", "Majapahit Edition"))
            val palapaTitle = SystemProperties.get("ro.palapa.os.name", SystemProperties.get("ro.wowo.os.name", "Palapa OS"))
            layoutPreference.findViewById<TextView>(R.id.about_iqoo_banner_os_title)?.text = palapaTitle
            layoutPreference.findViewById<TextView>(R.id.about_iqoo_banner_os_subtitle)?.text =
                "$palapaEdition • Shinkai Project"

            // CLICK LISTENERS

            // Banner & Footer -> Single click opens FirmwareVersionSettings, Triple click opens Easter Egg
            val openFirmwareOrEgg = View.OnClickListener { v ->
                val ctx = v.context
                if (Utils.isMonkeyRunning()) return@OnClickListener
                for (i in 1 until hits.size) hits[i - 1] = hits[i]
                hits[hits.size - 1] = SystemClock.uptimeMillis()
                if (hits[hits.size - 1] - hits[0] <= 500L) {
                    try {
                        val intent = Intent(Intent.ACTION_MAIN)
                            .setClassName("android", PlatLogoActivity::class.java.name)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ctx.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                } else {
                    try {
                        SubSettingLauncher(ctx)
                            .setDestination("com.android.settings.deviceinfo.firmwareversion.FirmwareVersionSettings")
                            .setTitleRes(R.string.firmware_version)
                            .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                            .launch()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }

            layoutPreference.findViewById<View>(R.id.about_iqoo_banner_card)?.setOnClickListener(openFirmwareOrEgg)
            layoutPreference.findViewById<View>(R.id.about_footer_android)?.setOnClickListener(openFirmwareOrEgg)

            // Spec 1: Nama perangkat click -> edit dialog, long click -> copy
            val openEditDialog = View.OnClickListener { v ->
                showEditDeviceNameDialog(v.context, layoutPreference)
            }
            layoutPreference.findViewById<View>(R.id.about_spec_device_name_container)?.setOnClickListener(openEditDialog)
            layoutPreference.findViewById<View>(R.id.about_spec_device_name_value)?.setOnClickListener(openEditDialog)
            layoutPreference.findViewById<View>(R.id.about_spec_device_name_edit_btn)?.setOnClickListener(openEditDialog)
            layoutPreference.findViewById<View>(R.id.about_spec_device_name_container)?.setOnLongClickListener { v ->
                val current = layoutPreference.findViewById<TextView>(R.id.about_spec_device_name_value)?.text?.toString() ?: deviceName
                copyToClipboard(v.context, context.getString(R.string.iqoo_spec_device_name_label), current)
                true
            }

            // Spec 2: Prosesor click -> copy
            layoutPreference.findViewById<View>(R.id.about_spec_processor_container)?.setOnClickListener { v ->
                copyToClipboard(v.context, context.getString(R.string.iqoo_spec_processor_label), processor)
            }

            // Spec 3: Storage click -> Open Storage Dashboard
            layoutPreference.findViewById<View>(R.id.about_spec_storage_container)?.setOnClickListener { v ->
                try {
                    SubSettingLauncher(v.context)
                        .setDestination("com.android.settings.deviceinfo.StorageDashboardFragment")
                        .setTitleRes(R.string.storage_settings)
                        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                        .launch()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // Spec 4: RAM click -> copy
            layoutPreference.findViewById<View>(R.id.about_spec_ram_container)?.setOnClickListener { v ->
                val ramStr = layoutPreference.findViewById<TextView>(R.id.about_spec_ram_value)?.text?.toString() ?: "8 GB"
                copyToClipboard(v.context, context.getString(R.string.iqoo_spec_ram_label), ramStr)
            }

            // Spec 5: Battery click -> Open Battery Info
            layoutPreference.findViewById<View>(R.id.about_spec_battery_container)?.setOnClickListener { v ->
                try {
                    SubSettingLauncher(v.context)
                        .setDestination("com.android.settings.deviceinfo.batteryinfo.BatteryInfoFragment")
                        .setTitleRes(R.string.battery_info)
                        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                        .launch()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 4 Grouped Menus:

            // 1. Info versi -> FirmwareVersionSettings
            layoutPreference.findViewById<View>(R.id.about_menu_version)?.setOnClickListener { v ->
                try {
                    SubSettingLauncher(v.context)
                        .setDestination("com.android.settings.deviceinfo.firmwareversion.FirmwareVersionSettings")
                        .setTitleRes(R.string.firmware_version)
                        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                        .launch()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 2. Detail perangkat -> DeviceStatusDashboardFragment (Sub-screen baru mengelompokkan SIM, IMEI, Radio, IP, MAC, Up time)
            layoutPreference.findViewById<View>(R.id.about_menu_status)?.setOnClickListener { v ->
                try {
                    SubSettingLauncher(v.context)
                        .setDestination("com.android.settings.deviceinfo.aboutphone.DeviceStatusDashboardFragment")
                        .setTitleRes(R.string.iqoo_menu_status_title)
                        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                        .launch()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 3. Informasi hukum -> LegalSettings
            layoutPreference.findViewById<View>(R.id.about_menu_legal)?.setOnClickListener { v ->
                try {
                    SubSettingLauncher(v.context)
                        .setDestination("com.android.settings.LegalSettings")
                        .setTitleRes(R.string.iqoo_menu_legal_title)
                        .setSourceMetricsCategory(SettingsEnums.DEVICEINFO)
                        .launch()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 4. Layanan pelanggan -> Regulatory Info / Manual
            layoutPreference.findViewById<View>(R.id.about_menu_customer)?.setOnClickListener { v ->
                val ctx = v.context
                try {
                    val regIntent = Intent("android.settings.SHOW_REGULATORY_INFO").setPackage(ctx.packageName)
                    if (regIntent.resolveActivity(ctx.packageManager) != null) {
                        ctx.startActivity(regIntent)
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    // fallback
                }
                try {
                    val manualIntent = Intent("android.settings.SHOW_MANUAL").setPackage(ctx.packageName)
                    if (manualIntent.resolveActivity(ctx.packageManager) != null) {
                        ctx.startActivity(manualIntent)
                    } else {
                        Toast.makeText(ctx, ctx.getString(R.string.iqoo_menu_customer_title), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        private fun getActivity(context: Context): Activity? {
            var ctx: Context? = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx
                ctx = ctx.baseContext
            }
            return null
        }

        private fun showEditDeviceNameDialog(context: Context, layoutPreference: LayoutPreference) {
            val act = getActivity(context) ?: getActivity(layoutPreference.context)
            val dialogContext = act ?: context
            var currentName = Settings.Global.getString(
                dialogContext.contentResolver,
                Settings.Global.DEVICE_NAME
            )
            if (currentName.isNullOrBlank()) {
                currentName = if (Build.DEVICE.equals("marble", ignoreCase = true)) {
                    "Gustyx POCO F5"
                } else if (!Build.MODEL.isNullOrBlank() && Build.MODEL != "unknown") {
                    Build.MODEL
                } else {
                    "POCO F5"
                }
            }

            val density = dialogContext.resources.displayMetrics.density
            val input = EditText(dialogContext).apply {
                setText(currentName)
                setSelection(currentName.length)
                isSingleLine = true
                filters = arrayOf(InputFilter.LengthFilter(32))
                val padH = (16 * density).toInt()
                val padV = (12 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = dialogContext.getDrawable(R.drawable.palapa_stat_box_bg)
                setTextColor(Utils.getColorAttr(dialogContext, android.R.attr.textColorPrimary))
                setHintTextColor(Utils.getColorAttr(dialogContext, android.R.attr.textColorSecondary))
            }

            val container = FrameLayout(dialogContext).apply {
                val padH = (24 * density).toInt()
                val padV = (16 * density).toInt()
                setPadding(padH, padV, padH, padV)
                addView(input)
            }

            AlertDialog.Builder(dialogContext)
                .setTitle(dialogContext.getString(R.string.my_device_info_device_name_preference_title))
                .setView(container)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        // 1. Save to Global Settings
                        Settings.Global.putString(
                            dialogContext.contentResolver,
                            Settings.Global.DEVICE_NAME,
                            newName
                        )

                        // 2. Set Bluetooth adapter name if available
                        try {
                            val btAdapter = BluetoothAdapter.getDefaultAdapter()
                            if (btAdapter != null) {
                                btAdapter.name = DeviceNamePreferenceController.getFilteredBluetoothString(newName)
                            }
                        } catch (e: Exception) {
                            // ignore
                        }

                        // 3. Update Wi-Fi SoftAp SSID if available
                        try {
                            val wifiMan = dialogContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                            val config = wifiMan?.softApConfiguration
                            if (wifiMan != null && config != null) {
                                wifiMan.softApConfiguration =
                                    SoftApConfiguration.Builder(config).setSsid(newName).build()
                            }
                        } catch (e: Exception) {
                            // ignore
                        }

                        // 4. Update the TextView in the dashboard
                        layoutPreference.findViewById<TextView>(R.id.about_spec_device_name_value)?.text = newName

                        Toast.makeText(dialogContext, newName, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        private fun copyToClipboard(context: Context, label: String, text: String) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard?.setPrimaryClip(clip)
            val msg = context.getString(R.string.about_copied_to_clipboard, label)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
