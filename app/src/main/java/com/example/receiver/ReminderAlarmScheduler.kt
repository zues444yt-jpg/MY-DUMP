package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.ReminderItem

class ReminderAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderItem) {
        if (reminder.dueTimestamp <= System.currentTimeMillis() || reminder.isCompleted) {
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_DESC, reminder.description)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_PRIORITY, reminder.priority.name)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_CATEGORY, reminder.category)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.dueTimestamp,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.dueTimestamp,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueTimestamp,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("ReminderAlarmScheduler", "Could not set exact alarm, falling back: ${e.message}")
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueTimestamp,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e("ReminderAlarmScheduler", "Failed to schedule alarm", ex)
            }
        } catch (e: Exception) {
            Log.e("ReminderAlarmScheduler", "Failed to schedule alarm", e)
        }
    }

    fun cancel(reminderId: Long) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ReminderAlarmReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
