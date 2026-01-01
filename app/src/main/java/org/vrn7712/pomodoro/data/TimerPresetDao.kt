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

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerPresetDao {
    
    /**
     * Get all presets ordered by sortOrder
     */
    @Query("SELECT * FROM timer_preset ORDER BY sortOrder ASC")
    fun getAllPresets(): Flow<List<TimerPreset>>
    
    /**
     * Get all presets synchronously (for backup)
     */
    @Query("SELECT * FROM timer_preset ORDER BY sortOrder ASC")
    suspend fun getAllPresetsSync(): List<TimerPreset>
    
    /**
     * Get the currently selected preset
     */
    @Query("SELECT * FROM timer_preset WHERE isSelected = 1 LIMIT 1")
    fun getSelectedPreset(): Flow<TimerPreset?>
    
    /**
     * Get the currently selected preset synchronously
     */
    @Query("SELECT * FROM timer_preset WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedPresetSync(): TimerPreset?
    
    /**
     * Get preset by ID
     */
    @Query("SELECT * FROM timer_preset WHERE id = :id")
    suspend fun getPresetById(id: Int): TimerPreset?
    
    /**
     * Get count of all presets
     */
    @Query("SELECT COUNT(*) FROM timer_preset")
    suspend fun getPresetCount(): Int
    
    /**
     * Insert a new preset
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: TimerPreset): Long
    
    /**
     * Insert multiple presets
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<TimerPreset>)
    
    /**
     * Update an existing preset
     */
    @Update
    suspend fun updatePreset(preset: TimerPreset)
    
    /**
     * Delete a preset
     */
    @Delete
    suspend fun deletePreset(preset: TimerPreset)
    
    /**
     * Deselect all presets
     */
    @Query("UPDATE timer_preset SET isSelected = 0")
    suspend fun deselectAllPresets()
    
    /**
     * Select a preset by ID
     */
    @Query("UPDATE timer_preset SET isSelected = 1 WHERE id = :presetId")
    suspend fun selectPresetById(presetId: Int)
    
    /**
     * Atomically select a preset (deselects others first)
     */
    @Transaction
    suspend fun selectPreset(presetId: Int) {
        deselectAllPresets()
        selectPresetById(presetId)
    }
    
    /**
     * Delete all presets (for restore)
     */
    @Query("DELETE FROM timer_preset")
    suspend fun deleteAllPresets()
}
