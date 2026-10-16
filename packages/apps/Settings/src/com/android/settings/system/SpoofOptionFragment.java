package com.android.settings.system;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.KeyboxDataPreference;
import com.android.settings.development.PifDataPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.LayoutPreference;

import java.util.concurrent.Executors;

@SearchIndexable
public class SpoofOptionFragment extends DashboardFragment {

    private static final String TAG = "SpoofOptionFragment";

    private ActivityResultLauncher<Intent> mKeyboxLauncher;
    private ActivityResultLauncher<Intent> mPifLauncher;

    private LayoutPreference mHeaderCardPref;
    private KeyboxDataPreference mKeyboxPref;
    private PifDataPreference mPifPref;
    private ListPreference mPresetPref;
    private Preference mKeyboxStatusPref;
    private EditTextPreference mBootHashPref;
    private ListPreference mTargetModePref;
    private SwitchPreferenceCompat mRkpOnlyPref;
    private SwitchPreferenceCompat mAutoSplPref;
    private ListPreference mTeeSecurityLevelPref;
    private Preference mAutoPifPref;
    private Preference mClearGmsPref;
    private Preference mLaunchKeyAttestPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mKeyboxLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (mKeyboxPref != null) {
                            mKeyboxPref.handleFileSelected(uri);
                            updateHeaderCard();
                        }
                    }
                });

        mPifLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (mPifPref != null) {
                            mPifPref.handleFileSelected(uri);
                            updateHeaderCard();
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        final Context context = getContext();
        if (context == null) return;

        // 1. Header Card
        mHeaderCardPref = findPreference("spoof_header_card");
        updateHeaderCard();

        // 2. PIF Preset Selector
        mPresetPref = findPreference("pif_preset_profile");
        if (mPresetPref != null) {
            String activeSummary = AutoPifHelper.getActiveProfileSummary(context);
            mPresetPref.setSummary(getString(R.string.pif_preset_profile_summary, activeSummary));
            mPresetPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String presetKey = (String) newValue;
                String modelTitle = AutoPifHelper.applyPreset(context, presetKey);
                if (modelTitle != null) {
                    mPresetPref.setSummary(getString(R.string.pif_preset_profile_summary, modelTitle));
                    Toast.makeText(context, getString(R.string.pif_preset_applied, modelTitle), Toast.LENGTH_SHORT).show();
                    updateHeaderCard();
                }
                return true;
            });
        }

        // 3. Keybox Data & Status
        mKeyboxPref = findPreference("keybox_data_setting");
        if (mKeyboxPref != null) mKeyboxPref.setFilePickerLauncher(mKeyboxLauncher);

        mPifPref = findPreference("pif_data_setting");
        if (mPifPref != null) mPifPref.setFilePickerLauncher(mPifLauncher);

        mKeyboxStatusPref = findPreference("keybox_status_info");
        if (mKeyboxStatusPref != null) {
            mKeyboxStatusPref.setSummary(AutoPifHelper.getKeyboxStatus(context));
        }

        // 4. Custom Verified Boot Hash
        mBootHashPref = findPreference("verified_boot_hash_setting");
        if (mBootHashPref != null) {
            String currentHash = AutoPifHelper.getVerifiedBootHash(context);
            if (!currentHash.isEmpty()) {
                mBootHashPref.setText(currentHash);
                mBootHashPref.setSummary(currentHash);
            } else {
                mBootHashPref.setText("");
                mBootHashPref.setSummary(R.string.verified_boot_hash_summary);
            }
            mBootHashPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String hash = (String) newValue;
                if (hash == null) hash = "";
                hash = hash.trim();
                if (!hash.isEmpty() && hash.length() != 64) {
                    Toast.makeText(context, R.string.verified_boot_hash_invalid, Toast.LENGTH_SHORT).show();
                    return false;
                }
                boolean success = AutoPifHelper.setVerifiedBootHash(context, hash);
                if (success) {
                    mBootHashPref.setSummary(hash.isEmpty() ? getString(R.string.verified_boot_hash_summary) : hash);
                    Toast.makeText(context, R.string.verified_boot_hash_saved, Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }

        // 5. Keybox Target Mode
        mTargetModePref = findPreference("keybox_target_mode");
        if (mTargetModePref != null) {
            String mode = Settings.Secure.getString(context.getContentResolver(), "keybox_target_mode");
            if (mode == null) mode = "recommended";
            mTargetModePref.setValue(mode);
            mTargetModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Secure.putString(context.getContentResolver(), "keybox_target_mode", (String) newValue);
                return true;
            });
        }

        // 6. TEESimulator Options
        mRkpOnlyPref = findPreference("tee_rkp_only_setting");
        if (mRkpOnlyPref != null) {
            boolean rkpOnly = Settings.Secure.getInt(context.getContentResolver(), "tee_rkp_only", 0) != 0;
            mRkpOnlyPref.setChecked(rkpOnly);
            mRkpOnlyPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                Settings.Secure.putInt(context.getContentResolver(), "tee_rkp_only", val ? 1 : 0);
                return true;
            });
        }

        mAutoSplPref = findPreference("tee_auto_spl_setting");
        if (mAutoSplPref != null) {
            boolean autoSpl = Settings.Secure.getInt(context.getContentResolver(), "tee_auto_spl", 1) != 0;
            mAutoSplPref.setChecked(autoSpl);
            mAutoSplPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean val = (Boolean) newValue;
                Settings.Secure.putInt(context.getContentResolver(), "tee_auto_spl", val ? 1 : 0);
                return true;
            });
        }

        mTeeSecurityLevelPref = findPreference("tee_security_level");
        if (mTeeSecurityLevelPref != null) {
            String level = Settings.Secure.getString(context.getContentResolver(), "tee_security_level");
            if (level == null) level = "tee";
            mTeeSecurityLevelPref.setValue(level);
            mTeeSecurityLevelPref.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Secure.putString(context.getContentResolver(), "tee_security_level", (String) newValue);
                return true;
            });
        }

        // 7. Auto PIF Online Fetch
        mAutoPifPref = findPreference("auto_pif_generate");
        if (mAutoPifPref != null) {
            mAutoPifPref.setOnPreferenceClickListener(preference -> {
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
                                updateHeaderCard();
                            } else {
                                Toast.makeText(context, R.string.auto_pif_failed, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                return true;
            });
        }

        // 8. Clear GMS, GSF, Play Store Data
        mClearGmsPref = findPreference("clear_gms_playstore_data");
        if (mClearGmsPref != null) {
            mClearGmsPref.setOnPreferenceClickListener(preference -> {
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
                                            updateHeaderCard();
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

        // 9. Launch Key Attestation App
        mLaunchKeyAttestPref = findPreference("launch_key_attestation");
        if (mLaunchKeyAttestPref != null) {
            mLaunchKeyAttestPref.setOnPreferenceClickListener(preference -> {
                PackageManager pm = context.getPackageManager();
                String[] candidatePackages = new String[]{
                        "io.github.vvb2060.keyattestation",
                        "io.github.qwq233.keyattestation"
                };
                Intent launchIntent = null;
                for (String pkg : candidatePackages) {
                    launchIntent = pm.getLaunchIntentForPackage(pkg);
                    if (launchIntent != null) break;
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent);
                } else {
                    Toast.makeText(context, R.string.launch_key_attestation_not_installed, Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    private void updateHeaderCard() {
        if (mHeaderCardPref == null) return;
        Context context = getContext();
        if (context == null) return;

        TextView profileTitle = (TextView) mHeaderCardPref.findViewById(R.id.spoof_active_profile_title);
        TextView fingerprintText = (TextView) mHeaderCardPref.findViewById(R.id.spoof_active_fingerprint_text);
        TextView chipIntegrity = (TextView) mHeaderCardPref.findViewById(R.id.spoof_chip_integrity);
        TextView chipKeybox = (TextView) mHeaderCardPref.findViewById(R.id.spoof_chip_keybox);
        TextView chipBootloader = (TextView) mHeaderCardPref.findViewById(R.id.spoof_chip_bootloader);
        TextView statusBadge = (TextView) mHeaderCardPref.findViewById(R.id.spoof_status_badge);

        boolean hasKeybox = AutoPifHelper.hasKeybox(context);

        if (profileTitle != null) {
            profileTitle.setText(AutoPifHelper.getActiveProfileSummary(context));
        }
        if (fingerprintText != null) {
            fingerprintText.setText(AutoPifHelper.getActiveFingerprint(context));
        }
        if (chipIntegrity != null) {
            chipIntegrity.setText(hasKeybox ? "STRONG" : "DEVICE");
            chipIntegrity.setTextColor(Color.parseColor("#10B981"));
        }
        if (chipKeybox != null) {
            chipKeybox.setText(hasKeybox ? "HARDWARE" : "NOT SET");
            chipKeybox.setTextColor(hasKeybox ? Color.parseColor("#06B6D4") : Color.parseColor("#F59E0B"));
        }
        if (chipBootloader != null) {
            chipBootloader.setText("LOCKED");
            chipBootloader.setTextColor(Color.parseColor("#10B981"));
        }
        if (statusBadge != null) {
            statusBadge.setText(hasKeybox ? "PROTECTED" : "ACTIVE");
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
