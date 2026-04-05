package com.example.mypocket.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.data.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import android.app.Dialog
import android.widget.GridLayout
import com.example.mypocket.R
import com.example.mypocket.adapter.WalletAdapter
import com.example.mypocket.adapter.WalletMonthlyAdapter
import com.example.mypocket.entity.TransactionEntity
import com.example.mypocket.model.TransactionType
import com.example.mypocket.model.WalletRow
import com.example.mypocket.utils.toCurrency

class WalletFragment : Fragment() {

    // NOTE: We keep 2 adapters:
    // - WalletAdapter = daily list (SectionHeader + Detail)
    // - WalletMonthlyAdapter = monthly/year list (MonthHeader + WeekSummary)
    private lateinit var dailyAdapter: WalletAdapter
    private lateinit var monthlyAdapter: WalletMonthlyAdapter

    // NOTE: Single calendar used as "current period pointer".
    // - In Daily mode it represents the current MONTH to show.
    // - In Monthly mode it represents the current YEAR to show (month still stored, used to decide how many months to show).
    private val monthCal: Calendar = Calendar.getInstance()

    // true = Daily (group by day in selected month)
    // false = Monthly (show months in selected year, expandable to weeks)
    private var isDailyMode = true

    // NOTE: For Monthly mode expand/collapse.
    // Store the expanded month key ("YYYY-MM"), only one expanded at a time.
    private var expandedMonthKey: String? = null

    // NOTE: Trigger to refresh flows when:
    // - prev/next clicked
    // - toggle changed
    // - expand/collapse changed
    private val periodSignal = MutableStateFlow(0)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Views ---
        val btnPrev: ImageButton = view.findViewById(R.id.btnPrevMonth)
        val btnNext: ImageButton = view.findViewById(R.id.btnNextMonth)
        val tvMonthYear: TextView = view.findViewById(R.id.tvMonthYear)
        val btnStatistics: ImageButton = view.findViewById(R.id.btnStatistics)
        val btnBalances: ImageButton = view.findViewById(R.id.btnBalances)

        val toggle: MaterialButtonToggleGroup = view.findViewById(R.id.togglePeriod)
        val btnDaily: MaterialButton = view.findViewById(R.id.btnDaily)
        val btnMonthly: MaterialButton = view.findViewById(R.id.btnMonthly)

