/*
 * Copyright (C) 2026 Shinkai Project / Wowo OS
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

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.core.TogglePreferenceController;

import java.util.List;

public class OneUIQSStylePreferenceController extends TogglePreferenceController {

    private static final String TAG = "OneUIQSStyle";
    public static final String SETTING_KEY = "qs_oneui_style";

    public OneUIQSStylePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(), SETTING_KEY, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean success = Settings.System.putInt(mContext.getContentResolver(), SETTING_KEY, isChecked ? 1 : 0);
        restartSystemUI(mContext);
        return success;
    }

    public static void restartSystemUI(Context context) {
        if (context == null) return;

        // 1. Direct broadcast to SysuiRestartReceiver (triggers Process.killProcess(Process.myPid()) directly inside SystemUI)
        try {
            Intent intent = new Intent("com.android.systemui.action.RESTART");
            intent.setPackage("com.android.systemui");
            context.sendBroadcastAsUser(intent, UserHandle.ALL);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to broadcast RESTART to SystemUI", t);
        }

        // 2. Kill persistent process via ActivityManagerService with accurate package UID
        try {
            PackageManager pm = context.getPackageManager();
            int uid = pm.getPackageUid("com.android.systemui", 0);
            ActivityManager.getService().killApplicationProcess("com.android.systemui", uid);
        } catch (Throwable t) {
            Log.e(TAG, "Failed killApplicationProcess", t);
        }

        // 3. Official AMS killUid mechanism for persistent packages
        try {
            PackageManager pm = context.getPackageManager();
            int uid = pm.getPackageUid("com.android.systemui", 0);
            int appId = UserHandle.getAppId(uid);
            ActivityManager.getService().killUid(appId, UserHandle.USER_SYSTEM, "Restart SystemUI from Settings");
        } catch (Throwable t) {
            Log.e(TAG, "Failed killUid", t);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}

