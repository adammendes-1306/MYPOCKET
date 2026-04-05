package com.example.mypocket

import com.example.mypocket.entity.TransactionEntity
import com.example.mypocket.model.TransactionType

// Find on WalletMonthlyRow.kt
import com.example.mypocket.model.PeriodSummary
import com.example.mypocket.model.MonthGroup

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min

object WalletMonthlyGrouper {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val monthLabelFmt = DateTimeFormatter.ofPattern("MMM")      // "Feb"
    private val rangeFmt = DateTimeFormatter.ofPattern("dd.MM")        // "08.02"

    fun buildMonthGroups(
        all: List<TransactionEntity>,
        expandedState: Map<String, Boolean> = emptyMap()
    ): List<MonthGroup> {
        if (all.isEmpty()) return emptyList()

        // Group tx by YearMonth (based on timestamp)
        val byMonth: Map<YearMonth, List<TransactionEntity>> =
            all.groupBy { ymOf(it.timestamp) }

        // Sort latest month first
        val monthsSorted = byMonth.keys.sortedDescending()

        return monthsSorted.map { ym ->
            val monthKey = "${ym.year}-${ym.monthValue.toString().padStart(2, '0')}"
            val monthTx = byMonth[ym].orEmpty()

            val monthIncome = sumIncome(monthTx)
            val monthExpense = sumExpense(monthTx)

            val header = PeriodSummary(
                label = ym.format(monthLabelFmt), // "Feb"
                income = monthIncome,
                expense = monthExpense
            )

            val weeks = buildWeekBlocks(ym).map { (startDay, endDay) ->
                val start = ym.atDay(startDay)
                val end = ym.atDay(endDay)

                val txInBlock = monthTx.filter { tx ->
                    val d = dateOf(tx.timestamp)
                    !d.isBefore(start) && !d.isAfter(end)
                }

                PeriodSummary(
                    label = "${start.format(rangeFmt)} - ${end.format(rangeFmt)}",
                    income = sumIncome(txInBlock),
                    expense = sumExpense(txInBlock)
                )
            }.reversed() // if you want newest week first; remove if you want 01-07 at top

            MonthGroup(
                key = monthKey,
                header = header,
                weeks = weeks,
                expanded = expandedState[monthKey] ?: false
            )
        }
    }

    private fun buildWeekBlocks(ym: YearMonth): List<Pair<Int, Int>> {
        val days = ym.lengthOfMonth()
        val starts = listOf(1, 8, 15, 22, 29)
        return starts
            .filter { it <= days }
            .map { s -> s to min(s + 6, days) }
    }

    private fun ymOf(ts: Long): YearMonth = YearMonth.from(dateOf(ts))
    private fun dateOf(ts: Long): LocalDate =
        Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()

    private fun sumIncome(list: List<TransactionEntity>): Double =
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    private fun sumExpense(list: List<TransactionEntity>): Double =
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
}