        val tvIncome: TextView = view.findViewById(R.id.tvIncome)
        val tvExpense: TextView = view.findViewById(R.id.tvExpense)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)

        val rv: RecyclerView = view.findViewById(R.id.rvWallet)
        rv.layoutManager = LinearLayoutManager(requireContext())

        // Adapters - Activity-based
        dailyAdapter = WalletAdapter { detail ->
            startActivity(
                TransactionDetailActivity.newIntent(requireContext(), detail.id)
            )
        }

        monthlyAdapter = WalletMonthlyAdapter { monthKey ->
            // NOTE: Expand/collapse logic:
            // tap same month -> collapse; tap other month -> expand that one
            expandedMonthKey = if (expandedMonthKey == monthKey) null else monthKey
            periodSignal.value += 1
        }

        // Default: daily adapter
        rv.adapter = dailyAdapter

        // FAB to add transaction (FOR ACTIVITY)
        val fabAdd: FloatingActionButton = view.findViewById(R.id.fabAdd)
        fabAdd.setOnClickListener {
            val intent = AddTransactionActivity.newIntent(
                requireContext(),
                TransactionType.EXPENSE
            )
            startActivity(intent)
        }

        // Avoid screen don't change when click bottom nav (FOR FRAGMENT)
        btnStatistics.setOnClickListener {
            findNavController().navigate(R.id.statisticsFragment)
        }

        btnBalances.setOnClickListener {
            findNavController().navigate(R.id.accountBalancesFragment)
        }

        // --- Helpers (UI label + query range) ---
        fun updatePeriodLabel() {
            tvMonthYear.text = if (isDailyMode) {
                // Daily mode shows month-year
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
            } else {
                // Monthly mode shows year only
                SimpleDateFormat("yyyy", Locale.getDefault()).format(monthCal.time)
            }
        }

        fun rangeMillis(): Pair<Long, Long> {
            return if (isDailyMode) {
                // NOTE: Query only current month
                val start = (monthCal.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                start.timeInMillis to end.timeInMillis
            } else {
                // NOTE: Query entire year
                val start = (monthCal.clone() as Calendar).apply {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val end = (start.clone() as Calendar).apply { add(Calendar.YEAR, 1) }
                start.timeInMillis to end.timeInMillis
            }
        }

        // --- Default toggle state ---
        toggle.check(btnDaily.id)
        isDailyMode = true
        updatePeriodLabel()

        // Open month/year picker when tapping the period label
        tvMonthYear.setOnClickListener {
            showMonthYearPicker()
        }

        // --- Prev / Next buttons ---
        btnPrev.setOnClickListener {
            // NOTE: In daily mode, prev/next moves month. In monthly mode, moves year.
            if (isDailyMode) monthCal.add(Calendar.MONTH, -1) else monthCal.add(Calendar.YEAR, -1)

            // When period changes, collapse any expanded month
            expandedMonthKey = null

            updatePeriodLabel()
            periodSignal.value += 1
        }

        btnNext.setOnClickListener {
            if (isDailyMode) monthCal.add(Calendar.MONTH, 1) else monthCal.add(Calendar.YEAR, 1)

            expandedMonthKey = null

            updatePeriodLabel()
            periodSignal.value += 1
        }

        // --- Toggle Daily / Monthly ---
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            isDailyMode = checkedId == btnDaily.id

            // Switch adapter depending on mode
            rv.adapter = if (isDailyMode) dailyAdapter else monthlyAdapter

            // Reset expand when switching modes
            expandedMonthKey = null

            updatePeriodLabel()
            periodSignal.value += 1
        }

        // --- Observe data ---
        val dao = AppDatabase.getInstance(requireContext()).transactionDao()

        // NOTE: Build one combined flow that changes when periodSignal changes.
        val rangeFlow = periodSignal.flatMapLatest {
            val (start, end) = rangeMillis()
            val txFlow = dao.observeInRange(start, end)
            val totalsFlow = dao.observeTotalsInRange(start, end)
            combine(txFlow, totalsFlow) { txs, totals -> txs to totals }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    rangeFlow.collect { (txs, totals) ->

                        // --- Summary ---
                        val income = totals.incomeTotal
                        val expense = totals.expenseTotal
                        val total = income - expense

                        tvIncome.text = income.toCurrency()
                        tvExpense.text = expense.toCurrency()
                        tvTotal.text = total.toCurrency(withSign = true)

                        val db = AppDatabase.getInstance(requireContext())

                        // --- List rows ---
                        if (isDailyMode) {
                            val dailyRows = buildDailyRows(txs, db) // suspend
                            withContext(Dispatchers.Main) { dailyAdapter.submit(dailyRows) }
                        } else {
                            monthlyAdapter.submit(buildYearMonthlyRows(txs, monthCal))
                        }
                    }
                }
            }
        }

        // NOTE: Trigger initial load
        periodSignal.value += 1
    }

    // ---------------------------
    // DAILY MODE: group by day
    // ---------------------------
    private suspend fun buildDailyRows(txs: List<TransactionEntity>, db: AppDatabase): List<WalletRow> {
        val rows = mutableListOf<WalletRow>()

        // Group by day key (yyyyMMdd), sort latest day first
        val groups = txs.groupBy { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            c.get(Calendar.YEAR) * 10000 +
                    (c.get(Calendar.MONTH) + 1) * 100 +
                    c.get(Calendar.DAY_OF_MONTH)
        }.toSortedMap(compareByDescending { it })

        for ((_, list) in groups) {
            val dayCal = Calendar.getInstance().apply { timeInMillis = list.first().timestamp }
            val label = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(dayCal.time)

            val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            // Header first
            rows += WalletRow.SectionHeader(label, income, expense)
            // Then details
            for (tx in list) {
                rows += tx.toWalletDetail(db) // suspend call is now OK
            }
        }

        return rows
    }

    // ------------------------------------------
    // MONTHLY MODE: year view (months + weeks)
    // ------------------------------------------
    private fun buildYearMonthlyRows(
        txsInYear: List<TransactionEntity>,
        yearCal: Calendar
    ): List<WalletRow> {

        val year = yearCal.get(Calendar.YEAR)
        val rows = mutableListOf<WalletRow>()

        // NOTE: You wanted: "when I'm in March, show March, Feb, Jan"
        // so we show months from current month down to January.
        val currentMonth = yearCal.get(Calendar.MONTH)

        for (m in currentMonth downTo Calendar.JANUARY) {

            val monthKey = "%04d-%02d".format(year, m + 1)

            // Transactions in this month
            val monthTx = txsInYear.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == m
            }

            // NOTE: Even if monthTx is empty, income/expense stays 0.00 -> month still shown
            val income = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            // Month label like "Feb"
            val label = SimpleDateFormat("MMM", Locale.getDefault()).format(
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, 1)
                }.time
            )

            val expanded = (expandedMonthKey == monthKey)

            // Month header row
            rows += WalletRow.MonthHeader(
                key = monthKey,
                label = label,
                totalIncome = income,
                totalExpense = expense,
                expanded = expanded
            )

            // Expanded children: weekly blocks (01-07, 08-14, etc)
            if (expanded) {
                rows += buildWeekRowsForMonth(monthTx, year, m)
            }
        }

        return rows
    }

    // Week rows inside a month (7-day blocks starting from day 1)
    private fun buildWeekRowsForMonth(
        monthTx: List<TransactionEntity>,
        year: Int,
        month: Int // 0..11
    ): List<WalletRow> {

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val starts = listOf(1, 8, 15, 22, 29).filter { it <= daysInMonth }
        val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())

        val rows = mutableListOf<WalletRow>()

        // NOTE: Newest week first; remove reversed() if you want 01-07 at top
        for (s in starts.reversed()) {
            val e = min(s + 6, daysInMonth)

            val start = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, s)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val end = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, e)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val inBlock = monthTx.filter { it.timestamp in start.timeInMillis..end.timeInMillis }

            val income = inBlock.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = inBlock.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

            rows += WalletRow.WeekSummary(
                label = "${sdf.format(start.time)} - ${sdf.format(end.time)}",
                totalIncome = income,
                totalExpense = expense
            )
        }

        return rows
    }

    // Convert TransactionEntity -> WalletRow.Detail (used only in daily mode list)
    suspend fun TransactionEntity.toWalletDetail(db: AppDatabase): WalletRow.Detail {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)

        // Resolve category name
        val categoryText = when (type) {
            TransactionType.INCOME, TransactionType.EXPENSE ->
                categoryId?.let { db.categoryDao().getCategoryByIdNullable(it)?.name } ?: type.name
            TransactionType.TRANSFER -> "Transfer"
        }

        // Title prefers note, then category
        val titleText = note?.takeIf { it.isNotBlank() } ?: categoryText

        // Resolve account names
        val accountText = when (type) {
            TransactionType.TRANSFER -> {
                val from = fromAccountId?.let { db.accountDao().getAccountById(it)?.name } ?: "?"
                val to = toAccountId?.let { db.accountDao().getAccountById(it)?.name } ?: "?"
                "$from → $to"
            }
            else -> accountId?.let { db.accountDao().getAccountById(it)?.name } ?: "-"
        }

        return WalletRow.Detail(
            id = transactionId,
            type = type,
            category = categoryText,
            title = titleText,
            time = timeStr,
            account = accountText,
            amount = amount
        )
    }

    // Show a dialog that lets the user jump directly to a month and year
