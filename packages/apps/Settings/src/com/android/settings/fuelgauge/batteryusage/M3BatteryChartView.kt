/*
 * Copyright (C) 2026 Palapa OS / Shinkai Project
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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class M3BatteryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val MODE_DAILY = 0
        const val MODE_SINCE_LAST_CHARGE = 1
    }

    var chartMode: Int = MODE_DAILY
        set(value) {
            field = value
            invalidate()
        }

    var currentBatteryLevel: Int = 56
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var averageUsageValue: Int = 23
        set(value) {
            field = value.coerceIn(5, 100)
            invalidate()
        }

    // Daily bars data (values 0..100%)
    private var dailyUsageValues = floatArrayOf(12f, 18f, 25f, 15f, 14f, 28f, 5f)
    private var dayLabels = arrayOf("M", "S", "S", "R", "K", "J", "S")
    var currentDayIndex = 6
        set(value) {
            field = value.coerceIn(0, 6)
            invalidate()
        }

    // Colors matching M3 Solid Monet palette
    private val colorTrack = Color.parseColor("#25332B")
    private val colorInactiveBar = Color.parseColor("#2E4D3B")
    private val colorActiveBar = Color.parseColor("#7EE2A7")
    private val colorDashedLine = Color.parseColor("#648671")
    private val colorTextInactive = Color.parseColor("#A3C6B1")
    private val colorTextActive = Color.parseColor("#044426")
    private val colorAvgBadgeBg = Color.parseColor("#B4F0CE")

    // Paints
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorTrack
    }

    private val inactiveBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorInactiveBar
    }

    private val activeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorActiveBar
    }

    private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = colorDashedLine
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }

    private val avgBadgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorAvgBadgeBg
    }

    private val avgBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTextActive
        textSize = 28f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 34f
    }

    private val activePercentageTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorActiveBar
        textSize = 30f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val activeDayCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorActiveBar
    }

    init {
        initDaysFromLocale()
    }

    private fun initDaysFromLocale() {
        val locale = Locale.getDefault()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEEEE", locale)
        for (i in 0 until 7) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, i - 6)
            }
            dayLabels[i] = sdf.format(targetCal.time).uppercase(locale)
        }
        currentDayIndex = 6
    }

    fun setDailyData(values: FloatArray, avg: Int) {
        if (values.size >= 7) {
            for (i in 0 until 7) {
                dailyUsageValues[i] = values[i].coerceIn(2f, 100f)
            }
        }
        averageUsageValue = avg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawDailyChart(canvas)
    }

    private fun drawDailyChart(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        val paddingLeft = 36f
        val paddingRight = 130f // Space on right for the 23 Avg. badge
        val paddingTop = 50f   // Space for 5% active label above bar
        val paddingBottom = 70f // Space for M S S R K J S letters

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val maxUsage = 100f

        // 1. Draw 7 Thick Capsule Bars
        val barCount = 7
        val slotWidth = chartWidth / barCount
        val barWidth = (slotWidth * 0.72f).coerceAtMost(56f)
        val barRadius = barWidth / 2f

        for (i in 0 until barCount) {
            val cx = paddingLeft + (i * slotWidth) + (slotWidth / 2f)
            val left = cx - barRadius
            val right = cx + barRadius
            val topTrack = paddingTop
            val bottomTrack = paddingTop + chartHeight

            // Draw full capsule track
            val trackRect = RectF(left, topTrack, right, bottomTrack)
            canvas.drawRoundRect(trackRect, barRadius, barRadius, trackPaint)

            // Draw usage progress capsule inside
            val valUsage = dailyUsageValues.getOrElse(i) { 15f }.coerceIn(2f, maxUsage)
            val barH = (chartHeight * (valUsage / maxUsage)).coerceAtLeast(barWidth)
            val topBar = bottomTrack - barH

            val progressRect = RectF(left, topBar, right, bottomTrack)
            val paintToUse = if (i == currentDayIndex) activeBarPaint else inactiveBarPaint
            canvas.drawRoundRect(progressRect, barRadius, barRadius, paintToUse)

            // Above active bar: draw usage text (e.g. "5%")
            if (i == currentDayIndex) {
                val percentStr = "${valUsage.toInt()}%"
                canvas.drawText(percentStr, cx, topBar - 14f, activePercentageTextPaint)
            }

            // Bottom day letter
            val labelY = height - 20f
            if (i == currentDayIndex) {
                // Circle highlight around active day
                val circleRadius = 26f
                canvas.drawCircle(cx, labelY - 10f, circleRadius, activeDayCirclePaint)
                textPaint.color = colorTextActive
                textPaint.isFakeBoldText = true
            } else {
                textPaint.color = colorTextInactive
                textPaint.isFakeBoldText = false
            }
            canvas.drawText(dayLabels.getOrElse(i) { "S" }, cx, labelY, textPaint)
        }

        // 2. Draw Dashed Average Baseline
        val avgY = paddingTop + chartHeight * (1f - (averageUsageValue.toFloat() / maxUsage))
        val dashPath = Path().apply {
            moveTo(paddingLeft - 8f, avgY)
            lineTo(width - paddingRight + 12f, avgY)
        }
        canvas.drawPath(dashPath, dashedLinePaint)

        // 3. Draw Solid Pill Badge: "23 Avg."
        val badgeW = 104f
        val badgeH = 44f
        val badgeLeft = width - paddingRight + 18f
        val badgeRight = badgeLeft + badgeW
        val badgeTop = avgY - (badgeH / 2f)
        val badgeBottom = avgY + (badgeH / 2f)
        val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)
        canvas.drawRoundRect(badgeRect, badgeH / 2f, badgeH / 2f, avgBadgeBgPaint)

        val badgeText = "$averageUsageValue Avg."
        val badgeFontMetrics = avgBadgeTextPaint.fontMetrics
        val textCenterY = badgeRect.centerY() - (badgeFontMetrics.top + badgeFontMetrics.bottom) / 2f
        canvas.drawText(badgeText, badgeRect.centerX(), textCenterY, avgBadgeTextPaint)
    }
}
