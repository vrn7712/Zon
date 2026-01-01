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

package org.vrn7712.pomodoro.ui.timerScreen.viewModel

sealed interface TimerAction {
    data class SkipTimer(val fromButton: Boolean) : TimerAction

    data object ResetTimer : TimerAction
    data object UndoReset : TimerAction
    data object StopAlarm : TimerAction
    data object ToggleTimer : TimerAction
    data object ToggleMusic : TimerAction
    data object SkipForwardMusic : TimerAction
    data object SkipBackwardMusic : TimerAction
    
    /**
     * Seek to a specific position in the currently playing music.
     * @param progress Position to seek to, as a fraction from 0.0 (start) to 1.0 (end)
     */
    data class SeekMusic(val progress: Float) : TimerAction
    
    data object ReloadMusic : TimerAction
    data object RefreshWidget : TimerAction
    data object RefreshStatsWidget : TimerAction
    
    // Preset actions
    data class SelectPreset(val presetId: Int) : TimerAction
    data class CreatePreset(
        val name: String, 
        val focusMinutes: Int, 
        val shortBreakMinutes: Int, 
        val longBreakMinutes: Int
    ) : TimerAction
    data class DeletePreset(val presetId: Int) : TimerAction
}
