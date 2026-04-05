package com.example.mypocket.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mypocket.R
import com.example.mypocket.databinding.FragmentStatisticsBinding
import android.graphics.Color
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.mypocket.data.AppDatabase
import com.example.mypocket.utils.toCurrency
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var statisticsAdapter: StatisticsAdapter

    private val viewModel: StatisticsViewModel by viewModels {
        StatisticsViewModelFactory(
            StatisticsRepository(
                AppDatabase.getInstance(requireActivity().applicationContext).transactionDao()
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)

        binding.layoutIncome.setOnClickListener {
            viewModel.onCategoryModeChanged(StatisticsCategoryMode.INCOME)
        }

        binding.layoutExpense.setOnClickListener {
            viewModel.onCategoryModeChanged(StatisticsCategoryMode.EXPENSE)
        }

        setupToolbar()
        setupDropdown()
        setupRecyclerView()
        setupPeriodLabelClick()
        setupClicks()
        observeUi()
    }

    private fun setupToolbar() {
        binding.toolbarStatistics.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupDropdown() {
        val items = listOf("Weekly", "Monthly", "Annually")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, items)
        binding.dropdownPeriod.setAdapter(adapter)
        binding.dropdownPeriod.setText("Monthly", false)

        binding.dropdownPeriod.setOnClickListener {
            binding.dropdownPeriod.showDropDown()
        }

        binding.dropdownPeriod.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> viewModel.onPeriodTypeChanged(StatisticsPeriodType.WEEKLY)
                1 -> viewModel.onPeriodTypeChanged(StatisticsPeriodType.MONTHLY)
                2 -> viewModel.onPeriodTypeChanged(StatisticsPeriodType.ANNUALLY)
            }
        }
    }

    private fun setupRecyclerView() {
        statisticsAdapter = StatisticsAdapter()
        binding.rvStatistics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = statisticsAdapter
        }
    }

    private fun setupPeriodLabelClick() {
        binding.tvMonthYear.setOnClickListener {
            when (viewModel.uiState.value.periodType) {
                StatisticsPeriodType.MONTHLY -> showMonthYearPicker()
                StatisticsPeriodType.WEEKLY -> showWeekDatePicker()
                StatisticsPeriodType.ANNUALLY -> { }
            }
        }
    }

    private fun setupClicks() {
        binding.btnPrevious.setOnClickListener {
            viewModel.goToPrevious()
        }

        binding.btnNext.setOnClickListener {
            viewModel.goToNext()
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvMonthYear.text = state.periodLabel
                    binding.tvTotalIncome.text = state.totalIncome.toCurrency()
                    binding.tvTotalExpense.text = state.totalExpense.toCurrency()

                    // Visual feedback so user knows which is selected
                    val isIncome = state.categoryMode == StatisticsCategoryMode.INCOME
                    binding.layoutIncome.alpha = if (isIncome) 1.0f else 0.5f
                    binding.layoutExpense.alpha = if (isIncome) 0.5f else 1.0f

                    statisticsAdapter.submitList(state.categories)

                    val hasChartData = state.categories.isNotEmpty()
                    binding.pieChart.visibility = if (hasChartData) View.VISIBLE else View.GONE
                    binding.tvEmptyChart.visibility = if (hasChartData) View.GONE else View.VISIBLE

                    binding.dropdownPeriod.setText(
                        when (state.periodType) {
                            StatisticsPeriodType.WEEKLY -> "Weekly"
                            StatisticsPeriodType.MONTHLY -> "Monthly"
                            StatisticsPeriodType.ANNUALLY -> "Annually"
                        },
                        false
                    )

                    if (hasChartData) {
                        renderPieChart(state.categories, state.categoryMode)
                    } else {
                        binding.pieChart.clear()
                    }
                }
            }
        }
    }

    private fun renderPieChart(
        items: List<StatisticCategoryItem>,
        mode: StatisticsCategoryMode
    ) {
        if (items.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        val entries = items.map { item ->
            PieEntry(item.amount.toFloat(), item.categoryName)
        }

        val colorTemplate = listOf(
            Color.parseColor("#FF6384"),
            Color.parseColor("#36A2EB"),
            Color.parseColor("#FFCE56"),
            Color.parseColor("#4BC0C0"),
            Color.parseColor("#9966FF"),
            Color.parseColor("#FF9F40"),
            Color.parseColor("#8BC34A"),
            Color.parseColor("#E91E63")
        )

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 2f
            valueTextSize = 12f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            colors = items.indices.map { index ->
                colorTemplate[index % colorTemplate.size]
            }
            valueLinePart1Length = 0.4f
            valueLinePart2Length = 0.3f
            valueLineColor = Color.GRAY
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    val index = dataSet.getEntryIndex(pieEntry)
                    if (index < 0 || index >= items.size) return ""

                    val category = items[index].categoryName
                    val percentage = items[index].percentage
                    return "$category $percentage%"
                }
            })
            setValueTextColor(Color.BLACK)
            setValueTextSize(12f)
        }

        binding.pieChart.apply {
            this.data = data
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 55f
            transparentCircleRadius = 60f
            setUsePercentValues(false)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            centerText = if (mode == StatisticsCategoryMode.INCOME) "Income" else "Expenses"
            setCenterTextSize(16f)
            legend.isEnabled = false
            setDrawSlicesUnderHole(false)
            setHoleColor(Color.TRANSPARENT)
            setDrawEntryLabels(false)
            animateY(700)
            invalidate()
        }

        binding.pieChart.legend.apply {
            isEnabled = true

            // Control text style
            textSize = 12f
            textColor = Color.BLACK

            // Control Alignment
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            setDrawInside(false)

            // Control Spacing
            xEntrySpace = 24f   // space between legend items (horizontal)
            yEntrySpace = 8f    // vertical spacing (if wrapped)
            formToTextSpace = 6f // space between color box and text
        }
    }

    private fun showWeekDatePicker() {
        val currentDate = viewModel.getSelectedDate()

        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                viewModel.setSelectedDate(selectedDate)
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )

        dialog.show()
    }

    private fun showMonthYearPicker() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_month_year_picker)

        val tvYear = dialog.findViewById<TextView>(R.id.tvYear)
        val btnPrevYear = dialog.findViewById<ImageButton>(R.id.btnPrevYear)
        val btnNextYear = dialog.findViewById<ImageButton>(R.id.btnNextYear)
        val gridMonths = dialog.findViewById<GridLayout>(R.id.gridMonths)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancel)

        val currentDate = viewModel.getSelectedDate()
        var selectedYear = currentDate.year

        tvYear.text = selectedYear.toString()

        btnPrevYear.setOnClickListener {
            selectedYear--
            tvYear.text = selectedYear.toString()
        }

        btnNextYear.setOnClickListener {
            selectedYear++
            tvYear.text = selectedYear.toString()
        }

        for (i in 0 until gridMonths.childCount) {
            val monthCell = gridMonths.getChildAt(i) as TextView
            monthCell.setOnClickListener {
                viewModel.setSelectedMonth(selectedYear, i + 1)
                dialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        val width = (resources.displayMetrics.widthPixels * 0.95).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}