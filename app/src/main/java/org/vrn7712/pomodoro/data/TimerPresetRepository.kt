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

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing timer presets.
 * Handles initialization of default presets and provides coroutine-safe access.
 */
class TimerPresetRepository(private val dao: TimerPresetDao) {
    
    /**
     * Flow of all presets, ordered by sortOrder
     */
    val allPresets: Flow<List<TimerPreset>> = dao.getAllPresets()
    
    /**
     * Flow of the currently selected preset
     */
    val selectedPreset: Flow<TimerPreset?> = dao.getSelectedPreset()
    
    /**
     * Get all presets synchronously (for backup)
     */
    suspend fun getAllPresetsSync(): List<TimerPreset> = dao.getAllPresetsSync()
    
    /**
     * Get the currently selected preset synchronously
     */
    suspend fun getSelectedPresetSync(): TimerPreset? = dao.getSelectedPresetSync()
    
    /**
     * Get a preset by ID
     */
    suspend fun getPresetById(id: Int): TimerPreset? = dao.getPresetById(id)
    
    /**
     * Select a preset, making it the active timer configuration
     */
    suspend fun selectPreset(presetId: Int) {
        dao.selectPreset(presetId)
    }
    
    /**
     * Create a new preset
     */
    suspend fun createPreset(
        name: String,
        focusMinutes: Int,
        shortBreakMinutes: Int,
        longBreakMinutes: Int
    ): Long {
        val count = dao.getPresetCount()
        val preset = TimerPreset(
            name = name,
            focusMinutes = focusMinutes,
            shortBreakMinutes = shortBreakMinutes,
            longBreakMinutes = longBreakMinutes,
            isBuiltIn = false,
            sortOrder = count // Add at the end
        )
        return dao.insertPreset(preset)
    }
    
    /**
     * Update an existing preset
     */
    suspend fun updatePreset(preset: TimerPreset) {
        dao.updatePreset(preset)
    }
    
    /**
     * Delete a preset (only if not built-in and not the only one)
     */
    suspend fun deletePreset(preset: TimerPreset): Boolean {
        if (preset.isBuiltIn) return false
        if (dao.getPresetCount() <= 1) return false
        
        // If deleting the selected preset, select another one first
        if (preset.isSelected) {
            val allPresets = dao.getAllPresetsSync()
            val nextPreset = allPresets.firstOrNull { it.id != preset.id }
            nextPreset?.let { dao.selectPreset(it.id) }
        }
        
        dao.deletePreset(preset)
        return true
    }
    
    /**
     * Initialize default presets if none exist
     */
    suspend fun initializeDefaultPresetsIfNeeded() {
        if (dao.getPresetCount() == 0) {
            dao.insertPresets(TimerPreset.DEFAULTS)
        }
    }
    
    /**
     * Clear all presets and insert new ones (for restore)
     */
    suspend fun replaceAllPresets(presets: List<TimerPreset>) {
        dao.deleteAllPresets()
        if (presets.isEmpty()) {
            dao.insertPresets(TimerPreset.DEFAULTS)
        } else {
            dao.insertPresets(presets)
        }
    }
}
