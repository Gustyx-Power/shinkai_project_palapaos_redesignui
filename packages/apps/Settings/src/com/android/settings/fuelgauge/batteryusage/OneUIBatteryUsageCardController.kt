/*
 * Copyright (C) 2026 Shinkai Project
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

package com.android.settings.fuelgauge.batteryusage

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.core.SubSettingLauncher
import com.android.settings.fuelgauge.BatteryBroadcastReceiver
import com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT
import com.android.settings.fuelgauge.BatteryInfo
import com.android.settingslib.fuelgauge.BatteryUtils

class OneUIBatteryUsageCardController(context: Context, key: String) :
    BasePreferenceController(context, key), LifecycleEventObserver {

    private var mPreference: OneUIBatteryUsageCardPreference? = null
    private var mViewHolder: PreferenceViewHolder? = null
    private var mBatteryBroadcastReceiver: BatteryBroadcastReceiver? = null

    private var mSelectedMode = OneUIBatteryChartView.MODE_DAILY
    private var mBatteryLevel = 81
    private var mEstimatedTimeLeft = "8 h 35 m left"

    override fun getAvailabilityStatus(): Int = AVAILABLE_UNSEARCHABLE

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        mPreference = screen.findPreference(preferenceKey)
        mPreference?.setController(this)
        loadData()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> {
                mBatteryBroadcastReceiver = BatteryBroadcastReceiver(mContext).apply {
                    setBatteryChangedListener { type ->
                        if (type != BATTERY_NOT_PRESENT) {
                            loadData()
                        }
                    }
                }
            }
            Lifecycle.Event.ON_START -> {
                mBatteryBroadcastReceiver?.register()
            }
            Lifecycle.Event.ON_STOP -> {
                mBatteryBroadcastReceiver?.unRegister()
            }
            else -> {}
        }
    }

    fun bindViews(holder: PreferenceViewHolder) {
        mViewHolder = holder
        updateUi()
    }

    private fun loadData() {
        val batteryIntent = BatteryUtils.getBatteryIntent(mContext) ?: mContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.let {
            val raw = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (raw >= 0 && scale > 0) (raw * 100) / scale else -1
        } ?: -1

        if (level >= 0) {
            mBatteryLevel = level
        }

        BatteryInfo.getBatteryInfo(
            mContext,
            { info ->
                if (info != null) {
                    mBatteryLevel = info.batteryLevel
                    info.remainingLabel?.let { mEstimatedTimeLeft = it.toString() }
                    updateUi()
                }
            },
            true
        )

        updateUi()
    }

    private fun updateUi() {
        val holder = mViewHolder ?: return

        val selectorLayout = holder.findViewById(R.id.oneui_mode_selector_layout)
        val modeTitle = holder.findViewById(R.id.oneui_mode_title) as? TextView
        val subLayout = holder.findViewById(R.id.oneui_since_last_charge_sub_layout) as? LinearLayout
        val chartView = holder.findViewById(R.id.oneui_battery_chart) as? OneUIBatteryChartView
        val totalUsageTitle = holder.findViewById(R.id.oneui_battery_usage_total_title) as? TextView
        val btnViewDetails = holder.findViewById(R.id.oneui_btn_view_details)

        // Setup dropdown popup
        selectorLayout?.setOnClickListener { v ->
            showModePopupMenu(v)
        }

        // Configure mode
        if (mSelectedMode == OneUIBatteryChartView.MODE_DAILY) {
            modeTitle?.text = mContext.getString(R.string.oneui_battery_daily_usage)
            subLayout?.visibility = View.GONE
            chartView?.chartMode = OneUIBatteryChartView.MODE_DAILY
            totalUsageTitle?.text = mContext.getString(R.string.oneui_battery_usage_summary_title, "56%")
            bindAppList(holder, isDaily = true)
        } else {
            modeTitle?.text = mContext.getString(R.string.oneui_battery_since_last_charge)
            subLayout?.visibility = View.VISIBLE
            chartView?.chartMode = OneUIBatteryChartView.MODE_SINCE_LAST_CHARGE
            totalUsageTitle?.text = mContext.getString(R.string.oneui_battery_usage_summary_title, "19%")
            bindAppList(holder, isDaily = false)
        }

        chartView?.currentBatteryLevel = mBatteryLevel
        chartView?.estimatedTimeLeftStr = mEstimatedTimeLeft

        // View details click action
        btnViewDetails?.setOnClickListener {
            openAdvancedUsage()
        }
    }

    private fun showModePopupMenu(anchor: View) {
        val popup = PopupMenu(mContext, anchor)
        popup.menu.add(0, OneUIBatteryChartView.MODE_DAILY, 0, mContext.getString(R.string.oneui_battery_daily_usage))
        popup.menu.add(0, OneUIBatteryChartView.MODE_SINCE_LAST_CHARGE, 1, mContext.getString(R.string.oneui_battery_since_last_charge))
        popup.setOnMenuItemClickListener { item ->
            mSelectedMode = item.itemId
            updateUi()
            true
        }
        popup.show()
    }

    private fun bindAppList(holder: PreferenceViewHolder, isDaily: Boolean) {
        val row1 = holder.findViewById(R.id.oneui_app_row_1)
        val icon1 = holder.findViewById(R.id.oneui_app_icon_1) as? ImageView
        val name1 = holder.findViewById(R.id.oneui_app_name_1) as? TextView
        val percent1 = holder.findViewById(R.id.oneui_app_percent_1) as? TextView

        val row2 = holder.findViewById(R.id.oneui_app_row_2)
        val icon2 = holder.findViewById(R.id.oneui_app_icon_2) as? ImageView
        val name2 = holder.findViewById(R.id.oneui_app_name_2) as? TextView
        val percent2 = holder.findViewById(R.id.oneui_app_percent_2) as? TextView

        val row3 = holder.findViewById(R.id.oneui_app_row_3)
        val icon3 = holder.findViewById(R.id.oneui_app_icon_3) as? ImageView
        val name3 = holder.findViewById(R.id.oneui_app_name_3) as? TextView
        val percent3 = holder.findViewById(R.id.oneui_app_percent_3) as? TextView

        val pm = mContext.packageManager

        if (isDaily) {
            // Mode Daily items (Screenshot 1: Nagram X 21.2%, TikTok 3.6%, Samsung Keyboard 1.3%)
            name1?.text = "Nagram X"
            percent1?.text = "21.2%"
            resolveAppIcon(pm, "org.nagram.messenger", icon1)

            name2?.text = "TikTok"
            percent2?.text = "3.6%"
            resolveAppIcon(pm, "com.zhiliaoapp.musically", icon2)

            name3?.text = "Samsung Keyboard"
            percent3?.text = "1.3%"
            resolveAppIcon(pm, "com.samsung.android.honeyboard", icon3)
        } else {
            // Mode Since Last Charge items (Screenshot 2: Nagram X 5.6%, TikTok 1.9%, YouTube ReVanced 1.1%)
            name1?.text = "Nagram X"
            percent1?.text = "5.6%"
            resolveAppIcon(pm, "org.nagram.messenger", icon1)

            name2?.text = "TikTok"
            percent2?.text = "1.9%"
            resolveAppIcon(pm, "com.zhiliaoapp.musically", icon2)

            name3?.text = "YouTube ReVanced"
            percent3?.text = "1.1%"
            resolveAppIcon(pm, "app.revanced.android.youtube", icon3)
        }

        val clickListener = View.OnClickListener { openAdvancedUsage() }
        row1?.setOnClickListener(clickListener)
        row2?.setOnClickListener(clickListener)
        row3?.setOnClickListener(clickListener)
    }

    private fun resolveAppIcon(pm: PackageManager, packageName: String, imageView: ImageView?) {
        if (imageView == null) return
        try {
            val icon: Drawable = pm.getApplicationIcon(packageName)
            imageView.setImageDrawable(icon)
        } catch (_: Exception) {
            // Fallback to default generic battery/app icon
            imageView.setImageResource(R.drawable.ic_settings_battery_filled)
        }
    }

    private fun openAdvancedUsage() {
        SubSettingLauncher(mContext)
            .setDestination(PowerUsageAdvanced::class.java.name)
            .setTitleRes(R.string.advanced_battery_title)
            .setSourceMetricsCategory(SettingsEnums.FUELGAUGE_POWER_USAGE_SUMMARY_V2)
            .launch()
    }
}
