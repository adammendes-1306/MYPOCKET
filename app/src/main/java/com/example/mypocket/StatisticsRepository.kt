package com.example.mypocket.ui

import com.example.mypocket.data.TransactionDao
import com.example.mypocket.model.TransactionType
import com.example.mypocket.utils.calculatePercentages

class StatisticsRepository(
    private val transactionDao: TransactionDao
) {
    suspend fun getStatistics(
        startTimestamp: Long,
        endTimestamp: Long,
        periodLabel: String,
        periodType: StatisticsPeriodType,
        categoryMode: StatisticsCategoryMode
    ): StatisticsUiState {
        val totalIncome = transactionDao.getTotalByType(
            type = TransactionType.INCOME,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )

        val totalExpense = transactionDao.getTotalByType(
            type = TransactionType.EXPENSE,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )

        val categoryRows = transactionDao.getCategoryStatsByType(
            type = if (categoryMode == StatisticsCategoryMode.INCOME) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            },
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )

        val percentages = calculatePercentages(categoryRows.map { it.totalAmount })

        val items = categoryRows.mapIndexed { index, row ->
            StatisticCategoryItem(
                categoryId = row.categoryId,
                categoryName = row.categoryName,
                amount = row.totalAmount,
                percentage = percentages[index]
            )
        }

        return StatisticsUiState(
            periodLabel = periodLabel,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            categories = items,
            periodType = periodType,
            categoryMode = categoryMode
        )
    }
}