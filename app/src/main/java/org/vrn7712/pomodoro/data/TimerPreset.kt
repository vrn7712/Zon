/*
 * Copyright (c) 2025 Vrushal
 *
 * This file is part of Zon - a minimalist pomodoro timer for Android.
 *
 * Zon is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.vrn7712.pomodoro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a timer preset configuration.
 * Users can create custom presets with different focus/break durations.
 */
@Entity(tableName = "timer_preset")
@Serializable
data class TimerPreset(
    @PrimaryKey(autoGenerate = true) 
    val id: Int = 0,
    val name: String,               // e.g., "Deep Work" or auto-generated "25/5/15"
    val focusMinutes: Int,          // Focus session duration in minutes
    val shortBreakMinutes: Int,     // Short break duration in minutes
    val longBreakMinutes: Int,      // Long break duration in minutes
    val isSelected: Boolean = false,// Whether this preset is currently active
    val isBuiltIn: Boolean = false, // Built-in presets cannot be deleted
    val sortOrder: Int = 0          // For custom ordering
) {
    /**
     * Returns a display name in the format "Focus/Short/Long" (e.g., "25/5/15")
     */
    val displayName: String
        get() = "$focusMinutes/$shortBreakMinutes/$longBreakMinutes"
    
    companion object {
        // Default presets to create on first launch
        val DEFAULTS = listOf(
            TimerPreset(
                name = "Classic",
                focusMinutes = 25,
                shortBreakMinutes = 5,
                longBreakMinutes = 15,
                isBuiltIn = true,
                isSelected = true,
                sortOrder = 0
            ),
            TimerPreset(
                name = "Deep Work",
                focusMinutes = 50,
                shortBreakMinutes = 10,
                longBreakMinutes = 30,
                isBuiltIn = true,
                sortOrder = 1
            ),
            TimerPreset(
                name = "Quick Bursts",
                focusMinutes = 15,
                shortBreakMinutes = 3,
                longBreakMinutes = 10,
                isBuiltIn = true,
                sortOrder = 2
            )
        )
    }
}
