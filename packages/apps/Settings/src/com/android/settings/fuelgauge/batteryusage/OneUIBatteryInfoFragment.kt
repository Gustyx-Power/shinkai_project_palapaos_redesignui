/*
 * Copyright (C) 2026 Shinkai Project
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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.internal.os.PowerProfile
import com.android.settings.R
import com.android.settingslib.fuelgauge.BatteryUtils
import java.io.File
import kotlin.math.roundToInt

import android.app.settings.SettingsEnums
import com.android.settings.core.InstrumentedFragment

class OneUIBatteryInfoFragment : InstrumentedFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.FUELGAUGE_POWER_USAGE_SUMMARY_V2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.oneui_battery_info_fragment, container, false)
        bindViews(root)
        return root
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.battery_info)
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.battery_info)
    }

    private fun bindViews(view: View) {
        val context = context ?: return
        val batteryIntent = BatteryUtils.getBatteryIntent(context) ?: context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryManager = context.getSystemService(BatteryManager::class.java)

        val statusView = view.findViewById<TextView>(R.id.oneui_info_battery_status)
        val levelView = view.findViewById<TextView>(R.id.oneui_info_battery_level)
        val healthView = view.findViewById<TextView>(R.id.oneui_info_battery_health)
        val cycleView = view.findViewById<TextView>(R.id.oneui_info_battery_cycle)
        val capacityView = view.findViewById<TextView>(R.id.oneui_info_battery_capacity)
        val tempView = view.findViewById<TextView>(R.id.oneui_info_battery_temp)
        val voltageView = view.findViewById<TextView>(R.id.oneui_info_battery_voltage)
        val disclaimerView = view.findViewById<TextView>(R.id.oneui_info_disclaimer_text)

        // 1. Status
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> context.getString(com.android.settingslib.R.string.battery_info_status_charging)
            BatteryManager.BATTERY_STATUS_FULL -> context.getString(com.android.settingslib.R.string.battery_info_status_full_charged)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> context.getString(com.android.settingslib.R.string.battery_info_status_discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> context.getString(com.android.settingslib.R.string.battery_info_status_not_charging)
            else -> context.getString(com.android.settingslib.R.string.battery_info_status_unknown)
        }
        statusView?.text = statusStr

        // 2. Level
        val level = batteryIntent?.let {
            val rawLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (rawLevel >= 0 && scale > 0) (rawLevel * 100) / scale else -1
        } ?: -1
        levelView?.text = if (level >= 0) "$level%" else "--%"

        // 3. Health Percentage
        var healthPct = resolveBatteryHealthPercentage(batteryIntent)
        val healthStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val healthLabel = when (healthStatus) {
            BatteryManager.BATTERY_HEALTH_GOOD -> context.getString(R.string.battery_health_good)
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> context.getString(R.string.battery_health_overheat)
            BatteryManager.BATTERY_HEALTH_DEAD -> context.getString(R.string.battery_health_dead)
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> context.getString(R.string.battery_health_over_voltage)
            else -> context.getString(R.string.battery_health_good)
        }
        healthView?.text = if (healthPct in 1..100) "$healthPct% ($healthLabel)" else healthLabel

        // 4. Cycle Count
        val cycleCount = resolveCycleCount(batteryIntent, batteryManager)
        cycleView?.text = if (cycleCount >= 0) {
            context.getString(R.string.oneui_battery_cycle_count_format, cycleCount)
        } else {
            context.getString(R.string.battery_cycle_count_not_available)
        }

        // 5. Capacity (Typical)
        var typicalCapacity = 5000
        try {
            val powerProfile = PowerProfile(context)
            val cap = powerProfile.getBatteryCapacity().roundToInt()
            if (cap > 0) {
                typicalCapacity = cap
            }
        } catch (_: Throwable) {}

        val designCapacityUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1) ?: -1
        if (designCapacityUah > 0) {
            typicalCapacity = designCapacityUah / 1000
        }
        capacityView?.text = context.getString(R.string.oneui_battery_capacity_format, typicalCapacity)

        // 6. Temperature
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (tempRaw > 0) {
            val tempC = tempRaw / 10.0f
            tempView?.text = context.getString(R.string.oneui_battery_temperature_format, tempC)
        } else {
            tempView?.text = "-- °C"
        }

        // 7. Voltage
        val voltageRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        if (voltageRaw > 0) {
            val voltageV = if (voltageRaw > 1000) voltageRaw / 1000.0f else voltageRaw.toFloat()
            voltageView?.text = context.getString(R.string.oneui_battery_voltage_format, voltageV)
        } else {
            voltageView?.text = "-- V"
        }

        // 8. Disclaimer with rated capacity
        val ratedCapacity = (typicalCapacity * 0.97).roundToInt()
        disclaimerView?.text = context.getString(R.string.oneui_battery_info_disclaimer, ratedCapacity)
    }

    private fun resolveBatteryHealthPercentage(batteryIntent: Intent?): Int {
        // Method A: EXTRA_MAXIMUM_CAPACITY vs EXTRA_DESIGN_CAPACITY
        val maxUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_MAXIMUM_CAPACITY, -1) ?: -1
        val designUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1) ?: -1
        if (maxUah > 0 && designUah > 0) {
            val pct = (maxUah * 100) / designUah
            if (pct in 1..100) return pct
        }

        // Method B: Sysfs SOH / State of Health
        val sysfsSoh = readSysfs("/sys/class/power_supply/battery/soh")
            ?: readSysfs("/sys/class/power_supply/bms/soh")
        sysfsSoh?.toIntOrNull()?.let {
            if (it in 1..100) return it
        }

        // Method C: charge_full vs charge_full_design
        val chargeFull = readSysfs("/sys/class/power_supply/battery/charge_full")?.toLongOrNull()
            ?: readSysfs("/sys/class/power_supply/bms/charge_full")?.toLongOrNull()
        val chargeDesign = readSysfs("/sys/class/power_supply/battery/charge_full_design")?.toLongOrNull()
            ?: readSysfs("/sys/class/power_supply/bms/charge_full_design")?.toLongOrNull()
        if (chargeFull != null && chargeDesign != null && chargeDesign > 0) {
            val pct = ((chargeFull * 100) / chargeDesign).toInt()
            if (pct in 1..100) return pct
        }

        // Fallback default for good condition
        return 100
    }

    private fun resolveCycleCount(batteryIntent: Intent?, bm: BatteryManager?): Int {
        // Method A: BatteryManager Intent EXTRA_CYCLE_COUNT
        val intentCycle = batteryIntent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        if (intentCycle >= 0) return intentCycle

        // Method B: Sysfs cycle_count
        val sysfsCycle = readSysfs("/sys/class/power_supply/battery/cycle_count")
            ?: readSysfs("/sys/class/power_supply/bms/cycle_count")
            ?: readSysfs("/sys/class/power_supply/battery/battery_cycle")
            ?: readSysfs("/sys/class/power_supply/bms/battery_cycle")
            ?: readSysfs("/sys/class/qcom-battery/cycle_count")
            ?: readSysfs("/sys/class/power_supply/battery/device/cycle_count")
        sysfsCycle?.toIntOrNull()?.let {
            if (it >= 0) return it
        }

        // Method C: Dynamic property reflection (if present in OEM/ROM framework)
        try {
            val field = BatteryManager::class.java.getField("BATTERY_PROPERTY_CYCLE_COUNT")
            val propId = field.getInt(null)
            val propCycle = bm?.getIntProperty(propId) ?: -1
            if (propCycle >= 0) return propCycle
        } catch (_: Throwable) {}

        return -1
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
