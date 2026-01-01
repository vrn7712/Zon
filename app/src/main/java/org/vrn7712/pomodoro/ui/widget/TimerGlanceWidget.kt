/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon - a minimalist pomodoro timer for Android.
 *
 * Zon is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.vrn7712.pomodoro.ui.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.GlanceTheme.colors
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.vrn7712.pomodoro.MainActivity
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.service.TimerService
import org.vrn7712.pomodoro.ui.widget.ZonWidgetSize.Height2

class TimerGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            key(LocalSize.current) {
                GlanceTheme {
                    TimerWidgetContent()
                }
            }
        }
    }
}

@Composable
fun TimerWidgetContent() {
    val context = LocalContext.current
    val size = LocalSize.current
    val roundedCornersSupported = Build.VERSION.SDK_INT >= 31
    
    // State from DataStore
    val time = currentState(key = stringPreferencesKey("time")) ?: "25:00"
    val isRunning = currentState(key = booleanPreferencesKey("isRunning")) ?: false
    val progress = currentState(key = floatPreferencesKey("progress")) ?: 0f
    val timerModeName = currentState(key = stringPreferencesKey("timerMode")) ?: "FOCUS"
    
    val isFocus = timerModeName == "FOCUS"
    val isShortBreak = timerModeName == "SHORT_BREAK"
    
    // Mode display text
    val modeText = when (timerModeName) {
        "FOCUS" -> context.getString(R.string.focus)
        "SHORT_BREAK" -> context.getString(R.string.short_break)
        "LONG_BREAK" -> context.getString(R.string.long_break)
        else -> context.getString(R.string.focus)
    }
    
    // Mode icon
    val modeIcon = when (timerModeName) {
        "FOCUS" -> R.drawable.focus_icon
        "SHORT_BREAK" -> R.drawable.short_break_icon
        "LONG_BREAK" -> R.drawable.long_break_icon
        else -> R.drawable.focus_icon
    }

    // Create progress ring bitmap
    val progressBitmap = createCircularProgressBitmap(
        progress = progress,
        activeColor = colors.primary.getColor(context).toArgb(),
        inactiveColor = colors.surfaceVariant.getColor(context).toArgb(),
        sizePx = 600,
        isFocus = isFocus
    )
    
    // Responsive layout - calculate based on actual widget size
    val showTitleBar = size.height >= 150.dp
    val showControls = size.height >= 120.dp
    val buttonSize = if (size.width > 180.dp) 44.dp else 36.dp
    val playButtonSize = if (size.width > 180.dp) 52.dp else 44.dp
    val ringSize = minOf(size.width - 24.dp, size.height - (if (showTitleBar) 80.dp else 60.dp))
    
    // Intent to open MainActivity (Timer screen)
    val timerIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(
                if (roundedCornersSupported)
                    GlanceModifier.background(colors.widgetBackground).cornerRadius(16.dp)
                else
                    GlanceModifier.background(
                        ImageProvider(R.drawable.rounded_16dp),
                        colorFilter = ColorFilter.tint(colors.widgetBackground)
                    )
            )
            .clickable(actionStartActivity(timerIntent)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Title Bar (Mode indicator) - only on taller widgets
            if (showTitleBar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Image(
                        provider = ImageProvider(modeIcon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.primary),
                        modifier = GlanceModifier.size(18.dp)
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = modeText,
                        style = TextStyle(
                            color = colors.onSurface,
                            fontSize = typography.labelLarge.fontSize,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
            }
            
            // Timer Ring with Time - takes most of the space
            Box(
                contentAlignment = Alignment.Center,
                modifier = GlanceModifier
                    .size(ringSize.coerceAtLeast(60.dp))
            ) {
                // Progress Ring
                Image(
                    provider = ImageProvider(progressBitmap),
                    contentDescription = context.getString(R.string.timer_progress),
                    modifier = GlanceModifier.fillMaxSize()
                )
                
                // Timer Text - Large, Bold, Premium
                Text(
                    text = time,
                    style = TextStyle(
                        color = colors.onSurface,
                        fontSize = when {
                            ringSize > 150.dp -> 38.sp
                            ringSize > 100.dp -> 28.sp
                            else -> 20.sp
                        },
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
            }
            
            // Control Buttons - only if enough height
            if (showControls) {
                Spacer(GlanceModifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    // Reset Button
                    ControlButton(
                        iconRes = R.drawable.restart,
                        contentDesc = context.getString(R.string.restart),
                        actionClass = ResetTimerAction::class.java,
                        size = buttonSize,
                        iconSize = buttonSize / 2,
                        backgroundColor = colors.secondaryContainer,
                        iconColor = colors.onSecondaryContainer
                    )
                    
                    Spacer(GlanceModifier.width(8.dp))
                    
                    // Play/Pause Button - Larger, Primary action
                    ControlButton(
                        iconRes = if (isRunning) R.drawable.pause_large else R.drawable.play_large,
                        contentDesc = if (isRunning) context.getString(R.string.pause) else context.getString(R.string.start),
                        actionClass = ToggleTimerAction::class.java,
                        size = playButtonSize,
                        iconSize = playButtonSize / 2,
                        backgroundColor = colors.primary,
                        iconColor = colors.onPrimary
                    )
                    
                    Spacer(GlanceModifier.width(8.dp))
                    
                    // Skip Button
                    ControlButton(
                        iconRes = R.drawable.skip_next,
                        contentDesc = context.getString(R.string.skip),
                        actionClass = SkipTimerAction::class.java,
                        size = buttonSize,
                        iconSize = buttonSize / 2,
                        backgroundColor = colors.secondaryContainer,
                        iconColor = colors.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Creates a circular progress ring bitmap with optional wiggle effect for break mode.
 * 
 * Break mode features a visible wave pattern with:
 * - 7 waves around the full circle
 * - Moderate amplitude for clear visual effect
 * - Thick, anti-aliased strokes for premium feel
 */
fun createCircularProgressBitmap(
    progress: Float,
    activeColor: Int,
    inactiveColor: Int,
    sizePx: Int = 600,
    isFocus: Boolean
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Stroke and wave configuration
    val baseStrokeWidth = sizePx * 0.05f
    val isWavy = !isFocus
    val waveAmplitude = if (isWavy) sizePx * 0.035f else 0f  // Balanced wave amplitude
    val padding = baseStrokeWidth + waveAmplitude + 12f
    
    val rect = RectF(padding, padding, sizePx - padding, sizePx - padding)
    val baseRadius = (sizePx - 2 * padding) / 2f
    val centerX = sizePx / 2f
    val centerY = sizePx / 2f

    val activePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = activeColor
        strokeWidth = baseStrokeWidth
    }
    
    val inactivePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = baseStrokeWidth
        strokeCap = Paint.Cap.ROUND
        color = inactiveColor
    }

    if (isFocus) {
        // FOCUS MODE: Clean solid arc
        canvas.drawArc(rect, 0f, 360f, false, inactivePaint)
        if (progress > 0f) {
            val sweepAngle = (progress * 360f).coerceIn(0f, 360f)
            canvas.drawArc(rect, -90f, sweepAngle, false, activePaint)
        }
    } else {
        // BREAK MODE: Elegant wave animation
        val sweepAngle = (progress * 360f).coerceIn(0f, 360f)
        
        // Draw inactive portion (also wavy for consistency)
        if (progress < 1f) {
            val startInactive = -90f + sweepAngle
            val sweepInactive = 360f - sweepAngle
            canvas.drawArc(rect, startInactive, sweepInactive, false, inactivePaint)
        }
        
        // Draw active wavy portion
        if (progress > 0f) {
            // Wave parameters: 7 waves for balanced wiggle effect
            val numWaves = 7.0
            val waveFrequency = numWaves  // waves per full rotation
            
            val path = android.graphics.Path()
            val stepSize = 0.3f  // Smaller steps for smoother curve
            var currentAngleDeg = 0f
            
            // Starting point at top (-90 degrees)
            val startRad = Math.toRadians(-90.0)
            val startR = baseRadius + waveAmplitude * Math.sin(0.0).toFloat()
            path.moveTo(
                (centerX + startR * Math.cos(startRad)).toFloat(),
                (centerY + startR * Math.sin(startRad)).toFloat()
            )

            while (currentAngleDeg < sweepAngle) {
                currentAngleDeg += stepSize
                if (currentAngleDeg > sweepAngle) currentAngleDeg = sweepAngle

                val absAngle = -90f + currentAngleDeg
                val absRad = Math.toRadians(absAngle.toDouble())
                
                // Calculate wave phase based on angle traveled
                // phase = (angle / 360) * 2π * numWaves
                val normalizedAngle = currentAngleDeg / 360.0
                val phase = normalizedAngle * 2.0 * Math.PI * waveFrequency
                val oscillation = Math.sin(phase)
                
                val r = baseRadius + waveAmplitude * oscillation.toFloat()
                
                val x = centerX + r * Math.cos(absRad).toFloat()
                val y = centerY + r * Math.sin(absRad).toFloat()
                path.lineTo(x, y)
            }
            canvas.drawPath(path, activePaint)
        }
    }

    return bitmap
}

@Composable
fun ControlButton(
    iconRes: Int,
    contentDesc: String,
    actionClass: Class<out ActionCallback>,
    size: Dp,
    iconSize: Dp,
    backgroundColor: ColorProvider,
    iconColor: ColorProvider
) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(backgroundColor)
            .cornerRadius(size / 2)
            .clickable(actionRunCallback(actionClass)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = contentDesc,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

class ToggleTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.Actions.TOGGLE.toString()
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            // Handle Android 12+ ForegroundServiceStartNotAllowedException
            // when app is in background without exemptions
            e.printStackTrace()
        }
    }
}

class ResetTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.Actions.RESET.toString()
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class SkipTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = android.content.Intent(context, TimerService::class.java).apply {
            action = TimerService.Actions.SKIP.toString()
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
