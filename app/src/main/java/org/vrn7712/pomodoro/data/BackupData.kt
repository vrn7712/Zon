package org.vrn7712.pomodoro.data

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val tasks: List<Task>,
    val stats: List<Stat>,
    val booleanPreferences: List<BooleanPreference>,
    val intPreferences: List<IntPreference>,
    val stringPreferences: List<StringPreference>,
    val timerPresets: List<TimerPreset> = emptyList() // Default for backward compatibility
)

