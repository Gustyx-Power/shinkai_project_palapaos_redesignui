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
import android.os.Process;
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
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo proc : processes) {
                        if ("com.android.systemui".equals(proc.processName)) {
                            Process.killProcess(proc.pid);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to kill systemui via ActivityManager", t);
        }

        try {
            ActivityManager.getService().killApplicationProcess("com.android.systemui", -1);
        } catch (Throwable ignored) {}

        try {
            Runtime.getRuntime().exec(new String[]{"pkill", "-f", "com.android.systemui"});
        } catch (Throwable ignored) {}
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}

