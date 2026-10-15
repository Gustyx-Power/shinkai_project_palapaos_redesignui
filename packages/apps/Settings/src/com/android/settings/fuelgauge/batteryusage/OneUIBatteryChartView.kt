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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    var startBatteryLevel: Int = 100
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    var estimatedTimeLeftStr: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var averageUsageValue: Int = 120
        set(value) {
            field = value.coerceIn(10, 300)
            invalidate()
        }

    // Daily bars data (values relative to max 300%)
    private var dailyUsageValues = floatArrayOf(80f, 65f, 140f, 75f, 70f, 110f, 45f)
    private var dayLabels = arrayOf("S", "S", "R", "K", "J", "S", "M")
    private var currentDayIndex = 6

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
        strokeWidth = 4.5f
    }
    private val pinBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pinTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        initDaysFromLocale()
        updateColors()
    }

    private fun initDaysFromLocale() {
        val locale = Locale.getDefault()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("EEEEE", locale) // single-letter day of week
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
                dailyUsageValues[i] = values[i].coerceIn(5f, 300f)
            }
        }
        averageUsageValue = avg
        invalidate()
    }

    private fun updateColors() {
        val monetPrimary = ContextCompat.getColor(context, R.color.oneui_battery_blue)
        val textPrimary = ContextCompat.getColor(context, R.color.oneui_battery_text_primary)
        val textSecondary = ContextCompat.getColor(context, R.color.oneui_battery_text_secondary)
        val trackColor = ContextCompat.getColor(context, R.color.oneui_battery_track)

        barPaint.color = monetPrimary
        bgBarTrackPaint.color = Color.argb(45, Color.red(trackColor), Color.green(trackColor), Color.blue(trackColor))
        dashedLinePaint.color = Color.argb(120, Color.red(textSecondary), Color.green(textSecondary), Color.blue(textSecondary))
        curveStrokePaint.color = monetPrimary
        pinBgPaint.color = monetPrimary
        pinTextPaint.color = Color.WHITE
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

        val paddingLeft = 32f
        val paddingRight = 85f
        val paddingTop = 28f
        val paddingBottom = 65f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val maxUsage = 300f

        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.LEFT
        val textSecondary = ContextCompat.getColor(context, R.color.oneui_battery_text_secondary)
        textPaint.color = textSecondary

        // 1. Draw Y-Axis scale
        canvas.drawText("300", width - paddingRight + 14f, paddingTop + 18f, textPaint)
        canvas.drawText("0", width - paddingRight + 14f, paddingTop + chartHeight - 8f, textPaint)
        canvas.drawText("(%)", width - paddingRight + 14f, paddingTop + chartHeight + 14f, textPaint)

        // 2. Draw Average Baseline
        val avgY = paddingTop + chartHeight * (1f - (averageUsageValue.toFloat() / maxUsage))
        val dashPath = Path().apply {
            moveTo(paddingLeft, avgY)
            lineTo(width - paddingRight, avgY)
        }
        canvas.drawPath(dashPath, dashedLinePaint)

        // Draw Avg Badge
        val avgBadgeRect = RectF(width - paddingRight + 8f, avgY - 18f, width - 8f, avgY + 18f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 140, 140, 140)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(avgBadgeRect, 10f, 10f, badgeBgPaint)
        val avgBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 19f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("$averageUsageValue", avgBadgeRect.centerX(), avgY - 2f, avgBadgeTextPaint)
        canvas.drawText("Avg.", avgBadgeRect.centerX(), avgY + 14f, avgBadgeTextPaint)

        // 3. Draw 7 Vertical Pill Bars
        val barCount = 7
        val slotWidth = chartWidth / barCount
        val barWidth = 24f
        val barRadius = barWidth / 2f

        for (i in 0 until barCount) {
            val cx = paddingLeft + (i * slotWidth) + (slotWidth / 2f)

            // Lane highlight track
            val trackColor = ContextCompat.getColor(context, R.color.oneui_battery_track)
            val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                val laneAlpha = if (i % 2 == 0) 15 else 35
                color = Color.argb(laneAlpha, Color.red(trackColor), Color.green(trackColor), Color.blue(trackColor))
            }
            canvas.drawRect(trackRect, lanePaint)

            // Dynamic Bar
            val valUsage = dailyUsageValues.getOrElse(i) { 50f }.coerceIn(8f, maxUsage)
            val barH = chartHeight * (valUsage / maxUsage)
            val top = paddingTop + chartHeight - barH
            val bottom = paddingTop + chartHeight

            val barRect = RectF(cx - barRadius, top, cx + barRadius, bottom)
            canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)

            // Day label
            val labelY = paddingTop + chartHeight + 32f
            if (i == currentDayIndex) {
                // Circle highlight around today
                val circleRadius = 17f
                val dayCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ContextCompat.getColor(context, R.color.oneui_battery_text_primary)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, labelY - 6f, circleRadius, dayCirclePaint)

                textPaint.color = ContextCompat.getColor(context, R.color.oneui_battery_card_bg)
                textPaint.isFakeBoldText = true
            } else {
                textPaint.color = textSecondary
                textPaint.isFakeBoldText = false
            }
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 22f
            canvas.drawText(dayLabels.getOrElse(i) { "D" }, cx, labelY, textPaint)
        }

        // 4. Legend at bottom: "● This week"
        val legendY = height - 10f
        legendPaint.color = ContextCompat.getColor(context, R.color.oneui_battery_blue)
        canvas.drawCircle(paddingLeft + 6f, legendY - 6f, 6f, legendPaint)
        textPaint.color = textSecondary
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = false
        canvas.drawText(context.getString(R.string.oneui_battery_this_week), paddingLeft + 20f, legendY, textPaint)
    }

    private fun drawSinceLastChargeChart(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        val paddingLeft = 32f
        val paddingRight = 75f
        val paddingTop = 36f
        val paddingBottom = 65f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        val textSecondary = ContextCompat.getColor(context, R.color.oneui_battery_text_secondary)
        val monetPrimary = ContextCompat.getColor(context, R.color.oneui_battery_blue)

        // 1. Y-axis
        textPaint.textSize = 22f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = textSecondary
        canvas.drawText("100", width - paddingRight + 12f, paddingTop + 18f, textPaint)
        canvas.drawText("0", width - paddingRight + 12f, paddingTop + chartHeight - 8f, textPaint)
        canvas.drawText("(%)", width - paddingRight + 12f, paddingTop + chartHeight + 14f, textPaint)

        // Top 100% baseline
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(45, 150, 150, 150)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawLine(paddingLeft, paddingTop + 10f, width - paddingRight, paddingTop + 10f, baselinePaint)

        // 2. Real Curve from startBatteryLevel to currentBatteryLevel
        val currentProgress = (100 - currentBatteryLevel) / 100f
        val currentX = paddingLeft + (chartWidth * 0.45f)
        val currentY = paddingTop + (chartHeight * currentProgress) + 10f

        val path = Path().apply {
            moveTo(paddingLeft, paddingTop + 10f)
            cubicTo(
                paddingLeft + (currentX - paddingLeft) * 0.4f, paddingTop + 12f,
                paddingLeft + (currentX - paddingLeft) * 0.8f, currentY - 10f,
                currentX, currentY
            )
        }

        // Fill area gradient
        val fillPath = Path(path).apply {
            lineTo(currentX, paddingTop + chartHeight)
            lineTo(paddingLeft, paddingTop + chartHeight)
            close()
        }

        areaFillPaint.shader = LinearGradient(
            0f, paddingTop, 0f, paddingTop + chartHeight,
            Color.argb(85, Color.red(monetPrimary), Color.green(monetPrimary), Color.blue(monetPrimary)),
            Color.argb(10, Color.red(monetPrimary), Color.green(monetPrimary), Color.blue(monetPrimary)),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, areaFillPaint)

        // Draw solid curve line
        curveStrokePaint.color = monetPrimary
        canvas.drawPath(path, curveStrokePaint)

        // 3. Current Level Vertical Indicator + Pin Bubble
        val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, Color.red(monetPrimary), Color.green(monetPrimary), Color.blue(monetPrimary))
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawLine(currentX, currentY, currentX, paddingTop + chartHeight, indicatorPaint)

        // Pin bubble [ 65% ]
        val pinWidth = 84f
        val pinHeight = 38f
        val pinRect = RectF(currentX + 8f, currentY - (pinHeight / 2f), currentX + 8f + pinWidth, currentY + (pinHeight / 2f))
        canvas.drawRoundRect(pinRect, 19f, 19f, pinBgPaint)

        pinTextPaint.textSize = 21f
        canvas.drawText("$currentBatteryLevel%", pinRect.centerX(), pinRect.centerY() + 7f, pinTextPaint)

        // Start badge ⚡100
        val startBadgeRect = RectF(paddingLeft - 8f, paddingTop - 22f, paddingLeft + 68f, paddingTop + 10f)
        val startBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 120, 120, 120)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(startBadgeRect, 14f, 14f, startBadgePaint)
        val startTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 19f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚡$startBatteryLevel", startBadgeRect.centerX(), startBadgeRect.centerY() + 6f, startTextPaint)

        // 4. Dotted Projection Line to 0%
        val projectionEndX = width - paddingRight
        val projectionEndY = paddingTop + chartHeight
        val dottedProjectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = monetPrimary
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
        }
        canvas.drawLine(currentX, currentY, projectionEndX, projectionEndY, dottedProjectionPaint)

        // Bottom label
        if (estimatedTimeLeftStr.isNotEmpty()) {
            textPaint.color = textSecondary
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 22f
            canvas.drawText(estimatedTimeLeftStr, (currentX + projectionEndX) / 2f, paddingTop + chartHeight + 32f, textPaint)
        }

        // 5. Legends at bottom
        val legendY = height - 10f
        var lx = paddingLeft

        legendPaint.color = monetPrimary
        canvas.drawCircle(lx + 6f, legendY - 6f, 5f, legendPaint)
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = 19f
        canvas.drawText(context.getString(R.string.oneui_battery_level_legend), lx + 16f, legendY, textPaint)

        lx += 170f
        legendPaint.color = Color.GRAY
        canvas.drawCircle(lx + 6f, legendY - 6f, 5f, legendPaint)
        canvas.drawText(context.getString(R.string.oneui_battery_estimated_level_legend), lx + 16f, legendY, textPaint)
    }
}
