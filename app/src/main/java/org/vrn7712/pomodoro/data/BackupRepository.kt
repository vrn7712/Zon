package org.vrn7712.pomodoro.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import org.vrn7712.pomodoro.utils.toColor
import androidx.compose.ui.graphics.toArgb

class BackupRepository(
    private val appDatabase: AppDatabase,
    private val context: Context
) {
    // More lenient JSON parsing for backup compatibility
    private val jsonFormat = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun exportData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tasks: List<Task> = appDatabase.taskDao().getAllTasksList()
            val stats: List<Stat> = appDatabase.statDao().getAllStats()
            val booleanPrefs: List<BooleanPreference> = appDatabase.preferenceDao().getAllBooleanPreferences()
            val intPrefs: List<IntPreference> = appDatabase.preferenceDao().getAllIntPreferences()
            val stringPrefs: List<StringPreference> = appDatabase.preferenceDao().getAllStringPreferences()

            val timerPresets: List<TimerPreset> = appDatabase.timerPresetDao().getAllPresetsSync()

            val backupData = BackupData(
                tasks = tasks,
                stats = stats,
                booleanPreferences = booleanPrefs,
                intPreferences = intPrefs,
                stringPreferences = stringPrefs,
                timerPresets = timerPresets
            )
            val jsonString = jsonFormat.encodeToString(backupData)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importData(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            
            // Clean the JSON string (remove BOM and extra whitespace)
            val jsonString = stringBuilder.toString()
                .trim()
                .removePrefix("\uFEFF") // Remove UTF-8 BOM if present
            val backupData: BackupData = jsonFormat.decodeFromString(jsonString)

            appDatabase.withTransaction {
                // Clear existing data
                appDatabase.taskDao().deleteAll()
                appDatabase.statDao().deleteAll()
                
                val prefDao = appDatabase.preferenceDao()
                prefDao.resetBooleanPreferences()
                prefDao.resetIntPreferences()
                prefDao.resetStringPreferences()

                // Restore timer presets
                val presetDao = appDatabase.timerPresetDao()
                presetDao.deleteAllPresets()
                if (backupData.timerPresets.isNotEmpty()) {
                    presetDao.insertPresets(backupData.timerPresets)
                } else {
                    // Initialize defaults if no presets in backup
                    presetDao.insertPresets(TimerPreset.DEFAULTS)
                }
                
                // Insert new data
                val tasks = backupData.tasks
                for (i in 0 until tasks.size) {
                    appDatabase.taskDao().insertTask(tasks[i])
                }
                
                val stats = backupData.stats
                for (i in 0 until stats.size) {
                    appDatabase.statDao().insertStat(stats[i])
                }
                
                val boolPrefs = backupData.booleanPreferences
                for (i in 0 until boolPrefs.size) {
                    prefDao.insertBooleanPreference(boolPrefs[i])
                }
                
                val stringPrefs = backupData.stringPreferences
                val stringPrefKeys = stringPrefs.map { it.key }.toSet()

                // Color preference keys that need migration from Int to String hex format
                val colorPrefKeys = setOf(
                    "primary_color", "surface_color", "on_surface_color",
                    "surface_variant_color", "secondary_container_color", "on_secondary_container_color"
                )
                
                val intPrefs = backupData.intPreferences
                for (i in 0 until intPrefs.size) {
                    val pref = intPrefs[i]
                    if (pref.key in colorPrefKeys) {
                        // Only migrate if NOT present in string preferences
                        // This prioritizes the new String format if both exist (zombie data case)
                        if (pref.key !in stringPrefKeys) {
                             val hexColor = String.format(
                                java.util.Locale.US,
                                "#%08X",
                                pref.value.toLong() and 0xFFFFFFFFL
                            )
                            prefDao.insertStringPreference(StringPreference(pref.key, hexColor))
                        }
                    } else {
                        prefDao.insertIntPreference(pref)
                    }
                }
                
                for (i in 0 until stringPrefs.size) {
                    val pref = stringPrefs[i]
                    if (pref.key == "color_scheme" && pref.value.startsWith("Color(")) {
                        // Migrate legacy Color.toString() format to hex
                        try {
                            val color = pref.value.toColor()
                            val hexColor = String.format(
                                java.util.Locale.US,
                                "#%08X",
                                color.toArgb().toLong() and 0xFFFFFFFFL
                            )
                            prefDao.insertStringPreference(StringPreference(pref.key, hexColor))
                        } catch (e: Exception) {
                            // If parsing fails, insert as-is
                            prefDao.insertStringPreference(pref)
                        }
                    } else {
                        prefDao.insertStringPreference(pref)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteAllStats() = withContext(Dispatchers.IO) {
        appDatabase.statDao().clearAll()
    }
}
