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
import androidx.fragment.app.Fragment
import com.android.internal.os.PowerProfile
import com.android.settings.R
import com.android.settingslib.fuelgauge.BatteryUtils
import kotlin.math.roundToInt

class OneUIBatteryInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.oneui_battery_info_fragment, container, false)
        bindViews(root)
        return root
    }

    private fun bindViews(view: View) {
        val context = context ?: return
        val batteryIntent = BatteryUtils.getBatteryIntent(context) ?: context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val statusView = view.findViewById<TextView>(R.id.oneui_info_battery_status)
        val levelView = view.findViewById<TextView>(R.id.oneui_info_battery_level)
        val capacityView = view.findViewById<TextView>(R.id.oneui_info_battery_capacity)
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
        statusView.text = statusStr

        // 2. Level
        val level = batteryIntent?.let {
            val rawLevel = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (rawLevel >= 0 && scale > 0) (rawLevel * 100) / scale else -1
        } ?: -1
        levelView.text = if (level >= 0) "$level%" else "--%"

        // 3. Capacity (Typical)
        var typicalCapacity = 4500
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

        capacityView.text = context.getString(R.string.oneui_battery_capacity_format, typicalCapacity)

        // 4. Disclaimer with rated capacity
        val ratedCapacity = (typicalCapacity * 0.97).roundToInt()
        disclaimerView.text = context.getString(R.string.oneui_battery_info_disclaimer, ratedCapacity)
    }
}
