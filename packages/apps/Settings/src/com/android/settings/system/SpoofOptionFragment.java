package com.android.settings.system;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.KeyboxDataPreference;
import com.android.settings.development.PifDataPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.concurrent.Executors;

@SearchIndexable
public class SpoofOptionFragment extends DashboardFragment {

    private static final String TAG = "SpoofOptionFragment";

    private ActivityResultLauncher<Intent> mKeyboxLauncher;
    private ActivityResultLauncher<Intent> mPifLauncher;

    private KeyboxDataPreference mKeyboxPref;
    private PifDataPreference mPifPref;
    private Preference mAutoPifPref;
    private Preference mClearGmsPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyboxLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (mKeyboxPref != null) mKeyboxPref.handleFileSelected(uri);
                    }
                });

        mPifLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (mPifPref != null) mPifPref.handleFileSelected(uri);
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mKeyboxPref = findPreference("keybox_data_setting");
        mPifPref = findPreference("pif_data_setting");
        if (mKeyboxPref != null) mKeyboxPref.setFilePickerLauncher(mKeyboxLauncher);
        if (mPifPref != null) mPifPref.setFilePickerLauncher(mPifLauncher);

        mAutoPifPref = findPreference("auto_pif_generate");
        if (mAutoPifPref != null) {
            mAutoPifPref.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                if (context == null) return true;
                Toast.makeText(context, R.string.auto_pif_updating, Toast.LENGTH_SHORT).show();
                mAutoPifPref.setEnabled(false);

                Executors.newSingleThreadExecutor().execute(() -> {
                    String resultModel = AutoPifHelper.fetchAndApplyPif(context);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (mAutoPifPref != null) mAutoPifPref.setEnabled(true);
                            if (resultModel != null) {
                                Toast.makeText(context,
                                        getString(R.string.auto_pif_success, resultModel),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(context, R.string.auto_pif_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                return true;
            });
        }

        mClearGmsPref = findPreference("clear_gms_playstore_data");
        if (mClearGmsPref != null) {
            mClearGmsPref.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                if (context == null) return true;

                new AlertDialog.Builder(context)
                        .setTitle(R.string.clear_gms_playstore_dialog_title)
                        .setMessage(R.string.clear_gms_playstore_dialog_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Toast.makeText(context, R.string.clear_gms_playstore_clearing, Toast.LENGTH_SHORT).show();
                            Executors.newSingleThreadExecutor().execute(() -> {
                                boolean cleared = AutoPifHelper.clearGmsAndPlayStore(context);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (cleared) {
                                            Toast.makeText(context, R.string.clear_gms_playstore_success, Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                            });
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            });
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PAGE_UNKNOWN;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.spoof_option_fragment;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.spoof_option_fragment);
}
