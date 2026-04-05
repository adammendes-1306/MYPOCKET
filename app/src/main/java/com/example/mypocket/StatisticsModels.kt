// UI-specific
package com.example.mypocket.ui

data class StatisticCategoryItem(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Int
)

enum class StatisticsPeriodType {
    WEEKLY,
    MONTHLY,
    ANNUALLY
}

enum class StatisticsCategoryMode {
    INCOME,
    EXPENSE
}

data class StatisticsUiState(
    val periodLabel: String = "",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categories: List<StatisticCategoryItem> = emptyList(),
    val periodType: StatisticsPeriodType = StatisticsPeriodType.MONTHLY,
    val categoryMode: StatisticsCategoryMode = StatisticsCategoryMode.EXPENSE
)