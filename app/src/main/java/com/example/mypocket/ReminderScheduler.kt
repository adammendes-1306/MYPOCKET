package com.example.mypocket.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.mypocket.entity.EventEntity
import com.example.mypocket.ui.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderScheduler {

    fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val events: List<EventEntity> = db.eventDao().getAllEventsWithReminder()

            for (event in events) {
                // skip if no reminder
                val reminderMinutes = event.reminderMinutes
                if (reminderMinutes <= 0) continue

                scheduleReminder(context, event)
            }
        }
    }

    private fun scheduleReminder(context: Context, event: EventEntity) {
        val reminderMinutes = event.reminderMinutes
        val reminderTime = event.startAt - (reminderMinutes * 60 * 1000)
        if (reminderTime <= System.currentTimeMillis()) return // skip past reminders

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("eventId", event.eventId) // <- use eventId
            putExtra("title", event.title)
            putExtra("description", event.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.eventId.toInt(), // <- use eventId
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Check if exact alarms are allowed
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Optional: notify user to enable exact alarms in settings
                    // You can show a Toast or Notification
                    return
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle case where exact alarms are not allowed
            e.printStackTrace()
            // Optional: fallback to set() (inexact) or notify user
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }
}