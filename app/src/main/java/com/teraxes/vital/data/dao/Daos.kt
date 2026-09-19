package com.teraxes.vital.data.dao

import androidx.room.*
import com.teraxes.vital.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {
    @Query("SELECT * FROM cycles WHERE isDeleted = 0 ORDER BY startDate DESC")
    fun getAllCycles(): Flow<List<CycleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: CycleEntity): Long

    @Update
    suspend fun updateCycle(cycle: CycleEntity)

    @Query("UPDATE cycles SET isDeleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)

    @Query("DELETE FROM cycles")
    suspend fun deleteAll()
}

@Dao
interface SymptomDao {
    @Query("SELECT * FROM symptoms ORDER BY date DESC")
    fun getAllSymptoms(): Flow<List<SymptomLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(symptom: SymptomLogEntity): Long

    @Query("DELETE FROM symptoms WHERE id = :id")
    suspend fun deleteSymptom(id: Long)

    @Query("DELETE FROM symptoms")
    suspend fun deleteAll()
}

@Dao
interface PregnancyDao {
    @Query("SELECT * FROM pregnancies ORDER BY startDate DESC LIMIT 1")
    fun getActivePregnancy(): Flow<PregnancyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPregnancy(pregnancy: PregnancyEntity): Long

    @Query("DELETE FROM pregnancies")
    suspend fun deletePregnancy()

    @Query("DELETE FROM pregnancies")
    suspend fun deleteAll()
}

@Dao
interface HealthMetricDao {
    @Query("SELECT * FROM health_metrics ORDER BY loggedDate DESC, id DESC")
    fun getAllMetrics(): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE type = :type ORDER BY loggedDate DESC")
    fun getMetricsByType(type: String): Flow<List<HealthMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: HealthMetricEntity): Long

    @Query("DELETE FROM health_metrics WHERE id = :id")
    suspend fun deleteMetric(id: Long)

    @Query("DELETE FROM health_metrics")
    suspend fun deleteAll()
}

@Dao
interface PregnancyMilestoneDao {
    @Query("SELECT * FROM pregnancy_milestones WHERE pregnancyId = :pregnancyId ORDER BY week ASC")
    fun getMilestones(pregnancyId: Long): Flow<List<PregnancyMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: PregnancyMilestoneEntity)

    @Query("UPDATE pregnancy_milestones SET isCompleted = :completed WHERE id = :id")
    suspend fun setMilestoneCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM pregnancy_milestones")
    suspend fun deleteAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY id ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