// Same UI as CalendarFragment but updates monthCal instead of scrolling a calendar
    private fun showMonthYearPicker() {

        val context = requireContext()

        // Create dialog using the custom XML layout
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_month_year_picker)

        // Find views from the dialog layout
        val tvYear = dialog.findViewById<TextView>(R.id.tvYear)
        val btnPrevYear = dialog.findViewById<ImageButton>(R.id.btnPrevYear)
        val btnNextYear = dialog.findViewById<ImageButton>(R.id.btnNextYear)
        val gridMonths = dialog.findViewById<GridLayout>(R.id.gridMonths)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        // Initialize picker year from current calendar pointer
        var selectedYear = monthCal.get(Calendar.YEAR)

        // Display the year
        tvYear.text = selectedYear.toString()

        // Year navigation
        btnPrevYear.setOnClickListener {
            selectedYear--
            tvYear.text = selectedYear.toString()
        }

        btnNextYear.setOnClickListener {
            selectedYear++
            tvYear.text = selectedYear.toString()
        }

        // Attach click listeners to each month cell
        // XML already defines Jan -> Dec order
        for (i in 0 until gridMonths.childCount) {

            val monthCell = gridMonths.getChildAt(i) as TextView

            monthCell.setOnClickListener {

                // Update internal calendar pointer
                monthCal.set(Calendar.YEAR, selectedYear)
                monthCal.set(Calendar.MONTH, i)

                // Collapse any expanded month (used in monthly mode)
                expandedMonthKey = null

                // Refresh label and reload data
                val label = if (isDailyMode) {
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
                } else {
                    SimpleDateFormat("yyyy", Locale.getDefault()).format(monthCal.time)
                }

                view?.findViewById<TextView>(R.id.tvMonthYear)?.text = label

                periodSignal.value += 1

                dialog.dismiss()
            }
        }

        // Cancel button
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        // Avoid fixed width; make dialog ~95% of screen width
        val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}