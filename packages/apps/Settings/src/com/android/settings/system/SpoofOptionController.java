package com.android.settings.system;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;

public class SpoofOptionController extends TogglePreferenceController {

    private static final String SETTINGS_KEY = "spoof_option_enabled";

    private PreferenceScreen mScreen;

    public SpoofOptionController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        updateDependentPreferences();
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                SETTINGS_KEY, 1) != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.System.putInt(mContext.getContentResolver(),
                SETTINGS_KEY, isChecked ? 1 : 0);
        updateDependentPreferences();
        return true;
    }

    private void updateDependentPreferences() {
        if (mScreen == null) return;
        boolean enabled = isChecked();

        String[] categoryKeys = new String[]{
                "category_pif_suite",
                "category_trickystore_suite",
                "category_teesim_suite",
                "category_maintenance"
        };

        for (String catKey : categoryKeys) {
            Preference pref = mScreen.findPreference(catKey);
            if (pref != null) {
                pref.setEnabled(enabled);
            }
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return NO_RES;
    }
}
