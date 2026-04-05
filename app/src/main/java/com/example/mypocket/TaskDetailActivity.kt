package com.example.mypocket.ui

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.entity.TaskEntity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private var taskId: Long = -1L
    private var original: TaskEntity? = null

    private lateinit var tvDate: TextView
    private lateinit var etTitle: EditText
    private lateinit var etNote: EditText
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button

    private val dateFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        // Check if the Android version is 11 (API level 30) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Get the WindowInsetsController and ensure the status bar is visible
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        tvDate = findViewById(R.id.tvDate)
        etTitle = findViewById(R.id.etTitle)
        etNote = findViewById(R.id.etNote)
        btnUpdate = findViewById(R.id.btnUpdate)

        btnUpdate.visibility = View.GONE // Hidden until changes happen

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) {
            finish()
            return
        }

        loadTask()

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonVisibility()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etTitle.addTextChangedListener(watcher)
        etNote.addTextChangedListener(watcher)

        btnUpdate.setOnClickListener { updateTask() }
    }

    // Menu shown in toolbar (delete icon)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_task_detail, menu)
        return true
    }

    // Handle delete clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_delete -> {
                confirmDelete()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun loadTask() {
        val dao = AppDatabase.getInstance(applicationContext).taskDao()

        lifecycleScope.launch {
            val task = withContext(Dispatchers.IO) { dao.getById(taskId) }
            if (task == null) {
                toast("Task not found")
                finish()
                return@launch
            }

            original = task

            // Fill UI
            tvDate.text = LocalDate.ofEpochDay(task.dateEpochDay).format(dateFmt)
            etTitle.setText(task.title)
            etNote.setText(task.note)

            btnUpdate.visibility = View.GONE
        }
    }

    private fun updateButtonVisibility() {
        val old = original ?: return

        val newTitle = etTitle.text.toString().trim()
        val newNote = etNote.text.toString().trim()

        val changed = (newTitle != old.title) || (newNote != old.note)

        btnUpdate.visibility = if (changed) View.VISIBLE else View.GONE
    }

    private fun updateTask() {
        val old = original ?: return

        val title = etTitle.text.toString().trim()
        val note = etNote.text.toString().trim()

        if (title.isEmpty()) {
            toast("Title cannot be empty")
            return
        }

        val updated = old.copy(
            title = title,
            note = note
        )

        val dao = AppDatabase.getInstance(applicationContext).taskDao()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { dao.update(updated) }
            original = updated
            btnUpdate.visibility = View.GONE
            toast("Updated")
            setResult(RESULT_OK)
        }
    }

    private fun confirmDelete() {
        val task = original ?: return

        AlertDialog.Builder(this)
            .setTitle("Delete task?")
            .setMessage("This will permanently remove \"${task.title}\".")
            .setPositiveButton("Delete") { _, _ -> deleteTask(task) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: TaskEntity) {
        val dao = AppDatabase.getInstance(applicationContext).taskDao()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) { dao.delete(task) }
            toast("Deleted")
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
