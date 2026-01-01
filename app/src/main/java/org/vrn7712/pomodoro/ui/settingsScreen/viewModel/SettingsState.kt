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

package org.vrn7712.pomodoro.ui.settingsScreen.viewModel

import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class SettingsState(
    val theme: String = "auto",
    val colorScheme: String = Color.White.toString(),
    val blackTheme: Boolean = false,
    val aodEnabled: Boolean = false,
    val alarmEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val dndEnabled: Boolean = false,
    val mediaVolumeForAlarm: Boolean = false,
    val singleProgressBar: Boolean = false,
    val autostartNextSession: Boolean = false,
    val secureAod: Boolean = true,

    val focusTime: Long = 25 * 60 * 1000L,
    val shortBreakTime: Long = 5 * 60 * 1000L,
    val longBreakTime: Long = 15 * 60 * 1000L,

    val sessionLength: Int = 4,

    val alarmSoundUri: Uri? =
        Settings.System.DEFAULT_ALARM_ALERT_URI ?: Settings.System.DEFAULT_RINGTONE_URI,
    // Theme colors stored as hex strings (e.g., "#FF4E9A62") for better portability
    val primaryColor: String? = null,
    val surfaceColor: String? = null,
    val onSurfaceColor: String? = null,
    val surfaceVariantColor: String? = null,
    val secondaryContainerColor: String? = null,
    val onSecondaryContainerColor: String? = null,

    val isMusicEnabled: Boolean = false,
    val musicSoundUri: String? = null,
    val defaultMusicTrack: String = "cozy_lofi", // "cozy_lofi" or "study_music"
    val isShowingEraseDataDialog: Boolean = false
)
