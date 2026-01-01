/*
 * Copyright (c) 2025 Vrushal
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

package org.vrn7712.pomodoro.utils

import androidx.compose.ui.graphics.Color
import java.util.Locale
import java.util.concurrent.TimeUnit

fun millisecondsToStr(t: Long): String {
    require(t >= 0L)
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(t),
        TimeUnit.MILLISECONDS.toSeconds(t) % TimeUnit.MINUTES.toSeconds(1)
    )
}

fun millisecondsToHours(t: Long, format: String = "%dh"): String {
    require(t >= 0L)
    return String.format(
        Locale.getDefault(),
        format,
        TimeUnit.MILLISECONDS.toHours(t)
    )
}

fun millisecondsToMinutes(t: Long, format: String = "%dm"): String {
    require(t >= 0L)
    return String.format(
        Locale.getDefault(),
        format,
        TimeUnit.MILLISECONDS.toMinutes(t)
    )
}

fun millisecondsToHoursMinutes(t: Long, format: String = $$"%1$dh %2$dm"): String {
    require(t >= 0L)
    return String.format(
        Locale.getDefault(),
        format,
        TimeUnit.MILLISECONDS.toHours(t),
        TimeUnit.MILLISECONDS.toMinutes(t) % TimeUnit.HOURS.toMinutes(1)
    )
}

/**
 * Converts a string representation of a Color to a Color object.
 * Supports two formats:
 * - Hex format: "#AARRGGBB" (e.g., "#FF4E9A62")
 * - Legacy format: "Color(r, g, b, a, colorSpace)" (e.g., "Color(0.5, 0.5, 0.5, 1.0, sRGB IEC61966-2.1)")
 * 
 * Returns Color.White if the format cannot be parsed.
 */
fun String.toColor(): Color {
    return try {
        when {
            // Handle hex format: #AARRGGBB
            this.startsWith("#") -> {
                val colorInt = android.graphics.Color.parseColor(this)
                Color(colorInt)
            }
            // Handle legacy Color.toString() format
            this.startsWith("Color(") -> {
                val comma1 = this.indexOf(',')
                val comma2 = this.indexOf(',', comma1 + 1)
                val comma3 = this.indexOf(',', comma2 + 1)
                val comma4 = this.indexOf(',', comma3 + 1)

                val r = this.substringAfter('(').substringBefore(',').toFloat()
                val g = this.slice(comma1 + 1..<comma2).toFloat()
                val b = this.slice(comma2 + 1..<comma3).toFloat()
                val a = this.slice(comma3 + 1..<comma4).toFloat()
                Color(r, g, b, a)
            }
            else -> Color.White
        }
    } catch (e: Exception) {
        Color.White
    }
}
