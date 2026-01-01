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

package org.vrn7712.pomodoro.service

import android.content.Context
import android.content.Intent
import org.vrn7712.pomodoro.ui.timerScreen.viewModel.TimerAction

/**
 * Helper class that holds a reference to [Context] and helps call [Context.startService] in
 * [androidx.lifecycle.ViewModel]s. This class must be managed by an [android.app.Application] class
 * to scope it to the Activity's lifecycle and prevent leaks.
 */
class ServiceHelper(private val context: Context) {
    fun startService(action: TimerAction) {
        when (action) {
            TimerAction.ResetTimer ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.RESET.toString()
                    context.startService(it)
                }

            TimerAction.UndoReset ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.UNDO_RESET.toString()
                    context.startService(it)
                }

            is TimerAction.SkipTimer ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.SKIP.toString()
                    context.startService(it)
                }

            TimerAction.StopAlarm ->
                Intent(context, TimerService::class.java).also {
                    it.action =
                        TimerService.Actions.STOP_ALARM.toString()
                    context.startService(it)
                }

            TimerAction.ToggleTimer ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.TOGGLE.toString()
                    context.startService(it)
                }

            TimerAction.ToggleMusic ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.TOGGLE_MUSIC.toString()
                    context.startService(it)
                }

            TimerAction.SkipForwardMusic ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.SKIP_MUSIC_NEXT.toString()
                    context.startService(it)
                }

            TimerAction.SkipBackwardMusic ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.SKIP_MUSIC_PREV.toString()
                    context.startService(it)
                }

            is TimerAction.SeekMusic ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.SEEK_MUSIC.toString()
                    it.putExtra(TimerService.EXTRA_SEEK_PROGRESS, action.progress)
                    context.startService(it)
                }

            TimerAction.ReloadMusic ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.RELOAD_MUSIC.toString()
                    context.startService(it)
                }

            TimerAction.RefreshWidget ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.REFRESH_WIDGET.toString()
                    context.startService(it)
                }

            TimerAction.RefreshStatsWidget ->
                Intent(context, TimerService::class.java).also {
                    it.action = TimerService.Actions.REFRESH_STATS_WIDGET.toString()
                    context.startService(it)
                }
            
            // Preset actions are handled directly in ViewModel, not by Service
            is TimerAction.SelectPreset,
            is TimerAction.CreatePreset,
            is TimerAction.DeletePreset -> { /* No-op: handled by ViewModel */ }
        }
    }
}
