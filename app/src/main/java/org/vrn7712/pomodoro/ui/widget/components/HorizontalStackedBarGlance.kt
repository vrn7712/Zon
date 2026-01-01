/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon - a minimalist pomodoro timer for Android.
 */

package org.vrn7712.pomodoro.ui.widget.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme.colors
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import org.vrn7712.pomodoro.ui.statsScreen.components.HORIZONTAL_STACKED_BAR_HEIGHT

/**
 * A version of HorizontalStackedBar that uses Glance composables for use in widgets.
 */
@Composable
@GlanceComposable
fun HorizontalStackedBarGlance(
    values: List<Long>,
    width: Dp,
    modifier: GlanceModifier = GlanceModifier,
    rankList: List<Int> = remember(values) {
        val sortedIndices = values.indices.sortedByDescending { values[it] }
        val ranks = MutableList(values.size) { 0 }

        sortedIndices.forEachIndexed { rank, originalIndex ->
            ranks[originalIndex] = rank
        }

        ranks
    },
    height: Dp = HORIZONTAL_STACKED_BAR_HEIGHT,
    gap: Dp = 2.dp
) {
    val firstNonZeroIndex = remember(values) { values.indexOfFirst { it > 0L } }
    val nonZeroItems = remember(values) { values.count { it > 0 } }
    val effectiveWidth = width - ((nonZeroItems - 1).coerceAtLeast(0) * gap.value).dp
    val totalSum = remember(values) { values.sum() }

    val context = LocalContext.current

    if (firstNonZeroIndex != -1)
        Row(
            modifier = modifier
                .height(height)
                .cornerRadius(16.dp)
        ) {
            values.fastForEachIndexed { index, item ->
                if (item > 0L) {
                    Box(
                        GlanceModifier
                            .width((effectiveWidth.value * (item.toFloat() / totalSum)).dp)
                            .height(height)
                            .cornerRadius(4.dp)
                            .background(
                                colors.primary
                                    .getColor(context)
                                    .copy(
                                        (1f - (rankList.getOrNull(index) ?: 0) * 0.1f)
                                            .coerceAtLeast(0.1f)
                                    )
                                    .compositeOver(colors.surfaceVariant.getColor(context))
                            )
                    ) {}
                    Spacer(GlanceModifier.width(gap))
                }
            }
        }
    else
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .cornerRadius(16.dp)
                .background(colors.surfaceVariant)
        ) {}
}
