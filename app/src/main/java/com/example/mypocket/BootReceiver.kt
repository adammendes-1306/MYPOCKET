package com.example.mypocket.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mypocket.data.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all reminders/events here after device reboot
            ReminderScheduler.rescheduleAll(context)
        }
    }
}