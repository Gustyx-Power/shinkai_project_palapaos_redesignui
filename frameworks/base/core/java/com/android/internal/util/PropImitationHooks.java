/*
 * Copyright (C) 2022-2024 Paranoid Android
 *           (C) 2023 ArrowOS
 *           (C) 2023 The LibreMobileOS Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.ActivityTaskManager;
import android.app.Application;
import android.app.TaskStackListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Binder;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.R;
import com.android.internal.util.custom.KeyProviderManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import sun.misc.Unsafe;

/**
 * @hide
 */
public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Boolean sDisableGmsProps = SystemProperties.getBoolean(
            "persist.sys.pihooks.disable.gms_props", false);

    private static final Boolean sDisableKeyAttestationBlock = SystemProperties.getBoolean(
            "persist.sys.pihooks.disable.gms_key_attestation_block", false);
    private static final String DATA_FILE = "gms_certified_props.json";

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";
    private static final String PACKAGE_NETFLIX = "com.netflix.mediaclient";

    private static final ComponentName GMS_ADD_ACCOUNT_ACTIVITY = ComponentName.unflattenFromString(
            "com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity");

    private static volatile List<String> sCertifiedProps = new ArrayList<>();
    private static volatile String sStockFp, sNetflixModel;

    private static volatile String sProcessName;
    private static volatile boolean sIsGms, sIsFinsky, sIsPhotos;

    private static final Field OFFSET_FIELD;
    private static final Unsafe UNSAFE;

    static {
        Unsafe unsafe = null;
        Field offsetField = null;

        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            field.setAccessible(false);

            offsetField = Field.class.getDeclaredField("offset");
            offsetField.setAccessible(true);
        } catch (Exception e) {
            Log.e(TAG, "Unable to initialize Unsafe", e);
        }

        UNSAFE = unsafe;
        OFFSET_FIELD = offsetField;
    }

    public static void setProps(Context context) {
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName)) {
            Log.e(TAG, "Null package or process name");
            return;
        }

        final Resources res = context.getResources();
        if (res == null) {
            Log.e(TAG, "Null resources");
            return;
        }

        sStockFp = res.getString(R.string.config_originalDeviceBuildTag);
        sNetflixModel = res.getString(R.string.config_movieVerifiedKey);

        sProcessName = processName;
        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);

        /* Set Certified Properties for GMSCore
         * Set Stock Fingerprint for ARCore
         * Set custom model for Netflix
         */
        if (sIsGms || sIsFinsky) {
            if (!android.os.Process.isIsolated()) {
                setPlayIntegrityProps(context);
            } else {
                dlog("Not setting Play Integrity props in isolated process");
            }
            if (sIsGms) {
                spoofSignature();
            }
        } else if (!sStockFp.isEmpty() && packageName.equals(PACKAGE_ARCORE)) {
            dlog("Setting stock fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sStockFp);
        } else if (!sNetflixModel.isEmpty() && packageName.equals(PACKAGE_NETFLIX)) {
            dlog("Setting model to " + sNetflixModel + " for Netflix");
            setPropValue("MODEL", sNetflixModel);
        }
    }

    private static void setPropValue(String key, String value) {
        if (UNSAFE == null || OFFSET_FIELD == null) {
            Log.e(TAG, "Unsafe is unavailable", new IllegalStateException());
            return;
        }

        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Class clazz = Build.class;
            if (key.startsWith("VERSION.")) {
                clazz = Build.VERSION.class;
                key = key.substring(8);
            } else if (key.equals("SECURITY_PATCH") || key.equals("DEVICE_INITIAL_SDK_INT") || key.equals("RELEASE") || key.equals("SDK_INT") || key.equals("INCREMENTAL")) {
                clazz = Build.VERSION.class;
            } else if (key.equals("VERSION_RELEASE")) {
                clazz = Build.VERSION.class;
                key = "RELEASE";
            }
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            // Cast the value to int if it's an integer field, long if long field, otherwise string.
            long offset = OFFSET_FIELD.getInt(field);
            if (field.getType().equals(Integer.TYPE)) {
                UNSAFE.putInt(field.getDeclaringClass(), offset, Integer.parseInt(value));
            } else if (field.getType().equals(Long.TYPE)) {
                UNSAFE.putLong(field.getDeclaringClass(), offset, Long.parseLong(value));
            } else {
                UNSAFE.putObject(field.getDeclaringClass(), offset, value);
            }
            field.setAccessible(false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setPlayIntegrityProps(Context context) {
        if (sDisableGmsProps) {
            dlog("GMS prop imitation is disabled by user");
            return;
        }

        // Guard: isolated processes cannot access content providers (Settings.*).
        if (android.os.Process.isIsolated()) {
            dlog("Skipping setPlayIntegrityProps in isolated process");
            return;
        }

        String savedProps = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.PIF_DATA);
        if (savedProps == null || TextUtils.isEmpty(savedProps)) {
            savedProps = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.FETCHED_PIF);
        }

        if (savedProps == null || TextUtils.isEmpty(savedProps)) {
            final File[] pifFiles = new File[] {
                new File("/data/system/pif.json"),
                new File("/data/adb/pif.json"),
                new File("/data/system/gms_certified_props.json")
            };
            for (File pifFile : pifFiles) {
                if (pifFile.exists() && pifFile.canRead()) {
                    savedProps = readFromFile(pifFile);
                    if (savedProps != null && !savedProps.trim().isEmpty()) {
                        dlog("Loaded PIF props from file: " + pifFile.getAbsolutePath());
                        break;
                    }
                }
            }
        }

        if (savedProps == null || TextUtils.isEmpty(savedProps)) {
            dlog("Parsing props locally - fetched pif / user provided pif unavailable");
            sCertifiedProps = Arrays.asList(context.getResources().getStringArray(R.array.config_verifiedDeviceSpecs));
        } else {
            dlog("Parsing props fetched / provided by user");
            try {
                JSONObject parsedProps = new JSONObject(savedProps);
                Iterator<String> keys = parsedProps.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = parsedProps.getString(key);
                    sCertifiedProps.add(key + ":" + value);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON data", e);
                dlog("Parsing props locally as fallback");
                sCertifiedProps = Arrays.asList(context.getResources().getStringArray(R.array.config_verifiedDeviceSpecs));
            }
        }

        if (sCertifiedProps.isEmpty()) {
            dlog("Certified props are not set");
            return;
        }

        if (sIsGms) {
            final boolean was = isGmsAddAccountActivityOnTop();
            final TaskStackListener taskStackListener = new TaskStackListener() {
                @Override
                public void onTaskStackChanged() {
                    final boolean is = isGmsAddAccountActivityOnTop();
                    if (is ^ was) {
                        dlog("GmsAddAccountActivityOnTop is:" + is + " was:" + was +
                                ", killing myself!"); // process will restart automatically later
                        Process.killProcess(Process.myPid());
                    }
                }
            };

            if (!was) {
                dlog("Spoofing build for GMS");
                setCertifiedProps();
            } else {
                dlog("Skip spoofing build for GMS, because GmsAddAccountActivityOnTop");
            }

            try {
                ActivityTaskManager.getService().registerTaskStackListener(taskStackListener);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register task stack listener!", e);
            }
        } else {
            dlog("Spoofing build for Finsky");
            setCertifiedProps();
        }
    }

    private static void setCertifiedProps() {
        for (String entry : sCertifiedProps) {
            // Each entry must be of the format FIELD:value
            final String[] fieldAndProp = entry.split(":", 2);
            if (fieldAndProp.length != 2) {
                Log.e(TAG, "Invalid entry in certified props: " + entry);
                continue;
            }
            setPropValue(fieldAndProp[0], fieldAndProp[1]);
        }
    }

    private static String readFromFile(File file) {
        StringBuilder content = new StringBuilder();

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from file", e);
            }
        }
        return content.toString();
    }

    private static boolean isGmsAddAccountActivityOnTop() {
        try {
            final ActivityTaskManager.RootTaskInfo focusedTask =
                    ActivityTaskManager.getService().getFocusedRootTaskInfo();
            return focusedTask != null && focusedTask.topActivity != null
                    && focusedTask.topActivity.equals(GMS_ADD_ACCOUNT_ACTIVITY);
        } catch (Exception e) {
            Log.e(TAG, "Unable to get top activity!", e);
        }
        return false;
    }

    public static boolean shouldBypassTaskPermission(Context context) {
        if (sDisableGmsProps) {
            return false;
        }

        // GMS doesn't have MANAGE_ACTIVITY_TASKS permission
        final int callingUid = Binder.getCallingUid();
        final int gmsUid;
        try {
            gmsUid = context.getPackageManager().getApplicationInfo(PACKAGE_GMS, 0).uid;
            dlog("shouldBypassTaskPermission: gmsUid:" + gmsUid + " callingUid:" + callingUid);
        } catch (Exception e) {
            Log.e(TAG, "shouldBypassTaskPermission: unable to get gms uid", e);
            return false;
        }
        return gmsUid == callingUid;
    }

    public static boolean isGms() {
        if (sIsGms) return true;
        try {
            String processName = Application.getProcessName();
            return PROCESS_GMS_UNSTABLE.equals(processName);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isCallerPlayIntegrity() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::getClassName)
                .anyMatch(name -> name.toLowerCase(Locale.US).contains("droidguard"));
    }

    public static void onEngineGetCertificateChain() {
        if (sDisableKeyAttestationBlock) {
            dlog("Key attestation blocking is disabled by user");
            return;
        }

        // Inside GMS unstable (DroidGuard worker), always block key attestation so it
        // cleanly falls back to software-backed attestation (MEETS_DEVICE_INTEGRITY).
        if (sIsGms || isCallerPlayIntegrity()) {
            dlog("Blocked key attestation for play integrity (sIsGms=" + sIsGms + ")");
            throw new UnsupportedOperationException("Play integrity fallback to software attestation");
        }

        // If a keybox is found, don't block key attestation for other apps (e.g. Key Attestation app)
        if (KeyProviderManager.isKeyboxAvailable()) {
            dlog("Key attestation blocking is disabled because a keybox is defined to spoof");
            return;
        }
    }

    private static final String GOOGLE_RELEASE_SIGNATURE =
            "MIIFyTCCA7GgAwIBAgIVALyxxl+zDS9SL68SzOr48309eAZyMA0GCSqGSIb3DQEBCwUAMHQxCzAJ" +
            "BgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQw" +
            "EgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAg" +
            "Fw0yMjExMDExODExMzVaGA8yMDUyMTEwMTE4MTEzNVowdDELMAkGA1UEBhMCVVMxEzARBgNVBAgT" +
            "CkNhbGlmb3JuaWExFjAUBgNVBAcTDU1vdW50YWluIFZpZXcxFDASBgNVBAoTC0dvb2dsZSBJbmMu" +
            "MRAwDgYDVQQLEwdBbmRyb2lkMRAwDgYDVQQDEwdBbmRyb2lkMIICIjANBgkqhkiG9w0BAQEFAAOC" +
            "Ag8AMIICCgKCAgEAsqtalIy/nctKlrhd1UVoDffFGnDf9GLi0QQhsVoJkfF16vDDydZJOycG7/kQ" +
            "ziRZhFdcoMrIYZzzw0ppBjsSe1AiWMuKXwTBaEtxN99S1xsJiW4/QMI6N6kMunydWRMsbJ6aAxi1" +
            "lVq0bxSwr8Sg/8u9HGVivfdG8OpUM+qjuV5gey5xttNLK3BZDrAlco8RkJZryAD40flmJZrWXJmc" +
            "r2HhJJUnqG4Z3MSziEgW1u1JnnY3f/BFdgYsA54SgdUGdQP3aqzSjIpGK01/vjrXvifHazSANjvl" +
            "0AUE5i6AarMw2biEKB2ySUDp8idC5w12GpqDrhZ/QkW8yBSa87KbkMYXuRA2Gq1fYbQx3YJraw0U" +
            "gZ4M3fFKpt6raxxM5j0sWHlULD7dAZMERvNESVrKG3tQ7B39WAD8QLGYc45DFEGOhKv5Fv8510h5" +
            "sXK502IvGpI4FDwz2rbtAgJ0j+16db5wCSW5ThvNPhCheyciajc8dU1B5tJzZN/ksBpzne4Xf9gO" +
            "LZ9ZU0+3Z5gHVvTS/YpxBFwiFpmL7dvGxew0cXGSsG5UTBlgr7i0SX0WhY4Djjo8IfPwrvvA0QaC" +
            "FamdYXKqBsSHgEyXS9zgGIFPt2jWdhaS+sAa//5SXcWro0OdiKPuwEzLgj759ke1sHRnvO735dYn" +
            "5whVbzlGyLBh3L0CAwEAAaNQME4wDAYDVR0TBAUwAwEB/zAdBgNVHQ4EFgQUU1eXQ7NoYKjvOQlh" +
            "5V8jHQMoxA8wHwYDVR0jBBgwFoAUU1eXQ7NoYKjvOQlh5V8jHQMoxA8wDQYJKoZIhvcNAQELBQAD" +
            "ggIBAHFIazRLs3itnZKllPnboSd6sHbzeJURKehx8GJPvIC+xWlwWyFO5+GHmgc3yh/SVd3Xja/k" +
            "8Ud59WEYTjyJJWTw0Jygx37rHW7VGn2HDuy/x0D+els+S8HeLD1toPFMepjIXJn7nHLhtmzTPlDW" +
            "DrhiaYsls/k5Izf89xYnI4euuOY2+1gsweJqFGfbznqyqy8xLyzoZ6bvBJtgeY+G3i/9Be14HseS" +
            "Na4FvI1Oze/l2gUu1IXzN6DGWR/lxEyt+TncJfBGKbjafYrfSh3zsE4N3TU7BeOL5INirOMjre/j" +
            "VgB1YQG5qLVaPoz6mdn75AbBBm5a5ahApLiKqzy/hP+1rWgw8Ikb7vbUqov/bnY3IlIU6XcPJTCD" +
            "b9aRZQkStvYpQd82XTyxD/T0GgRLnUj5Uv6iZlikFx1KNj0YNS2T3gyvL++J9B0Y6gAkiG0EtNpl" +
            "z7Pomsv5pVdmHVdKMjqWw5/6zYzVmu5cXFtR384Ti1qwML1xkD6TC3VIv88rKIEjrkY2c+v1frh9" +
            "fRJ2OmzXmML9NgHTjEiJR2Ib2iNrMKxkuTIs9oxKZgrJtJKvdU9qJJKM5PnZuNuHhGs6A/9gt9Oc" +
            "cetYeQvVSqeEmQluWfcunQn9C9Vwi2BJIiVJh4IdWZf5/e2PlSSQ9CJjz2bKI17pzdxOmjQfE0JS" +
            "F7Xt";

    private static void spoofSignature() {
        try {
            final Signature spoofedSignature = new Signature(android.util.Base64.decode(
                    GOOGLE_RELEASE_SIGNATURE, android.util.Base64.DEFAULT));
            final Parcelable.Creator<PackageInfo> originalCreator = PackageInfo.CREATOR;
            final Parcelable.Creator<PackageInfo> customCreator = new Parcelable.Creator<PackageInfo>() {
                @Override
                public PackageInfo createFromParcel(Parcel source) {
                    PackageInfo packageInfo = originalCreator.createFromParcel(source);
                    if (packageInfo != null && "android".equals(packageInfo.packageName)) {
                        if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                            packageInfo.signatures[0] = spoofedSignature;
                        }
                        if (packageInfo.signingInfo != null) {
                            Signature[] signers = packageInfo.signingInfo.getApkContentsSigners();
                            if (signers != null && signers.length > 0) {
                                signers[0] = spoofedSignature;
                            }
                            Signature[] history = packageInfo.signingInfo.getSigningCertificateHistory();
                            if (history != null && history.length > 0) {
                                history[0] = spoofedSignature;
                            }
                        }
                    }
                    return packageInfo;
                }

                @Override
                public PackageInfo[] newArray(int size) {
                    return originalCreator.newArray(size);
                }
            };

            Field creatorField = PackageInfo.class.getDeclaredField("CREATOR");
            creatorField.setAccessible(true);
            if (UNSAFE != null && OFFSET_FIELD != null) {
                long offset = OFFSET_FIELD.getInt(creatorField);
                UNSAFE.putObject(creatorField.getDeclaringClass(), offset, customCreator);
            } else {
                creatorField.set(null, customCreator);
            }
            dlog("Successfully spoofed PackageInfo.CREATOR for GMS unstable");

            try {
                Field cacheField = PackageManager.class.getDeclaredField("sPackageInfoCache");
                cacheField.setAccessible(true);
                Object cache = cacheField.get(null);
                if (cache != null) {
                    Method clearMethod = cache.getClass().getMethod("clear");
                    clearMethod.invoke(cache);
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to spoof PackageInfo signature for GMS", t);
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}
