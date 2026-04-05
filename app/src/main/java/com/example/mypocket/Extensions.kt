package com.example.mypocket.utils

fun Double.toCurrency(withSign: Boolean = false): String {
    // UI display
    val formatted = "RM %.2f".format(kotlin.math.abs(this))

    // Total/Summaries (need negative for loss)
    // if withSign true, -RM 50.00 else RM 50.00
    return if (withSign && this < 0) "-$formatted" else formatted
}