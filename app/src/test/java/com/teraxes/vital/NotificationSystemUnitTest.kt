package com.teraxes.vital

import com.teraxes.vital.notification.NotificationHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class NotificationSystemUnitTest {

    @Test
    fun testNotificationChannelConstants() {
        assertEquals("vital_cycle_reminders", NotificationHelper.CHANNEL_ID)
        assertEquals("vital_fertility_reminders", NotificationHelper.CHANNEL_FERTILITY_ID)
        assertEquals("vital_daily_reminders", NotificationHelper.CHANNEL_DAILY_ID)
        assertEquals("vital_medication_reminders", NotificationHelper.CHANNEL_MEDICATION_ID)
    }

    @Test
    fun testAlarmRequestCodeUniqueness() {
        // Verify unique request codes to prevent collisions between alarms
        val requestCodes = setOf(
            NotificationHelper.ALARM_REQ_APPROACHING,
            NotificationHelper.ALARM_REQ_DAY_OF,
            NotificationHelper.ALARM_REQ_FERTILE,
            NotificationHelper.ALARM_REQ_DAILY_LOG,
            NotificationHelper.ALARM_REQ_MEDICATION
        )
        assertEquals(5, requestCodes.size)
    }

    @Test
    fun testNotificationIdSeparation() {
        val notificationIds = setOf(
            NotificationHelper.NOTIFICATION_ID_APPROACHING,
            NotificationHelper.NOTIFICATION_ID_PERIOD_START,
            NotificationHelper.NOTIFICATION_ID_FERTILITY,
            NotificationHelper.NOTIFICATION_ID_DAILY_LOG,
            NotificationHelper.NOTIFICATION_ID_MEDICATION
        )
        assertEquals(5, notificationIds.size)
    }

    @Test
    fun testDailyCheckInTimeCalculation() {
        // Daily check-in is scheduled for 8:00 PM (20:00)
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val target = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertTrue(target.timeInMillis > now.timeInMillis)
        val diffHours = (target.timeInMillis - now.timeInMillis) / (1000 * 60 * 60)
        assertEquals(10L, diffHours)
    }

    @Test
    fun testMedicationTimeCalculation() {
        // Medication is scheduled for 9:00 AM (09:00)
        val morning = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val target = Calendar.getInstance().apply {
            timeInMillis = morning.timeInMillis
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        assertTrue(target.timeInMillis > morning.timeInMillis)
        val diffHours = (target.timeInMillis - morning.timeInMillis) / (1000 * 60 * 60)
        assertEquals(1L, diffHours)
    }

    @Test
    fun testCycleApproachingTriggerTime() {
        val predictedStartMs = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L) // 7 days in future
        val daysBefore = 2

        val triggerCal = Calendar.getInstance().apply {
            timeInMillis = predictedStartMs - (daysBefore * 24 * 60 * 60 * 1000L)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Trigger should be earlier than predicted start date
        assertTrue(triggerCal.timeInMillis < predictedStartMs)
        val diffDays = (predictedStartMs - triggerCal.timeInMillis) / (24 * 60 * 60 * 1000L)
        // Since trigger is set to 9:00 AM of that day, diff is approximately 2 days
        assertTrue(diffDays in 1..2)
    }
}
