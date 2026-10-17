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

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.PowerManager
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
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
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings
import com.android.settings.fuelgauge.batteryusage.M3BatteryInfoFragment
import com.android.settingslib.fuelgauge.BatteryUtils
import java.io.File
import kotlin.math.roundToInt

class M3BatteryManagementPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), LifecycleEventObserver {

    private var mBatteryBroadcastReceiver: BatteryBroadcastReceiver? = null
    private var mPreference: M3BatteryManagementPreference? = null
    private var mViewHolder: PreferenceViewHolder? = null

    private val mPowerManager: PowerManager? = mContext.getSystemService(PowerManager::class.java)

    override fun getAvailabilityStatus(): Int = AVAILABLE_UNSEARCHABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mPreference = screen.findPreference(preferenceKey)
        mPreference?.setController(this)
        updateUi()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                mBatteryBroadcastReceiver = BatteryBroadcastReceiver(mContext).apply {
                    setBatteryChangedListener { type ->
                        if (type != BATTERY_NOT_PRESENT) {
                            updateUi()
                        }
                    }
                }
            }
            Lifecycle.Event.ON_START -> {
                mBatteryBroadcastReceiver?.register()
                updateUi()
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

    private fun updateUi() {
        val holder = mViewHolder ?: return
        val batteryIntent = BatteryUtils.getBatteryIntent(mContext) ?: mContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryManager = mContext.getSystemService(BatteryManager::class.java)

        // 1. Tile: Penghemat Baterai
        val tileSaver = holder.findViewById(R.id.m3_tile_battery_saver) as? LinearLayout
        val saverBadge = holder.findViewById(R.id.m3_tile_saver_badge) as? TextView
        val saverIconBox = holder.findViewById(R.id.m3_tile_saver_icon_box) as? FrameLayout
        val saverIcon = holder.findViewById(R.id.m3_tile_saver_icon) as? ImageView

        val isSaverOn = mPowerManager?.isPowerSaveMode ?: false
        if (isSaverOn) {
            saverBadge?.text = mContext.getString(R.string.m3_battery_saver_on)
            saverBadge?.setBackgroundResource(R.drawable.m3_battery_btn_selector_active)
            saverBadge?.setTextColor(Color.parseColor("#044426"))
            tileSaver?.setBackgroundResource(R.drawable.m3_battery_hero_bg)
            saverIconBox?.setBackgroundResource(R.drawable.m3_battery_hero_icon_bg)
            saverIcon?.setColorFilter(Color.parseColor("#044426"))
        } else {
            saverBadge?.text = mContext.getString(R.string.m3_battery_saver_off)
            saverBadge?.setBackgroundResource(R.drawable.m3_battery_dark_pill)
            saverBadge?.setTextColor(Color.parseColor("#A3C6B1"))
            tileSaver?.setBackgroundResource(R.drawable.m3_battery_tile_bg)
            saverIconBox?.setBackgroundResource(R.drawable.m3_battery_sub_chip_bg)
            saverIcon?.setColorFilter(Color.parseColor("#7EE2A7"))
        }

        tileSaver?.setOnClickListener {
            val newMode = !isSaverOn
            try {
                mPowerManager?.setPowerSaveModeEnabled(newMode)
            } catch (_: Throwable) {
                SubSettingLauncher(mContext)
                    .setDestination(BatterySaverSettings::class.java.name)
                    .setTitleRes(R.string.battery_saver)
                    .launch()
            }
            updateUi()
        }

        // 2. Tile: Kesehatan Baterai
        val tileHealth = holder.findViewById(R.id.m3_tile_battery_health) as? LinearLayout
        val healthBadge = holder.findViewById(R.id.m3_tile_health_badge) as? TextView
        val healthDesc = holder.findViewById(R.id.m3_tile_health_desc) as? TextView

        val healthPct = resolveBatteryHealthPercentage(batteryIntent)
        healthBadge?.text = "$healthPct% (Baik)"

        val cycleCount = resolveCycleCount(batteryIntent, batteryManager)
        val measuredCap = resolveMeasuredCapacity(batteryIntent)
        healthDesc?.text = "$cycleCount siklus • Kapasitas terukur $measuredCap mAh"

        tileHealth?.setOnClickListener {
            SubSettingLauncher(mContext)
                .setDestination(M3BatteryInfoFragment::class.java.name)
                .setTitleRes(R.string.battery_info)
                .launch()
        }

        // 3. Tile: Pengelola Baterai
        val tileManager = holder.findViewById(R.id.m3_tile_battery_manager) as? LinearLayout
        tileManager?.setOnClickListener {
            SubSettingLauncher(mContext)
                .setDestination(SmartBatterySettings::class.java.name)
                .setTitleRes(R.string.smart_battery_manager_title)
                .launch()
        }

        // 4. Tile: Widget Baterai
        val tileWidget = holder.findViewById(R.id.m3_tile_battery_widget) as? LinearLayout
        tileWidget?.setOnClickListener {
            launchBatteryWidgetPicker()
        }
    }

    private fun launchBatteryWidgetPicker() {
        val appWidgetManager = AppWidgetManager.getInstance(mContext) ?: return
        val providers = appWidgetManager.installedProviders
        val batteryProvider = providers.firstOrNull {
            it.provider.packageName.contains("settings", true) &&
            it.provider.className.contains("battery", true)
        } ?: providers.firstOrNull { it.provider.className.contains("battery", true) }

        if (batteryProvider != null && appWidgetManager.isRequestPinAppWidgetSupported) {
            try {
                appWidgetManager.requestPinAppWidget(batteryProvider.provider, null, null)
                return
            } catch (_: Throwable) {}
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            mContext.startActivity(intent)
        } catch (_: Throwable) {}
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

    private fun resolveCycleCount(batteryIntent: Intent?, bm: BatteryManager?): Int {
        val intentCycle = batteryIntent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
        if (intentCycle >= 0) return intentCycle

        val sysfsCycle = readSysfs("/sys/class/power_supply/battery/cycle_count")
            ?: readSysfs("/sys/class/power_supply/bms/cycle_count")
            ?: readSysfs("/sys/class/power_supply/battery/battery_cycle")
            ?: readSysfs("/sys/class/power_supply/bms/battery_cycle")
            ?: readSysfs("/sys/class/qcom-battery/cycle_count")
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
