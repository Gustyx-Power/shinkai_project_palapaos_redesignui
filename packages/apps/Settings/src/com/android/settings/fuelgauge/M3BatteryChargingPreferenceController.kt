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
import android.graphics.Color
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.android.internal.lineage.health.HealthInterface
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.lineage.health.ChargingControlSettings

class M3BatteryChargingPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key) {

    companion object {
        const val MODE_ADAPTIVE = 1
        const val MODE_LIMIT_80 = 2
        const val MODE_STANDARD_100 = 3
    }

    private var mPreference: M3BatteryChargingPreference? = null
    private var mViewHolder: PreferenceViewHolder? = null
    private var mSelectedMode = MODE_LIMIT_80

    private val mHealthInterface: HealthInterface? by lazy {
        try {
            HealthInterface.getInstance(mContext)
        } catch (_: Throwable) {
            null
        }
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE_UNSEARCHABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mPreference = screen.findPreference(preferenceKey)
        mPreference?.setController(this)
        readCurrentMode()
        updateUi()
    }

    fun bindViews(holder: PreferenceViewHolder) {
        mViewHolder = holder
        readCurrentMode()
        updateUi()
    }

    private fun readCurrentMode() {
        try {
            val hi = mHealthInterface
            if (hi != null && HealthInterface.isChargingControlSupported(mContext)) {
                if (!hi.enabled) {
                    mSelectedMode = MODE_STANDARD_100
                } else {
                    val mode = hi.mode
                    val limit = hi.limit
                    mSelectedMode = if (mode == HealthInterface.MODE_LIMIT || limit in 75..85) {
                        MODE_LIMIT_80
                    } else if (mode == HealthInterface.MODE_AUTO) {
                        MODE_ADAPTIVE
                    } else {
                        MODE_STANDARD_100
                    }
                }
                return
            }
        } catch (_: Throwable) {}

        // Fallback to Settings.System / Settings.Secure
        val savedMode = Settings.System.getInt(mContext.contentResolver, "m3_charging_control_mode", MODE_LIMIT_80)
        mSelectedMode = savedMode
    }

    private fun applyMode(mode: Int) {
        mSelectedMode = mode
        Settings.System.putInt(mContext.contentResolver, "m3_charging_control_mode", mode)

        try {
            val hi = mHealthInterface
            if (hi != null && HealthInterface.isChargingControlSupported(mContext)) {
                when (mode) {
                    MODE_ADAPTIVE -> {
                        hi.enabled = true
                        hi.mode = HealthInterface.MODE_AUTO
                    }
                    MODE_LIMIT_80 -> {
                        hi.enabled = true
                        hi.mode = HealthInterface.MODE_LIMIT
                        hi.limit = 80
                    }
                    MODE_STANDARD_100 -> {
                        hi.enabled = false
                    }
                }
            }
        } catch (_: Throwable) {}

        updateUi()
    }

    private fun updateUi() {
        val holder = mViewHolder ?: return

        val headerLayout = holder.findViewById(R.id.m3_charging_control_header_layout)

        val btnAdaptive = holder.findViewById(R.id.m3_btn_charge_adaptive) as? LinearLayout
        val titleAdaptive = holder.findViewById(R.id.m3_btn_charge_adaptive_title) as? TextView
        val subAdaptive = holder.findViewById(R.id.m3_btn_charge_adaptive_sub) as? TextView

        val btnLimit80 = holder.findViewById(R.id.m3_btn_charge_limit80) as? LinearLayout
        val titleLimit80 = holder.findViewById(R.id.m3_btn_charge_limit80_title) as? TextView
        val subLimit80 = holder.findViewById(R.id.m3_btn_charge_limit80_sub) as? TextView

        val btnStandard100 = holder.findViewById(R.id.m3_btn_charge_standard100) as? LinearLayout
        val titleStandard100 = holder.findViewById(R.id.m3_btn_charge_standard100_title) as? TextView
        val subStandard100 = holder.findViewById(R.id.m3_btn_charge_standard100_sub) as? TextView

        headerLayout?.setOnClickListener {
            SubSettingLauncher(mContext)
                .setDestination(ChargingControlSettings::class.java.name)
                .setTitleRes(R.string.charging_control_title)
                .launch()
        }

        // Apply styles for button 1 (Adaptive)
        if (mSelectedMode == MODE_ADAPTIVE) {
            btnAdaptive?.setBackgroundResource(R.drawable.m3_battery_btn_selector_active)
            titleAdaptive?.setTextColor(Color.parseColor("#044426"))
            subAdaptive?.setTextColor(Color.parseColor("#044426"))
        } else {
            btnAdaptive?.setBackgroundResource(R.drawable.m3_battery_btn_selector_normal)
            titleAdaptive?.setTextColor(Color.parseColor("#FFFFFF"))
            subAdaptive?.setTextColor(Color.parseColor("#A3C6B1"))
        }

        // Apply styles for button 2 (Limit 80%)
        if (mSelectedMode == MODE_LIMIT_80) {
            btnLimit80?.setBackgroundResource(R.drawable.m3_battery_btn_selector_active)
            titleLimit80?.setTextColor(Color.parseColor("#044426"))
            subLimit80?.setTextColor(Color.parseColor("#044426"))
        } else {
            btnLimit80?.setBackgroundResource(R.drawable.m3_battery_btn_selector_normal)
            titleLimit80?.setTextColor(Color.parseColor("#FFFFFF"))
            subLimit80?.setTextColor(Color.parseColor("#A3C6B1"))
        }

        // Apply styles for button 3 (Standard 100%)
        if (mSelectedMode == MODE_STANDARD_100) {
            btnStandard100?.setBackgroundResource(R.drawable.m3_battery_btn_selector_active)
            titleStandard100?.setTextColor(Color.parseColor("#044426"))
            subStandard100?.setTextColor(Color.parseColor("#044426"))
        } else {
            btnStandard100?.setBackgroundResource(R.drawable.m3_battery_btn_selector_normal)
            titleStandard100?.setTextColor(Color.parseColor("#FFFFFF"))
            subStandard100?.setTextColor(Color.parseColor("#A3C6B1"))
        }

        btnAdaptive?.setOnClickListener { applyMode(MODE_ADAPTIVE) }
        btnLimit80?.setOnClickListener { applyMode(MODE_LIMIT_80) }
        btnStandard100?.setOnClickListener { applyMode(MODE_STANDARD_100) }
    }
}
