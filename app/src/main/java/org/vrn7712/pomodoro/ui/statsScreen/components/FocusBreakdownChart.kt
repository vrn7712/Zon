/*
 * Copyright (c) 2025 Nishant Mishra
 *
 * This file is part of Zon (forked from Tomato) - a minimalist pomodoro timer for Android.
 *
 * Zon is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Zon is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Zon.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.vrn7712.pomodoro.ui.statsScreen.components

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import org.vrn7712.pomodoro.R

@Composable
fun ColumnScope.FocusBreakdownChart(
    expanded: Boolean,
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier,
    axisTypeface: Typeface = Typeface.DEFAULT,
    markerTypeface: Typeface = Typeface.DEFAULT
) {
    AnimatedVisibility(expanded) {
        TimeColumnChart(
            modelProducer,
            hoursFormat = stringResource(R.string.hours_format),
            hoursMinutesFormat = stringResource(R.string.hours_and_minutes_format),
            minutesFormat = stringResource(R.string.minutes_format),
            axisTypeface = axisTypeface,
            markerTypeface = markerTypeface,
            xValueFormatter = CartesianValueFormatter { _, value, _ ->
                when (value) {
                    0.0 -> "0 - 6"
                    1.0 -> "6 - 12"
                    2.0 -> "12 - 18"
                    3.0 -> "18 - 24"
                    else -> ""
                }
            },
            modifier = modifier
        )
    }
}
