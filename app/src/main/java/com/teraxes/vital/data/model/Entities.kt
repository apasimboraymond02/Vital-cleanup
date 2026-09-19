package com.teraxes.vital.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycles")
data class CycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long, // timestamp in millis
    val endDate: Long? = null, // timestamp in millis
    val flowIntensity: String = "Medium", // Spotting, Light, Medium, Heavy
    val mood: String = "Neutral",
    val symptomsJson: String = "[]", // List<String> as JSON
    val notes: String = "",
    val isDeleted: Boolean = false
)

@Entity(tableName = "symptoms")
data class SymptomLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val symptomType: String, // Cramps, Headache, Bloating, Fatigue, Mood Swings, etc.
    val severity: String = "Medium", // Low, Medium, High
    val notes: String = ""
)

@Entity(tableName = "pregnancies")
data class PregnancyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: Long, // timestamp in millis
    val dueDate: Long, // timestamp in millis
    val lastPeriodDate: Long, // timestamp in millis
    val currentWeek: Int = 1,
    val isConfirmedByDoctor: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // temperature, weight, blood_pressure, blood_sugar, mood
    val value: Double,
    val unit: String,
    val loggedDate: String, // YYYY-MM-DD
    val notes: String = ""
)

@Entity(tableName = "pregnancy_milestones")
data class PregnancyMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pregnancyId: Long,
    val week: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val time: String,
    val isEnabled: Boolean = true
)
