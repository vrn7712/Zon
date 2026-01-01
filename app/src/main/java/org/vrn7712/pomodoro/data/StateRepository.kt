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

package org.vrn7712.pomodoro.data

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.vrn7712.pomodoro.service.TimerStateSnapshot
import org.vrn7712.pomodoro.ui.settingsScreen.viewModel.SettingsState
import org.vrn7712.pomodoro.ui.timerScreen.viewModel.TimerState

class StateRepository {
    val timerState = MutableStateFlow(TimerState())
    val settingsState = MutableStateFlow(SettingsState())
    var timerFrequency: Float = 60f
    var colorScheme: ColorScheme = lightColorScheme()
    var timerStateSnapshot: TimerStateSnapshot =
        TimerStateSnapshot(time = 0, timerState = TimerState())
}
