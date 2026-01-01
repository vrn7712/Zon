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
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.GlanceTheme.colors
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.vrn7712.pomodoro.MainActivity
import org.vrn7712.pomodoro.R
import org.vrn7712.pomodoro.data.AppStatRepository
import org.vrn7712.pomodoro.data.Stat
import org.vrn7712.pomodoro.ui.widget.ZonWidgetSize.Width4
import org.vrn7712.pomodoro.utils.millisecondsToHoursMinutes

class FocusHistoryGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {
        val statRepository = AppStatRepository.get(context)
        val history = statRepository.getLastNDaysStats(30).first().reversed()

        provideContent {
            val size = LocalSize.current
            val historyToShow = history.takeLast(((size.width.value - 32) / 24).toInt())
            key(size) {
                GlanceTheme {
                    Content(
                        historyToShow,
                        historyToShow.maxByOrNull { it.totalFocusTime() }?.totalFocusTime() ?: 1L
                    )
                }
            }
        }
    }

    @Composable
    private fun Content(history: List<Stat>, maxFocus: Long) {
        val context = LocalContext.current
        val size = LocalSize.current
        val scope = rememberCoroutineScope()
        val roundedCornersSupported = Build.VERSION.SDK_INT >= 31
        
        // Navigate to Stats screen when widget is tapped
        val statsIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "stats")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Scaffold(
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(R.drawable.monitoring),
                    title = context.getString(R.string.focus_history),
                    iconColor = colors.primary,
                    textColor = colors.onPrimaryContainer,
                    actions = {
                        if (size.width >= Width4) {
                            Box(GlanceModifier.padding(horizontal = 16.dp)) {
                                Image(
                                    provider = ImageProvider(R.drawable.refresh),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colors.onPrimaryContainer),
                                    modifier = GlanceModifier
                                        .cornerRadius(24.dp)
                                        .clickable {
                                            scope.launch { this@FocusHistoryGlanceWidget.updateAll(context) }
                                        }
                                )
                            }
                        }
                    }
                )
            },
            horizontalPadding = 16.dp,
            modifier = GlanceModifier
                .padding(bottom = 16.dp)
                .clickable(actionStartActivity(statsIntent))
        ) {
            Column(GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = millisecondsToHoursMinutes(
                            if (history.isNotEmpty()) history.sumOf { it.totalFocusTime() } / history.size else 0L,
                            context.getString(R.string.hours_and_minutes_format)
                        ) + " ",
                        style = TextStyle(
                            color = colors.onPrimaryContainer,
                            fontSize = typography.headlineSmall.fontSize,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    if (size.width >= Width4) {
                        Text(
                            text = context.getString(R.string.focus_per_day_avg),
                            style = TextStyle(color = colors.onPrimaryContainer)
                        )
                    }
                }
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    history.chunked(10).fastForEachIndexed { baseIndex, chunk ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = GlanceModifier.fillMaxHeight()
                        ) {
                            chunk.fastForEachIndexed { index, stat ->
                                val flatIndex = baseIndex * 10 + index
                                Box(GlanceModifier.padding(end = if (flatIndex != history.lastIndex) 4.dp else 0.dp)) {
                                    Spacer(
                                        GlanceModifier
                                            .width(20.dp)
                                            .height(
                                                (84 * (stat.totalFocusTime().toFloat() / maxFocus)).dp
                                            )
                                            .then(
                                                if (roundedCornersSupported)
                                                    GlanceModifier.background(colors.primary).cornerRadius(16.dp)
                                                else
                                                    GlanceModifier.background(
                                                        ImageProvider(R.drawable.rounded_16dp),
                                                        colorFilter = ColorFilter.tint(colors.primary)
                                                    )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
