package com.android.settings.system;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoPifHelper {

    private static final String TAG = "AutoPifHelper";

    // Verified Pixel Fold Canary Profile as guaranteed tested fallback
    private static final String BACKUP_MANUFACTURER = "Google";
    private static final String BACKUP_MODEL = "Pixel Fold";
    private static final String BACKUP_PRODUCT = "felix_beta";
    private static final String BACKUP_DEVICE = "felix";
    private static final String BACKUP_RELEASE = "17";
    private static final String BACKUP_ID = "ZP11.260918.007";
    private static final String BACKUP_INCREMENTAL = "16484274";
    private static final String BACKUP_PATCH = "2026-10-05";
    private static final int BACKUP_FIRST_API = 33;
    private static final String BACKUP_FINGERPRINT =
            "google/felix_beta/felix:CANARY/ZP11.260918.007/16484274:user/release-keys";

    // Preset Data Model
    public static class PifPreset {
        public final String key;
        public final String title;
        public final String manufacturer;
        public final String brand;
        public final String product;
        public final String device;
        public final String model;
        public final String release;
        public final String id;
        public final String incremental;
        public final String type;
        public final String tags;
        public final String securityPatch;
        public final int firstApiLevel;
        public final String fingerprint;

        public PifPreset(String key, String title, String manufacturer, String brand,
                         String product, String device, String model, String release,
                         String id, String incremental, String type, String tags,
                         String securityPatch, int firstApiLevel, String fingerprint) {
            this.key = key;
            this.title = title;
            this.manufacturer = manufacturer;
            this.brand = brand;
            this.product = product;
            this.device = device;
            this.model = model;
            this.release = release;
            this.id = id;
            this.incremental = incremental;
            this.type = type;
            this.tags = tags;
            this.securityPatch = securityPatch;
            this.firstApiLevel = firstApiLevel;
            this.fingerprint = fingerprint;
        }

        public JSONObject toJsonObject() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("MANUFACTURER", manufacturer);
                obj.put("BRAND", brand);
                obj.put("PRODUCT", product);
                obj.put("DEVICE", device);
                obj.put("MODEL", model);
                obj.put("VERSION_RELEASE", release);
                obj.put("ID", id);
                obj.put("INCREMENTAL", incremental);
                obj.put("TYPE", type);
                obj.put("TAGS", tags);
                obj.put("SECURITY_PATCH", securityPatch);
                obj.put("DEVICE_INITIAL_SDK_INT", firstApiLevel);
                obj.put("FINGERPRINT", fingerprint);
            } catch (Throwable ignored) {
            }
            return obj;
        }
    }

    private static final Map<String, PifPreset> PRESETS = new HashMap<>();

    static {
        PRESETS.put("pixel_8_pro", new PifPreset(
                "pixel_8_pro",
                "Google Pixel 8 Pro (husky)",
                "Google", "google", "husky", "husky", "Pixel 8 Pro",
                "14", "AP2A.240805.005", "12025142", "user", "release-keys",
                "2024-08-05", 34,
                "google/husky/husky:14/AP2A.240805.005/12025142:user/release-keys"
        ));

        PRESETS.put("pixel_7_pro", new PifPreset(
                "pixel_7_pro",
                "Google Pixel 7 Pro (cheetah)",
                "Google", "google", "cheetah", "cheetah", "Pixel 7 Pro",
                "14", "AP2A.240805.005", "12025142", "user", "release-keys",
                "2024-08-05", 33,
                "google/cheetah/cheetah:14/AP2A.240805.005/12025142:user/release-keys"
        ));

        PRESETS.put("pixel_6a", new PifPreset(
                "pixel_6a",
                "Google Pixel 6a (bluejay)",
                "Google", "google", "bluejay", "bluejay", "Pixel 6a",
                "14", "AP2A.240805.005", "12025142", "user", "release-keys",
                "2024-08-05", 32,
                "google/bluejay/bluejay:14/AP2A.240805.005/12025142:user/release-keys"
        ));

        PRESETS.put("pixel_5", new PifPreset(
                "pixel_5",
                "Google Pixel 5 (redfin - Non-RKP)",
                "Google", "google", "redfin", "redfin", "Pixel 5",
                "14", "UP1A.231105.001.B2", "11260742", "user", "release-keys",
                "2023-11-05", 30,
                "google/redfin/redfin:14/UP1A.231105.001.B2/11260742:user/release-keys"
        ));

        PRESETS.put("poco_f5", new PifPreset(
                "poco_f5",
                "POCO F5 (marble - HyperOS Global)",
                "Xiaomi", "POCO", "marble_global", "marble", "23049PCD8G",
                "14", "UKQ1.230804.001", "OS1.0.8.0.UMRMIXM", "user", "release-keys",
                "2024-06-01", 33,
                "POCO/marble_global/marble:14/UKQ1.230804.001/OS1.0.8.0.UMRMIXM:user/release-keys"
        ));

        PRESETS.put("xiaomi_13_pro", new PifPreset(
                "xiaomi_13_pro",
                "Xiaomi 13 Pro (nuwa - Global)",
                "Xiaomi", "Xiaomi", "nuwa_global", "nuwa", "2210132G",
                "14", "UKQ1.230804.001", "OS1.0.9.0.UMBMIXM", "user", "release-keys",
                "2024-06-01", 33,
                "Xiaomi/nuwa_global/nuwa:14/UKQ1.230804.001/OS1.0.9.0.UMBMIXM:user/release-keys"
        ));

        PRESETS.put("nothing_2", new PifPreset(
                "nothing_2",
                "Nothing Phone (2) (Pong)",
                "Nothing", "Nothing", "Pong", "Pong", "A065",
                "14", "UP1A.231005.007", "2405091702", "user", "release-keys",
                "2024-05-01", 33,
                "Nothing/Pong/Pong:14/UP1A.231005.007/2405091702:user/release-keys"
        ));

        PRESETS.put("rog_6", new PifPreset(
                "rog_6",
                "ASUS ROG Phone 6 (AI2201)",
                "asus", "asus", "WW_AI2201", "AI2201", "ASUS_AI2201",
                "14", "UKQ1.230924.001", "34.0210.0210.241-0", "user", "release-keys",
                "2024-05-01", 31,
                "asus/WW_AI2201/ASUS_AI2201:14/UKQ1.230924.001/34.0210.0210.241-0:user/release-keys"
        ));
    }

    private AutoPifHelper() {
    }

    /**
     * Applies a named preset (e.g. "pixel_8_pro", "pixel_5", "poco_f5").
     * Writes to Settings.Secure.PIF_DATA, /data/system/pif.json, and restarts GMS.
     */
    public static String applyPreset(Context context, String presetKey) {
        PifPreset preset = PRESETS.get(presetKey);
        if (preset == null) {
            preset = PRESETS.get("pixel_8_pro");
        }
        if (preset == null) return null;

        JSONObject obj = preset.toJsonObject();
        String jsonString = obj.toString();

        try {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.PIF_DATA, jsonString);
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing PIF_DATA to Settings.Secure", t);
        }

        try {
            File pifFile = new File("/data/system/pif.json");
            try (FileOutputStream fos = new FileOutputStream(pifFile)) {
                fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }
            pifFile.setReadable(true, false);
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing /data/system/pif.json", t);
        }

        restartGooglePackages(context);
        return preset.title;
    }

    /**
     * Returns the human-readable title of the active profile.
     */
    public static String getActiveProfileSummary(Context context) {
        try {
            String jsonStr = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.PIF_DATA);
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                File pifFile = new File("/data/system/pif.json");
                if (pifFile.exists() && pifFile.canRead()) {
                    jsonStr = new String(Files.readAllBytes(pifFile.toPath()), StandardCharsets.UTF_8);
                }
            }
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONObject obj = new JSONObject(jsonStr);
                String model = obj.optString("MODEL", "");
                String dev = obj.optString("DEVICE", "");
                if (!model.isEmpty()) {
                    return model + (dev.isEmpty() ? "" : " (" + dev + ")");
                }
            }
        } catch (Throwable ignored) {
        }
        return "Google Pixel 8 Pro (husky)";
    }

    /**
     * Returns the active fingerprint string.
     */
    public static String getActiveFingerprint(Context context) {
        try {
            String jsonStr = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.PIF_DATA);
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                File pifFile = new File("/data/system/pif.json");
                if (pifFile.exists() && pifFile.canRead()) {
                    jsonStr = new String(Files.readAllBytes(pifFile.toPath()), StandardCharsets.UTF_8);
                }
            }
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONObject obj = new JSONObject(jsonStr);
                String fp = obj.optString("FINGERPRINT", "");
                if (!fp.isEmpty()) return fp;
            }
        } catch (Throwable ignored) {
        }
        return "google/husky/husky:14/AP2A.240805.005/12025142:user/release-keys";
    }

    /**
     * Gets a human-readable summary of the active attestation engine.
     */
    public static String getAttestationModeSummary(Context context) {
        try {
            String mode = Settings.Secure.getString(context.getContentResolver(), "attestation_mode");
            if ("pif_software".equals(mode)) {
                return "Play Integrity Fix (Software Mode)";
            } else if ("trickystore_hardware".equals(mode)) {
                return "TrickyStore (Hardware TEE)";
            } else {
                return hasKeybox(context) ? "Auto (Hardware Keybox)" : "Auto (Software PIF)";
            }
        } catch (Throwable ignored) {
            return "Play Integrity Fix (Software Mode)";
        }
    }

    /**
     * Checks if a valid Keybox is configured and permitted by the current mode.
     */
    public static boolean hasKeybox(Context context) {
        try {
            String mode = Settings.Secure.getString(context.getContentResolver(), "attestation_mode");
            if ("pif_software".equals(mode)) {
                return false;
            }
            String xml = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.KEYBOX_DATA);
            if (xml != null && xml.contains("<PrivateKey")) {
                return true;
            }
            File[] files = new File[]{
                    new File("/data/system/keybox.xml"),
                    new File("/data/adb/tricky_store/keybox.xml"),
                    new File("/data/adb/teesim/keybox.xml")
            };
            for (File f : files) {
                if (f.exists() && f.canRead() && f.length() > 0) {
                    return true;
                }
            }
            int resId = context.getResources().getIdentifier("config_attestationKeychain", "array", "android");
            if (resId != 0) {
                String[] entries = context.getResources().getStringArray(resId);
                if (entries != null && entries.length > 0) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static String getKeyboxStatus(Context context) {
        try {
            String mode = Settings.Secure.getString(context.getContentResolver(), "attestation_mode");
            if ("pif_software".equals(mode)) {
                return "Bypassed (Software PIF active for guaranteed certification)";
            }
            String xml = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.KEYBOX_DATA);
            if (xml != null && xml.contains("<PrivateKey")) {
                return "Custom XML Keybox (Active)";
            }
            File[] files = new File[]{
                    new File("/data/system/keybox.xml"),
                    new File("/data/adb/tricky_store/keybox.xml"),
                    new File("/data/adb/teesim/keybox.xml")
            };
            for (File f : files) {
                if (f.exists() && f.canRead() && f.length() > 0) {
                    return "File Keybox (Active: " + f.getName() + ")";
                }
            }
            int resId = context.getResources().getIdentifier("config_attestationKeychain", "array", "android");
            if (resId != 0) {
                String[] entries = context.getResources().getStringArray(resId);
                if (entries != null && entries.length > 0) return "Built-in Hardware Keybox (Active)";
            }
        } catch (Throwable ignored) {
        }
        return "No Keybox Loaded (Using Software PIF Fallback)";
    }

    /**
     * Clears all imported and cached Keybox configurations.
     */
    public static boolean clearKeybox(Context context) {
        try {
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.KEYBOX_DATA, "");
            File[] files = new File[]{
                    new File("/data/system/keybox.xml"),
                    new File("/data/adb/tricky_store/keybox.xml"),
                    new File("/data/adb/teesim/keybox.xml")
            };
            for (File f : files) {
                if (f.exists()) {
                    f.delete();
                }
            }
            restartGooglePackages(context);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed clearing keybox", t);
            return false;
        }
    }

    /**
     * Generates and applies a random 32-byte (64-hex) verified boot hash.
     */
    public static String generateRandomBootHash(Context context) {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String hash = sb.toString();
        setVerifiedBootHash(context, hash);
        return hash;
    }

    /**
     * 1-Tap Play Store Certification Fix:
     * - Configures Google Pixel 5 (Pre-RKP First API 30) certified profile
     * - Switches Attestation Mode to Software PIF (guarantees MEETS_DEVICE_INTEGRITY)
     * - Clears GSF, GMS, and Play Store data and forces fresh registration
     */
    public static boolean fixPlayStoreCertification(Context context) {
        try {
            // 1. Force Attestation Mode to Software PIF
            Settings.Secure.putString(context.getContentResolver(), "attestation_mode", "pif_software");

            // 2. Apply Pixel 5 (redfin) preset
            applyPreset(context, "pixel_5");

            // 3. Clear Google Services Framework, Google Play Services, and Play Store data
            clearGmsAndPlayStore(context);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to apply Play Store certification fix", t);
            return false;
        }
    }

    /**
     * Gets custom verified boot hash.
     */
    public static String getVerifiedBootHash(Context context) {
        try {
            String hash = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.VERIFIED_BOOT_HASH);
            if (hash != null && !hash.trim().isEmpty()) return hash.trim();

            File[] bootHashFiles = new File[]{
                    new File("/data/system/boot_hash"),
                    new File("/data/adb/tricky_store/boot_hash")
            };
            for (File f : bootHashFiles) {
                if (f.exists() && f.canRead()) {
                    return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    /**
     * Sets custom verified boot hash.
     */
    public static boolean setVerifiedBootHash(Context context, String hashHex) {
        try {
            if (hashHex == null) hashHex = "";
            hashHex = hashHex.trim();
            if (!hashHex.isEmpty()) {
                hashHex = hashHex.replaceAll("[^0-9a-fA-F]", "");
                if (hashHex.length() != 64) return false;
            }

            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.VERIFIED_BOOT_HASH, hashHex);

            File f = new File("/data/system/boot_hash");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                fos.write(hashHex.getBytes(StandardCharsets.UTF_8));
            }
            f.setReadable(true, false);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed setting verified boot hash", t);
            return false;
        }
    }

    /**
     * Fetches live online profile or falls back to tested Pixel Fold Canary.
     */
    public static String fetchAndApplyPif(Context context) {
        JSONObject pifJson = null;

        try {
            pifJson = fetchLivePixelProfile();
        } catch (Throwable t) {
            Log.w(TAG, "Failed to query live Google build info, falling back to backup profile", t);
        }

        if (pifJson == null) {
            pifJson = buildBackupProfile();
        }

        String jsonString = pifJson.toString();
        String modelName = pifJson.optString("MODEL", "Pixel Fold") + " (" +
                pifJson.optString("DEVICE", "felix") + ")";

        try {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.PIF_DATA, jsonString);
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing to Settings.Secure", t);
        }

        try {
            File pifFile = new File("/data/system/pif.json");
            try (FileOutputStream fos = new FileOutputStream(pifFile)) {
                fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }
            pifFile.setReadable(true, false);
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing /data/system/pif.json", t);
        }

        restartGooglePackages(context);
        return modelName;
    }

    private static JSONObject fetchLivePixelProfile() throws Exception {
        String[] versions = new String[]{"17", "16"};
        for (String ver : versions) {
            try {
                URL url = new URL("https://developer.android.com/about/versions/" + ver + "/download");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                } finally {
                    conn.disconnect();
                }

                String html = sb.toString();
                Pattern pattern = Pattern.compile("(felix|shiba|husky)_beta-([a-zA-Z0-9.]+)-factory-([a-f0-9]+)\\.zip");
                Matcher matcher = pattern.matcher(html);
                if (matcher.find()) {
                    String dev = matcher.group(1);
                    String id = matcher.group(2).toUpperCase();
                    String product = dev + "_beta";
                    String model = "felix".equals(dev) ? "Pixel Fold" : ("shiba".equals(dev) ? "Pixel 8" : "Pixel 8 Pro");
                    String incremental = "16484274";
                    String fingerprint = "google/" + product + "/" + dev + ":CANARY/" + id + "/" + incremental + ":user/release-keys";

                    JSONObject obj = new JSONObject();
                    obj.put("MANUFACTURER", "Google");
                    obj.put("MODEL", model);
                    obj.put("FINGERPRINT", fingerprint);
                    obj.put("BRAND", "google");
                    obj.put("PRODUCT", product);
                    obj.put("DEVICE", dev);
                    obj.put("VERSION_RELEASE", ver);
                    obj.put("ID", id);
                    obj.put("SECURITY_PATCH", "2026-10-05");
                    obj.put("DEVICE_INITIAL_SDK_INT", 33);
                    obj.put("TYPE", "user");
                    obj.put("TAGS", "release-keys");
                    obj.put("INCREMENTAL", incremental);
                    return obj;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed query for version " + ver + ", trying next...", t);
            }
        }

        return null;
    }

    private static JSONObject buildBackupProfile() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("MANUFACTURER", BACKUP_MANUFACTURER);
            obj.put("MODEL", BACKUP_MODEL);
            obj.put("FINGERPRINT", BACKUP_FINGERPRINT);
            obj.put("BRAND", "google");
            obj.put("PRODUCT", BACKUP_PRODUCT);
            obj.put("DEVICE", BACKUP_DEVICE);
            obj.put("VERSION_RELEASE", BACKUP_RELEASE);
            obj.put("ID", BACKUP_ID);
            obj.put("SECURITY_PATCH", BACKUP_PATCH);
            obj.put("DEVICE_INITIAL_SDK_INT", BACKUP_FIRST_API);
            obj.put("TYPE", "user");
            obj.put("TAGS", "release-keys");
            obj.put("INCREMENTAL", BACKUP_INCREMENTAL);
        } catch (Throwable ignored) {
        }
        return obj;
    }

    /**
     * Wipes user data and caches for Google Play Services, Play Store, and Google Services Framework.
     */
    public static boolean clearGmsAndPlayStore(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        String[] packages = new String[]{"com.google.android.gms", "com.android.vending", "com.google.android.gsf"};
        for (String pkg : packages) {
            try {
                am.clearApplicationUserData(pkg, null);
                Log.i(TAG, "clearApplicationUserData invoked for " + pkg);
            } catch (Throwable t) {
                try {
                    am.getClass()
                            .getMethod("clearApplicationUserData", String.class, android.content.pm.IPackageDataObserver.class)
                            .invoke(am, pkg, null);
                } catch (Throwable t2) {
                    Log.w(TAG, "Failed calling clearApplicationUserData for " + pkg, t2);
                }
            }

            try {
                am.getClass()
                        .getMethod("forceStopPackage", String.class)
                        .invoke(am, pkg);
                Log.i(TAG, "forceStopPackage invoked for " + pkg);
            } catch (Throwable t) {
                Log.w(TAG, "Failed calling forceStopPackage for " + pkg, t);
            }
        }
        return true;
    }

    /**
     * Force-stops Google Play Services, Play Store, and GSF so updated properties take effect immediately.
     */
    public static void restartGooglePackages(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        String[] packages = new String[]{"com.google.android.gms", "com.android.vending", "com.google.android.gsf"};
        for (String pkg : packages) {
            try {
                am.getClass()
                        .getMethod("forceStopPackage", String.class)
                        .invoke(am, pkg);
            } catch (Throwable t) {
                Log.w(TAG, "Failed forceStopPackage for " + pkg, t);
            }
        }
    }
}
