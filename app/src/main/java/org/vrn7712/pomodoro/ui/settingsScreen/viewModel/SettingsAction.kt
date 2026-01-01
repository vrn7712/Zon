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
import androidx.compose.ui.graphics.Color

sealed interface SettingsAction {
    data class SaveAlarmEnabled(val enabled: Boolean) : SettingsAction
    data class SaveVibrateEnabled(val enabled: Boolean) : SettingsAction
    data class SaveBlackTheme(val enabled: Boolean) : SettingsAction
    data class SaveAodEnabled(val enabled: Boolean) : SettingsAction
    data class ExportData(val uri: Uri) : SettingsAction
    data class ImportData(val uri: Uri) : SettingsAction
    data class SaveDndEnabled(val enabled: Boolean) : SettingsAction
    data class SaveMediaVolumeForAlarm(val enabled: Boolean) : SettingsAction
    data class SaveSingleProgressBar(val enabled: Boolean) : SettingsAction
    data class SaveAutostartNextSession(val enabled: Boolean) : SettingsAction
    data class SaveSecureAod(val enabled: Boolean) : SettingsAction
    data class SaveAlarmSound(val uri: Uri?) : SettingsAction
    data class SaveTheme(val theme: String) : SettingsAction
    data class SaveColorScheme(val color: Color) : SettingsAction
    data class ToggleMusic(val enabled: Boolean) : SettingsAction
    data class UpdateMusicSound(val uri: String) : SettingsAction
    data object ClearMusicSound : SettingsAction
    data class UpdateDefaultMusicTrack(val trackId: String) : SettingsAction
    data object AskEraseData : SettingsAction
    data object CancelEraseData : SettingsAction
    data object EraseData : SettingsAction
}
