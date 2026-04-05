package com.example.mypocket.model

// UI Layer
sealed class WalletRow {
    data class SectionHeader(
        val label: String,          // "20 Feb 2026" or "Feb 2026" for monthly mode
        val totalIncome: Double,
        val totalExpense: Double
    ) : WalletRow()

    data class Detail(
        val id: Long,
        val type: TransactionType,
        val category: String,
        val title: String,
        val time: String,
        val account: String,
        val amount: Double      // Always positive value
    ) : WalletRow()

    // Monthly expandable header (month row)
    data class MonthHeader(
        val key: String,              // "2026-02"
        val label: String,            // "Feb"
        val totalIncome: Double,
        val totalExpense: Double,
        val expanded: Boolean
    ) : WalletRow()

    // Weekly summary row (child rows when month expands)
    data class WeekSummary(
        val label: String,            // "01.02 - 07.02"
        val totalIncome: Double,
        val totalExpense: Double
    ) : WalletRow() {
        val total: Double get() = totalIncome - totalExpense
    }
}