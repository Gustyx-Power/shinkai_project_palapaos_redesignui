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

package com.android.settings.gestures;

import static android.os.UserHandle.USER_CURRENT;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.settings.core.TogglePreferenceController;

public class GestureHideImeSpacePreferenceController extends TogglePreferenceController {

    private static final String OVERLAY_PACKAGE_IME =
            "com.android.internal.systemui.navbar.gestural.hidden_ime";

    private final IOverlayManager mOverlayManager;

    public GestureHideImeSpacePreferenceController(Context context, String key) {
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
            OverlayInfo info = mOverlayManager.getOverlayInfo(OVERLAY_PACKAGE_IME, USER_CURRENT);
            return info != null && info.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mOverlayManager == null) {
            return false;
        }
        try {
            mOverlayManager.setEnabled(OVERLAY_PACKAGE_IME, isChecked, USER_CURRENT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }
}
