package com.opankcs.costa.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.opankcs.costa.R

@Composable
fun budgetProgressColor(percent: Double?): Color {
    val value = percent ?: 0.0

    return when {
        value >= 100.0 -> MaterialTheme.colorScheme.error
        value >= 70.0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
fun budgetStatusLabel(percent: Double?): String = when {
    percent == null -> ""
    percent >= 100.0 -> stringResource(R.string.status_over_budget)
    percent >= 70.0 -> stringResource(R.string.status_almost)
    else -> stringResource(R.string.status_safe)
}
