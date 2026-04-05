package com.example.mypocket.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.entity.EventEntity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    private var currentEvent: EventEntity? = null

    private lateinit var tvTitle: TextView
    private lateinit var tvDayDateTime: TextView
    private lateinit var tvReminder: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvBusyFree: TextView
    private val eventViewModel: EventViewModel by viewModels()

    private var eventId: Long = -1L

    private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // Check if the Android version is 11 (API level 30) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        // Views
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvDayDateTime = findViewById(R.id.tvDetailDayDateTime)
        tvReminder = findViewById(R.id.tvDetailReminder)
        tvDescription = findViewById(R.id.tvDetailDescription)
        tvBusyFree = findViewById(R.id.tvDetailBusyFree)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Read event id
        eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) {
            finish()
            return
        }

        // Observe event changes (auto refresh after edit)
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.eventDao()

        lifecycleScope.launch {
            dao.observeById(eventId).collectLatest { event ->
                if (event == null) {
                    finish()
                    return@collectLatest
                }
                currentEvent = event
                showEvent(event)

                // Remove scheduling from here—reminder is now handled in ViewModel during insert/update
            }
        }
    }

    // Menu shown in toolbar (edit/delete icons)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_event_detail, menu)
        return true
    }

    // Handle toolbar clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_edit -> {
                openEdit()
                true
            }
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEvent(event: EventEntity) {
        tvTitle.text = event.title

        val startZdt = Instant.ofEpochMilli(event.startAt).atZone(ZoneId.systemDefault())
        val endZdt = Instant.ofEpochMilli(event.endAt).atZone(ZoneId.systemDefault())

        val startDate = startZdt.toLocalDate()
        val endDate = endZdt.toLocalDate()

        tvDayDateTime.text = when {
            event.allDay && startDate == endDate -> {
                "${startZdt.format(dayFmt)} | All day"
            }
            event.allDay && startDate != endDate -> {
                "${startZdt.format(dayFmt)} to ${endZdt.format(dayFmt)} | All day"
            }
            !event.allDay && startDate == endDate -> {
                "${startZdt.format(dayFmt)} | ${startZdt.format(timeFmt)} - ${endZdt.format(timeFmt)}"
            }
            else -> {
                "${startZdt.format(dayFmt)} ${startZdt.format(timeFmt)} to " +
                        "${endZdt.format(dayFmt)} ${endZdt.format(timeFmt)}"
            }
        }

        if (event.reminderMinutes <= 0) {
            tvReminder.visibility = View.GONE
        } else {
            tvReminder.visibility = View.VISIBLE
            tvReminder.text = "Reminder: ${event.reminderMinutes} minutes before"
        }

        tvDescription.text = event.description.ifEmpty { "No description" }
        tvBusyFree.text = if (event.isBusy) {
            "You are Busy this day"
        } else {
            "You are Free this day"
        }
    }

    private fun openEdit() {
        val e = currentEvent ?: return
        val i = Intent(this, EditEventActivity::class.java)
        i.putExtra(EditEventActivity.EXTRA_EVENT_ID, e.eventId)
        startActivity(i)
    }

    private fun confirmDelete() {
        val e = currentEvent ?: return

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete event?")
            .setMessage("This will permanently remove \"${e.title}\".")
            .setPositiveButton("Delete") { _, _ -> deleteEvent(e) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(e: EventEntity) {
        eventViewModel.deleteEvent(this, e) {
            Toast.makeText(this, "Event deleted and reminder canceled", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}