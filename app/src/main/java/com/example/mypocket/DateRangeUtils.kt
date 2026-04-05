package com.example.mypocket.ui

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateRangeUtils {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun startOfMonthMillis(yearMonth: YearMonth): Long {
        return yearMonth.atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfMonthMillis(yearMonth: YearMonth): Long {
        return yearMonth.plusMonths(1).atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun startOfYearMillis(year: Int): Long {
        return LocalDate.of(year, 1, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfYearMillis(year: Int): Long {
        return LocalDate.of(year + 1, 1, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun startOfWeekMillis(date: LocalDate): Long {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun endOfWeekMillis(date: LocalDate): Long {
        val nextMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1)
        return nextMonday.atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun weekLabel(date: LocalDate): String {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
        return "${monday.format(formatter)} - ${sunday.format(formatter)}"
    }
}