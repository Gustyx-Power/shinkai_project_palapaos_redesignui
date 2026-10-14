/*
 * Copyright (C) 2023 Paranoid Android
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

package com.android.settings.display;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.LayoutPreference;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.TopIntroPreference;

import java.util.List;
import java.util.stream.Collectors;

/** Preference fragment used for switching refresh rate */
@SearchIndexable
public class RefreshRateSettings extends RadioButtonPickerFragment {

    private static final String TAG = "RefreshRateSettings";
    private static final String KEY_VRR_PREF = "refresh_rate_vrr";

    private Context mContext;
    private RefreshRateUtils mUtils;
    private SwitchPreferenceCompat mVrrSwitchPref;
    private LayoutPreference mPreviewPref;
    private RefreshRatePreviewView mPreviewView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mUtils = new RefreshRateUtils(context);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.refresh_rate_settings;
    }

    @Override
    public void updateCandidates() {
        super.updateCandidates();
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            setupPreviewPreference(screen);
        }
    }

    private void setupPreviewPreference(PreferenceScreen screen) {
        if (mPreviewPref == null) {
            mPreviewPref = new LayoutPreference(screen.getContext(), R.layout.refresh_rate_preview_preference);
            mPreviewPref.setKey("refresh_rate_preview");
            mPreviewPref.setSelectable(false);
            mPreviewView = mPreviewPref.findViewById(R.id.refresh_rate_preview_view);
        }

        if (mPreviewView != null) {
            mPreviewView.setRefreshRates(mUtils.getRefreshRates().stream()
                    .filter(r -> r >= RefreshRateUtils.DEFAULT_REFRESH_RATE)
                    .collect(Collectors.toList()));
            mPreviewView.setSelectedRefreshRate(mUtils.getCurrentRefreshRate());
            mPreviewView.setOnRefreshRateSelectedListener(rate -> {
                final String key = String.valueOf(rate);
                onRadioButtonConfirmed(key);
            });
        }

        screen.setOrderingAsAdded(false);
        mPreviewPref.setOrder(-100);
        if (screen.findPreference(mPreviewPref.getKey()) == null) {
            screen.addPreference(mPreviewPref);
        }

        int order = 1;
        for (int i = 0; i < screen.getPreferenceCount(); i++) {
            androidx.preference.Preference pref = screen.getPreference(i);
            if (pref != mPreviewPref) {
                pref.setOrder(order++);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPreviewView != null) {
            mPreviewView.setSelectedRefreshRate(mUtils.getCurrentRefreshRate());
            mPreviewView.startAnimations();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPreviewView != null) {
            mPreviewView.stopAnimations();
        }
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        mVrrSwitchPref = new SwitchPreferenceCompat(screen.getContext());
        mVrrSwitchPref.setKey(KEY_VRR_PREF);
        mVrrSwitchPref.setTitle(R.string.refresh_rate_vrr_title);
        mVrrSwitchPref.setSummary(R.string.refresh_rate_vrr_summary);
        mVrrSwitchPref.setOnPreferenceChangeListener((pref, newValue) -> {
            mUtils.setVrrEnabled((Boolean) newValue);
            return true;
        });
        screen.addPreference(mVrrSwitchPref);
        updateVrrPref();

        final FooterPreference footerPreference = new FooterPreference(screen.getContext());
        footerPreference.setTitle(R.string.refresh_rate_footer);
        footerPreference.setSelectable(false);
        footerPreference.setLayoutResource(
                com.android.settingslib.widget.preference.footer.R.layout.preference_footer);
        screen.addPreference(footerPreference);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        return mUtils.getRefreshRates().stream()
                .filter(r -> r >= RefreshRateUtils.DEFAULT_REFRESH_RATE)
                .map(RefreshRateCandidateInfo::new)
                .collect(Collectors.toList());
    }

    private void updateVrrPref() {
        if (mVrrSwitchPref == null) return;
        mVrrSwitchPref.setEnabled(mUtils.isVrrPossible());
        mVrrSwitchPref.setChecked(mUtils.isVrrEnabled());
    }

    @Override
    protected String getDefaultKey() {
        return String.valueOf(mUtils.getCurrentRefreshRate());
    }

    @Override
    protected boolean setDefaultKey(final String key) {
        final int refreshRate = Integer.parseInt(key);
        mUtils.setCurrentRefreshRate(refreshRate);
        updateVrrPref();
        if (mPreviewView != null) {
            mPreviewView.setSelectedRefreshRate(refreshRate);
        }
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return -1;
    }

    private class RefreshRateCandidateInfo extends CandidateInfo {
        private final CharSequence mLabel;
        private final String mKey;

        RefreshRateCandidateInfo(Integer refreshRate) {
            super(true);
            mLabel = String.format("%d Hz", refreshRate.intValue());
            mKey = refreshRate.toString();
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.refresh_rate_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return new RefreshRateUtils(context).isHighRefreshRateAvailable();
                }
            };
}
