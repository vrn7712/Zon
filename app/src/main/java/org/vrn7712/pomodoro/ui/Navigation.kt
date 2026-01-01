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

package org.vrn7712.pomodoro.ui

import org.vrn7712.pomodoro.R

val mainScreens = listOf(
    NavItem(
        Screen.Timer,
        R.drawable.timer_outlined,
        R.drawable.timer_filled,
        R.string.timer
    ),
    NavItem(
        Screen.Tasks,
        R.drawable.view_day, // Using view_day as a temporary icon for tasks
        R.drawable.view_day,
        R.string.tasks
    ),
    NavItem(
        Screen.Stats.Main,
        R.drawable.monitoring,
        R.drawable.monitoring_filled,
        R.string.stats
    ),
    NavItem(
        Screen.Settings.Main,
        R.drawable.settings,
        R.drawable.settings_filled,
        R.string.settings
    )
)

val settingsScreens = listOf(
    SettingsNavItem(
        Screen.Settings.Timer,
        R.drawable.timer_filled,
        R.string.timer,
        listOf(R.string.durations, R.string.dnd, R.string.always_on_display)
    ),
    SettingsNavItem(
        Screen.Settings.Alarm,
        R.drawable.alarm,
        R.string.alarm,
        listOf(
            R.string.alarm_sound,
            R.string.sound,
            R.string.vibrate,
            R.string.media_volume_for_alarm
        )
    ),
    SettingsNavItem(
        Screen.Settings.Appearance,
        R.drawable.palette,
        R.string.appearance,
        listOf(R.string.theme, R.string.color_scheme, R.string.black_theme)
    )
)
