package com.example.mypocket.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class StatisticsViewModel(
    private val repository: StatisticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState

    private var selectedPeriodType = StatisticsPeriodType.MONTHLY
    private var selectedCategoryMode = StatisticsCategoryMode.EXPENSE
    private var selectedDate = LocalDate.now()

    init {
        loadStatistics()
    }

    fun onCategoryModeChanged(mode: StatisticsCategoryMode) {
        selectedCategoryMode = mode
        loadStatistics()
    }

    fun onPeriodTypeChanged(type: StatisticsPeriodType) {
        selectedPeriodType = type
        loadStatistics()
    }

    fun goToPrevious() {
        selectedDate = when (selectedPeriodType) {
            StatisticsPeriodType.WEEKLY -> selectedDate.minusWeeks(1)
            StatisticsPeriodType.MONTHLY -> selectedDate.minusMonths(1)
            StatisticsPeriodType.ANNUALLY -> selectedDate.minusYears(1)
        }
        loadStatistics()
    }

    fun goToNext() {
        selectedDate = when (selectedPeriodType) {
            StatisticsPeriodType.WEEKLY -> selectedDate.plusWeeks(1)
            StatisticsPeriodType.MONTHLY -> selectedDate.plusMonths(1)
            StatisticsPeriodType.ANNUALLY -> selectedDate.plusYears(1)
        }
        loadStatistics()
    }

    fun getSelectedDate(): LocalDate = selectedDate

    fun setSelectedDate(date: LocalDate) {
        selectedDate = date
        loadStatistics()
    }


    fun setSelectedMonth(year: Int, month: Int) {
        selectedDate = LocalDate.of(year, month, 1)
        loadStatistics()
    }

    fun setSelectedYear(year: Int) {
        selectedDate = LocalDate.of(year, 1, 1)
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val (start, end, label) = getDateRange()

            _uiState.value = repository.getStatistics(
                startTimestamp = start,
                endTimestamp = end,
                periodLabel = label,
                periodType = selectedPeriodType,
                categoryMode = selectedCategoryMode
            )
        }
    }

    private fun getDateRange(): Triple<Long, Long, String> {
        return when (selectedPeriodType) {
            StatisticsPeriodType.MONTHLY -> {
                val ym = YearMonth.of(selectedDate.year, selectedDate.monthValue)
                val start = DateRangeUtils.startOfMonthMillis(ym)
                val end = DateRangeUtils.endOfMonthMillis(ym)
                val label = "${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${selectedDate.year}"
                Triple(start, end, label)
            }

            StatisticsPeriodType.WEEKLY -> {
                val start = DateRangeUtils.startOfWeekMillis(selectedDate)
                val end = DateRangeUtils.endOfWeekMillis(selectedDate)
                val label = DateRangeUtils.weekLabel(selectedDate)
                Triple(start, end, label)
            }

            StatisticsPeriodType.ANNUALLY -> {
                val start = DateRangeUtils.startOfYearMillis(selectedDate.year)
                val end = DateRangeUtils.endOfYearMillis(selectedDate.year)
                val label = selectedDate.year.toString()
                Triple(start, end, label)
            }
        }
    }
}