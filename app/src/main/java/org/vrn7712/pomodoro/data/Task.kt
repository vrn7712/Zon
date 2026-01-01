package org.vrn7712.pomodoro.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "tasks")
@Serializable
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val subject: String = "General",
    val dueDate: Long? = null,
    val notes: String? = null,
    val priority: Int = 1 // 1 = Low, 2 = Medium, 3 = High
)
