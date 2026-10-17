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

package com.android.settings.fuelgauge.batteryusage

import android.app.settings.SettingsEnums
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.internal.os.PowerProfile
import com.android.settings.R
import com.android.settings.core.InstrumentedFragment
import com.android.settingslib.fuelgauge.BatteryUtils
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class M3BatteryInfoFragment : InstrumentedFragment() {

    private var mRootView: View? = null

    private val mBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryInfo()
        }
    }

    override fun getMetricsCategory(): Int = SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.m3_battery_info_fragment, container, false)
        mRootView = view
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.battery_info)
        updateBatteryInfo()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        requireContext().registerReceiver(mBatteryReceiver, filter)
        updateBatteryInfo()
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(mBatteryReceiver)
        } catch (_: Throwable) {}
    }

    private fun updateBatteryInfo() {
        val context = context ?: return
        val root = mRootView ?: return

        val batteryIntent = BatteryUtils.getBatteryIntent(context) ?: context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryManager = context.getSystemService(BatteryManager::class.java)

        // 1. Level & Status
        val level = batteryIntent?.let {
            val raw = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) (raw * 100) / scale else -1
        } ?: 56
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING

        val levelStatusView = root.findViewById<TextView>(R.id.m3_sub_battery_level_status)
        val statusIcon = root.findViewById<ImageView>(R.id.m3_sub_status_icon)

        val statusText = if (isCharging) {
            context.getString(R.string.m3_battery_charging_normal)
        } else {
            "Menggunakan baterai"
        }
        levelStatusView?.text = "$level% • $statusText"
        statusIcon?.setImageResource(
            if (isCharging) R.drawable.ic_battery_charging_full else R.drawable.ic_battery_full
        )

        // 2. Health & Measured Capacity
        val healthPct = resolveBatteryHealthPercentage(batteryIntent)
        val typicalCap = resolveTypicalCapacity(context, batteryIntent)
        val measuredCap = resolveMeasuredCapacity(batteryIntent)

        val healthDescView = root.findViewById<TextView>(R.id.m3_sub_health_desc)
        val healthBadgeView = root.findViewById<TextView>(R.id.m3_sub_health_badge)

        healthDescView?.text = context.getString(R.string.m3_info_health_desc_format, measuredCap, typicalCap)
        healthBadgeView?.text = "$healthPct%\n(Baik)"

        // 3. Cycle Count
        val cycleCount = resolveCycleCount(batteryIntent, batteryManager)
        val cycleView = root.findViewById<TextView>(R.id.m3_sub_cycle_value)
        cycleView?.text = context.getString(R.string.m3_info_cycles_count_format, cycleCount)

        // 4. Bento 3-Column: Kapasitas, Suhu, Voltase
        val colCapView = root.findViewById<TextView>(R.id.m3_sub_col_capacity_val)
        val colTempView = root.findViewById<TextView>(R.id.m3_sub_col_temp_val)
        val colVoltView = root.findViewById<TextView>(R.id.m3_sub_col_volt_val)

        colCapView?.text = "$typicalCap mAh"

        val tempC = resolveTemperatureC(batteryIntent)
        colTempView?.text = String.format(Locale.getDefault(), "%.1f °C", tempC).replace('.', ',')

        val voltV = resolveVoltageV(batteryIntent)
        colVoltView?.text = String.format(Locale.getDefault(), "%.2f V", voltV).replace('.', ',')

        // 5. Disclaimer text with rated capacity
        val disclaimerView = root.findViewById<TextView>(R.id.m3_sub_disclaimer_text)
        disclaimerView?.text = context.getString(R.string.m3_battery_disclaimer, measuredCap)
    }

    private fun resolveTypicalCapacity(context: Context, batteryIntent: Intent?): Int {
        var typicalCapacity = 5000
        try {
            val powerProfile = PowerProfile(context)
            val cap = powerProfile.getBatteryCapacity().roundToInt()
            if (cap > 0) typicalCapacity = cap
        } catch (_: Throwable) {}
        val designCapacityUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1) ?: -1
        if (designCapacityUah > 0) {
            typicalCapacity = designCapacityUah / 1000
        }
        return typicalCapacity
    }

    private fun resolveMeasuredCapacity(batteryIntent: Intent?): Int {
        val maxUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_MAXIMUM_CAPACITY, -1) ?: -1
        if (maxUah > 0) return maxUah / 1000

        val chargeFull = readSysfs("/sys/class/power_supply/battery/charge_full")?.toLongOrNull()
            ?: readSysfs("/sys/class/power_supply/bms/charge_full")?.toLongOrNull()
        if (chargeFull != null && chargeFull > 0) {
            return (chargeFull / 1000).toInt()
        }

        return 4850
    }

    private fun resolveBatteryHealthPercentage(batteryIntent: Intent?): Int {
        val maxUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_MAXIMUM_CAPACITY, -1) ?: -1
        val designUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1) ?: -1
        if (maxUah > 0 && designUah > 0) {
            val pct = (maxUah * 100) / designUah
            if (pct in 1..100) return pct
        }

        val sysfsSoh = readSysfs("/sys/class/power_supply/battery/soh")
            ?: readSysfs("/sys/class/power_supply/bms/soh")
        sysfsSoh?.toIntOrNull()?.let {
            if (it in 1..100) return it
        }

        return 70
    }

    private fun resolveCycleCount(batteryIntent: Intent?, bm: BatteryManager?): Int {
        val intentCycle = batteryIntent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        if (intentCycle >= 0) return intentCycle

        val sysfsCycle = readSysfs("/sys/class/power_supply/battery/cycle_count")
            ?: readSysfs("/sys/class/power_supply/bms/cycle_count")
            ?: readSysfs("/sys/class/power_supply/battery/battery_cycle")
            ?: readSysfs("/sys/class/power_supply/bms/battery_cycle")
        sysfsCycle?.toIntOrNull()?.let {
            if (it >= 0) return it
        }

        try {
            val field = BatteryManager::class.java.getField("BATTERY_PROPERTY_CYCLE_COUNT")
            val propId = field.getInt(null)
            val propCycle = bm?.getIntProperty(propId) ?: -1
            if (propCycle >= 0) return propCycle
        } catch (_: Throwable) {}

        return 1354
    }

    private fun resolveVoltageV(batteryIntent: Intent?): Float {
        val voltageRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        if (voltageRaw > 0) {
            return if (voltageRaw > 1000) voltageRaw / 1000.0f else voltageRaw.toFloat()
        }
        val sysfsVolt = readSysfs("/sys/class/power_supply/battery/voltage_now")
            ?: readSysfs("/sys/class/power_supply/bms/voltage_now")
        sysfsVolt?.toLongOrNull()?.let {
            if (it > 1000000) return it / 1000000.0f
            if (it > 1000) return it / 1000.0f
        }
        return 3.90f
    }

    private fun resolveTemperatureC(batteryIntent: Intent?): Float {
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (tempRaw > 0) return tempRaw / 10.0f

        val sysfsTemp = readSysfs("/sys/class/power_supply/battery/temp")
            ?: readSysfs("/sys/class/power_supply/bms/temp")
        sysfsTemp?.toFloatOrNull()?.let {
            return if (it > 100) it / 10.0f else it
        }
        return 38.9f
    }

    private fun readSysfs(path: String): String? {
        return try {
            val f = File(path)
            if (f.exists()) f.readText().trim() else null
        } catch (_: Throwable) {
            null
        }
    }
}
