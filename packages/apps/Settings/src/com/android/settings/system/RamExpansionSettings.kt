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

package com.android.settings.system

import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.os.PowerManager
import android.os.SystemProperties
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.widget.LayoutPreference
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

class RamExpansionSettings : DashboardFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.SETTINGS_SYSTEM_CATEGORY

    override fun getPreferenceScreenResId(): Int = R.xml.ram_expansion_settings

    override fun getLogTag(): String = "RamExpansionSettings"

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    private fun getPhysicalRamGb(): Int {
        val context = context ?: return 8
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            if (memInfo.totalMem > 0) {
                val rawGb = memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)
                when {
                    rawGb > 13.5 -> 16
                    rawGb > 9.5 -> 12
                    rawGb > 6.5 -> 8
                    rawGb > 4.5 -> 6
                    else -> ceil(rawGb).toInt()
                }
            } else 8
        } catch (e: Exception) {
            8
        }
    }

    private fun setupUI() {
        val pref = findPreference<LayoutPreference>("ram_expansion_card") ?: return
        val currentExpansion = SystemProperties.getInt("persist.vendor.ram_expansion", 4)
        val physicalRam = getPhysicalRamGb()

        updateStats(pref, physicalRam, currentExpansion)
        updateRadioSelection(pref, currentExpansion)

        val options = listOf(
            Pair(R.id.ram_row_opt_0, 0),
            Pair(R.id.ram_row_opt_2, 2),
            Pair(R.id.ram_row_opt_4, 4),
            Pair(R.id.ram_row_opt_8, 8),
            Pair(R.id.ram_row_opt_16, 16)
        )

        for ((rowId, value) in options) {
            pref.findViewById<View>(rowId)?.setOnClickListener {
                if (SystemProperties.getInt("persist.vendor.ram_expansion", 4) != value) {
                    // Write to vendor property - vendor_init can read this and
                    // the property trigger will auto-restart the ZRAM service
                    SystemProperties.set("persist.vendor.ram_expansion", value.toString())
                    updateStats(pref, physicalRam, value)
                    updateRadioSelection(pref, value)
                    showRebootDialog()
                }
            }
        }
    }

    private fun updateStats(pref: LayoutPreference, physicalRam: Int, expansionVal: Int) {
        val context = context ?: return
        val totalRam = if (expansionVal > 0) physicalRam + expansionVal else physicalRam

        // Calculate active RAM usage from MemoryInfo
        val memInfo = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.getMemoryInfo(memInfo)

        val totalMemBytes = if (memInfo.totalMem > 0) memInfo.totalMem else (physicalRam.toLong() * 1024L * 1024L * 1024L)
        val availMemBytes = if (memInfo.availMem > 0) memInfo.availMem else (totalMemBytes / 2)
        val usedMemBytes = (totalMemBytes - availMemBytes).coerceAtLeast(0L)

        val usedGb = usedMemBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val availGb = availMemBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val usedPct = ((usedMemBytes.toDouble() / totalMemBytes.toDouble()) * 100.0).roundToInt().coerceIn(1, 99)

        // 1. Badge % Digunakan
        pref.findViewById<TextView>(R.id.ram_badge_used_pct)?.text =
            context.getString(R.string.ram_badge_used_format, usedPct)

        // 2. Subtitle Status Hero
        val subText = if (expansionVal > 0) {
            if (expansionVal == 4) "+4 GB (${context.getString(R.string.ram_tag_default)})"
            else "+$expansionVal GB"
        } else {
            context.getString(R.string.ram_expansion_status_off)
        }
        pref.findViewById<TextView>(R.id.ram_hero_status_subtitle)?.text = subText

        // 3. Big Metric Used Number & Unit
        pref.findViewById<TextView>(R.id.ram_big_used_num)?.text =
            String.format(Locale.US, "%.1f", usedGb)
        pref.findViewById<TextView>(R.id.ram_big_used_unit)?.text = "GB"

        // Subtitle bawah big metric: "dari X GB Total RAM"
        pref.findViewById<TextView>(R.id.ram_stat_total_sub)?.text =
            context.getString(R.string.ram_stat_of_total_format, totalRam)

        // 4. Multi-Segment Progress Bar
        val barUsed = pref.findViewById<View>(R.id.ram_bar_used)
        val barExpanded = pref.findViewById<View>(R.id.ram_bar_expanded)
        val barFree = pref.findViewById<View>(R.id.ram_bar_free)
        val barDiv1 = pref.findViewById<View>(R.id.ram_bar_div1)
        val barDiv2 = pref.findViewById<View>(R.id.ram_bar_div2)

        if (barUsed != null && barExpanded != null && barFree != null) {
            val totalScale = totalRam.toDouble()
            val usedWeight = (usedGb / totalScale).toFloat().coerceAtLeast(0.05f)
            val expandedWeight = (expansionVal.toDouble() / totalScale).toFloat()
            val freeWeight = (1.0f - usedWeight - expandedWeight).coerceAtLeast(0.05f)

            val lpUsed = barUsed.layoutParams as? LinearLayout.LayoutParams
            if (lpUsed != null) {
                lpUsed.weight = usedWeight
                barUsed.layoutParams = lpUsed
            }

            val lpExpanded = barExpanded.layoutParams as? LinearLayout.LayoutParams
            if (lpExpanded != null) {
                if (expansionVal > 0) {
                    barExpanded.visibility = View.VISIBLE
                    barDiv1?.visibility = View.VISIBLE
                    barDiv2?.visibility = View.VISIBLE
                    lpExpanded.weight = expandedWeight
                    barExpanded.layoutParams = lpExpanded
                } else {
                    barExpanded.visibility = View.GONE
                    barDiv1?.visibility = View.GONE
                    barDiv2?.visibility = View.GONE
                }
            }

            val lpFree = barFree.layoutParams as? LinearLayout.LayoutParams
            if (lpFree != null) {
                lpFree.weight = freeWeight
                barFree.layoutParams = lpFree
            }
        }

        // 5. 3 Stat Tiles: Fisik, Diperluas, Tersedia
        pref.findViewById<TextView>(R.id.ram_stat_physical)?.text = "$physicalRam GB"

        pref.findViewById<TextView>(R.id.ram_stat_expanded)?.text =
            if (expansionVal > 0) "+$expansionVal GB" else context.getString(R.string.ram_expansion_status_off)

        pref.findViewById<TextView>(R.id.ram_stat_available)?.text =
            String.format(Locale.US, "%.1f GB", availGb)
    }

    private fun updateRadioSelection(pref: LayoutPreference, selectedVal: Int) {
        pref.findViewById<RadioButton>(R.id.ram_radio_opt_0)?.isChecked = (selectedVal == 0)
        pref.findViewById<RadioButton>(R.id.ram_radio_opt_2)?.isChecked = (selectedVal == 2)
        pref.findViewById<RadioButton>(R.id.ram_radio_opt_4)?.isChecked = (selectedVal == 4)
        pref.findViewById<RadioButton>(R.id.ram_radio_opt_8)?.isChecked = (selectedVal == 8)
        pref.findViewById<RadioButton>(R.id.ram_radio_opt_16)?.isChecked = (selectedVal == 16)
    }

    private fun showRebootDialog() {
        val activity = activity ?: return
        AlertDialog.Builder(activity)
            .setTitle(R.string.ram_expansion_reboot_dialog_title)
            .setMessage(R.string.ram_expansion_reboot_dialog_msg)
            .setPositiveButton(R.string.ram_expansion_reboot_now) { _, _ ->
                try {
                    val pm = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    pm?.reboot(null)
                } catch (e: Exception) {
                    // Ignore
                }
            }
            .setNegativeButton(R.string.ram_expansion_reboot_later, null)
            .show()
    }
}
