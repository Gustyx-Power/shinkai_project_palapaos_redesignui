package com.android.settings.system;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

@SearchIndexable
public class SpoofOptionFragment extends DashboardFragment {

    private static final String TAG = "SpoofOptionFragment";

    private ActivityResultLauncher<Intent> mKeyboxLauncher;
    private ActivityResultLauncher<Intent> mPifLauncher;

    private LayoutPreference mHeaderCardPref;
    private ListPreference mAttestationModePref;
    private ListPreference mPresetPref;
    private Preference mAutoPifPref;
    private PifDataPreference mPifPref;

    private Preference mKeyboxStatusPref;
    private KeyboxDataPreference mKeyboxPref;
    private Preference mClearKeyboxPref;
    private EditTextPreference mBootHashPref;
    private Preference mGenerateBootHashPref;
    private ListPreference mTargetModePref;
    private Preference mManageTargetAppsPref;

    private SwitchPreferenceCompat mRkpOnlyPref;
    private SwitchPreferenceCompat mAutoSplPref;
    private ListPreference mTeeSecurityLevelPref;

    private Preference mFixPlayStorePref;
    private Preference mClearGmsPref;
    private Preference mLaunchPlayStorePref;
    private Preference mLaunchKeyAttestPref;

    private static class TargetAppItem {
        final String label;
        final String packageName;
        final Drawable icon;
        boolean isChecked;

