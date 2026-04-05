package com.example.mypocket.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.entity.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val eventDao = db.eventDao()

    fun insert(event: EventEntity, onDone: (Long) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Insert event in DB
            val id = eventDao.insert(event)

            // Schedule reminder only if reminderMinutes > 0
            if (event.reminderMinutes > 0) {
                // Create a copy with the generated ID
                val eventWithId = event.copy(eventId = id)

                // Make sure scheduling runs on main thread if it needs UI (like Toast)
                withContext(Dispatchers.Main) {
                    scheduleReminderOnce(eventWithId)
                }
            }

            // Call onDone on main thread
            withContext(Dispatchers.Main) {
                onDone(id)
            }
        }
    }

    fun update(event: EventEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            // Update in DB
            eventDao.update(event)

            // Schedule reminder safely on main thread
            if (event.reminderMinutes > 0) {
                withContext(Dispatchers.Main) {
                    scheduleReminderOnce(event)
                }
            }

            // Call callback on main thread
            withContext(Dispatchers.Main) { onDone?.invoke() }
        }
    }

    fun deleteEvent(context: Context, event: EventEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Cancel the pending reminder alarm
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                event.eventId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            // 2. Delete the event from the database
            eventDao.delete(event)

            // 3. Optional callback on main thread
            launch(Dispatchers.Main) {
                onDone?.invoke()
            }
        }
    }

    // Schedule reminder only once when event is inserted/updated
   fun scheduleReminderOnce(event: EventEntity) {
        val reminderTime = event.startAt - (event.reminderMinutes.toLong() * 60_000)

        Log.d("Reminder", "Current time: ${System.currentTimeMillis()}, reminderTime: $reminderTime")

        if (reminderTime <= System.currentTimeMillis()) return // Don't schedule past reminders

        val intent = Intent(getApplication(), ReminderReceiver::class.java).apply {
            putExtra("eventId", event.eventId)
            putExtra("title", event.title)
            putExtra("startAt", event.startAt)
            putExtra("endAt", event.endAt)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            (event.eventId % Int.MAX_VALUE).toInt(), // Safe request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        checkAndScheduleExactAlarm(getApplication(), reminderTime, pendingIntent)
    }

    // Function to check and schedule exact alarm
    fun checkAndScheduleExactAlarm(context: Context, reminderTime: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check if we can schedule exact alarms
            if (!alarmManager.canScheduleExactAlarms()) {
                // If permission is not granted, ask user to enable it
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            } else {
                // If permission is granted, schedule the exact alarm
                try {
                    // Reminder fires on time even if phone is idle
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                    Toast.makeText(context, "Reminder scheduled", Toast.LENGTH_SHORT).show()
                } catch (e: SecurityException) {
                    // Handle the case where the permission is not granted
                    Toast.makeText(context, "Permission to schedule exact alarm is required.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // For Android versions below Android 12, schedule the alarm without permission check
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    suspend fun showToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}