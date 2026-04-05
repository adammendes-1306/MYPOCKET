package com.example.mypocket.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.adapter.TaskListSectionAdapter
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.data.TaskDao
import com.example.mypocket.data.TaskListRow
import com.example.mypocket.entity.TaskEntity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskListFragment : Fragment() {

    private lateinit var adapter: TaskListSectionAdapter
    private lateinit var taskDao: TaskDao
    private lateinit var fabTask: FloatingActionButton
    private lateinit var tvNoTasks: TextView

    // NEW: launcher for edit screen
    private val editTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadTasks()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fabTask = view.findViewById(R.id.fabTask)
        tvNoTasks = view.findViewById(R.id.tvNoTasks)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val db = AppDatabase.getInstance(requireContext().applicationContext)
        taskDao = db.taskDao()

        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(requireContext())

        // UPDATED: now TaskListSectionAdapter takes 2 callbacks:
        // 1) onToggleComplete
        // 2) onTaskClick
        adapter = TaskListSectionAdapter(
            onToggleComplete = { taskId, completed ->
                lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.setTaskCompleted(taskId, completed)
                    withContext(Dispatchers.Main) { loadTasks() }
                }
            },
            onTaskClick = { taskId ->
                val i = Intent(requireContext(), TaskDetailActivity::class.java)
                i.putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId)
                editTaskLauncher.launch(i)
            }
        )

        rvTasks.adapter = adapter
        loadTasks()

        fabTask.setOnClickListener {

            // Open AddTaskActivity
            val intent = Intent(requireContext(), AddTaskActivity::class.java)
            startActivity(intent)

        }
    }

    private fun buildRows(tasks: List<TaskEntity>): List<TaskListRow> {
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())

        return tasks
            .sortedBy { it.dateEpochDay }
            .groupBy { it.dateEpochDay }
            .toSortedMap()
            .flatMap { (epochDay, dayTasks) ->
                val header = LocalDate.ofEpochDay(epochDay).format(formatter)
                listOf(TaskListRow.Header(header)) +
                        dayTasks.map { TaskListRow.TaskItem(it) }
            }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            val tasks = withContext(Dispatchers.IO) { taskDao.getAllTasks() }
            val rows = buildRows(tasks)

            // If there are no tasks, show the "No tasks available" message, otherwise hide it
            if (tasks.isEmpty()) {
                tvNoTasks.visibility = View.VISIBLE
                adapter.submit(emptyList()) // Make sure to set an empty list for the adapter
            } else {
                tvNoTasks.visibility = View.GONE
                adapter.submit(rows) // Submit the rows with tasks
            }
        }
    }
}
