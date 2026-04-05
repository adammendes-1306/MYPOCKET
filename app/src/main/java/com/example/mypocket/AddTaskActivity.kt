package com.example.mypocket.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.entity.TaskEntity
import com.example.mypocket.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EPOCH_DAY = "extra_epoch_day"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        // Check if the Android version is 11 (API level 30) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Get the WindowInsetsController and ensure the status bar is visible
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        val tvDate = findViewById<TextView>(R.id.tvDate)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etNote = findViewById<EditText>(R.id.etNote)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = findViewById<MaterialButton>(R.id.btnCancel)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, LocalDate.now().toEpochDay())
        val date = LocalDate.ofEpochDay(epochDay)

        val fmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
        tvDate.text = date.format(fmt)

        toolbar.setNavigationOnClickListener {
            finish()   // closes the activity and goes back
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val note = etNote.text.toString().trim().ifEmpty { null }

            if (title.isBlank()) {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val task = TaskEntity(
                title = title,
                note = note,
                dateEpochDay = epochDay
            )

            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AddTaskActivity)
                withContext(Dispatchers.IO) { db.taskDao().insert(task) }

                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        btnCancel.setOnClickListener { finish() }
    }
}
