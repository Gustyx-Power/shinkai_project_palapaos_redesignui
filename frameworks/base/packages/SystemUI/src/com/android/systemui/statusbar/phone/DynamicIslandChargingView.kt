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

package com.android.systemui.statusbar.phone

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.android.systemui.res.R

/**
 * Dynamic Island Charging Capsule View tailored for POCO F5 (marble).
 * Expands smoothly from the top-center camera punch hole cutout when charging commences,
 * presenting charging state, battery percentage, and an animated neon green lightning bolt.
 *
 * Completely guarded against early boot / Setup Wizard exceptions.
 */
class DynamicIslandChargingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var capsuleView: View? = null
    private var statusTextView: TextView? = null
    private var batteryTextView: TextView? = null
    private var boltIconView: ImageView? = null

    private var wasPluggedIn: Boolean = false
    private var isExpanded: Boolean = false
    private var currentAnimSet: AnimatorSet? = null
    private var lastBatteryLevel: Int = -1
    private var isReceiverRegistered: Boolean = false

    private val dismissRunnable = Runnable {
        dismissIsland()
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                // Safeguard: Never trigger during Setup Wizard (OOBE) before device is provisioned
                val isProvisioned = try {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 1
                } catch (_: Throwable) {
                    false
                }
                if (!isProvisioned) return

                val isEnabled = try {
                    Settings.System.getInt(context.contentResolver, "dynamic_island_charging", 0) == 1
                } catch (_: Throwable) {
                    false
                }
                if (!isEnabled) return

                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                        val isPluggedIn = plugged != 0 || isCharging

                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else level
                        lastBatteryLevel = batteryPct

                        if (!wasPluggedIn && isPluggedIn) {
                            // Charger connected -> trigger dynamic island animation
                            triggerChargingIsland(batteryPct)
                        } else if (wasPluggedIn && isPluggedIn && isExpanded) {
                            // Update percentage while visible
                            batteryTextView?.text = "$batteryPct%"
                        } else if (wasPluggedIn && !isPluggedIn) {
                            // Charger disconnected -> retract immediately
                            dismissIsland()
                        }

                        wasPluggedIn = isPluggedIn
                    }
                    ACTION_TEST_DYNAMIC_ISLAND -> {
                        val level = intent.getIntExtra("level", if (lastBatteryLevel >= 0) lastBatteryLevel else 29)
                        triggerChargingIsland(level)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in batteryReceiver", t)
            }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        try {
            LayoutInflater.from(context).inflate(R.layout.dynamic_island_charging, this, true)

            capsuleView = findViewById(R.id.island_capsule)
            statusTextView = findViewById(R.id.island_status_text)
            batteryTextView = findViewById(R.id.island_battery_text)
            boltIconView = findViewById(R.id.island_bolt_icon)

            visibility = View.GONE
            alpha = 0f

            capsuleView?.setOnClickListener {
                dismissIsland()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to inflate DynamicIslandChargingView", t)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(ACTION_TEST_DYNAMIC_ISLAND)
            }
            val stickyIntent = context.registerReceiver(
                batteryReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
            isReceiverRegistered = true

            if (stickyIntent != null) {
                val status = stickyIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val plugged = stickyIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                wasPluggedIn = plugged != 0 || isCharging

                val level = stickyIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = stickyIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                lastBatteryLevel = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else level
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register receiver in onAttachedToWindow", t)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(batteryReceiver)
                isReceiverRegistered = false
            }
        } catch (_: Exception) {}
        mainHandler.removeCallbacks(dismissRunnable)
        currentAnimSet?.cancel()
    }

    /**
     * Triggers the Dynamic Island charging expansion from the POCO F5 punch-hole.
     */
    fun triggerChargingIsland(batteryLevel: Int) {
        try {
            val isProvisioned = try {
                Settings.Global.getInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 1
            } catch (_: Throwable) {
                false
            }
            if (!isProvisioned) return

            val isEnabled = try {
                Settings.System.getInt(context.contentResolver, "dynamic_island_charging", 0) == 1
            } catch (_: Throwable) {
                false
            }
            if (!isEnabled) return

            // In landscape mode, punch-hole is at the side, so only trigger in portrait
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                return
            }

            mainHandler.removeCallbacks(dismissRunnable)
            currentAnimSet?.cancel()

            batteryTextView?.text = if (batteryLevel >= 0) "$batteryLevel%" else ""
            statusTextView?.setText(R.string.dynamic_island_charging)

            visibility = View.VISIBLE
            isExpanded = true

            capsuleView?.let { capsule ->
                val capsuleWidth = if (capsule.width > 0) capsule.width.toFloat() else 210f * resources.displayMetrics.density
                val capsuleHeight = if (capsule.height > 0) capsule.height.toFloat() else 35f * resources.displayMetrics.density
                capsule.pivotX = capsuleWidth / 2f
                capsule.pivotY = capsuleHeight / 2f

                val scaleXAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_X, 0.15f, 1.0f).apply {
                    duration = 380L
                    interpolator = OvershootInterpolator(1.18f)
                }
                val scaleYAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_Y, 0.4f, 1.0f).apply {
                    duration = 350L
                    interpolator = OvershootInterpolator(1.15f)
                }
                val alphaAnim = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1.0f).apply {
                    duration = 240L
                }

                // Green bolt pulse animation
                boltIconView?.let { bolt ->
                    bolt.scaleX = 0.8f
                    bolt.scaleY = 0.8f
                    bolt.animate()
                        .scaleX(1.28f)
                        .scaleY(1.28f)
                        .setDuration(240L)
                        .withEndAction {
                            bolt.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180L).start()
                        }
                        .start()
                }

                AnimatorSet().apply {
                    playTogether(scaleXAnim, scaleYAnim, alphaAnim)
                    start()
                    currentAnimSet = this
                }
            }

            // Auto-retract back into the punch-hole after 4.5 seconds
            mainHandler.postDelayed(dismissRunnable, 4500L)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to trigger charging island", t)
        }
    }

    /**
     * Retracts the Dynamic Island smoothly back into the punch-hole.
     */
    fun dismissIsland() {
        try {
            if (!isExpanded && visibility != View.VISIBLE) return

            mainHandler.removeCallbacks(dismissRunnable)
            currentAnimSet?.cancel()

            capsuleView?.let { capsule ->
                val capsuleWidth = if (capsule.width > 0) capsule.width.toFloat() else 210f * resources.displayMetrics.density
                val capsuleHeight = if (capsule.height > 0) capsule.height.toFloat() else 35f * resources.displayMetrics.density
                capsule.pivotX = capsuleWidth / 2f
                capsule.pivotY = capsuleHeight / 2f

                val scaleXAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_X, 1.0f, 0.15f).apply {
                    duration = 260L
                    interpolator = AccelerateInterpolator(1.4f)
                }
                val scaleYAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_Y, 1.0f, 0.4f).apply {
                    duration = 260L
                    interpolator = AccelerateInterpolator(1.4f)
                }
                val alphaAnim = ObjectAnimator.ofFloat(this, View.ALPHA, 1.0f, 0.0f).apply {
                    duration = 220L
                }

                val animSet = AnimatorSet().apply {
                    playTogether(scaleXAnim, scaleYAnim, alphaAnim)
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            visibility = View.GONE
                            capsule.scaleX = 1.0f
                            capsule.scaleY = 1.0f
                            alpha = 1.0f
                            isExpanded = false
                        }
                    })
                    start()
                }
                currentAnimSet = animSet
            } ?: run {
                visibility = View.GONE
                isExpanded = false
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to dismiss island", t)
            visibility = View.GONE
            isExpanded = false
        }
    }

    companion object {
        private const val TAG = "DynamicIslandCharging"
        const val ACTION_TEST_DYNAMIC_ISLAND = "com.android.systemui.action.TEST_DYNAMIC_ISLAND"
    }
}
