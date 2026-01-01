/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon - a minimalist pomodoro timer for Android.
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.vrn7712.pomodoro.ZonApplication
import org.vrn7712.pomodoro.data.Stat
import org.vrn7712.pomodoro.data.StatRepository
import org.vrn7712.pomodoro.data.StateRepository
import org.vrn7712.pomodoro.data.TimerPreset
import org.vrn7712.pomodoro.data.TimerPresetRepository
import org.vrn7712.pomodoro.service.ServiceHelper
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(FlowPreview::class)
class TimerViewModel(
    private val serviceHelper: ServiceHelper,
    private val stateRepository: StateRepository,
    private val statRepository: StatRepository,
    private val timerPresetRepository: TimerPresetRepository,
    private val _time: MutableStateFlow<Long>
) : ViewModel() {
    val timerState: StateFlow<TimerState> = stateRepository.timerState.asStateFlow()

    val time: StateFlow<Long> = _time.asStateFlow()

    val progress = _time.combine(stateRepository.timerState) { remainingTime, uiState ->
        (uiState.totalTime.toFloat() - remainingTime) / uiState.totalTime
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Timer Presets
    val presets: StateFlow<List<TimerPreset>> = timerPresetRepository.allPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val selectedPreset: StateFlow<TimerPreset?> = timerPresetRepository.selectedPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // Initialize default presets if needed
            timerPresetRepository.initializeDefaultPresetsIfNeeded()
            
            var lastDate = statRepository.getLastDate()
            val today = LocalDate.now()

            // Fills dates between today and lastDate with 0s to ensure continuous history
            if (lastDate != null) {
                while (ChronoUnit.DAYS.between(lastDate, today) > 0) {
                    lastDate = lastDate?.plusDays(1)
                    statRepository.insertStat(Stat(lastDate!!, 0, 0, 0, 0, 0))
                }
            } else {
                statRepository.insertStat(Stat(today, 0, 0, 0, 0, 0))
            }

            delay(1500)

            stateRepository.timerState.update { currentState ->
                currentState.copy(showBrandTitle = false)
            }
        }
    }

    fun onAction(action: TimerAction) {
        when (action) {
            is TimerAction.SelectPreset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    timerPresetRepository.selectPreset(action.presetId)
                    // Update timer settings with the new preset
                    val preset = timerPresetRepository.getPresetById(action.presetId)
                    preset?.let { applyPreset(it) }
                }
            }
            is TimerAction.CreatePreset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    timerPresetRepository.createPreset(
                        name = action.name,
                        focusMinutes = action.focusMinutes,
                        shortBreakMinutes = action.shortBreakMinutes,
                        longBreakMinutes = action.longBreakMinutes
                    )
                }
            }
            is TimerAction.DeletePreset -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val preset = timerPresetRepository.getPresetById(action.presetId)
                    preset?.let { timerPresetRepository.deletePreset(it) }
                }
            }
            else -> {
                // Forward other actions to service
                serviceHelper.startService(action)
            }
        }
    }
    
    private fun applyPreset(preset: TimerPreset) {
        // Update the settings state with new durations
        stateRepository.settingsState.update { currentState ->
            currentState.copy(
                focusTime = preset.focusMinutes * 60 * 1000L,
                shortBreakTime = preset.shortBreakMinutes * 60 * 1000L,
                longBreakTime = preset.longBreakMinutes * 60 * 1000L
            )
        }
        // Only reset timer if it's NOT running (to avoid disrupting active session)
        if (!stateRepository.timerState.value.timerRunning) {
            serviceHelper.startService(TimerAction.ResetTimer)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as ZonApplication)
                val appStatRepository = application.container.appStatRepository
                val stateRepository = application.container.stateRepository
                val timerPresetRepository = application.container.timerPresetRepository
                val serviceHelper = application.container.serviceHelper
                val time = application.container.time

                TimerViewModel(
                    serviceHelper = serviceHelper,
                    stateRepository = stateRepository,
                    statRepository = appStatRepository,
                    timerPresetRepository = timerPresetRepository,
                    _time = time
                )
            }
        }
    }
}

