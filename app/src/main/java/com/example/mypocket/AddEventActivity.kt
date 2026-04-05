package com.example.mypocket.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mypocket.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddEventActivity : AppCompatActivity() {

    private val vm: EventViewModel by viewModels()

    private lateinit var etTitle: EditText
    private lateinit var etFrom: EditText
    private lateinit var etTo: EditText
    private lateinit var etDesc: EditText
    private lateinit var swAllDay: SwitchMaterial
    private lateinit var ddReminder: AutoCompleteTextView
    private lateinit var rbBusy: RadioButton

    private val fromCal = Calendar.getInstance()
    private val toCal = Calendar.getInstance()

    // reminder text -> minutes
    private val reminderOptions = listOf(
        "None" to 0,
        "10 minutes before" to 10,
        "30 minutes before" to 30,
        "1 hour before" to 60,
        "2 hours before" to 120,
        "1 day before" to 1440,
        "2 days before" to 2880
    )

    private val fmtDateTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val fmtDateOnly = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    companion object {
        const val EXTRA_EDIT_EVENT_ID = "extra_edit_event_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        // Show status bars if Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.statusBars())
        }

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // Views
        etTitle = findViewById(R.id.etTitle)
        etFrom = findViewById(R.id.etFrom)
        etTo = findViewById(R.id.etTo)
        etDesc = findViewById(R.id.etDescription)
        swAllDay = findViewById(R.id.swAllDay)
        ddReminder = findViewById(R.id.ddReminder)
        rbBusy = findViewById(R.id.rbBusy)

        // init default time: now -> +1 hour
        fromCal.timeInMillis = System.currentTimeMillis()
        toCal.timeInMillis = System.currentTimeMillis()
        toCal.add(Calendar.HOUR_OF_DAY, 1)

        setupReminderDropdown()
        refreshDateFields()

        swAllDay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                normalizeAllDay(fromCal, true)
                normalizeAllDay(toCal, false)
            }
            refreshDateFields()
        }

        etFrom.setOnClickListener { pickDateTime(isFrom = true) }
        etTo.setOnClickListener { pickDateTime(isFrom = false) }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { saveEvent() }
    }

    private fun setupReminderDropdown() {
        val labels = reminderOptions.map { it.first }
        val adapter = ArrayAdapter(this, R.layout.item_dropdown_reminder, labels)
        ddReminder.setAdapter(adapter)

        ddReminder.setOnClickListener { ddReminder.showDropDown() }
        ddReminder.setText(labels.first(), false) // default: None
    }

    private fun refreshDateFields() {
        if (swAllDay.isChecked) {
            etFrom.setText(fmtDateOnly.format(fromCal.time))
            etTo.setText(fmtDateOnly.format(toCal.time))
        } else {
            etFrom.setText(fmtDateTime.format(fromCal.time))
            etTo.setText(fmtDateTime.format(toCal.time))
        }
    }

    private fun pickDateTime(isFrom: Boolean) {
        val target = if (isFrom) fromCal else toCal
        val other = if (isFrom) toCal else fromCal

        DatePickerDialog(
            this,
            { _, y, m, d ->
                target.set(Calendar.YEAR, y)
                target.set(Calendar.MONTH, m)
                target.set(Calendar.DAY_OF_MONTH, d)

                if (swAllDay.isChecked) {
                    if (isFrom) normalizeAllDay(target, true) else normalizeAllDay(target, false)
                    syncIfInvalidRange()
                    refreshDateFields()
                } else {
                    TimePickerDialog(
                        this,
                        { _, hh, mm ->
                            target.set(Calendar.HOUR_OF_DAY, hh)
                            target.set(Calendar.MINUTE, mm)
                            target.set(Calendar.SECOND, 0)
                            target.set(Calendar.MILLISECOND, 0)

                            // if picking start time, make end time +1 hour by default
                            if (isFrom) {
                                other.timeInMillis = target.timeInMillis
                                other.add(Calendar.HOUR_OF_DAY, 1)
                            }

                            syncIfInvalidRange()
                            refreshDateFields()
                        },
                        target.get(Calendar.HOUR_OF_DAY),
                        target.get(Calendar.MINUTE),
                        true
                    ).show()
                }
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun normalizeAllDay(cal: Calendar, isStart: Boolean) {
        cal.set(Calendar.HOUR_OF_DAY, if (isStart) 0 else 23)
        cal.set(Calendar.MINUTE, if (isStart) 0 else 59)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun syncIfInvalidRange() {
        if (toCal.timeInMillis < fromCal.timeInMillis) {
            toCal.timeInMillis = fromCal.timeInMillis
            if (swAllDay.isChecked) {
                normalizeAllDay(toCal, false)
            }
        }
    }

    private fun saveEvent() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            toast("Title cannot be empty")
            return
        }

        val selectedReminderLabel = ddReminder.text.toString()
        val reminderMinutes =
            reminderOptions.firstOrNull { it.first == selectedReminderLabel }?.second ?: 0
        val isBusy = rbBusy.isChecked
        val start = fromCal.timeInMillis
        val end = toCal.timeInMillis

        if (end < start) {
            toast("End time cannot be before start time")
            return
        }

        // Create EventEntity with the updated times
        val event = com.example.mypocket.entity.EventEntity(
            title = title,
            description = etDesc.text.toString().trim(),
            startAt = start,
            endAt = end,
            allDay = swAllDay.isChecked,
            reminderMinutes = reminderMinutes,
            isBusy = isBusy
        )

        // Insert into database and get ID
        vm.insert(event) { _ ->
            Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}