package com.teraxes.vital.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.teraxes.vital.data.dao.*
import com.teraxes.vital.data.model.*

@Database(
    entities = [
        CycleEntity::class,
        SymptomLogEntity::class,
        PregnancyEntity::class,
        HealthMetricEntity::class,
        PregnancyMilestoneEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VitalDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun symptomDao(): SymptomDao
    abstract fun pregnancyDao(): PregnancyDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun pregnancyMilestoneDao(): PregnancyMilestoneDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: VitalDatabase? = null

        fun getDatabase(context: Context): VitalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VitalDatabase::class.java,
                    "vital_health_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
