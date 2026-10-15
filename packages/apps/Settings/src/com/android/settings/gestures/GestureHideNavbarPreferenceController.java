/*
 * Copyright (C) 2026 Shinkai Project / Palapa OS
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

package com.android.settings.gestures;

import static android.os.UserHandle.USER_CURRENT;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.settings.core.TogglePreferenceController;

public class GestureHideNavbarPreferenceController extends TogglePreferenceController {

    private static final String OVERLAY_PACKAGE_BAR =
            "com.android.internal.systemui.navbar.gestural.hidden_bar";
    private static final String OVERLAY_PACKAGE_SYSUI =
            "com.android.systemui.navbar.gestural.hidden_bar";
    private static final String OVERLAY_PACKAGE_LAUNCHER =
            "com.google.android.apps.nexuslauncher.navbar.gestural.hidden_bar";
    private static final String OVERLAY_PACKAGE_LAUNCHER3 =
            "com.android.launcher3.navbar.gestural.hidden_bar";

    private final IOverlayManager mOverlayManager;

    public GestureHideNavbarPreferenceController(Context context, String key) {
        super(context, key);
        mOverlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
    }

    @Override
    public int getAvailabilityStatus() {
        return SystemNavigationPreferenceController.isGestureNavigationEnabled(mContext)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        if (mOverlayManager == null) {
            return false;
        }
        try {
            OverlayInfo infoLauncher = mOverlayManager.getOverlayInfo(OVERLAY_PACKAGE_LAUNCHER, USER_CURRENT);
            if (infoLauncher != null && infoLauncher.isEnabled()) {
                return true;
            }
            OverlayInfo infoSysui = mOverlayManager.getOverlayInfo(OVERLAY_PACKAGE_SYSUI, USER_CURRENT);
            if (infoSysui != null && infoSysui.isEnabled()) {
                return true;
            }
            OverlayInfo infoBar = mOverlayManager.getOverlayInfo(OVERLAY_PACKAGE_BAR, USER_CURRENT);
            return infoBar != null && infoBar.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mOverlayManager == null) {
            return false;
        }
        boolean anySuccess = false;
        try {
            mOverlayManager.setEnabled(OVERLAY_PACKAGE_LAUNCHER, isChecked, USER_CURRENT);
            anySuccess = true;
        } catch (Exception e) {
            // Ignored
        }
        try {
            mOverlayManager.setEnabled(OVERLAY_PACKAGE_LAUNCHER3, isChecked, USER_CURRENT);
            anySuccess = true;
        } catch (Exception e) {
            // Ignored
        }
        try {
            mOverlayManager.setEnabled(OVERLAY_PACKAGE_SYSUI, isChecked, USER_CURRENT);
            anySuccess = true;
        } catch (Exception e) {
            // Ignored
        }
        try {
            mOverlayManager.setEnabled(OVERLAY_PACKAGE_BAR, isChecked, USER_CURRENT);
            anySuccess = true;
        } catch (Exception e) {
            // Ignored
        }
        return anySuccess;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
