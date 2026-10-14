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
import android.app.ActivityManager
import android.app.BroadcastOptions
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.projection.StopReason
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.systemui.Dependency
import com.android.systemui.res.R
import com.android.systemui.screenrecord.RecordingService
import com.android.systemui.screenrecord.ScreenRecordUxController
import com.android.systemui.screenrecord.service.ScreenRecordingService
import com.android.systemui.statusbar.policy.FlashlightController

/**
 * Dynamic Island View tailored for POCO F5 (marble).
 *
 * Features:
 * - Fluid iOS-style spring overshoot expansion and retracting recoil physics.
 * - Three primary events: Charging (ephemeral 7.8s), Flashlight (persistent), and Screen Recording (persistent with live timer).
 * - Dual-Island Meta-Ball Architecture: When both Torch and Recording are active, Recording takes the main pill
 *   and Torch becomes the secondary circular meta-ball bubble.
 * - Horizontal swipe gesture smoothly swaps priorities between the main capsule and the meta-ball.
 * - Interactive Taps: Tapping the compact capsule springs open the Expanded Floating Control Island
 *   with circular stop/action buttons and large digital timer/status.
 *
 * Completely guarded against early boot / Setup Wizard exceptions.
 */
class DynamicIslandChargingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        clipChildren = false
        clipToPadding = false
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // --- Compact Views ---
    private var rootContainer: LinearLayout? = null
    private var capsuleView: FrameLayout? = null
    private var chargingContentView: LinearLayout? = null
    private var torchContentView: LinearLayout? = null
    private var recordContentView: LinearLayout? = null

    private var statusTextView: TextView? = null
    private var batteryTextView: TextView? = null
    private var boltIconView: ImageView? = null

    private var torchActionTextView: TextView? = null
    private var torchIconView: ImageView? = null

    private var recordTimerView: TextView? = null
    private var recordDotView: ImageView? = null

    private var metaBallView: FrameLayout? = null
    private var metaIconView: ImageView? = null
    private var metaRecordDotView: ImageView? = null

    // --- Expanded Card Views (Floating Window) ---
    private var expandedWindowView: View? = null
    private var expandedCardContainer: FrameLayout? = null
    private var expandedRecordContent: LinearLayout? = null
    private var expandedTorchContent: LinearLayout? = null
    private var expandedChargingContent: LinearLayout? = null
    private var expandedRecordTimer: TextView? = null
    private var expandedBatteryText: TextView? = null
    private var expandedTorchStatus: TextView? = null
    private var isExpandedCardShowing = false

    // --- State Machine ---
    private var isCharging: Boolean = false
    private var wasPluggedIn: Boolean = false
    private var lastBatteryLevel: Int = -1

    private var isTorchOn: Boolean = false
    private var isRecording: Boolean = false
    private var recordingStartTimestamp: Long = 0L

    // Dual-island priority: true = Record is in main capsule, Torch is in meta-ball
    private var prioritizeRecord: Boolean = true

    private var isCompactExpanded: Boolean = false
    private var currentCompactAnimSet: AnimatorSet? = null
    private var isReceiverRegistered: Boolean = false

    // Status Bar Sides for OriginOS fluid expand/collapse evasion physics
    private var statusBarStartSide: View? = null
    private var statusBarEndSide: View? = null

    // --- Controllers ---
    private var flashlightController: FlashlightController? = null
    private var screenRecordController: ScreenRecordUxController? = null
    private var cameraManager: CameraManager? = null
    private var rearCameraId: String? = null

    // --- Runnables ---
    private val chargingDismissRunnable = Runnable {
        isCharging = false
        refreshIslandState(animate = true)
    }

    private val recordTickerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                updateRecordTimer()
                mainHandler.postDelayed(this, 1000L)
            }
        }
    }

    private val expandedAutoCollapseRunnable = Runnable {
        dismissExpandedCard()
    }

    // --- Callbacks ---
    private val flashlightListener = object : FlashlightController.FlashlightListener {
        override fun onFlashlightChanged(enabled: Boolean) {
            mainHandler.post {
                if (isTorchOn != enabled) {
                    isTorchOn = enabled
                    refreshIslandState(animate = true)
                }
            }
        }

        override fun onFlashlightError() {
            mainHandler.post {
                isTorchOn = false
                refreshIslandState(animate = true)
            }
        }

        override fun onFlashlightAvailabilityChanged(available: Boolean) {
            if (!available) {
                mainHandler.post {
                    isTorchOn = false
                    refreshIslandState(animate = true)
                }
            }
        }

        override fun onFlashlightStrengthChanged(level: Int) {}
    }

    private val screenRecordCallback = object : ScreenRecordUxController.StateChangeCallback {
        override fun onRecordingStart() {
            mainHandler.post {
                if (!isRecording) {
                    isRecording = true
                    recordingStartTimestamp = SystemClock.elapsedRealtime()
                    prioritizeRecord = true
                    startRecordTicker()
                    refreshIslandState(animate = true)
                }
            }
        }

        override fun onRecordingEnd() {
            mainHandler.post {
                if (isRecording) {
                    isRecording = false
                    stopRecordTicker()
                    dismissExpandedCard()
                    refreshIslandState(animate = true)
                }
            }
        }
    }

    private val cameraTorchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            mainHandler.post {
                if (isTorchOn != enabled) {
                    isTorchOn = enabled
                    refreshIslandState(animate = true)
                }
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            mainHandler.post {
                if (isTorchOn) {
                    isTorchOn = false
                    refreshIslandState(animate = true)
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (!isDeviceProvisioned() || !isFeatureEnabled()) return

                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                        val isPluggedIn = plugged != 0 || charging

                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val batteryPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else level
                        lastBatteryLevel = batteryPct

                        if (!wasPluggedIn && isPluggedIn) {
                            // Plugged in -> trigger charging capsule
                            isCharging = true
                            mainHandler.removeCallbacks(chargingDismissRunnable)
                            mainHandler.postDelayed(chargingDismissRunnable, 7800L)
                            refreshIslandState(animate = true)
                        } else if (wasPluggedIn && isPluggedIn && isCharging) {
                            batteryTextView?.text = "$batteryPct%"
                            expandedBatteryText?.text = "$batteryPct%"
                        } else if (wasPluggedIn && !isPluggedIn) {
                            isCharging = false
                            mainHandler.removeCallbacks(chargingDismissRunnable)
                            refreshIslandState(animate = true)
                        }

                        wasPluggedIn = isPluggedIn
                    }
                    ScreenRecordUxController.INTENT_UPDATE_STATE -> {
                        val state = intent.getBooleanExtra(ScreenRecordUxController.EXTRA_STATE, false)
                        if (state != isRecording) {
                            isRecording = state
                            if (isRecording) {
                                recordingStartTimestamp = SystemClock.elapsedRealtime()
                                prioritizeRecord = true
                                startRecordTicker()
                            } else {
                                stopRecordTicker()
                                dismissExpandedCard()
                            }
                            refreshIslandState(animate = true)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in batteryReceiver", t)
            }
        }
    }

    private val settingsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            if (!isFeatureEnabled()) {
                dismissIsland()
                dismissExpandedCard()
            } else {
                refreshIslandState(animate = true)
            }
        }
    }

    // --- Gesture Detection for Pill & Meta-Ball Swipe & Click ---
    private var isSwappingDualIsland = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        private var hasTriggeredScrollSwap = false

        override fun onDown(e: MotionEvent): Boolean {
            hasTriggeredScrollSwap = false
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            showExpandedCard()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (isTorchOn && isRecording && !hasTriggeredScrollSwap && !isSwappingDualIsland && e1 != null) {
                val totalDeltaX = e2.x - e1.x
                val totalDeltaY = Math.abs(e2.y - e1.y)
                val density = resources.displayMetrics.density
                // Sensitive horizontal drag threshold: 18dp horizontal, < 28dp vertical
                if (Math.abs(totalDeltaX) > 18f * density && totalDeltaY < 28f * density) {
                    hasTriggeredScrollSwap = true
                    swapDualIslandPriority()
                    return true
                }
            }
            return false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (isTorchOn && isRecording && !hasTriggeredScrollSwap && !isSwappingDualIsland) {
                swapDualIslandPriority()
                return true
            }
            return false
        }
    })

    private val metaGestureDetector by lazy {
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            private var hasTriggeredScrollSwap = false

            override fun onDown(e: MotionEvent): Boolean {
                hasTriggeredScrollSwap = false
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onMetaBallClicked()
                return true
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (isTorchOn && isRecording && !hasTriggeredScrollSwap && !isSwappingDualIsland && e1 != null) {
                    val totalDeltaX = e2.x - e1.x
                    val totalDeltaY = Math.abs(e2.y - e1.y)
                    val density = resources.displayMetrics.density
                    if (Math.abs(totalDeltaX) > 16f * density && totalDeltaY < 28f * density) {
                        hasTriggeredScrollSwap = true
                        swapDualIslandPriority()
                        return true
                    }
                }
                return false
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (isTorchOn && isRecording && !hasTriggeredScrollSwap && !isSwappingDualIsland) {
                    swapDualIslandPriority()
                    return true
                }
                return false
            }
        })
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        try {
            clipChildren = false
            clipToPadding = false

            LayoutInflater.from(context).inflate(R.layout.dynamic_island_charging, this, true)

            rootContainer = findViewById(R.id.island_root_container)
            rootContainer?.clipChildren = false
            rootContainer?.clipToPadding = false

            capsuleView = findViewById(R.id.island_capsule)
            chargingContentView = findViewById(R.id.island_charging_content)
            torchContentView = findViewById(R.id.island_torch_content)
            recordContentView = findViewById(R.id.island_record_content)

            statusTextView = findViewById(R.id.island_status_text)
            batteryTextView = findViewById(R.id.island_battery_text)
            boltIconView = findViewById(R.id.island_bolt_icon)

            torchActionTextView = findViewById(R.id.island_torch_action_text)
            torchIconView = findViewById(R.id.island_torch_icon)

            recordTimerView = findViewById(R.id.island_record_timer)
            recordDotView = findViewById(R.id.island_record_dot)

            metaBallView = findViewById(R.id.island_meta_ball)
            metaIconView = findViewById(R.id.island_meta_icon)
            metaRecordDotView = findViewById(R.id.island_meta_record_dot)

            visibility = View.GONE
            alpha = 0f

            capsuleView?.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                true
            }

            metaBallView?.setOnTouchListener { _, event ->
                metaGestureDetector.onTouchEvent(event)
                true
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to inflate DynamicIslandChargingView", t)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            if (!isDeviceProvisioned()) {
                visibility = View.GONE
                return
            }

            // Register Battery & ScreenRecord broadcast receiver
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(ScreenRecordUxController.INTENT_UPDATE_STATE)
            }
            val stickyIntent = context.registerReceiver(
                batteryReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
            isReceiverRegistered = true

            if (stickyIntent != null) {
                val status = stickyIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val plugged = stickyIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                wasPluggedIn = plugged != 0 || charging

                val level = stickyIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = stickyIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                lastBatteryLevel = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else level
            }

            // Initialize Flashlight Controller / Listener
            try {
                flashlightController = Dependency.get(FlashlightController::class.java)
                flashlightController?.addCallback(flashlightListener)
                isTorchOn = flashlightController?.isEnabled == true
            } catch (_: Throwable) {}

            // Always init Camera Torch Callback as well for dual assurance
            initCameraTorchCallback()

            // Initialize ScreenRecord Controller / Listener
            try {
                screenRecordController = Dependency.get(ScreenRecordUxController::class.java)
                screenRecordController?.addCallback(screenRecordCallback)
                if (screenRecordController?.isRecording == true) {
                    isRecording = true
                    recordingStartTimestamp = SystemClock.elapsedRealtime()
                    startRecordTicker()
                }
            } catch (t: Throwable) {
                Log.w(TAG, "ScreenRecordUxController not yet initialized in Dependency", t)
            }

            // Observe Dynamic Island toggle & Torch setting changes
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor("dynamic_island_charging"),
                false,
                settingsObserver
            )
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.FLASHLIGHT_ENABLED),
                false,
                settingsObserver
            )

            refreshIslandState(animate = false)
            ensureStatusBarSideViews()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed in onAttachedToWindow", t)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(batteryReceiver)
                isReceiverRegistered = false
            }
            flashlightController?.removeCallback(flashlightListener)
            screenRecordController?.removeCallback(screenRecordCallback)
            cameraManager?.unregisterTorchCallback(cameraTorchCallback)
            context.contentResolver.unregisterContentObserver(settingsObserver)
        } catch (_: Exception) {}

        mainHandler.removeCallbacks(chargingDismissRunnable)
        mainHandler.removeCallbacks(recordTickerRunnable)
        mainHandler.removeCallbacks(expandedAutoCollapseRunnable)
        currentCompactAnimSet?.cancel()
        dismissExpandedCard()
        resetStatusBarSides()
    }

    private fun initCameraTorchCallback() {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        rearCameraId = id
                        break
                    }
                }
                cm.registerTorchCallback(cameraTorchCallback, mainHandler)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "CameraManager callback init failed", t)
        }
    }

    /**
     * Refreshes the Dynamic Island layout state based on active events:
     * Charging, Flashlight, and Screen Recording.
     */
    fun refreshIslandState(animate: Boolean) {
        if (!isDeviceProvisioned() || !isFeatureEnabled()) {
            dismissIsland()
            dismissExpandedCard()
            return
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dismissIsland()
            dismissExpandedCard()
            return
        }

        val hasCharging = isCharging
        val secureTorch = try {
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.FLASHLIGHT_ENABLED, 0) == 1
        } catch (_: Throwable) { false }
        val controllerTorch = flashlightController?.isEnabled == true
        val hasTorch = isTorchOn || controllerTorch || secureTorch
        isTorchOn = hasTorch

        val hasRecord = isRecording || screenRecordController?.isRecording == true
        isRecording = hasRecord

        if (!hasCharging && !hasTorch && !hasRecord) {
            dismissIsland()
            dismissExpandedCard()
            return
        }

        // Determine who occupies the main pill and who occupies the meta-ball
        var mainSlot: IslandSlot = IslandSlot.NONE
        var metaSlot: IslandSlot = IslandSlot.NONE

        if (hasCharging) {
            // Charging has immediate presentation priority for its 7.8s duration
            mainSlot = IslandSlot.CHARGING
            if (hasRecord) {
                metaSlot = IslandSlot.RECORD
            } else if (hasTorch) {
                metaSlot = IslandSlot.TORCH
            }
        } else if (hasRecord && hasTorch) {
            // Dual-Island: Both record & flashlight active!
            if (prioritizeRecord) {
                mainSlot = IslandSlot.RECORD
                metaSlot = IslandSlot.TORCH
            } else {
                mainSlot = IslandSlot.TORCH
                metaSlot = IslandSlot.RECORD
            }
        } else if (hasRecord) {
            mainSlot = IslandSlot.RECORD
            metaSlot = IslandSlot.NONE
        } else if (hasTorch) {
            mainSlot = IslandSlot.TORCH
            metaSlot = IslandSlot.NONE
        }

        applyCompactLayout(mainSlot, metaSlot, animate)
        updateExpandedCardIfShowing(mainSlot)
    }

    private fun applyCompactLayout(mainSlot: IslandSlot, metaSlot: IslandSlot, animate: Boolean) {
        // Toggle Main Pill Subviews
        chargingContentView?.visibility = if (mainSlot == IslandSlot.CHARGING) View.VISIBLE else View.GONE
        torchContentView?.visibility = if (mainSlot == IslandSlot.TORCH) View.VISIBLE else View.GONE
        recordContentView?.visibility = if (mainSlot == IslandSlot.RECORD) View.VISIBLE else View.GONE

        when (mainSlot) {
            IslandSlot.CHARGING -> {
                batteryTextView?.text = if (lastBatteryLevel >= 0) "$lastBatteryLevel%" else ""
                statusTextView?.setText(R.string.dynamic_island_charging)
                animateBoltPulse()
            }
            IslandSlot.TORCH -> {
                torchActionTextView?.setText(R.string.dynamic_island_torch_on)
            }
            IslandSlot.RECORD -> {
                updateRecordTimer()
            }
            IslandSlot.NONE -> {}
        }

        // Toggle Meta-Ball View
        if (metaSlot != IslandSlot.NONE) {
            val wasGone = metaBallView?.visibility != View.VISIBLE
            metaBallView?.visibility = View.VISIBLE
            metaBallView?.alpha = 1.0f
            metaBallView?.scaleX = 1.0f
            metaBallView?.scaleY = 1.0f

            if (metaSlot == IslandSlot.TORCH) {
                metaIconView?.setImageResource(R.drawable.ic_origin_torch)
                metaIconView?.imageTintList = null
                metaIconView?.visibility = View.VISIBLE
                metaRecordDotView?.visibility = View.GONE
            } else if (metaSlot == IslandSlot.RECORD) {
                metaIconView?.visibility = View.GONE
                metaRecordDotView?.visibility = View.VISIBLE
            }

            if (animate && wasGone) {
                metaBallView?.scaleX = 0.2f
                metaBallView?.scaleY = 0.2f
                metaBallView?.alpha = 0f
                metaBallView?.animate()
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.alpha(1.0f)
                    ?.setDuration(320L)
                    ?.setInterpolator(OvershootInterpolator(1.4f))
                    ?.start()
            }
        } else {
            if (metaBallView?.visibility == View.VISIBLE) {
                metaBallView?.animate()
                    ?.scaleX(0.2f)
                    ?.scaleY(0.2f)
                    ?.alpha(0f)
                    ?.setDuration(220L)
                    ?.withEndAction {
                        metaBallView?.visibility = View.GONE
                        metaBallView?.scaleX = 1.0f
                        metaBallView?.scaleY = 1.0f
                        metaBallView?.alpha = 1.0f
                    }
                    ?.start()
            } else {
                metaBallView?.visibility = View.GONE
                metaBallView?.scaleX = 1.0f
                metaBallView?.scaleY = 1.0f
                metaBallView?.alpha = 1.0f
            }
        }

        // Expand compact capsule with spring overshoot
        if (!isCompactExpanded || visibility != View.VISIBLE) {
            expandCompactIsland(animate)
        }
    }

    /**
     * Bouncy Spring Overshoot entrance from the POCO F5 punch-hole.
     */
    private fun expandCompactIsland(animate: Boolean) {
        visibility = View.VISIBLE
        isCompactExpanded = true
        currentCompactAnimSet?.cancel()

        capsuleView?.let { capsule ->
            val capsuleWidth = if (capsule.width > 0) capsule.width.toFloat() else 210f * resources.displayMetrics.density
            val capsuleHeight = if (capsule.height > 0) capsule.height.toFloat() else 35f * resources.displayMetrics.density
            capsule.pivotX = capsuleWidth / 2f
            capsule.pivotY = capsuleHeight / 2f

            if (animate) {
                val scaleXAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_X, 0.15f, 1.0f).apply {
                    duration = 380L
                    interpolator = OvershootInterpolator(1.35f)
                }
                val scaleYAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_Y, 0.35f, 1.0f).apply {
                    duration = 360L
                    interpolator = OvershootInterpolator(1.22f)
                }
                val alphaAnim = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1.0f).apply {
                    duration = 200L
                }

                currentCompactAnimSet = AnimatorSet().apply {
                    playTogether(scaleXAnim, scaleYAnim, alphaAnim)
                    start()
                }
            } else {
                capsule.scaleX = 1.0f
                capsule.scaleY = 1.0f
                alpha = 1.0f
            }
        }
    }

    /**
     * Tactile iOS-style spring recoil closing animation back into the punch-hole.
     */
    fun dismissIsland() {
        if (!isCompactExpanded && visibility != View.VISIBLE) return

        mainHandler.removeCallbacks(chargingDismissRunnable)
        currentCompactAnimSet?.cancel()

        capsuleView?.let { capsule ->
            val capsuleWidth = if (capsule.width > 0) capsule.width.toFloat() else 210f * resources.displayMetrics.density
            val capsuleHeight = if (capsule.height > 0) capsule.height.toFloat() else 35f * resources.displayMetrics.density
            capsule.pivotX = capsuleWidth / 2f
            capsule.pivotY = capsuleHeight / 2f

            // Recoil Phase 1: Slight rubber stretch anticipation
            capsule.animate()
                .scaleX(1.05f)
                .scaleY(0.95f)
                .setDuration(70L)
                .withEndAction {
                    // Recoil Phase 2: Snap cleanly inward into the camera punch-hole
                    val scaleXAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_X, 1.05f, 0.15f).apply {
                        duration = 240L
                        interpolator = AccelerateInterpolator(1.5f)
                    }
                    val scaleYAnim = ObjectAnimator.ofFloat(capsule, View.SCALE_Y, 0.95f, 0.35f).apply {
                        duration = 240L
                        interpolator = AccelerateInterpolator(1.5f)
                    }
                    val alphaAnim = ObjectAnimator.ofFloat(this@DynamicIslandChargingView, View.ALPHA, 1.0f, 0.0f).apply {
                        duration = 200L
                    }

                    currentCompactAnimSet = AnimatorSet().apply {
                        playTogether(scaleXAnim, scaleYAnim, alphaAnim)
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                visibility = View.GONE
                                capsule.scaleX = 1.0f
                                capsule.scaleY = 1.0f
                                alpha = 1.0f
                                metaBallView?.visibility = View.GONE
                                metaBallView?.scaleX = 1.0f
                                metaBallView?.scaleY = 1.0f
                                metaBallView?.alpha = 1.0f
                                isCompactExpanded = false
                            }
                        })
                        start()
                    }
                }
                .start()
        } ?: run {
            visibility = View.GONE
            metaBallView?.visibility = View.GONE
            metaBallView?.scaleX = 1.0f
            metaBallView?.scaleY = 1.0f
            metaBallView?.alpha = 1.0f
            isCompactExpanded = false
        }
    }

    /**
     * Swaps priorities between Main Capsule and Meta-Ball when swiped horizontally.
     * Uses authentic OriginOS liquid physics: squash & stretch, full displacement merge,
     * tactile haptic feedback, and elastic spring recovery.
     */
    private fun swapDualIslandPriority() {
        if (!isTorchOn || !isRecording || isSwappingDualIsland) return
        isSwappingDualIsland = true

        val density = resources.displayMetrics.density

        // Phase 1: Magnetic Attraction, Liquid Squash & Stretch, and Absorption
        metaBallView?.animate()?.cancel()
        capsuleView?.animate()?.cancel()

        // Meta-Ball (on the left) stretches horizontally along movement axis and glides into the capsule
        metaBallView?.animate()
            ?.translationX(38f * density) // Full displacement directly into the capsule
            ?.scaleX(1.36f)             // Liquid stretch along direction of motion
            ?.scaleY(0.70f)             // Vertical compression (volume-preserving liquid squash)
            ?.alpha(0.35f)
            ?.setDuration(190L)
            ?.setInterpolator(PathInterpolator(0.35f, 0.0f, 0.15f, 1.0f))
            ?.withEndAction {
                // Tactile haptic click at the exact moment of liquid merger
                try {
                    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                } catch (_: Throwable) {}

                prioritizeRecord = !prioritizeRecord
                refreshIslandState(animate = false)

                // Phase 2: Elastic Spring Ejection of new secondary droplet & capsule recovery
                // Capsule rebounds slightly to the right, then settles with smooth liquid damping
                capsuleView?.translationX = 14f * density
                capsuleView?.scaleX = 1.05f
                capsuleView?.scaleY = 0.95f
                capsuleView?.animate()
                    ?.translationX(0f)
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.alpha(1.0f)
                    ?.setDuration(380L)
                    ?.setInterpolator(PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f))
                    ?.start()

                // New Meta-Ball droplet emerges from the capsule on the left with vertical stretch wobble
                metaBallView?.translationX = -22f * density
                metaBallView?.scaleX = 0.72f
                metaBallView?.scaleY = 1.28f
                metaBallView?.alpha = 0.85f
                metaBallView?.animate()
                    ?.translationX(0f)
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.alpha(1.0f)
                    ?.setDuration(400L)
                    ?.setInterpolator(OvershootInterpolator(1.35f))
                    ?.withEndAction {
                        isSwappingDualIsland = false
                    }
                    ?.start()
            }
            ?.start()

        // Capsule compresses slightly on the left edge as it absorbs the incoming metaball
        capsuleView?.animate()
            ?.translationX(-14f * density)
            ?.scaleX(0.92f)
            ?.scaleY(1.06f)
            ?.alpha(0.75f)
            ?.setDuration(190L)
            ?.setInterpolator(PathInterpolator(0.35f, 0.0f, 0.15f, 1.0f))
            ?.start()
    }

    /**
     * Interacting with the Meta-Ball bubble.
     */
    private fun onMetaBallClicked() {
        metaBallView?.let { ball ->
            try {
                ball.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            } catch (_: Throwable) {}
            ball.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(100L)
                .withEndAction {
                    ball.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(220L)
                        .setInterpolator(OvershootInterpolator(1.4f))
                        .start()
                }
                .start()
        }

        if (prioritizeRecord) {
            // Torch is inside the meta-ball -> tapping toggles torch off!
            toggleTorch()
        } else {
            // Record is inside the meta-ball -> tapping expands record controls!
            showExpandedCard()
        }
    }

    // --- Expanded Floating Card Logic (OriginOS Aesthetic) ---

    fun showExpandedCard() {
        if (isExpandedCardShowing) {
            dismissExpandedCard()
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        try {
            val root = LayoutInflater.from(context).inflate(R.layout.dynamic_island_expanded_card, null)
            expandedWindowView = root

            expandedCardContainer = root.findViewById(R.id.expanded_card_container)
            expandedRecordContent = root.findViewById(R.id.expanded_record_content)
            expandedTorchContent = root.findViewById(R.id.expanded_torch_content)
            expandedChargingContent = root.findViewById(R.id.expanded_charging_content)
            expandedRecordTimer = root.findViewById(R.id.expanded_record_timer)
            expandedBatteryText = root.findViewById(R.id.expanded_battery_text)
            expandedTorchStatus = root.findViewById(R.id.expanded_torch_status)

            // Primary Controls
            val stopBtn: FrameLayout? = root.findViewById(R.id.expanded_record_stop_btn)
            val torchBtn: FrameLayout? = root.findViewById(R.id.expanded_torch_btn)

            stopBtn?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                stopScreenRecording()
                dismissExpandedCard()
            }

            torchBtn?.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                toggleTorch()
                dismissExpandedCard()
            }

            // Outside touch listener collapses the expanded card back to compact pill
            root.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismissExpandedCard()
                    true
                } else {
                    false
                }
            }

            expandedCardContainer?.setOnClickListener {
                dismissExpandedCard()
            }

            // Determine active mode to display inside the expanded card
            val activeMode = when {
                isRecording && isTorchOn -> if (prioritizeRecord) IslandSlot.RECORD else IslandSlot.TORCH
                isRecording -> IslandSlot.RECORD
                isTorchOn -> IslandSlot.TORCH
                isCharging -> IslandSlot.CHARGING
                else -> IslandSlot.NONE
            }

            if (activeMode == IslandSlot.NONE) return

            isExpandedCardShowing = true
            populateExpandedCard(activeMode)

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_SUB_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (resources.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height) * 0.10f).toInt()
            }

            wm.addView(root, lp)

            // Animate native status bar content sliding out to left and right (OriginOS physics)
            animateStatusBarSidesOnExpand()

            // Hide compact pill with smooth scale & fade
            capsuleView?.animate()
                ?.scaleX(0.5f)
                ?.scaleY(0.5f)
                ?.alpha(0f)
                ?.setDuration(240L)
                ?.withEndAction { capsuleView?.visibility = View.INVISIBLE }
                ?.start()

            metaBallView?.animate()
                ?.scaleX(0.4f)
                ?.scaleY(0.4f)
                ?.alpha(0f)
                ?.setDuration(240L)
                ?.withEndAction { metaBallView?.visibility = View.INVISIBLE }
                ?.start()

            // Fluid spring animate expand from top punch-hole center
            expandedCardContainer?.let { container ->
                val displayWidth = resources.displayMetrics.widthPixels.toFloat()
                container.pivotX = displayWidth / 2f
                container.pivotY = 0f
                container.scaleX = 0.32f
                container.scaleY = 0.22f
                container.alpha = 0f
                container.translationY = -14f * resources.displayMetrics.density

                container.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .translationY(0f)
                    .setDuration(440L)
                    .setInterpolator(PathInterpolator(0.22f, 1.0f, 0.36f, 1.0f))
                    .start()
            }

            mainHandler.removeCallbacks(expandedAutoCollapseRunnable)
            mainHandler.postDelayed(expandedAutoCollapseRunnable, 10000L)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to show expanded island card", t)
        }
    }

    private fun populateExpandedCard(slot: IslandSlot) {
        expandedRecordContent?.visibility = if (slot == IslandSlot.RECORD) View.VISIBLE else View.GONE
        expandedTorchContent?.visibility = if (slot == IslandSlot.TORCH) View.VISIBLE else View.GONE
        expandedChargingContent?.visibility = if (slot == IslandSlot.CHARGING) View.VISIBLE else View.GONE

        when (slot) {
            IslandSlot.RECORD -> {
                updateRecordTimer()
            }
            IslandSlot.TORCH -> {
                val secureTorch = try {
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.FLASHLIGHT_ENABLED, 0) == 1
                } catch (_: Throwable) { false }
                val torchOn = isTorchOn || flashlightController?.isEnabled == true || secureTorch
                expandedTorchStatus?.setText(if (torchOn) R.string.dynamic_island_torch_on else R.string.dynamic_island_torch_label)
            }
            IslandSlot.CHARGING -> {
                expandedBatteryText?.text = if (lastBatteryLevel >= 0) "$lastBatteryLevel%" else ""
            }
            IslandSlot.NONE -> {
                dismissExpandedCard()
            }
        }
    }

    private fun updateExpandedCardIfShowing(slot: IslandSlot) {
        if (!isExpandedCardShowing) return
        populateExpandedCard(slot)
    }

    fun dismissExpandedCard() {
        if (!isExpandedCardShowing || expandedWindowView == null) return

        mainHandler.removeCallbacks(expandedAutoCollapseRunnable)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val view = expandedWindowView

        // Animate native status bar content sliding back in smoothly (OriginOS physics)
        animateStatusBarSidesOnCollapse()

        expandedCardContainer?.let { container ->
            val displayWidth = resources.displayMetrics.widthPixels.toFloat()
            container.pivotX = displayWidth / 2f
            container.pivotY = 0f

            container.animate()
                .scaleX(0.32f)
                .scaleY(0.22f)
                .alpha(0f)
                .translationY(-14f * resources.displayMetrics.density)
                .setDuration(380L)
                .setInterpolator(PathInterpolator(0.38f, 0.0f, 0.2f, 1.0f))
                .withEndAction {
                    try {
                        if (view != null && view.isAttachedToWindow) {
                            wm?.removeView(view)
                        }
                    } catch (_: Throwable) {}
                    expandedWindowView = null
                    isExpandedCardShowing = false

                    // Fluid restore entrance for compact capsule and meta-ball
                    capsuleView?.visibility = View.VISIBLE
                    capsuleView?.alpha = 0f
                    capsuleView?.scaleX = 0.65f
                    capsuleView?.scaleY = 0.65f
                    capsuleView?.animate()
                        ?.scaleX(1.0f)
                        ?.scaleY(1.0f)
                        ?.alpha(1.0f)
                        ?.setDuration(400L)
                        ?.setInterpolator(PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f))
                        ?.start()

                    if (isTorchOn && isRecording) {
                        metaBallView?.visibility = View.VISIBLE
                        metaBallView?.alpha = 0f
                        metaBallView?.scaleX = 0.5f
                        metaBallView?.scaleY = 0.5f
                        metaBallView?.animate()
                            ?.scaleX(1.0f)
                            ?.scaleY(1.0f)
                            ?.alpha(1.0f)
                            ?.setDuration(400L)
                            ?.setInterpolator(PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f))
                            ?.start()
                    }
                    refreshIslandState(animate = false)
                }
                .start()
        } ?: run {
            try {
                if (view != null && view.isAttachedToWindow) {
                    wm?.removeView(view)
                }
            } catch (_: Throwable) {}
            expandedWindowView = null
            isExpandedCardShowing = false
            capsuleView?.visibility = View.VISIBLE
            capsuleView?.alpha = 1.0f
            capsuleView?.scaleX = 1.0f
            capsuleView?.scaleY = 1.0f
            resetStatusBarSides()
            refreshIslandState(animate = false)
        }
    }

    private fun ensureStatusBarSideViews() {
        if (statusBarStartSide != null && statusBarEndSide != null) return
        val rootBar = (parent as? ViewGroup) ?: (rootView as? ViewGroup)
        if (rootBar != null) {
            statusBarStartSide = rootBar.findViewById(R.id.status_bar_start_side_content)
                ?: rootBar.findViewById(R.id.status_bar_start_side_container)
            statusBarEndSide = rootBar.findViewById(R.id.status_bar_end_side_content)
                ?: rootBar.findViewById(R.id.status_bar_end_side_container)
        }
    }

    private fun animateStatusBarSidesOnExpand() {
        ensureStatusBarSideViews()
        val density = resources.displayMetrics.density
        val shiftDistance = 56f * density // 56dp slide outwards

        statusBarStartSide?.let { startSide ->
            startSide.animate().cancel()
            startSide.animate()
                .translationX(-shiftDistance)
                .alpha(0.0f)
                .setDuration(420L)
                .setInterpolator(PathInterpolator(0.32f, 0.0f, 0.12f, 1.0f))
                .start()
        }

        statusBarEndSide?.let { endSide ->
            endSide.animate().cancel()
            endSide.animate()
                .translationX(shiftDistance)
                .alpha(0.0f)
                .setDuration(420L)
                .setInterpolator(PathInterpolator(0.32f, 0.0f, 0.12f, 1.0f))
                .start()
        }
    }

    private fun animateStatusBarSidesOnCollapse() {
        ensureStatusBarSideViews()

        statusBarStartSide?.let { startSide ->
            startSide.animate().cancel()
            startSide.animate()
                .translationX(0f)
                .alpha(1.0f)
                .setDuration(440L)
                .setInterpolator(PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f))
                .start()
        }

        statusBarEndSide?.let { endSide ->
            endSide.animate().cancel()
            endSide.animate()
                .translationX(0f)
                .alpha(1.0f)
                .setDuration(440L)
                .setInterpolator(PathInterpolator(0.18f, 1.0f, 0.35f, 1.0f))
                .start()
        }
    }

    private fun resetStatusBarSides() {
        ensureStatusBarSideViews()
        statusBarStartSide?.let {
            it.animate().cancel()
            it.translationX = 0f
            it.alpha = 1.0f
        }
        statusBarEndSide?.let {
            it.animate().cancel()
            it.translationX = 0f
            it.alpha = 1.0f
        }
    }

    // --- Action Handlers ---

    private fun toggleTorch() {
        try {
            val secureTorch = try {
                Settings.Secure.getInt(context.contentResolver, Settings.Secure.FLASHLIGHT_ENABLED, 0) == 1
            } catch (_: Throwable) { false }
            val currentOn = isTorchOn || flashlightController?.isEnabled == true || secureTorch
            val targetState = !currentOn

            flashlightController?.let { controller ->
                controller.setFlashlight(targetState)
            } ?: run {
                cameraManager?.let { cm ->
                    val id = rearCameraId ?: cm.cameraIdList.firstOrNull()
                    if (id != null) {
                        cm.setTorchMode(id, targetState)
                    }
                }
            }
            isTorchOn = targetState
            refreshIslandState(animate = true)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to toggle flashlight", t)
        }
    }

    private fun stopScreenRecording() {
        Log.d(TAG, "Dynamic Island: Stopping screen recording via multi-layered dispatch")

        // Layer 1: Trigger active screen record notification's stop PendingIntent directly
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.activeNotifications?.forEach { sbn ->
                val channelId = sbn.notification.channelId
                if (channelId == "screen_record" || sbn.id in 4273..4285) {
                    sbn.notification.actions?.forEach { act ->
                        try {
                            act.actionIntent?.send()
                            Log.d(TAG, "Sent stop via notification action intent")
                        } catch (e: Throwable) {
                            Log.w(TAG, "Failed sending notification actionIntent", e)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed inspecting notification actions", t)
        }

        // Layer 2: Send stop intent to ScreenRecordingService (New Android 15/17 Coroutine/Flow Service)
        try {
            val intent1 = Intent(context, ScreenRecordingService::class.java).apply {
                action = ScreenRecordingService.ACTION_STOP
                putExtra(ScreenRecordingService.EXTRA_STOP_REASON, StopReason.STOP_HOST_APP)
            }
            context.startService(intent1)
            Log.d(TAG, "Sent startService with ACTION_STOP to ScreenRecordingService")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to startService on ScreenRecordingService", t)
        }

        // Layer 3: Send stop intent to RecordingService (Legacy Foreground Service)
        try {
            val currentUserId = ActivityManager.getCurrentUser()
            val intent2 = Intent(context, RecordingService::class.java).apply {
                action = "com.android.systemui.screenrecord.STOP_FROM_NOTIF"
                putExtra("extra_stopReason", StopReason.STOP_HOST_APP)
                putExtra(Intent.EXTRA_USER_HANDLE, currentUserId)
            }
            context.startService(intent2)
            Log.d(TAG, "Sent startService with STOP_FROM_NOTIF to RecordingService")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to startService on RecordingService", t)
        }

        // Layer 4: Broadcast/Service PendingIntent with interactive BroadcastOptions
        try {
            val currentUserId = ActivityManager.getCurrentUser()
            val stopIntent3 = RecordingService.getStopIntent(context).apply {
                putExtra(Intent.EXTRA_USER_HANDLE, currentUserId)
                putExtra("extra_stopReason", StopReason.STOP_HOST_APP)
            }
            val bOptions = BroadcastOptions.makeBasic().setInteractive(true).toBundle()
            PendingIntent.getService(
                context,
                RecordingService.REQUEST_CODE,
                stopIntent3,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ).send(bOptions)
            Log.d(TAG, "Sent PendingIntent with BroadcastOptions to RecordingService")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to send PendingIntent to RecordingService", t)
        }

        // Layer 5: Fallback via ScreenRecordUxController
        try {
            screenRecordController?.stopRecording(StopReason.STOP_HOST_APP)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to stop recording via screenRecordController", t)
        }

        isRecording = false
        recordingStartTimestamp = 0L
        refreshIslandState(animate = true)
    }

    private fun startRecordTicker() {
        mainHandler.removeCallbacks(recordTickerRunnable)
        mainHandler.post(recordTickerRunnable)
    }

    private fun stopRecordTicker() {
        mainHandler.removeCallbacks(recordTickerRunnable)
    }

    private fun updateRecordTimer() {
        val elapsed = if (recordingStartTimestamp > 0) {
            Math.max(0L, (SystemClock.elapsedRealtime() - recordingStartTimestamp) / 1000L)
        } else {
            0L
        }
        val mins = elapsed / 60
        val secs = elapsed % 60
        val timeString = if (mins >= 60) {
            val hours = mins / 60
            val remMins = mins % 60
            String.format("%02d:%02d:%02d", hours, remMins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }

        recordTimerView?.text = timeString
        expandedRecordTimer?.text = timeString

        // Subtle pulse on the record red dot
        recordDotView?.let { dot ->
            dot.animate().alpha(0.35f).setDuration(400L).withEndAction {
                dot.animate().alpha(1.0f).setDuration(400L).start()
            }.start()
        }
    }

    private fun animateBoltPulse() {
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
    }

    private fun isDeviceProvisioned(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 1
        } catch (_: Throwable) {
            false
        }
    }

    private fun isFeatureEnabled(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, "dynamic_island_charging", 0) == 1
        } catch (_: Throwable) {
            false
        }
    }

    enum class IslandSlot {
        NONE,
        CHARGING,
        TORCH,
        RECORD
    }

    companion object {
        private const val TAG = "DynamicIsland"
        const val ACTION_TEST_DYNAMIC_ISLAND = "com.android.systemui.action.TEST_DYNAMIC_ISLAND"
    }
}
