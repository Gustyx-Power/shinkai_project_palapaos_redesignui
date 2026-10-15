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

package com.android.settings.fuelgauge

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT
import com.android.settings.fuelgauge.batteryusage.OneUIBatteryInfoFragment
import com.android.settingslib.fuelgauge.BatteryUtils

class OneUIBatteryHeaderPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key), LifecycleEventObserver {

    private var mBatteryBroadcastReceiver: BatteryBroadcastReceiver? = null
    private var mPreference: OneUIBatteryHeaderPreference? = null
    private var mViewHolder: PreferenceViewHolder? = null

    private var mBatteryLevel: Int = 81
    private var mRemainingLabel: String? = null
    private var mStatusLabel: String? = null
    private var mDischarging: Boolean = true

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

        val percentView = holder.findViewById(R.id.oneui_battery_percent) as? TextView
        val timeLeftView = holder.findViewById(R.id.oneui_battery_time_left) as? TextView
        val progressBar = holder.findViewById(R.id.oneui_battery_progress) as? ProgressBar
        val fullChargeView = holder.findViewById(R.id.oneui_battery_full_charge_desc) as? TextView
        val infoIcon = holder.findViewById(R.id.oneui_battery_info_icon) as? ImageView

        percentView?.text = "$mBatteryLevel%"
        progressBar?.progress = mBatteryLevel

        if (!mDischarging) {
            timeLeftView?.text = mStatusLabel ?: mContext.getString(com.android.settingslib.R.string.battery_info_status_charging)
        } else {
            timeLeftView?.text = mRemainingLabel ?: "8 h 35 m left"
        }

        val estimateStr = mRemainingLabel ?: "10 h 36 m"
        fullChargeView?.text = mContext.getString(R.string.oneui_battery_full_charge_desc, estimateStr)

        infoIcon?.setOnClickListener {
            SubSettingLauncher(mContext)
                .setDestination(OneUIBatteryInfoFragment::class.java.name)
                .setTitleRes(R.string.battery_info)
                .launch()
        }
    }
}
