package com.example.mypocket.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.mypocket.ui.EventDetailActivity
import com.example.mypocket.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra("eventId", -1)
        val title = intent.getStringExtra("title") ?: "Reminder" // fallback
        val startAt = intent.getLongExtra("startAt", -1L)
        val endAt = intent.getLongExtra("endAt", -1L)

        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeRange = if (startAt > 0 && endAt > 0) {
            "${fmt.format(java.util.Date(startAt))} - ${fmt.format(java.util.Date(endAt))}"
        } else {
            "No time info"
        }

        // Check if the app is targeting Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if we have the permission to post notifications
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // If permission is not granted, show a message (or request the permission if needed)
                Toast.makeText(context, "Permission to post notifications is required.", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Create a notification channel for Android O and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "eventReminderChannel"
            val channelName = "Event Reminders"
            val channelDescription = "Notifications for event reminders"

            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            channel.description = channelDescription
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent to open EventDetailActivity when the notification is tapped
        val openEventIntent = Intent(context, EventDetailActivity::class.java).apply {
            putExtra(EventDetailActivity.EXTRA_EVENT_ID, eventId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId.toInt(),
            openEventIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create the notification
        Log.d("ReminderReceiver", "Notification for $title at $timeRange")
        val notification = NotificationCompat.Builder(context, "eventReminderChannel")
            .setContentTitle(title)
            .setContentText(timeRange)
            .setSmallIcon(R.drawable.ic_event_reminder)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show the notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId.toInt(), notification)
    }
}