        TargetAppItem(String label, String packageName, Drawable icon, boolean isChecked) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
            this.isChecked = isChecked;
        }
    }

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
                            if (mKeyboxStatusPref != null && getContext() != null) {
                                mKeyboxStatusPref.setSummary(AutoPifHelper.getKeyboxStatus(getContext()));
                            }
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

        // 2. Attestation Engine Mode
        mAttestationModePref = findPreference("attestation_mode");
        if (mAttestationModePref != null) {
            String currentMode = Settings.Secure.getString(context.getContentResolver(), "attestation_mode");
            if (currentMode == null) currentMode = "pif_software";
            mAttestationModePref.setValue(currentMode);
            mAttestationModePref.setSummary(getString(R.string.attestation_mode_summary, AutoPifHelper.getAttestationModeSummary(context)));
            mAttestationModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String mode = (String) newValue;
                Settings.Secure.putString(context.getContentResolver(), "attestation_mode", mode);
                AutoPifHelper.restartGooglePackages(context);
                mAttestationModePref.setSummary(getString(R.string.attestation_mode_summary, AutoPifHelper.getAttestationModeSummary(context)));
                if (mKeyboxStatusPref != null) {
                    mKeyboxStatusPref.setSummary(AutoPifHelper.getKeyboxStatus(context));
                }
                updateHeaderCard();
                return true;
            });
        }

        // 3. PIF Preset Selector
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

        // 4. Custom PIF Data & Auto Fetch
        mPifPref = findPreference("pif_data_setting");
        if (mPifPref != null) mPifPref.setFilePickerLauncher(mPifLauncher);

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

        // 5. TrickyStore Suite: Keybox Status, File Picker & Clear Keybox
        mKeyboxStatusPref = findPreference("keybox_status_info");
        if (mKeyboxStatusPref != null) {
            mKeyboxStatusPref.setSummary(AutoPifHelper.getKeyboxStatus(context));
        }

        mKeyboxPref = findPreference("keybox_data_setting");
        if (mKeyboxPref != null) mKeyboxPref.setFilePickerLauncher(mKeyboxLauncher);

        mClearKeyboxPref = findPreference("clear_keybox");
        if (mClearKeyboxPref != null) {
            mClearKeyboxPref.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.clear_keybox_dialog_title)
                        .setMessage(R.string.clear_keybox_dialog_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            boolean cleared = AutoPifHelper.clearKeybox(context);
                            if (cleared) {
                                Toast.makeText(context, R.string.clear_keybox_success, Toast.LENGTH_SHORT).show();
                                if (mKeyboxStatusPref != null) {
                                    mKeyboxStatusPref.setSummary(AutoPifHelper.getKeyboxStatus(context));
                                }
                                updateHeaderCard();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            });
        }

        // 6. Custom Verified Boot Hash & Random Generator
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

        mGenerateBootHashPref = findPreference("generate_boot_hash");
        if (mGenerateBootHashPref != null) {
            mGenerateBootHashPref.setOnPreferenceClickListener(preference -> {
                String hash = AutoPifHelper.generateRandomBootHash(context);
                if (mBootHashPref != null) {
                    mBootHashPref.setText(hash);
                    mBootHashPref.setSummary(hash);
                }
                Toast.makeText(context, getString(R.string.generate_boot_hash_success, hash.substring(0, 16) + "..."), Toast.LENGTH_LONG).show();
                return true;
            });
        }

        // 7. Keybox Target Mode & Per-App Target Apps
        mTargetModePref = findPreference("keybox_target_mode");
        mManageTargetAppsPref = findPreference("manage_target_apps");
        updateTargetAppsSummary(context);

        if (mTargetModePref != null) {
            String mode = Settings.Secure.getString(context.getContentResolver(), "keybox_target_mode");
            if (mode == null) mode = "recommended";
            mTargetModePref.setValue(mode);
            mTargetModePref.setSummary(getString(R.string.keybox_target_mode_summary, AutoPifHelper.getTargetModeSummary(context)));
            mTargetModePref.setOnPreferenceChangeListener((preference, newValue) -> {
                String newMode = (String) newValue;
                Settings.Secure.putString(context.getContentResolver(), "keybox_target_mode", newMode);
                mTargetModePref.setSummary(getString(R.string.keybox_target_mode_summary, AutoPifHelper.getTargetModeSummary(context)));
                AutoPifHelper.restartGooglePackages(context);
                return true;
            });
        }

        if (mManageTargetAppsPref != null) {
            mManageTargetAppsPref.setOnPreferenceClickListener(preference -> {
                showTargetAppsDialog(context);
                return true;
            });
        }

        // 8. TEESimulator Suite: RKP Only, Auto SPL, Security Level
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

        // 9. Fix Play Store Certification (Recommended 1-Tap Solution)
        mFixPlayStorePref = findPreference("fix_playstore_certification");
        if (mFixPlayStorePref != null) {
            mFixPlayStorePref.setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.fix_playstore_certification_dialog_title)
                        .setMessage(R.string.fix_playstore_certification_dialog_message)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Toast.makeText(context, R.string.fix_playstore_certification_running, Toast.LENGTH_SHORT).show();
                            Executors.newSingleThreadExecutor().execute(() -> {
                                boolean fixed = AutoPifHelper.fixPlayStoreCertification(context);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (fixed) {
                                            if (mAttestationModePref != null) {
                                                mAttestationModePref.setValue("pif_software");
                                                mAttestationModePref.setSummary(getString(R.string.attestation_mode_summary, AutoPifHelper.getAttestationModeSummary(context)));
                                            }
                                            if (mPresetPref != null) {
                                                mPresetPref.setValue("pixel_5");
                                                mPresetPref.setSummary(getString(R.string.pif_preset_profile_summary, AutoPifHelper.getActiveProfileSummary(context)));
                                            }
                                            if (mKeyboxStatusPref != null) {
                                                mKeyboxStatusPref.setSummary(AutoPifHelper.getKeyboxStatus(context));
                                            }
                                            updateHeaderCard();
                                            Toast.makeText(context, R.string.fix_playstore_certification_success, Toast.LENGTH_LONG).show();
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

        // 10. Clear GMS, GSF, Play Store Data
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

        // 11. Launch Google Play Store Settings
        mLaunchPlayStorePref = findPreference("launch_play_store_settings");
        if (mLaunchPlayStorePref != null) {
            mLaunchPlayStorePref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setPackage("com.android.vending");
                    intent.setData(Uri.parse("https://play.google.com/store/apps"));
                    context.startActivity(intent);
                } catch (Throwable t) {
                    Toast.makeText(context, "Google Play Store not found", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }

        // 12. Launch Key Attestation App
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

    private void showTargetAppsDialog(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Set<String> currentTargets = new LinkedHashSet<>(AutoPifHelper.getTargetPackages(context));

        List<TargetAppItem> allApps = new ArrayList<>();
        for (ApplicationInfo info : installed) {
            boolean isSystem = (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean isCore = "com.google.android.gms".equals(info.packageName)
                    || "com.android.vending".equals(info.packageName)
                    || "com.google.android.gsf".equals(info.packageName)
                    || "io.github.vvb2060.keyattestation".equals(info.packageName)
                    || "io.github.qwq233.keyattestation".equals(info.packageName);
            if (!isSystem || isCore) {
                String label = info.loadLabel(pm).toString();
                Drawable icon = info.loadIcon(pm);
                boolean checked = currentTargets.contains(info.packageName);
                allApps.add(new TargetAppItem(label, info.packageName, icon, checked));
            }
        }

        Collections.sort(allApps, (a, b) -> a.label.compareToIgnoreCase(b.label));

        final List<TargetAppItem> displayApps = new ArrayList<>(allApps);

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_target_apps_picker, null);
        EditText searchBox = dialogView.findViewById(R.id.target_apps_search);
        ListView listView = dialogView.findViewById(R.id.target_apps_list);

        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return displayApps.size();
            }

            @Override
            public TargetAppItem getItem(int position) {
                return displayApps.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    row = inflater.inflate(R.layout.item_target_app_picker, parent, false);
                }
                TargetAppItem item = getItem(position);
                ImageView iconView = row.findViewById(R.id.app_icon);
                TextView labelView = row.findViewById(R.id.app_label);
                TextView pkgView = row.findViewById(R.id.app_package);
                CheckBox checkBox = row.findViewById(R.id.app_checkbox);

                iconView.setImageDrawable(item.icon);
                labelView.setText(item.label);
                pkgView.setText(item.packageName);
                checkBox.setChecked(item.isChecked);

                return row;
            }
        };

        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            TargetAppItem item = displayApps.get(position);
            item.isChecked = !item.isChecked;
            if (item.isChecked) {
                currentTargets.add(item.packageName);
            } else {
                currentTargets.remove(item.packageName);
            }
            adapter.notifyDataSetChanged();
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                displayApps.clear();
                for (TargetAppItem item : allApps) {
                    if (query.isEmpty() || item.label.toLowerCase(Locale.ROOT).contains(query)
                            || item.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                        displayApps.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        new AlertDialog.Builder(context)
                .setTitle(R.string.target_apps_dialog_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    AutoPifHelper.saveTargetPackages(context, currentTargets);
                    updateTargetAppsSummary(context);
                    Toast.makeText(context,
                            getString(R.string.target_apps_saved, currentTargets.size()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.target_apps_reset_recommended, (dialog, which) -> {
                    AutoPifHelper.saveTargetPackages(context, AutoPifHelper.RECOMMENDED_TARGET_PACKAGES);
                    updateTargetAppsSummary(context);
                    Toast.makeText(context,
                            getString(R.string.target_apps_saved, AutoPifHelper.RECOMMENDED_TARGET_PACKAGES.size()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateTargetAppsSummary(Context context) {
        if (mManageTargetAppsPref != null) {
            int count = AutoPifHelper.getTargetPackages(context).size();
            mManageTargetAppsPref.setSummary(getString(R.string.manage_target_apps_summary, count));
        }
    }

    private void updateHeaderCard() {
        if (mHeaderCardPref == null) return;
        Context context = getContext();
        if (context == null) return;

        TextView profileTitle = (TextView) mHeaderCardPref.findViewById(R.id.spoof_active_profile_title);
        TextView engineMode = (TextView) mHeaderCardPref.findViewById(R.id.spoof_active_engine_mode);
        TextView fingerprintText = (TextView) mHeaderCardPref.findViewById(R.id.spoof_active_fingerprint_text);
        TextView statusBadge = (TextView) mHeaderCardPref.findViewById(R.id.spoof_status_badge);

        boolean hasKeybox = AutoPifHelper.hasKeybox(context);

        if (profileTitle != null) {
            profileTitle.setText(AutoPifHelper.getActiveProfileSummary(context));
        }
        if (engineMode != null) {
            engineMode.setText(AutoPifHelper.getAttestationModeSummary(context));
        }
        if (fingerprintText != null) {
            fingerprintText.setText(AutoPifHelper.getActiveFingerprint(context));
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
