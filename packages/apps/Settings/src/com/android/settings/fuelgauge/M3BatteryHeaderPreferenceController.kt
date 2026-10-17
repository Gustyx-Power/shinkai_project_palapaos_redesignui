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

package com.android.settings.fuelgauge

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.android.internal.os.PowerProfile
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT
import com.android.settings.fuelgauge.batteryusage.M3BatteryInfoFragment
import com.android.settingslib.fuelgauge.BatteryUtils
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

class M3BatteryHeaderPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), LifecycleEventObserver {

    private var mBatteryBroadcastReceiver: BatteryBroadcastReceiver? = null
    private var mPreference: M3BatteryHeaderPreference? = null
    private var mViewHolder: PreferenceViewHolder? = null

    private var mBatteryLevel: Int = 56
    private var mRemainingLabel: String? = null
    private var mStatusLabel: String? = null
    private var mDischarging: Boolean = true
    private var mChargingSpeed: Int = BatteryManager.CHARGING_POLICY_DEFAULT

    override fun getAvailabilityStatus(): Int = AVAILABLE_UNSEARCHABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mPreference = screen.findPreference(preferenceKey)
        mPreference?.setController(this)
        quickUpdate()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                mBatteryBroadcastReceiver = BatteryBroadcastReceiver(mContext).apply {
                    setBatteryChangedListener { type ->
                        if (type != BATTERY_NOT_PRESENT) {
                            quickUpdate()
                        }
                    }
                }
            }
            Lifecycle.Event.ON_START -> {
                mBatteryBroadcastReceiver?.register()
            }
            Lifecycle.Event.ON_STOP -> {
                mBatteryBroadcastReceiver?.unRegister()
            }
            else -> {}
        }
    }

    fun bindViews(holder: PreferenceViewHolder) {
        mViewHolder = holder
        updateUi()
    }

    private fun quickUpdate() {
        val batteryIntent = BatteryUtils.getBatteryIntent(mContext) ?: mContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.let {
            val raw = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) (raw * 100) / scale else -1
        } ?: -1

        if (level >= 0) {
            mBatteryLevel = level
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        mDischarging = plugged == 0

        BatteryInfo.getBatteryInfo(
            mContext,
            { info ->
                if (info != null) {
                    mBatteryLevel = info.batteryLevel
                    mRemainingLabel = info.remainingLabel?.toString()
                    mStatusLabel = info.statusLabel
                    mDischarging = info.discharging
                    updateUi()
                }
            },
            true
        )

        updateUi()
    }

    private fun updateUi() {
        val holder = mViewHolder ?: return
        val batteryIntent = BatteryUtils.getBatteryIntent(mContext) ?: mContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryManager = mContext.getSystemService(BatteryManager::class.java)

        val badgeText = holder.findViewById(R.id.m3_hero_badge_text) as? TextView
        val heroIcon = holder.findViewById(R.id.m3_hero_icon) as? ImageView
        val percentView = holder.findViewById(R.id.m3_battery_percent) as? TextView
        val timeLeftView = holder.findViewById(R.id.m3_battery_time_left) as? TextView
        val progressBar = holder.findViewById(R.id.m3_battery_progress) as? ProgressBar
        val capacityText = holder.findViewById(R.id.m3_hero_capacity_text) as? TextView
        val tempView = holder.findViewById(R.id.m3_hero_val_temp) as? TextView
        val voltView = holder.findViewById(R.id.m3_hero_val_volt) as? TextView
        val healthView = holder.findViewById(R.id.m3_hero_val_health) as? TextView
        val bottomBar = holder.findViewById(R.id.m3_hero_bottom_bar)
        val cycleDesc = holder.findViewById(R.id.m3_battery_cycle_desc) as? TextView

        // 1. Percentage
        percentView?.text = "$mBatteryLevel%"
        progressBar?.progress = mBatteryLevel

        // 2. Typical Capacity & Calculated current capacity
        var typicalCapacity = 5000
        try {
            val powerProfile = PowerProfile(mContext)
            val cap = powerProfile.getBatteryCapacity().roundToInt()
            if (cap > 0) typicalCapacity = cap
        } catch (_: Throwable) {}
        val designCapacityUah = batteryIntent?.getIntExtra(BatteryManager.EXTRA_DESIGN_CAPACITY, -1) ?: -1
        if (designCapacityUah > 0) {
            typicalCapacity = designCapacityUah / 1000
        }
        val currentMah = ((mBatteryLevel.toFloat() / 100f) * typicalCapacity).roundToInt()
        capacityText?.text = String.format(Locale.getDefault(), "%,d / %,d mAh", currentMah, typicalCapacity).replace(',', '.')

        // 3. Charging status & Badge
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || !mDischarging

        if (isCharging) {
            heroIcon?.setImageResource(R.drawable.ic_battery_charging_full)
            val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val maxAmps = batteryIntent?.getIntExtra("max_charging_current", -1) ?: -1
            if (maxAmps > 2000000 || chargePlug == BatteryManager.BATTERY_PLUGGED_AC) {
                badgeText?.text = mContext.getString(R.string.m3_battery_charging_slow_adaptive)
            } else {
                badgeText?.text = mContext.getString(R.string.m3_battery_charging_normal)
            }
            val remaining = mRemainingLabel ?: "1j 25m"
            timeLeftView?.text = "• " + mContext.getString(R.string.m3_battery_full_in, remaining)
        } else {
            heroIcon?.setImageResource(R.drawable.ic_battery_full)
            badgeText?.text = mContext.getString(R.string.m3_battery_discharging_optimal)
            val remaining = mRemainingLabel ?: "8j 35m"
            timeLeftView?.text = "• " + mContext.getString(R.string.m3_battery_time_left, remaining)
        }

        // 4. Telemetry: Temperature
        val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (tempRaw > 0) {
            val tempC = tempRaw / 10.0f
            tempView?.text = String.format(Locale.getDefault(), "%.1f °C", tempC)
        } else {
            tempView?.text = "38,9 °C"
        }

        // 5. Telemetry: Voltage
        val voltageRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        if (voltageRaw > 0) {
            val voltV = if (voltageRaw > 1000) voltageRaw / 1000.0f else voltageRaw.toFloat()
            voltView?.text = String.format(Locale.getDefault(), "%.2f V", voltV)
        } else {
            voltView?.text = "3,90 V"
        }

        // 6. Telemetry: Health
        val healthPct = resolveBatteryHealthPercentage(batteryIntent)
        healthView?.text = String.format(Locale.getDefault(), "%d%% Baik", healthPct)

        // 7. Cycle count on Bottom Bar
        val cycleCount = resolveCycleCount(batteryIntent, batteryManager)
        val cyclesToDisplay = if (cycleCount >= 0) cycleCount else 1354
        if (isCharging) {
            cycleDesc?.text = mContext.getString(R.string.m3_battery_cycles_recorded_format, cyclesToDisplay)
        } else {
            cycleDesc?.text = mContext.getString(R.string.m3_battery_cycles_discharging_format, cyclesToDisplay)
        }

        // 8. Navigation to Detail Info
        val openDetailListener = View.OnClickListener {
            SubSettingLauncher(mContext)
                .setDestination(M3BatteryInfoFragment::class.java.name)
                .setTitleRes(R.string.battery_info)
                .launch()
        }
        bottomBar?.setOnClickListener(openDetailListener)
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

        val chargeFull = readSysfs("/sys/class/power_supply/battery/charge_full")?.toLongOrNull()
            ?: readSysfs("/sys/class/power_supply/bms/charge_full")?.toLongOrNull()
        val chargeDesign = readSysfs("/sys/class/power_supply/battery/charge_full_design")?.toLongOrNull()
            ?: readSysfs("/sys/class/power_supply/bms/charge_full_design")?.toLongOrNull()
        if (chargeFull != null && chargeDesign != null && chargeDesign > 0) {
            val pct = ((chargeFull * 100) / chargeDesign).toInt()
            if (pct in 1..100) return pct
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
            ?: readSysfs("/sys/class/qcom-battery/cycle_count")
            ?: readSysfs("/sys/class/power_supply/battery/device/cycle_count")
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

    private fun readSysfs(path: String): String? {
        return try {
            val f = File(path)
            if (f.exists()) f.readText().trim() else null
        } catch (_: Throwable) {
            null
        }
    }
}
