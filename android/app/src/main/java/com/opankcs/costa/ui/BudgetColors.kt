package com.opankcs.costa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun budgetProgressColor(percent: Double?): Color {
    val value = percent ?: 0.0

    return when {
        value >= 100.0 -> MaterialTheme.colorScheme.error
        value >= 70.0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
}

fun budgetStatusLabel(percent: Double?): String = when {
    percent == null -> ""
    percent >= 100.0 -> "Lewat budget"
    percent >= 70.0 -> "Hampir habis"
    else -> "Aman"
}
