/*
 * Copyright (c) 2025 Vrushal
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.vrn7712.pomodoro.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        IntPreference::class, 
        BooleanPreference::class, 
        StringPreference::class, 
        Stat::class, 
        Task::class,
        TimerPreset::class
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun preferenceDao(): PreferenceDao
    abstract fun statDao(): StatDao
    abstract fun taskDao(): TaskDao
    abstract fun timerPresetDao(): TimerPresetDao

    companion object {

        @Volatile
        private var Instance: AppDatabase? = null
        
        // Migration from v4 to v5: Add timer_preset table
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS timer_preset (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        focusMinutes INTEGER NOT NULL,
                        shortBreakMinutes INTEGER NOT NULL,
                        longBreakMinutes INTEGER NOT NULL,
                        isSelected INTEGER NOT NULL DEFAULT 0,
                        isBuiltIn INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
