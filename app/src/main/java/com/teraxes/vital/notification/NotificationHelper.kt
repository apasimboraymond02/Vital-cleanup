package com.teraxes.vital.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.teraxes.vital.MainActivity
import com.teraxes.vital.R

object NotificationHelper {
    const val CHANNEL_ID = "vital_notifications"
    const val CHANNEL_FERTILITY_ID = "fertility_notifications"
    const val CHANNEL_DAILY_ID = "daily_checkin_notifications"
    const val CHANNEL_MEDICATION_ID = "medication_notifications"

    const val NOTIFICATION_ID_PERIOD = 101
    const val NOTIFICATION_ID_OVULATION = 102
    const val NOTIFICATION_ID_SYMPTOMS = 103
    const val NOTIFICATION_ID_APPROACHING = 104
    const val NOTIFICATION_ID_FERTILITY = 105
    const val NOTIFICATION_ID_DAILY_LOG = 106
    const val NOTIFICATION_ID_MEDICATION = 107

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channels = listOf(
                NotificationChannel(CHANNEL_ID, "Cycle Reminders", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_FERTILITY_ID, "Fertility Alerts", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_DAILY_ID, "Daily Check-ins", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_MEDICATION_ID, "Medication Reminders", NotificationManager.IMPORTANCE_HIGH)
            )
            manager.createNotificationChannels(channels)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        channelId: String = CHANNEL_ID,
        navTarget: String = "DASHBOARD"
    ): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAV_TARGET", navTarget)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_vital)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun scheduleCycleReminder(context: Context, predictedStartMs: Long, fertileWindowStartMs: Long, daysBefore: Int, isDiscreetMode: Boolean) {}
    fun cancelCycleReminders(context: Context) {}
    fun scheduleDailyCheckInReminder(context: Context, hour: Int = 20, minute: Int = 0, isDiscreetMode: Boolean) {}
    fun cancelDailyCheckInReminder(context: Context) {}
    fun scheduleMedicationReminder(context: Context, hour: Int = 9, minute: Int = 0, isDiscreetMode: Boolean) {}
    fun cancelMedicationReminder(context: Context) {}
}
