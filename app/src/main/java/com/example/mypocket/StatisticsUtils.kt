package com.example.mypocket.utils

import kotlin.math.floor

fun calculatePercentages(amounts: List<Double>): List<Int> {
    val total = amounts.sum()
    if (total <= 0.0) return List(amounts.size) { 0 }

    data class Temp(
        val index: Int,
        val floorValue: Int,
        val remainder: Double
    )

    val tempList = amounts.mapIndexed { index, amount ->
        val raw = (amount / total) * 100.0
        val floored = floor(raw).toInt()
        Temp(index, floored, raw - floored)
    }

    val result = tempList.map { it.floorValue }.toMutableList()
    val remaining = 100 - result.sum()

    tempList
        .sortedByDescending { it.remainder }
        .take(remaining)
        .forEach { result[it.index]++ }

    return result
}