package com.teraxes.vital.data.repository

import com.teraxes.vital.data.database.VitalDatabase
import com.teraxes.vital.data.model.*
import kotlinx.coroutines.flow.Flow

class VitalRepository(private val db: VitalDatabase) {
    val cycles: Flow<List<CycleEntity>> = db.cycleDao().getAllCycles()
    val symptoms: Flow<List<SymptomLogEntity>> = db.symptomDao().getAllSymptoms()
    val activePregnancy: Flow<PregnancyEntity?> = db.pregnancyDao().getActivePregnancy()
    val healthMetrics: Flow<List<HealthMetricEntity>> = db.healthMetricDao().getAllMetrics()
    val reminders: Flow<List<ReminderEntity>> = db.reminderDao().getAllReminders()

    suspend fun insertCycle(cycle: CycleEntity): Long = db.cycleDao().insertCycle(cycle)
    suspend fun updateCycle(cycle: CycleEntity) = db.cycleDao().updateCycle(cycle)
    suspend fun deleteCycle(id: Long) = db.cycleDao().markDeleted(id)

    suspend fun insertSymptom(symptom: SymptomLogEntity): Long = db.symptomDao().insertSymptom(symptom)
    suspend fun deleteSymptom(id: Long) = db.symptomDao().deleteSymptom(id)

    suspend fun insertPregnancy(pregnancy: PregnancyEntity): Long = db.pregnancyDao().insertPregnancy(pregnancy)
    suspend fun deletePregnancy() = db.pregnancyDao().deletePregnancy()

    fun getPregnancyMilestones(pregnancyId: Long): Flow<List<PregnancyMilestoneEntity>> =
        db.pregnancyMilestoneDao().getMilestones(pregnancyId)

    suspend fun insertMilestone(milestone: PregnancyMilestoneEntity) =
        db.pregnancyMilestoneDao().insertMilestone(milestone)

    suspend fun setMilestoneCompleted(id: Long, completed: Boolean) =
        db.pregnancyMilestoneDao().setMilestoneCompleted(id, completed)

    suspend fun insertMetric(metric: HealthMetricEntity): Long = db.healthMetricDao().insertMetric(metric)
    suspend fun deleteMetric(id: Long) = db.healthMetricDao().deleteMetric(id)

    suspend fun insertReminder(reminder: ReminderEntity) = db.reminderDao().insertReminder(reminder)
    suspend fun clearAllCycles() = db.cycleDao().deleteAll()
    suspend fun clearAllData() {
        db.cycleDao().deleteAll()
        db.symptomDao().deleteAll()
        db.pregnancyDao().deleteAll()
        db.pregnancyMilestoneDao().deleteAll()
        db.healthMetricDao().deleteAll()
        db.reminderDao().deleteAll()
    }
}
