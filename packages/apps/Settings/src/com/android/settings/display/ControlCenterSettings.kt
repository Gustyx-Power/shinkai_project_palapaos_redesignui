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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.provider.Settings
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.widget.LayoutPreference

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

        pref.findViewById<View>(R.id.cc_option_oneui)?.setOnClickListener {
            val current = Settings.System.getInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0) == 1
            if (!current) {
                Settings.System.putInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 1)
                updateSelection(pref, true)
                OneUIQSStylePreferenceController.restartSystemUI(ctx)
            }
        }

        pref.findViewById<View>(R.id.cc_option_pixel)?.setOnClickListener {
            val current = Settings.System.getInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0) == 1
            if (current) {
                Settings.System.putInt(ctx.contentResolver, OneUIQSStylePreferenceController.SETTING_KEY, 0)
                updateSelection(pref, false)
                OneUIQSStylePreferenceController.restartSystemUI(ctx)
            }
        }
    }

    private fun updateSelection(pref: LayoutPreference, isOneUi: Boolean) {
        pref.findViewById<RadioButton>(R.id.cc_radio_oneui)?.isChecked = isOneUi
        pref.findViewById<RadioButton>(R.id.cc_radio_pixel)?.isChecked = !isOneUi
        pref.findViewById<TextView>(R.id.cc_desc_text)?.setText(
            if (isOneUi) R.string.control_center_desc_oneui else R.string.control_center_desc_pixel
        )
    }
}
