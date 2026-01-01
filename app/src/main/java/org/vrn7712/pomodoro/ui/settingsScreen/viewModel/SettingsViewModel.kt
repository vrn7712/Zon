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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vrn7712.pomodoro.data.PreferenceRepository
import org.vrn7712.pomodoro.data.StateRepository
import org.vrn7712.pomodoro.data.BackupRepository
import org.vrn7712.pomodoro.service.ServiceHelper
import org.vrn7712.pomodoro.ui.Screen
import org.vrn7712.pomodoro.ui.timerScreen.viewModel.TimerAction
import org.vrn7712.pomodoro.ui.timerScreen.viewModel.TimerMode
import org.vrn7712.pomodoro.utils.millisecondsToStr

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
class SettingsViewModel(
    private val preferenceRepository: PreferenceRepository,
    private val stateRepository: StateRepository,
    private val backupRepository: BackupRepository,
    private val serviceHelper: ServiceHelper,
    private val time: MutableStateFlow<Long>
) : ViewModel() {
    val backStack = mutableStateListOf<Screen.Settings>(Screen.Settings.Main)
    
    val message = MutableSharedFlow<String>()

    val isPlus = MutableStateFlow(true) // Always unlocked for Zon FOSS
    val serviceRunning = stateRepository.timerState
        .map { it.serviceRunning }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

    private val _settingsState = stateRepository.settingsState
    val settingsState = _settingsState.asStateFlow()

    val focusTimeTextFieldState by lazy {
        TextFieldState((_settingsState.value.focusTime / 60000).toString())
    }
    val shortBreakTimeTextFieldState by lazy {
        TextFieldState((_settingsState.value.shortBreakTime / 60000).toString())
    }
    val longBreakTimeTextFieldState by lazy {
        TextFieldState((_settingsState.value.longBreakTime / 60000).toString())
    }

    val sessionsSliderState by lazy {
        SliderState(
            value = _settingsState.value.sessionLength.toFloat(),
            steps = 4,
            valueRange = 1f..6f,
            onValueChangeFinished = ::updateSessionLength
        )
    }

    private var focusFlowCollectionJob: Job? = null
    private var shortBreakFlowCollectionJob: Job? = null
    private var longBreakFlowCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            reloadSettings()
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SaveAlarmSound -> saveAlarmSound(action.uri)
            is SettingsAction.SaveAlarmEnabled -> saveAlarmEnabled(action.enabled)
            is SettingsAction.SaveVibrateEnabled -> saveVibrateEnabled(action.enabled)
            is SettingsAction.SaveDndEnabled -> saveDndEnabled(action.enabled)
            is SettingsAction.SaveMediaVolumeForAlarm -> saveMediaVolumeForAlarm(action.enabled)
            is SettingsAction.SaveSingleProgressBar -> saveSingleProgressBar(action.enabled)
            is SettingsAction.SaveAutostartNextSession -> saveAutostartNextSession(action.enabled)
            is SettingsAction.SaveSecureAod -> saveSecureAod(action.enabled)
            is SettingsAction.SaveColorScheme -> {
                saveColorScheme(action.color)
            }

            is SettingsAction.ToggleMusic -> {
                viewModelScope.launch {
                    _settingsState.update { it.copy(isMusicEnabled = action.enabled) }
                    preferenceRepository.saveBooleanPreference("music_enabled", action.enabled)
                }
            }
            is SettingsAction.UpdateMusicSound -> {
                viewModelScope.launch {
                    _settingsState.update { it.copy(musicSoundUri = action.uri) }
                    preferenceRepository.saveStringPreference("music_sound_uri", action.uri)
                    serviceHelper.startService(TimerAction.ReloadMusic)
                }
            }
            is SettingsAction.ClearMusicSound -> {
                viewModelScope.launch {
                    _settingsState.update { it.copy(musicSoundUri = null) }
                    preferenceRepository.saveStringPreference("music_sound_uri", "") // Save empty string for null
                    serviceHelper.startService(TimerAction.ReloadMusic)
                }
            }
            is SettingsAction.UpdateDefaultMusicTrack -> {
                viewModelScope.launch {
                    _settingsState.update { it.copy(defaultMusicTrack = action.trackId) }
                    preferenceRepository.saveStringPreference("default_music_track", action.trackId)
                    serviceHelper.startService(TimerAction.ReloadMusic)
                }
            }
            is SettingsAction.SaveTheme -> saveTheme(action.theme)
            is SettingsAction.SaveBlackTheme -> saveBlackTheme(action.enabled)
            is SettingsAction.SaveAodEnabled -> saveAodEnabled(action.enabled)
            is SettingsAction.ExportData -> exportData(action.uri)
            is SettingsAction.ImportData -> importData(action.uri)
            is SettingsAction.AskEraseData -> askEraseData()
            is SettingsAction.CancelEraseData -> cancelEraseData()
            is SettingsAction.EraseData -> deleteStats()
        }
    }

    private fun updateSessionLength() {
        viewModelScope.launch(Dispatchers.IO) {
            _settingsState.update { currentState ->
                currentState.copy(
                    sessionLength = preferenceRepository.saveIntPreference(
                        "session_length",
                        sessionsSliderState.value.toInt()
                    )
                )
            }
            refreshTimer()
        }
    }

    private fun askEraseData() {
        _settingsState.update { it.copy(isShowingEraseDataDialog = true) }
    }

    private fun cancelEraseData() {
        _settingsState.update { it.copy(isShowingEraseDataDialog = false) }
    }

    private fun deleteStats() {
        viewModelScope.launch {
            backupRepository.deleteAllStats()
            _settingsState.update { it.copy(isShowingEraseDataDialog = false) }
        }
    }

    fun runTextFieldFlowCollection() {
        focusFlowCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            snapshotFlow { focusTimeTextFieldState.text }
                .debounce(500)
                .collect {
                    if (it.isNotEmpty()) {
                        _settingsState.update { currentState ->
                            currentState.copy(focusTime = it.toString().toLong() * 60 * 1000)
                        }
                        refreshTimer()
                        preferenceRepository.saveIntPreference(
                            "focus_time",
                            _settingsState.value.focusTime.toInt()
                        )
                    }
                }
        }
        shortBreakFlowCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            snapshotFlow { shortBreakTimeTextFieldState.text }
                .debounce(500)
                .collect {
                    if (it.isNotEmpty()) {
                        _settingsState.update { currentState ->
                            currentState.copy(shortBreakTime = it.toString().toLong() * 60 * 1000)
                        }
                        refreshTimer()
                        preferenceRepository.saveIntPreference(
                            "short_break_time",
                            _settingsState.value.shortBreakTime.toInt()
                        )
                    }
                }
        }
        longBreakFlowCollectionJob = viewModelScope.launch(Dispatchers.IO) {
            snapshotFlow { longBreakTimeTextFieldState.text }
                .debounce(500)
                .collect {
                    if (it.isNotEmpty()) {
                        _settingsState.update { currentState ->
                            currentState.copy(longBreakTime = it.toString().toLong() * 60 * 1000)
                        }
                        refreshTimer()
                        preferenceRepository.saveIntPreference(
                            "long_break_time",
                            _settingsState.value.longBreakTime.toInt()
                        )
                    }
                }
        }
    }

    fun cancelTextFieldFlowCollection() {
        if (!serviceRunning.value) serviceHelper.startService(TimerAction.ResetTimer)
        focusFlowCollectionJob?.cancel()
        shortBreakFlowCollectionJob?.cancel()
        longBreakFlowCollectionJob?.cancel()
    }

    private fun saveAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(alarmEnabled = enabled)
            }
            preferenceRepository.saveBooleanPreference("alarm_enabled", enabled)
        }
    }

    private fun saveVibrateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(vibrateEnabled = enabled)
            }
            preferenceRepository.saveBooleanPreference("vibrate_enabled", enabled)
        }
    }

    private fun saveDndEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(dndEnabled = enabled)
            }
            preferenceRepository.saveBooleanPreference("dnd_enabled", enabled)
        }
    }

    private fun saveAlarmSound(uri: Uri?) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(alarmSoundUri = uri)
            }
            preferenceRepository.saveStringPreference("alarm_sound", uri.toString())
        }
    }

    private fun saveColorScheme(colorScheme: Color) {
        viewModelScope.launch {
            // Store color as hex string for better portability
            // Use toLong() and mask to handle signed Int to unsigned conversion
            val hexColor = String.format(java.util.Locale.US, "#%08X", colorScheme.toArgb().toLong() and 0xFFFFFFFFL)
            _settingsState.update { currentState ->
                currentState.copy(colorScheme = hexColor)
            }
            preferenceRepository.saveStringPreference("color_scheme", hexColor)
        }
    }

    private fun saveTheme(theme: String) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(theme = theme)
            }
            preferenceRepository.saveStringPreference("theme", theme)
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            preferenceRepository.saveBooleanPreference("is_onboarding_completed", true)
        }
    }

    private fun saveBlackTheme(blackTheme: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(blackTheme = blackTheme)
            }
            preferenceRepository.saveBooleanPreference("black_theme", blackTheme)
        }
    }

    private fun saveAodEnabled(aodEnabled: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(aodEnabled = aodEnabled)
            }
            preferenceRepository.saveBooleanPreference("aod_enabled", aodEnabled)
        }
    }

    private fun saveMediaVolumeForAlarm(mediaVolumeForAlarm: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(mediaVolumeForAlarm = mediaVolumeForAlarm)
            }
            preferenceRepository.saveBooleanPreference(
                "media_volume_for_alarm",
                mediaVolumeForAlarm
            )
        }
    }

    private fun saveSingleProgressBar(singleProgressBar: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(singleProgressBar = singleProgressBar)
            }
            preferenceRepository.saveBooleanPreference(
                "single_progress_bar",
                singleProgressBar
            )
        }
    }

    private fun saveAutostartNextSession(autostartNextSession: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(autostartNextSession = autostartNextSession)
            }
            preferenceRepository.saveBooleanPreference(
                "autostart_next_session",
                autostartNextSession
            )
        }
    }

    private fun saveSecureAod(secureAod: Boolean) {
        viewModelScope.launch {
            _settingsState.update { currentState ->
                currentState.copy(secureAod = secureAod)
            }
            preferenceRepository.saveBooleanPreference(
                "secure_aod",
                secureAod
            )
        }
    }

    suspend fun reloadSettings() {
        var settingsState = _settingsState.value
        val focusTime =
            preferenceRepository.getIntPreference("focus_time")?.toLong()
                ?: preferenceRepository.saveIntPreference(
                    "focus_time",
                    settingsState.focusTime.toInt()
                ).toLong()
        val shortBreakTime =
            preferenceRepository.getIntPreference("short_break_time")?.toLong()
                ?: preferenceRepository.saveIntPreference(
                    "short_break_time",
                    settingsState.shortBreakTime.toInt()
                ).toLong()
        val longBreakTime =
            preferenceRepository.getIntPreference("long_break_time")?.toLong()
                ?: preferenceRepository.saveIntPreference(
                    "long_break_time",
                    settingsState.longBreakTime.toInt()
                ).toLong()
        val sessionLength =
            preferenceRepository.getIntPreference("session_length")
                ?: preferenceRepository.saveIntPreference(
                    "session_length",
                    settingsState.sessionLength
                )

        val alarmSoundUri = (
                preferenceRepository.getStringPreference("alarm_sound")
                    ?: preferenceRepository.saveStringPreference(
                        "alarm_sound",
                        (Settings.System.DEFAULT_ALARM_ALERT_URI
                            ?: Settings.System.DEFAULT_RINGTONE_URI).toString()
                    )
                ).toUri()

        val theme = preferenceRepository.getStringPreference("theme")
            ?: preferenceRepository.saveStringPreference("theme", settingsState.theme)
        val colorScheme = preferenceRepository.getStringPreference("color_scheme")
            ?: preferenceRepository.saveStringPreference("color_scheme", settingsState.colorScheme)
        val blackTheme = preferenceRepository.getBooleanPreference("black_theme")
            ?: preferenceRepository.saveBooleanPreference("black_theme", settingsState.blackTheme)
        val aodEnabled = preferenceRepository.getBooleanPreference("aod_enabled")
            ?: preferenceRepository.saveBooleanPreference("aod_enabled", settingsState.aodEnabled)
        val alarmEnabled = preferenceRepository.getBooleanPreference("alarm_enabled")
            ?: preferenceRepository.saveBooleanPreference(
                "alarm_enabled",
                settingsState.alarmEnabled
            )
        val vibrateEnabled = preferenceRepository.getBooleanPreference("vibrate_enabled")
            ?: preferenceRepository.saveBooleanPreference(
                "vibrate_enabled",
                settingsState.vibrateEnabled
            )
        val dndEnabled = preferenceRepository.getBooleanPreference("dnd_enabled")
            ?: preferenceRepository.saveBooleanPreference("dnd_enabled", settingsState.dndEnabled)
        val mediaVolumeForAlarm =
            preferenceRepository.getBooleanPreference("media_volume_for_alarm")
                ?: preferenceRepository.saveBooleanPreference(
                    "media_volume_for_alarm",
                    settingsState.mediaVolumeForAlarm
                )
        val singleProgressBar = preferenceRepository.getBooleanPreference("single_progress_bar")
            ?: preferenceRepository.saveBooleanPreference(
                "single_progress_bar",
                settingsState.singleProgressBar
            )
        val autostartNextSession =
            preferenceRepository.getBooleanPreference("autostart_next_session")
                ?: preferenceRepository.saveBooleanPreference(
                    "autostart_next_session",
                    settingsState.autostartNextSession
                )
        val secureAod = preferenceRepository.getBooleanPreference("secure_aod")
            ?: preferenceRepository.saveBooleanPreference("secure_aod", true)

        _settingsState.update { currentState ->
            currentState.copy(
                focusTime = focusTime,
                shortBreakTime = shortBreakTime,
                longBreakTime = longBreakTime,
                sessionLength = sessionLength,
                theme = theme,
                colorScheme = colorScheme,
                alarmSoundUri = alarmSoundUri,
                blackTheme = blackTheme,
                aodEnabled = aodEnabled,
                alarmEnabled = alarmEnabled,
                vibrateEnabled = vibrateEnabled,
                dndEnabled = dndEnabled,
                mediaVolumeForAlarm = mediaVolumeForAlarm,
                singleProgressBar = singleProgressBar,
                autostartNextSession = autostartNextSession,
                secureAod = secureAod,
                // Load colors as hex strings
                primaryColor = preferenceRepository.getStringPreference("primary_color"),
                surfaceColor = preferenceRepository.getStringPreference("surface_color"),
                onSurfaceColor = preferenceRepository.getStringPreference("on_surface_color"),
                surfaceVariantColor = preferenceRepository.getStringPreference("surface_variant_color"),
                secondaryContainerColor = preferenceRepository.getStringPreference("secondary_container_color"),
                onSecondaryContainerColor = preferenceRepository.getStringPreference("on_secondary_container_color"),
                isMusicEnabled = preferenceRepository.getBooleanPreference("music_enabled") ?: false,
                musicSoundUri = preferenceRepository.getStringPreference("music_sound_uri")?.takeIf { it.isNotEmpty() },
                defaultMusicTrack = (preferenceRepository.getStringPreference("default_music_track") ?: "cozy_lofi").let { 
                    if (it == "rainy_day") "cozy_lofi" else it 
                }
            )
        }

        settingsState = _settingsState.value

        if (!stateRepository.timerState.value.serviceRunning) {
            time.update { settingsState.focusTime }
            stateRepository.timerState.update { currentState ->
                currentState.copy(
                    timerMode = TimerMode.FOCUS,
                    timeStr = millisecondsToStr(time.value),
                    totalTime = time.value,
                    nextTimerMode = if (settingsState.sessionLength > 1) TimerMode.SHORT_BREAK else TimerMode.LONG_BREAK,
                    nextTimeStr = millisecondsToStr(if (settingsState.sessionLength > 1) settingsState.shortBreakTime else settingsState.longBreakTime),
                    currentFocusCount = 1,
                    totalFocusCount = settingsState.sessionLength
                )
            }
        }
    }

    private fun refreshTimer() {
        if (!serviceRunning.value) {
            val settingsState = _settingsState.value

            time.update { settingsState.focusTime }

            stateRepository.timerState.update { currentState ->
                currentState.copy(
                    timerMode = TimerMode.FOCUS,
                    timeStr = millisecondsToStr(time.value),
                    totalTime = time.value,
                    nextTimerMode = if (settingsState.sessionLength > 1) TimerMode.SHORT_BREAK else TimerMode.LONG_BREAK,
                    nextTimeStr = millisecondsToStr(if (settingsState.sessionLength > 1) settingsState.shortBreakTime else settingsState.longBreakTime),
                    currentFocusCount = 1,
                    totalFocusCount = settingsState.sessionLength
                )
            }
        }
    }

    private fun exportData(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.exportData(uri)
            if (result.isSuccess) message.emit("Backup created successfully")
            else message.emit(result.exceptionOrNull()?.message ?: "Export failed")
        }
    }

    private fun importData(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.importData(uri)
            if (result.isSuccess) {
                reloadSettings() // Reload settings after import
                message.emit("Data restored successfully")
            } else message.emit(result.exceptionOrNull()?.message ?: "Import failed")
        }
    }


    fun updateThemeColors(scheme: ColorScheme) {
        // Convert colors to hex strings for better portability and readability
        // Use toLong() and mask to handle signed Int to unsigned conversion
        val primary = String.format(java.util.Locale.US, "#%08X", scheme.primary.toArgb().toLong() and 0xFFFFFFFFL)
        val surface = String.format(java.util.Locale.US, "#%08X", scheme.surface.toArgb().toLong() and 0xFFFFFFFFL)
        val onSurface = String.format(java.util.Locale.US, "#%08X", scheme.onSurface.toArgb().toLong() and 0xFFFFFFFFL)
        val surfaceVariant = String.format(java.util.Locale.US, "#%08X", scheme.surfaceVariant.toArgb().toLong() and 0xFFFFFFFFL)
        val secondaryContainer = String.format(java.util.Locale.US, "#%08X", scheme.secondaryContainer.toArgb().toLong() and 0xFFFFFFFFL)
        val onSecondaryContainer = String.format(java.util.Locale.US, "#%08X", scheme.onSecondaryContainer.toArgb().toLong() and 0xFFFFFFFFL)

        viewModelScope.launch {
            _settingsState.update {
                it.copy(
                    primaryColor = primary,
                    surfaceColor = surface,
                    onSurfaceColor = onSurface,
                    surfaceVariantColor = surfaceVariant,
                    secondaryContainerColor = secondaryContainer,
                    onSecondaryContainerColor = onSecondaryContainer
                )
            }
            preferenceRepository.saveStringPreference("primary_color", primary)
            preferenceRepository.saveStringPreference("surface_color", surface)
            preferenceRepository.saveStringPreference("on_surface_color", onSurface)
            preferenceRepository.saveStringPreference("surface_variant_color", surfaceVariant)
            preferenceRepository.saveStringPreference("secondary_container_color", secondaryContainer)
            preferenceRepository.saveStringPreference("on_secondary_container_color", onSecondaryContainer)
            
            stateRepository.colorScheme = scheme
            
            // Trigger widget refresh with updated colors
            serviceHelper.startService(TimerAction.RefreshWidget)
            serviceHelper.startService(TimerAction.RefreshStatsWidget)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as org.vrn7712.pomodoro.ZonApplication)
                val appPreferenceRepository = application.container.appPreferenceRepository
                val serviceHelper = application.container.serviceHelper
                val stateRepository = application.container.stateRepository
                val backupRepository = application.container.backupRepository
                val time = application.container.time

                SettingsViewModel(
                    preferenceRepository = appPreferenceRepository,
                    serviceHelper = serviceHelper,
                    stateRepository = stateRepository,
                    backupRepository = backupRepository,
                    time = time
                )
            }
        }
    }
}
