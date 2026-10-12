/*
 * Copyright (C) 2026 Wowo OS / Shinkai Project
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

package com.android.settings.deviceinfo.firmwareversion

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.SystemProperties
import android.text.BidiFormatter
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.preference.Preference
import com.android.internal.app.PlatLogoActivity
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.utils.getLocale
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.development.DevelopmentSettingsEnabler
import com.android.settingslib.utils.StringUtil
import com.android.settingslib.widget.LayoutPreference

class ShinkaiLogoPreference : PreferenceMetadata, PreferenceBinding {

    private val hits = LongArray(3)

    override val key: String
        get() = "shinkai_logo"

    override val purpose: Int
        get() = R.string.shinkai_logo_purpose

    override fun createWidget(context: Context): Preference =
        LayoutPreference(context, R.layout.shinkai_logo).apply {
            isSelectable = false
            isAllowDividerBelow = false
            populateViews(this)
        }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is LayoutPreference) {
            populateViews(preference)
        }
    }

    private fun populateViews(layoutPreference: LayoutPreference) {
        val context = layoutPreference.context

        // Top Hero Card Stats
        val androidVer = Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY
        layoutPreference.findViewById<TextView>(R.id.wowo_stat_android_version)?.text = androidVer
        layoutPreference.findViewById<View>(R.id.wowo_stat_android_card)?.setOnClickListener { v ->
            val ctx = v.context
            if (Utils.isMonkeyRunning()) return@setOnClickListener
            try {
                val intent = Intent(Intent.ACTION_MAIN)
                    .setClassName("android", PlatLogoActivity::class.java.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }

        val deviceName = if (Build.DEVICE.equals("marble", ignoreCase = true)) {
            "POCO F5"
        } else if (!Build.MODEL.isNullOrBlank() && Build.MODEL != "unknown") {
            Build.MODEL
        } else if (!Build.DEVICE.isNullOrBlank()) {
            Build.DEVICE.replaceFirstChar { it.uppercase() }
        } else {
            "Marble"
        }
        layoutPreference.findViewById<TextView>(R.id.wowo_stat_device_name)?.text = deviceName
        layoutPreference.findViewById<View>(R.id.wowo_shinkai_credit_bar)?.setOnClickListener { v ->
            Toast.makeText(v.context, "Shinkai Project • #ShinkaiProjectOnTop!", Toast.LENGTH_SHORT).show()
        }

        // 1. Versi Android Row
        layoutPreference.findViewById<TextView>(R.id.wowo_val_android_version)?.text = androidVer
        layoutPreference.findViewById<View>(R.id.wowo_row_android_version)?.setOnClickListener { v ->
            val ctx = v.context
            if (Utils.isMonkeyRunning()) return@setOnClickListener
            for (i in 1 until hits.size) hits[i - 1] = hits[i]
            hits[hits.size - 1] = SystemClock.uptimeMillis()
            if (hits[hits.size - 1] - hits[0] <= 500L) {
                try {
                    val intent = Intent(Intent.ACTION_MAIN)
                        .setClassName("android", PlatLogoActivity::class.java.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        layoutPreference.findViewById<View>(R.id.wowo_row_android_version)?.setOnLongClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.firmware_version), androidVer)
            true
        }

        // 2. Versi Shinkai Row
        val shinkaiVer = SystemProperties.get("ro.shinkai.version", "")
        val shinkaiVerDisplay = if (shinkaiVer.isNotEmpty()) {
            shinkaiVer
        } else {
            "heptakaideka-marble-20260921-2138"
        }
        layoutPreference.findViewById<TextView>(R.id.wowo_val_wowo_version)?.text = shinkaiVerDisplay
        layoutPreference.findViewById<View>(R.id.wowo_row_wowo_version)?.setOnClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.shinkai_version), shinkaiVerDisplay)
        }

        // 3. Update Keamanan Android Row
        val patch = DeviceInfoUtils.getSecurityPatch(context.getLocale()) ?: Build.VERSION.SECURITY_PATCH
        layoutPreference.findViewById<TextView>(R.id.wowo_val_security_patch)?.text = patch
        layoutPreference.findViewById<View>(R.id.wowo_row_security_patch)?.setOnClickListener { v ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://source.android.com/docs/security/bulletin/"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                v.context.startActivity(intent)
            } catch (e: Exception) {
                copyToClipboard(v.context, context.getString(R.string.security_patch), patch)
            }
        }
        layoutPreference.findViewById<View>(R.id.wowo_row_security_patch)?.setOnLongClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.security_patch), patch)
            true
        }

        // 4. Maintainer Source Row (Static: SkyyVerse & kylieeXD)
        layoutPreference.findViewById<View>(R.id.wowo_row_maintainer_source)?.setOnClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.shinkai_maintainer_source), "SkyyVerse & kylieeXD")
        }

        // 5. Maintainer Device Row
        val rawMaintainer = SystemProperties.get("ro.shinkai.maintainer", "Gustyx Anti Baper")
        val maintainer = if (rawMaintainer == "Gustyx" || rawMaintainer.isBlank() || rawMaintainer == "unknown") {
            "Gustyx Anti Baper"
        } else {
            rawMaintainer.replace("_", " ")
        }
        layoutPreference.findViewById<TextView>(R.id.wowo_val_maintainer)?.text = maintainer
        layoutPreference.findViewById<View>(R.id.wowo_row_maintainer)?.setOnClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.shinkai_maintainer), maintainer)
        }

        // 5. Versi Pita Basis (Baseband) Row
        val basebandRaw = SystemProperties.get("gsm.version.baseband", context.getString(R.string.device_info_default))
        val baseband = basebandRaw.split(",").firstOrNull { it.isNotEmpty() } ?: basebandRaw
        layoutPreference.findViewById<TextView>(R.id.wowo_val_baseband)?.text = baseband
        layoutPreference.findViewById<View>(R.id.wowo_row_baseband)?.setOnClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.baseband_version), baseband)
        }

        // 6. Versi Kernel Row
        val kernel = DeviceInfoUtils.getFormattedKernelVersion(context) ?: "Unknown"
        layoutPreference.findViewById<TextView>(R.id.wowo_val_kernel)?.text = kernel
        layoutPreference.findViewById<View>(R.id.wowo_row_kernel)?.setOnClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.kernel_version), kernel)
        }

        // 7. Nomor Build Row - 7 ketukan untuk membuka Opsi Pengembang (Developer Options)
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val buildNumber = BidiFormatter.getInstance(isRtl).unicodeWrap(Build.DISPLAY)
        layoutPreference.findViewById<TextView>(R.id.wowo_val_build_number)?.text = buildNumber

        var devHitCountdown = if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(context)) -1 else 7
        var devToast: Toast? = null

        layoutPreference.findViewById<View>(R.id.wowo_row_build_number)?.setOnClickListener { v ->
            val ctx = v.context
            if (DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(ctx)) {
                devToast?.cancel()
                devToast = Toast.makeText(ctx, R.string.show_dev_already_cm, Toast.LENGTH_SHORT)
                devToast?.show()
                return@setOnClickListener
            }
            if (devHitCountdown > 0) {
                devHitCountdown--
                if (devHitCountdown == 0) {
                    DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(ctx, true)
                    devToast?.cancel()
                    devToast = Toast.makeText(ctx, R.string.show_dev_on_cm, Toast.LENGTH_LONG)
                    devToast?.show()
                    devHitCountdown = -1
                } else if (devHitCountdown < 5) {
                    devToast?.cancel()
                    val countMsg = try {
                        StringUtil.getIcuPluralsString(ctx, devHitCountdown, R.string.show_dev_countdown_cm)
                    } catch (e: Exception) {
                        "$devHitCountdown steps away from being a developer"
                    }
                    devToast = Toast.makeText(ctx, countMsg, Toast.LENGTH_SHORT)
                    devToast?.show()
                }
            }
        }
        layoutPreference.findViewById<View>(R.id.wowo_row_build_number)?.setOnLongClickListener { v ->
            copyToClipboard(v.context, context.getString(R.string.build_number), buildNumber.toString())
            true
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        val msg = context.getString(R.string.wowo_copied_toast, label)
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
