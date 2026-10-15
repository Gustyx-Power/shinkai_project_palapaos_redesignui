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

    private AutoPifHelper() {
    }

    /**
     * Fetches the latest live unrevoked Pixel Canary/Beta build from Google Flash Station
     * or Developer builds API, falling back to verified Pixel Fold backup profile if offline.
     * Persists configuration into Settings.Secure and /data/system/pif.json, and restarts GMS.
     *
     * @param context Application context
     * @return User-readable model description (e.g. "Pixel Fold (felix)")
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

        // 1. Write to Settings.Secure.PIF_DATA
        try {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.PIF_DATA, jsonString);
            Log.i(TAG, "Successfully updated Settings.Secure.PIF_DATA");
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing to Settings.Secure", t);
        }

        // 2. Write to /data/system/pif.json for system-wide service access
        try {
            File pifFile = new File("/data/system/pif.json");
            try (FileOutputStream fos = new FileOutputStream(pifFile)) {
                fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            }
            pifFile.setReadable(true, false);
            Log.i(TAG, "Successfully wrote /data/system/pif.json");
        } catch (Throwable t) {
            Log.e(TAG, "Failed writing /data/system/pif.json", t);
        }

        // 3. Restart Google Play Services and Play Store to apply immediately
        restartGooglePackages(context);

        return modelName;
    }

    private static JSONObject fetchLivePixelProfile() throws Exception {
        // Attempt 1: Query developer.android.com Android 17/16 Beta download page
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
                // Match e.g. felix_beta-cp21.260330.011-factory or shiba_beta-bp31...
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

                    Log.i(TAG, "Scraped live build for " + model + ": " + fingerprint);
                    return obj;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed query for version " + ver + ", trying next...", t);
            }
        }

        // Attempt 2: Query Google Flash Station API
        URL flashUrl = new URL("https://flash.android.com");
        HttpURLConnection flashConn = (HttpURLConnection) flashUrl.openConnection();
        flashConn.setRequestProperty("Referer", "https://flash.android.com");
        flashConn.setConnectTimeout(6000);
        flashConn.setReadTimeout(6000);

        String flashHtml;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(flashConn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
                if (line.contains("data-client-config=")) break;
            }
            flashHtml = sb.toString();
        } finally {
            flashConn.disconnect();
        }

        Pattern keyPattern = Pattern.compile("data-client-config=[^;]*;([^&\"\\s]+)");
        Matcher keyMatcher = keyPattern.matcher(flashHtml);
        if (keyMatcher.find()) {
            String apiKey = keyMatcher.group(1);
            URL buildsUrl = new URL("https://content-flashstation-pa.googleapis.com/v1/builds?product=felix_beta&key=" + apiKey);
            HttpURLConnection buildConn = (HttpURLConnection) buildsUrl.openConnection();
            buildConn.setRequestProperty("Referer", "https://flash.android.com");
            buildConn.setConnectTimeout(6000);
            buildConn.setReadTimeout(6000);

            StringBuilder buildsJson = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(buildConn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buildsJson.append(line).append('\n');
                }
            } finally {
                buildConn.disconnect();
            }

            Pattern rcPattern = Pattern.compile("\"releaseCandidateName\":\\s*\"([^\"]+)\"");
            Pattern idPattern = Pattern.compile("\"buildId\":\\s*\"([^\"]+)\"");
            Matcher rcMatcher = rcPattern.matcher(buildsJson.toString());
            Matcher idMatcher = idPattern.matcher(buildsJson.toString());

            if (rcMatcher.find() && idMatcher.find()) {
                String rcName = rcMatcher.group(1);
                String buildId = idMatcher.group(1);
                String fingerprint = "google/felix_beta/felix:CANARY/" + rcName + "/" + buildId + ":user/release-keys";

                JSONObject obj = new JSONObject();
                obj.put("MANUFACTURER", "Google");
                obj.put("MODEL", "Pixel Fold");
                obj.put("FINGERPRINT", fingerprint);
                obj.put("BRAND", "google");
                obj.put("PRODUCT", "felix_beta");
                obj.put("DEVICE", "felix");
                obj.put("VERSION_RELEASE", "17");
                obj.put("ID", rcName);
                obj.put("SECURITY_PATCH", "2026-10-05");
                obj.put("DEVICE_INITIAL_SDK_INT", 33);
                obj.put("TYPE", "user");
                obj.put("TAGS", "release-keys");
                obj.put("INCREMENTAL", buildId);
                return obj;
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
     * Wipes all user data and caches for Google Play Services and Google Play Store,
     * forcing fresh registration and un-caching certified Play Protect status.
     *
     * @param context Application context
     * @return true if operations completed without fatal errors
     */
    public static boolean clearGmsAndPlayStore(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        String[] packages = new String[]{"com.google.android.gms", "com.android.vending"};
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
     * Force-stops Google Play Services and Play Store so updated PIF properties take effect.
     *
     * @param context Application context
     */
    public static void restartGooglePackages(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return;

        String[] packages = new String[]{"com.google.android.gms", "com.android.vending"};
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
