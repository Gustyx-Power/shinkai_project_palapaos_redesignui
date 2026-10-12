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

package com.android.settings.deviceinfo.aboutphone

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.LayoutPreference

class AboutPhoneDashboardPreference(context: Context) : PreferenceMetadata, PreferenceBinding {

    override val key: String
        get() = AboutPhoneDashboardPreferenceController.KEY

    override val purpose: Int
        get() = R.string.about_phone_dashboard_purpose

    override fun createWidget(context: Context): Preference =
        LayoutPreference(context, R.layout.about_phone_dashboard).apply {
            isSelectable = false
            isAllowDividerBelow = false
            AboutPhoneDashboardPreferenceController.populateViews(this)
        }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is LayoutPreference) {
            AboutPhoneDashboardPreferenceController.populateViews(preference)
        }
    }
}
