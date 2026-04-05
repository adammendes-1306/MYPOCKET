package com.example.mypocket.model

data class PeriodSummary(
    val label: String,      // "Feb" or "01.02 - 07.02"
    val income: Double,
    val expense: Double
) {
    val total: Double get() = income - expense
}

data class MonthGroup(
    val key: String,        // "2026-02"
    val header: PeriodSummary,
    val weeks: List<PeriodSummary>,
    var expanded: Boolean = false
)