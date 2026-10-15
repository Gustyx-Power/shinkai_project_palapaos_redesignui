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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.android.settings.R
import java.util.Calendar

class OneUIBatteryChartView @JvmOverloads constructor(
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

    var currentBatteryLevel: Int = 81
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var estimatedTimeLeftStr: String = "8 h 35 m left"
        set(value) {
            field = value
            invalidate()
        }

    // Daily bars data (values relative to max 300%)
    private var dailyUsageValues = floatArrayOf(180f, 150f, 230f, 150f, 140f, 180f, 60f)
    private val dayLabels = arrayOf("T", "W", "T", "F", "S", "S", "M")
    private var currentDayIndex = 6 // Monday as in screenshot

    // Paints
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val bgBarTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val areaFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val curveStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val pinBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pinTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    init {
        updateColors()
        calculateCurrentDayIndex()
    }

    private fun calculateCurrentDayIndex() {
        val cal = Calendar.getInstance()
        // Convert Calendar.DAY_OF_WEEK to 0-6 index where Monday is last or according to week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Set labels according to recent 7 days
        val shortDays = arrayOf("S", "M", "T", "W", "T", "F", "S")
        for (i in 0 until 7) {
            val targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, i - 6)
            }
            dayLabels[i] = shortDays[targetCal.get(Calendar.DAY_OF_WEEK) - 1]
        }
        currentDayIndex = 6
    }

    fun updateDailyData(usages: FloatArray) {
        if (usages.isNotEmpty()) {
            for (i in usages.indices) {
                if (i < dailyUsageValues.size) {
                    dailyUsageValues[i] = usages[i]
                }
            }
            invalidate()
        }
    }

    private fun updateColors() {
        val blueColor = ContextCompat.getColor(context, R.color.oneui_battery_blue)
        val textPrimary = ContextCompat.getColor(context, R.color.oneui_battery_text_primary)
        val textSecondary = ContextCompat.getColor(context, R.color.oneui_battery_text_secondary)
        val trackColor = ContextCompat.getColor(context, R.color.oneui_battery_track)

        barPaint.color = blueColor
        bgBarTrackPaint.color = Color.argb(40, Color.red(trackColor), Color.green(trackColor), Color.blue(trackColor))
        dashedLinePaint.color = Color.argb(120, Color.red(textSecondary), Color.green(textSecondary), Color.blue(textSecondary))
        curveStrokePaint.color = Color.WHITE
        pinBgPaint.color = Color.WHITE
        pinTextPaint.color = Color.BLACK
        textPaint.color = textSecondary
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateColors()

        if (chartMode == MODE_DAILY) {
            drawDailyChart(canvas)
        } else {
            drawSinceLastChargeChart(canvas)
        }
    }

    private fun drawDailyChart(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        val paddingLeft = 40f
        val paddingRight = 90f // room for y-axis (300, 195 Avg, 0 (%))
        val paddingTop = 30f
        val paddingBottom = 70f // room for day labels and legend

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val maxUsage = 300f

        // 1. Draw Y-Axis scale and Avg Line
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.LEFT
        val textSecondary = ContextCompat.getColor(context, R.color.oneui_battery_text_secondary)
        textPaint.color = textSecondary

        // "300"
        canvas.drawText("300", width - paddingRight + 16f, paddingTop + 20f, textPaint)

        // "0 (%)"
        canvas.drawText("0", width - paddingRight + 16f, paddingTop + chartHeight - 10f, textPaint)
        canvas.drawText("(%)", width - paddingRight + 16f, paddingTop + chartHeight + 15f, textPaint)

        // Avg Line at 195
        val avgY = paddingTop + chartHeight * (1f - (195f / maxUsage))
        val dashPath = Path().apply {
            moveTo(paddingLeft, avgY)
            lineTo(width - paddingRight, avgY)
        }
        canvas.drawPath(dashPath, dashedLinePaint)

        // Draw "195 Avg." badge on the right
        val avgBadgeRect = RectF(width - paddingRight + 10f, avgY - 18f, width - 10f, avgY + 18f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 140, 140, 140)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(avgBadgeRect, 10f, 10f, badgeBgPaint)
        val avgBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("195", avgBadgeRect.centerX(), avgY - 2f, avgBadgeTextPaint)
        canvas.drawText("Avg.", avgBadgeRect.centerX(), avgY + 15f, avgBadgeTextPaint)

        // 2. Draw 7 vertical pill bars
        val barCount = 7
        val slotWidth = chartWidth / barCount
        val barWidth = 24f
        val barRadius = barWidth / 2f

        for (i in 0 until barCount) {
            val cx = paddingLeft + (i * slotWidth) + (slotWidth / 2f)

            // Background vertical track lane (subtle dark tint)
            val trackRect = RectF(cx - (slotWidth / 2.2f), paddingTop, cx + (slotWidth / 2.2f), paddingTop + chartHeight)
            val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (i % 2 == 0) Color.argb(15, 255, 255, 255) else Color.argb(30, 255, 255, 255)
            }
            canvas.drawRect(trackRect, lanePaint)

            // Active pill bar
            val valUsage = dailyUsageValues.getOrElse(i) { 100f }.coerceIn(10f, maxUsage)
            val barH = chartHeight * (valUsage / maxUsage)
            val top = paddingTop + chartHeight - barH
            val bottom = paddingTop + chartHeight

            val barRect = RectF(cx - barRadius, top, cx + barRadius, bottom)
            canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

            // Day label below
            val labelY = paddingTop + chartHeight + 35f
            if (i == currentDayIndex) {
                // Circle badge around today's label
                val circleRadius = 18f
                val dayCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, labelY - 7f, circleRadius, dayCirclePaint)

                textPaint.color = Color.BLACK
                textPaint.isFakeBoldText = true
            } else {
                textPaint.color = textSecondary
                textPaint.isFakeBoldText = false
            }
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 24f
            canvas.drawText(dayLabels.getOrElse(i) { "D" }, cx, labelY, textPaint)
        }

        // 3. Legend at bottom-left: "● This week"
        val legendY = height - 12f
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.oneui_battery_blue)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(paddingLeft + 8f, legendY - 6f, 6f, legendPaint)
        textPaint.color = textSecondary
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 22f
        textPaint.isFakeBoldText = false
        canvas.drawText(context.getString(R.string.oneui_battery_this_week), paddingLeft + 24f, legendY, textPaint)
    }

    private fun drawSinceLastChargeChart(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        val paddingLeft = 40f
        val paddingRight = 80f
        val paddingTop = 40f
        val paddingBottom = 70f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val textSecondary = ContextCompat.getColor(context, R.color.oneui_battery_text_secondary)

        // 1. Y-axis: 100, 0 (%)
        textPaint.textSize = 24f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = textSecondary
        canvas.drawText("100", width - paddingRight + 16f, paddingTop + 20f, textPaint)
        canvas.drawText("0", width - paddingRight + 16f, paddingTop + chartHeight - 10f, textPaint)
        canvas.drawText("(%)", width - paddingRight + 16f, paddingTop + chartHeight + 15f, textPaint)

        // 100% Top Baseline Line
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 150, 150, 150)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawLine(paddingLeft, paddingTop + 10f, width - paddingRight, paddingTop + 10f, baselinePaint)

        // 2. Area curve from 100% to currentBatteryLevel
        val currentProgress = (100 - currentBatteryLevel) / 100f
        val currentX = paddingLeft + (chartWidth * 0.35f)
        val currentY = paddingTop + (chartHeight * currentProgress * 0.5f) + 10f

        val path = Path().apply {
            moveTo(paddingLeft, paddingTop + 10f)
            quadTo(
                paddingLeft + (currentX - paddingLeft) * 0.5f,
                paddingTop + 15f,
                currentX,
                currentY
            )
        }

        // Fill area below path
        val fillPath = Path(path).apply {
            lineTo(currentX, paddingTop + chartHeight)
            lineTo(paddingLeft, paddingTop + chartHeight)
            close()
        }

        val blueColor = ContextCompat.getColor(context, R.color.oneui_battery_blue)
        areaFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + chartHeight,
            Color.argb(80, Color.red(blueColor), Color.green(blueColor), Color.blue(blueColor)),
            Color.argb(10, Color.red(blueColor), Color.green(blueColor), Color.blue(blueColor)),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, areaFillPaint)

        // Draw solid curve line
        curveStrokePaint.color = Color.argb(220, 255, 255, 255)
        canvas.drawPath(path, curveStrokePaint)

        // 3. Current Level Vertical Indicator + Pin Bubble
        val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawLine(currentX, currentY, currentX, paddingTop + chartHeight, indicatorPaint)

        // Pin bubble [ 81% ]
        val pinWidth = 80f
        val pinHeight = 40f
        val pinRect = RectF(currentX + 10f, currentY - (pinHeight / 2f), currentX + 10f + pinWidth, currentY + (pinHeight / 2f))
        canvas.drawRoundRect(pinRect, 20f, 20f, pinBgPaint)

        pinTextPaint.textSize = 22f
        canvas.drawText("$currentBatteryLevel%", pinRect.centerX(), pinRect.centerY() + 7f, pinTextPaint)

        // ⚡100 badge at top left
        val startBadgeRect = RectF(paddingLeft - 10f, paddingTop - 25f, paddingLeft + 70f, paddingTop + 10f)
        val startBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 100, 100, 100)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(startBadgeRect, 15f, 15f, startBadgePaint)
        val startTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚡100", startBadgeRect.centerX(), startBadgeRect.centerY() + 6f, startTextPaint)

        // 4. Dotted Projection Line to 0%
        val projectionEndX = width - paddingRight
        val projectionEndY = paddingTop + chartHeight
        val dottedProjectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blueColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        canvas.drawLine(currentX, currentY, projectionEndX, projectionEndY, dottedProjectionPaint)

        // Bottom axis label: "8 h 35 m left"
        textPaint.color = textSecondary
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 24f
        canvas.drawText(estimatedTimeLeftStr, (currentX + projectionEndX) / 2f, paddingTop + chartHeight + 35f, textPaint)

        // 5. Legends at bottom
        val legendY = height - 12f
        var lx = paddingLeft

        // White dot: Battery level
        legendPaint.color = Color.WHITE
        canvas.drawCircle(lx + 6f, legendY - 6f, 5f, legendPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 20f
        canvas.drawText(context.getString(R.string.oneui_battery_level_legend), lx + 18f, legendY, textPaint)

        lx += 180f
        // Grey dot: Estimated level
        legendPaint.color = Color.GRAY
        canvas.drawCircle(lx + 6f, legendY - 6f, 5f, legendPaint)
        canvas.drawText(context.getString(R.string.oneui_battery_estimated_level_legend), lx + 18f, legendY, textPaint)
    }

    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
}
