package com.example.mypocket.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mypocket.R
import com.example.mypocket.model.TransactionType
import com.example.mypocket.adapter.WalletAdapter
import com.example.mypocket.model.WalletRow
import com.example.mypocket.adapter.EventAdapter
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.utils.toCurrency
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class DayOverviewActivity : AppCompatActivity() {

    private lateinit var rvEvents: RecyclerView
    private lateinit var tvEmptyEvents: TextView
    private lateinit var adapterEvents: EventAdapter

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvEmptyTransactions: TextView
    private lateinit var adapterTransactions: WalletAdapter

    private lateinit var tvDayTitle: TextView
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvTotal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_day_overview)

        // Check if the Android version is 11 (API level 30) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Get the WindowInsetsController and ensure the status bar is visible
            val insetsController = window.insetsController
            insetsController?.show(WindowInsets.Type.statusBars())
        }

        // --- Initialize views ---
        tvDayTitle = findViewById(R.id.tvDayTitle)

        // Events
        rvEvents = findViewById(R.id.rvEvents)
        tvEmptyEvents = findViewById(R.id.tvEmptyEvents)
        rvEvents.layoutManager = LinearLayoutManager(this)
        adapterEvents = EventAdapter { event ->
            val i = Intent(this, EventDetailActivity::class.java)
            i.putExtra(EventDetailActivity.EXTRA_EVENT_ID, event.eventId)
            startActivity(i)
        }
        rvEvents.adapter = adapterEvents

        // Transactions
        rvTransactions = findViewById(R.id.rvTransactions)
        tvEmptyTransactions = findViewById(R.id.tvEmptyTransactions)
        rvTransactions.layoutManager = LinearLayoutManager(this)
        adapterTransactions = WalletAdapter { detail ->
            // Handle transaction detail click if needed
        }
        rvTransactions.adapter = adapterTransactions

        // Summary TextViews
        tvIncome = findViewById(R.id.tvIncome)
        tvExpense = findViewById(R.id.tvExpense)

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // --- Get selected date ---
        val year = intent.getIntExtra("year", 0)
        val month = intent.getIntExtra("month", 1)
        val day = intent.getIntExtra("day", 1)
        val date = LocalDate.of(year, month, day)
        tvDayTitle.text = date.toString()

        // --- Database access ---
        val db = AppDatabase.getInstance(applicationContext)
        val eventDao = db.eventDao()
        val transactionDao = db.transactionDao()

        lifecycleScope.launch {
            // --- Observe Events ---
            eventDao.observeAll().collect { allEvents ->
                val (dayStart, dayEnd) = dayBoundsMillis(date)
                val filteredEvents = allEvents.filter { e ->
                    e.startAt < dayEnd && e.endAt >= dayStart
                }

                adapterEvents.submit(filteredEvents)

                tvEmptyEvents.visibility = if (filteredEvents.isEmpty()) View.VISIBLE else View.GONE
                rvEvents.visibility = if (filteredEvents.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            // --- Observe Transactions ---
            transactionDao.observeAll().collect { allTransactions ->
                val (dayStart, dayEnd) = dayBoundsMillis(date)
                val filteredTransactions = allTransactions.filter { t ->
                    t.timestamp in dayStart until dayEnd
                }

                // Prepare WalletRow list for adapter
                val walletRows = filteredTransactions.map { t ->
                    // Resolve category name
                    val categoryName = t.categoryId?.let { db.categoryDao().getCategoryByIdNullable(it)?.name }
                        ?: t.type.name.lowercase().replaceFirstChar { it.uppercase() } // Capitalize only first letter

                    // Resolve account string
                    val accountName = if (t.type == TransactionType.TRANSFER) {
                        val from = t.fromAccountId?.let { db.accountDao().getAccountById(it)?.name } ?: ""
                        val to = t.toAccountId?.let { db.accountDao().getAccountById(it)?.name } ?: ""
                        "$from -> $to"
                    } else {
                        t.accountId?.let { db.accountDao().getAccountById(it)?.name } ?: ""
                    }

                    WalletRow.Detail(
                        id = t.transactionId,
                        category = categoryName,
                        title = t.note ?: t.description ?: "Transaction",
                        time = formatTime(t.timestamp),
                        account = accountName,
                        amount = t.amount,
                        type = t.type
                    )
                }
                adapterTransactions.submit(walletRows)

                // Update empty state
                tvEmptyTransactions.visibility = if (filteredTransactions.isEmpty()) View.VISIBLE else View.GONE
                rvTransactions.visibility = if (filteredTransactions.isEmpty()) View.GONE else View.VISIBLE

                // --- Update summary ---
                val income = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val expense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

                tvIncome.text = getString(R.string.label_income, income.toCurrency())
                tvExpense.text = getString(R.string.label_expense, expense.toCurrency())
            }
        }
    }

    private fun dayBoundsMillis(date: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    private fun formatTime(timestamp: Long): String {
        val localTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return localTime.toString().substring(0, 5) // HH:mm
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}