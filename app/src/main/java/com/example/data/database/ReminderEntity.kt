package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetTimeMillis: Long,
    val formattedDateTime: String,
    val isCompleted: Boolean = false,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)
