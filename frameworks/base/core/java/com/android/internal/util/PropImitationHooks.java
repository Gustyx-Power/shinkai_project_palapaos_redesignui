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
import android.os.Build;
import android.os.Binder;
import android.os.Environment;
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

        // Check stack for Play Integrity - block DroidGuard so it falls back to software attestation
        if (isCallerPlayIntegrity()) {
            dlog("Blocked key attestation for play integrity");
            throw new UnsupportedOperationException();
        }

        // If a keybox is found, don't block key attestation for other apps (e.g. Key Attestation app)
        if (KeyProviderManager.isKeyboxAvailable()) {
            dlog("Key attestation blocking is disabled because a keybox is defined to spoof");
            return;
        }
    }

    public static void dlog(String msg) {
        if (DEBUG) Log.d(TAG, "[" + sProcessName + "] " + msg);
    }
}
