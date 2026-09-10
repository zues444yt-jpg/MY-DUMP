package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.Priority
import com.example.data.model.ReminderItem
import com.example.data.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        when (action) {
            ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Reminder"
                val desc = intent.getStringExtra(EXTRA_REMINDER_DESC) ?: ""
                val priority = intent.getStringExtra(EXTRA_REMINDER_PRIORITY) ?: Priority.MEDIUM.name
                val category = intent.getStringExtra(EXTRA_REMINDER_CATEGORY) ?: "General"

                showNotification(context, reminderId, title, desc, priority, category)

                // Check if repeating and reschedule next
                if (reminderId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        val reminder = db.reminderDao().getReminderById(reminderId)
                        if (reminder != null && reminder.repeatMode != RepeatMode.NONE && !reminder.isCompleted) {
                            val nextDue = calculateNextDueTime(reminder.dueTimestamp, reminder.repeatMode)
                            val updated = reminder.copy(dueTimestamp = nextDue)
                            db.reminderDao().update(updated)
                            ReminderAlarmScheduler(context).schedule(updated)
                        }
                    }
                }
            }

            ACTION_MARK_DONE -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (reminderId != -1L) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(reminderId.toInt())

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        db.reminderDao().setCompleted(reminderId, true, System.currentTimeMillis())
                    }
                }
            }

            ACTION_SNOOZE_15 -> {
                val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
                if (reminderId != -1L) {
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(reminderId.toInt())

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.getDatabase(context)
                        val reminder = db.reminderDao().getReminderById(reminderId)
                        if (reminder != null) {
                            val snoozedDue = System.currentTimeMillis() + 15 * 60 * 1000
                            val updated = reminder.copy(dueTimestamp = snoozedDue, isCompleted = false)
                            db.reminderDao().update(updated)
                            ReminderAlarmScheduler(context).schedule(updated)
                        }
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val scheduler = ReminderAlarmScheduler(context)
                    val pendingList = db.reminderDao().getUpcomingPendingReminders()
                    pendingList.forEach { scheduler.schedule(it) }
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: Long,
        title: String,
        description: String,
        priority: String,
        category: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "yf_reminder_channel_high"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Notifications for scheduled reminders and tasks"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap opens app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done Action
        val doneIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15 min Action
        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_15
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (description.isNotBlank()) {
            description
        } else {
            "Category: $category"
        }

        val notifPriority = when (priority) {
            Priority.HIGH.name -> NotificationCompat.PRIORITY_MAX
            Priority.LOW.name -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_HIGH
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\nCategory: $category"))
            .setPriority(notifPriority)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", donePendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze 15m", snoozePendingIntent)

        notificationManager.notify(reminderId.toInt(), builder.build())
    }

    private fun calculateNextDueTime(currentDue: Long, repeatMode: RepeatMode): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentDue
        }
        val now = System.currentTimeMillis()

        while (calendar.timeInMillis <= now) {
            when (repeatMode) {
                RepeatMode.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatMode.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatMode.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RepeatMode.NONE -> break
            }
        }
        return calendar.timeInMillis
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val ACTION_MARK_DONE = "com.example.ACTION_MARK_DONE"
        const val ACTION_SNOOZE_15 = "com.example.ACTION_SNOOZE_15"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_DESC = "extra_reminder_desc"
        const val EXTRA_REMINDER_PRIORITY = "extra_reminder_priority"
        const val EXTRA_REMINDER_CATEGORY = "extra_reminder_category"
    }
}
