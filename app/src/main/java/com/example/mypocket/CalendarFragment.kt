package com.example.mypocket.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.data.TaskDao
import com.example.mypocket.adapter.TaskAdapter
import com.example.mypocket.model.TransactionType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.DaySize
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class CalendarFragment : Fragment() {

    // Calendar views
    private lateinit var calendarView: CalendarView
    private lateinit var tvMonthYear: TextView
    private lateinit var btnPrevMonth: View
    private lateinit var btnNextMonth: View

    // Track today's date and currently selected date
    private val today: LocalDate = LocalDate.now()
    private var selectedDate: LocalDate? = null
    private var selectedEpochDay: Long = LocalDate.now().toEpochDay()

    // Floating Action Button menu
    private var isFabOpen = false
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabTask: FloatingActionButton
    private lateinit var fabEvent: FloatingActionButton
    private lateinit var fabExpense: FloatingActionButton

    // Task section
    private lateinit var tvTasksHeader: TextView
    private lateinit var rvTasks: RecyclerView
    private lateinit var taskAdapter: TaskAdapter

    // Room DAOs
    private lateinit var taskDao: TaskDao
    private lateinit var eventDao: com.example.mypocket.data.EventDao

    // Events grouped by date for quick calendar lookup
    private var eventsByDate: Map<LocalDate, List<com.example.mypocket.entity.EventEntity>> = emptyMap()

    // Calendar month range
    private val minMonth = YearMonth.of(2000, 1)
    private val maxMonth = YearMonth.of(2099, 12)

    // Formatter for the month title
    private val monthTitleFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    // Launcher for AddTaskActivity
    private val addTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadIncompleteTasks()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        // Calendar UI
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        calendarView = view.findViewById(R.id.calendarView)
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth)
        btnNextMonth = view.findViewById(R.id.btnNextMonth)

        // Task UI
        tvTasksHeader = view.findViewById(R.id.tvTasksHeader)
        rvTasks = view.findViewById(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(requireContext())

        // FABs
        fabMain = view.findViewById(R.id.fabMain)
        fabTask = view.findViewById(R.id.fabTask)
        fabEvent = view.findViewById(R.id.fabEvent)
        fabExpense = view.findViewById(R.id.fabExpense)

        fabMain.setOnClickListener {
            toggleFabMenu()
        }

        fabTask.setOnClickListener {
            toggleFabMenu()
            val intent = Intent(requireContext(), AddTaskActivity::class.java)
            intent.putExtra(AddTaskActivity.EXTRA_EPOCH_DAY, selectedEpochDay)
            addTaskLauncher.launch(intent)
        }

        fabEvent.setOnClickListener {
            toggleFabMenu()
            val intent = Intent(requireContext(), AddEventActivity::class.java)
            startActivity(intent)
        }

        fabExpense.setOnClickListener {
            toggleFabMenu()

            startActivity(
                AddTransactionActivity.newIntent(
                    requireContext(),
                    TransactionType.EXPENSE
                )
            )
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database and DAOs
        val db = AppDatabase.getInstance(requireContext().applicationContext)
        taskDao = db.taskDao()
        eventDao = db.eventDao()

        // Task adapter for incomplete task list
        taskAdapter = TaskAdapter(
            onToggleComplete = { taskId, completed ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        taskDao.setTaskCompleted(taskId, completed)
                    }
                    loadIncompleteTasks()
                }
            }
        )
        rvTasks.adapter = taskAdapter

        // Open TaskListFragment when the button is clicked
        val btnTaskList = view.findViewById<ImageButton>(R.id.btnTaskList)
        btnTaskList.setOnClickListener {
            findNavController().navigate(R.id.taskListFragment)
        }

        setupMonthCalendar(tvMonthYear)
        loadIncompleteTasks()
        observeEventsForCalendar()

    }

    // Load incomplete tasks and update the RecyclerView
    private fun loadIncompleteTasks() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                taskDao.getIncompleteTasks()
            }

            // Check if the list is empty
            if (list.isEmpty()) {
                // Show the "No incomplete tasks" message and change the font size to 18sp
                tvTasksHeader.visibility = View.VISIBLE
                tvTasksHeader.text = "No incomplete tasks at the moment"

                // Change the font size programmatically
                tvTasksHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

                // Hide the RecyclerView
                rvTasks.visibility = View.GONE
            } else {
                // Hide the "No incomplete tasks" message and reset font size to default (22sp)
                tvTasksHeader.visibility = View.GONE

                // Show the RecyclerView
                rvTasks.visibility = View.VISIBLE
                taskAdapter.submitList(list)
            }
        }
    }

    // Show or hide the FAB submenu
    private fun toggleFabMenu() {
        if (isFabOpen) {
            fabTask.visibility = View.GONE
            fabEvent.visibility = View.GONE
            fabExpense.visibility = View.GONE
        } else {
            fabTask.visibility = View.VISIBLE
            fabEvent.visibility = View.VISIBLE
            fabExpense.visibility = View.VISIBLE
        }
        isFabOpen = !isFabOpen
    }

    // Observe all events from Room and group them by LocalDate
    // This allows each calendar day cell to quickly know how many events it has
    private fun observeEventsForCalendar() {
        viewLifecycleOwner.lifecycleScope.launch {
            eventDao.observeAll().collect { list ->

                val map = mutableMapOf<LocalDate, MutableList<com.example.mypocket.entity.EventEntity>>()

                for (event in list) {
                    var currentDate = event.startAt.toLocalDate()
                    val endDate = event.endAt.toLocalDate()

                    // Safety check: if end date is earlier than start date,
                    // treat it as a one-day event
                    if (endDate.isBefore(currentDate)) {
                        map.getOrPut(currentDate) { mutableListOf() }.add(event)
                        continue
                    }

                    // Put the same event into every day it spans
                    while (!currentDate.isAfter(endDate)) {
                        map.getOrPut(currentDate) { mutableListOf() }.add(event)
                        currentDate = currentDate.plusDays(1)
                    }
                }

                // Sort events in each day by start time
                eventsByDate = map.mapValues { (_, value) ->
                    value.sortedBy { it.startAt }
                }

                // Refresh the whole calendar after event data changes
                calendarView.notifyCalendarChanged()
            }
        }
    }

    private fun setupMonthCalendar(tvMonthYear: TextView) {
        val currentMonth = YearMonth.now()
        val daysOfWeek = daysOfWeek()

        // Rectangle makes the day cells fill available height more evenly
        calendarView.daySize = DaySize.Rectangle

        // EndOfGrid keeps every month as a full 6-row grid
        calendarView.outDateStyle = OutDateStyle.EndOfGrid

        calendarView.setup(minMonth, maxMonth, daysOfWeek.first())
        calendarView.scrollToMonth(currentMonth)

        btnPrevMonth.setOnClickListener {
            val prevMonth = calendarView.findFirstVisibleMonth()?.yearMonth?.minusMonths(1)
            if (prevMonth != null) {
                calendarView.smoothScrollToMonth(prevMonth)
            }
        }

        btnNextMonth.setOnClickListener {
            val nextMonth = calendarView.findFirstVisibleMonth()?.yearMonth?.plusMonths(1)
            if (nextMonth != null) {
                calendarView.smoothScrollToMonth(nextMonth)
            }
        }

        tvMonthYear.text = currentMonth.format(monthTitleFormatter)

        // ViewContainer for each calendar day cell
        // Now only contains:
        // 1. tvDay
        // 2. tvEventCount
        class DayViewContainer(view: View) : ViewContainer(view) {
            val root: View = view.findViewById(R.id.dayRoot)
            val tvDay: TextView = view.findViewById(R.id.tvDay)
            val tvEventCount: TextView = view.findViewById(R.id.tvEventCount)

            lateinit var day: CalendarDay

            init {
                view.setOnClickListener {
                    val clickedDay = day

                    // Ignore days outside the current month
                    if (clickedDay.position != DayPosition.MonthDate) return@setOnClickListener

                    // If the same selected date is tapped again, open the event details page
                    if (selectedDate == clickedDay.date) {
                        openDayEvents(clickedDay.date)
                        return@setOnClickListener
                    }

                    // Otherwise just select the date
                    selectedDate = clickedDay.date
                    selectedEpochDay = clickedDay.date.toEpochDay()

                    calendarView.notifyCalendarChanged()
                    loadIncompleteTasks()
                }
            }
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {

            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }

            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data

                val isInMonth = data.position == DayPosition.MonthDate

                // Hide everything for in/out dates that do not belong to the visible month
                if (!isInMonth) {
                    container.tvDay.visibility = View.INVISIBLE
                    container.tvEventCount.visibility = View.GONE
                    container.root.background = null
                    container.tvDay.background = null
                    return
                }

                // Show the day number
                container.tvDay.visibility = View.VISIBLE
                container.tvDay.text = data.date.dayOfMonth.toString()

                // Reset view state first because RecyclerView reuses views
                container.root.background = null
                container.tvDay.background = null
                container.tvDay.setTextColor(requireContext().getColor(android.R.color.black))
                container.tvEventCount.visibility = View.GONE

                // Apply selected / today / normal styling
                when {
                    data.date == selectedDate -> {
                        container.root.setBackgroundResource(R.drawable.bg_selected_day)
                        container.tvDay.setTextColor(requireContext().getColor(android.R.color.black))
                    }

                    data.date == today -> {
                        container.tvDay.setBackgroundResource(R.drawable.bg_today)
                        container.tvDay.setTextColor(requireContext().getColor(android.R.color.white))
                    }

                    else -> {
                        container.root.background = null
                        container.tvDay.background = null
                        container.tvDay.setTextColor(requireContext().getColor(android.R.color.black))
                    }
                }

                // Show only the event count text such as +1, +2, +5
                val eventCount = eventsByDate[data.date].orEmpty().size

                if (eventCount > 0) {
                    container.tvEventCount.visibility = View.VISIBLE
                    container.tvEventCount.text = "+$eventCount"
                } else {
                    container.tvEventCount.visibility = View.GONE
                }
            }
        }

        // Header container for weekday labels
        class MonthHeaderContainer(view: View) : ViewContainer(view) {
            val legendLayout: ViewGroup = view.findViewById(R.id.legendLayout)
        }

        calendarView.monthHeaderBinder =
            object : MonthHeaderFooterBinder<MonthHeaderContainer> {
                override fun create(view: View): MonthHeaderContainer {
                    return MonthHeaderContainer(view)
                }

                override fun bind(
                    container: MonthHeaderContainer,
                    data: com.kizitonwose.calendar.core.CalendarMonth
                ) {
                    val days = daysOfWeek()
                    for (i in 0 until container.legendLayout.childCount) {
                        val tv = container.legendLayout.getChildAt(i) as TextView
                        val dayOfWeek = days[i]
                        tv.text = dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault()
                        )
                    }
                }
            }

        // Update title when the visible month changes
        calendarView.monthScrollListener = { month ->
            tvMonthYear.text = month.yearMonth.format(monthTitleFormatter)
        }

        // Open the month/year picker when tapping the title
        tvMonthYear.setOnClickListener {
            showMonthYearPicker()
        }
    }

    // Open full event list page for a selected date
    private fun openDayEvents(date: LocalDate) {
        val intent = Intent(requireContext(), DayOverviewActivity::class.java)
        intent.putExtra("year", date.year)
        intent.putExtra("month", date.monthValue)
        intent.putExtra("day", date.dayOfMonth)
        startActivity(intent)
    }

    // Convert epoch milliseconds to LocalDate
    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    // Show a dialog that lets the user jump directly to a month and year
    // The layout is fully defined in XML (4x3 grid of months)
    private fun showMonthYearPicker() {

        val context = requireContext()

        // Create dialog using the custom XML layout
        val dialog = android.app.Dialog(context)
        dialog.setContentView(R.layout.dialog_month_year_picker)

        // Find views from the dialog layout
        val tvYear = dialog.findViewById<TextView>(R.id.tvYear)
        val btnPrevYear = dialog.findViewById<ImageButton>(R.id.btnPrevYear)
        val btnNextYear = dialog.findViewById<ImageButton>(R.id.btnNextYear)
        val gridMonths = dialog.findViewById<GridLayout>(R.id.gridMonths)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        // Determine the year currently visible in the calendar
        var selectedYear =
            calendarView.findFirstVisibleMonth()?.yearMonth?.year ?: YearMonth.now().year

        // Display the year in the dialog header
        tvYear.text = selectedYear.toString()

        // Navigate to previous year
        btnPrevYear.setOnClickListener {
            selectedYear--
            tvYear.text = selectedYear.toString()
        }

        // Navigate to next year
        btnNextYear.setOnClickListener {
            selectedYear++
            tvYear.text = selectedYear.toString()
        }

        // Attach click listeners to each month cell in the GridLayout
        // The XML already contains 12 TextViews in order: Jan -> Dec
        for (i in 0 until gridMonths.childCount) {

            val monthCell = gridMonths.getChildAt(i) as TextView

            monthCell.setOnClickListener {

                // Convert the clicked index to a YearMonth
                // Example: index 0 = January, index 11 = December
                val targetMonth = YearMonth.of(selectedYear, i + 1)

                // Scroll calendar to the selected month
                calendarView.scrollToMonth(targetMonth)

                // Close the picker
                dialog.dismiss()
            }
        }

        // Cancel simply closes the dialog
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // Avoid setting fixed width on root LL
        // Adjust the logic programmatically (95% of screen)
        val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}