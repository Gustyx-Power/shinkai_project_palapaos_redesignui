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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.widget.LayoutPreference

import android.util.Log
import android.widget.CompoundButton

class ControlCenterSettings : DashboardFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.PAGE_UNKNOWN

    override fun getPreferenceScreenResId(): Int = R.xml.control_center_settings

    override fun getLogTag(): String = "ControlCenterSettings"

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    private fun setupUI() {
        val pref = findPreference<LayoutPreference>("control_center_choice_card") ?: return
        val ctx = context ?: return
        val isOneUi = Settings.System.getInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0) == 1

        updateSelection(pref, isOneUi)
        updateRestartCardVisibility(pref, isChanged = false)

        pref.findViewById<View>(R.id.cc_option_oneui)?.setOnClickListener {
            val current = Settings.System.getInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0) == 1
            if (!current) {
                Settings.System.putInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 1)
                updateSelection(pref, true)
                updateRestartCardVisibility(pref, isChanged = true)
            }
        }

        pref.findViewById<View>(R.id.cc_option_pixel)?.setOnClickListener {
            val current = Settings.System.getInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0) == 1
            if (current) {
                Settings.System.putInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0)
                updateSelection(pref, false)
                updateRestartCardVisibility(pref, isChanged = true)
            }
        }

        pref.findViewById<View>(R.id.cc_restart_button)?.setOnClickListener {
            OneUIQSStylePreferenceController.restartSystemUI(ctx)
        }

        // Dynamic Island Charging (Experimental) Switch
        try {
            val islandSwitch = pref.findViewById<CompoundButton>(R.id.cc_dynamic_island_switch)
            val isIslandEnabled = Settings.System.getInt(ctx.contentResolver, "dynamic_island_charging", 0) == 1
            islandSwitch?.isChecked = isIslandEnabled
            islandSwitch?.setOnCheckedChangeListener { _, isChecked ->
                Settings.System.putInt(ctx.contentResolver, "dynamic_island_charging", if (isChecked) 1 else 0)
                updateRestartCardVisibility(pref, isChanged = true)
            }
            pref.findViewById<View>(R.id.cc_dynamic_island_card)?.setOnClickListener {
                islandSwitch?.toggle()
            }
        } catch (t: Throwable) {
            Log.e("ControlCenterSettings", "Error initializing dynamic island switch", t)
        }
    }

    private fun updateRestartCardVisibility(pref: LayoutPreference, isChanged: Boolean) {
        val restartCard = pref.findViewById<View>(R.id.cc_restart_card) ?: return
        restartCard.visibility = if (isChanged) View.VISIBLE else View.GONE
    }

    private fun updateSelection(pref: LayoutPreference, isOneUi: Boolean) {
        pref.findViewById<RadioButton>(R.id.cc_radio_oneui)?.isChecked = isOneUi
        pref.findViewById<RadioButton>(R.id.cc_radio_pixel)?.isChecked = !isOneUi
        pref.findViewById<TextView>(R.id.cc_desc_text)?.setText(
            if (isOneUi) R.string.control_center_desc_oneui else R.string.control_center_desc_pixel
        )
    }
}
