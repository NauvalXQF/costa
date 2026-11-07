package com.opankcs.costa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.opankcs.costa.R
import com.opankcs.costa.data.Expense
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val haptics = rememberCostaHaptics()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50),
            )
            .clickable {
                haptics.tick()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: CostaUiState, viewModel: CostaViewModel) {
    val haptics = rememberCostaHaptics()

    var period by rememberSaveable { mutableStateOf("week") }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    var editing by remember { mutableStateOf<Expense?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }
    val currentMonth = YearMonth.now().toString()
    val summary = if (period == "week") state.week else state.historyMonthSummary

    val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)
    val budgetPeriodStart = localDateFromIsoDate(state.settings.periodStart) ?: weekStart
    val budgetPeriodEnd = localDateFromIsoDate(state.settings.periodEnd) ?: weekEnd
    val effectiveWeekStart = if (budgetPeriodStart.isAfter(weekStart)) budgetPeriodStart else weekStart
    val effectiveWeekEnd = if (budgetPeriodEnd.isBefore(weekEnd)) budgetPeriodEnd else weekEnd
    val weekExpenses = state.expenses.filter { expense ->
        val date = parseSpentAt(expense.spentAt)?.toLocalDate()
        date != null &&
            !date.isBefore(effectiveWeekStart) &&
            !date.isAfter(effectiveWeekEnd)
    }

    val displayedExpenses = if (period == "week") weekExpenses else state.historyMonthExpenses

    LaunchedEffect(period) {
        if (period == "month") {
            viewModel.loadHistoryMonth(state.historyMonth)
        }
    }

    val grouped: Map<Any, List<Expense>> = displayedExpenses
        .groupBy { parseSpentAt(it.spentAt)?.toLocalDate() ?: "Tanpa tanggal" }

    PullToRefreshBox(
        isRefreshing = state.loading || (period == "month" && state.historyMonthLoading),
        onRefresh = {
            viewModel.refresh()
            if (period == "month") {
                viewModel.loadHistoryMonth(state.historyMonth, force = true)
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PeriodChip(stringResource(R.string.this_week), period == "week") { period = "week" }
                    PeriodChip(stringResource(R.string.this_month), period == "month") { period = "month" }
                    if (period == "month") {
                        Spacer(Modifier.weight(1f))
                        Box {
                            TextButton(
                                onClick = {
                                    haptics.tick()
                                    showExportMenu = true
                                },
                            ) {
                                Text("⋮", style = MaterialTheme.typography.titleLarge)
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_csv_action)) },
                                    onClick = {
                                        showExportMenu = false
                                        haptics.confirm()
                                        viewModel.exportData("csv", state.historyMonth)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.export_pdf_action)) },
                                    onClick = {
                                        showExportMenu = false
                                        haptics.confirm()
                                        viewModel.exportData("pdf", state.historyMonth)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (period == "month") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                haptics.tick()
                                viewModel.moveHistoryMonth(-1)
                            },
                        ) {
                            Text("‹", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = formatMonthLabel(state.historyMonth),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(
                            onClick = {
                                haptics.tick()
                                viewModel.moveHistoryMonth(1)
                            },
                            enabled = state.historyMonth < currentMonth,
                        ) {
                            Text("›", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            if (period == "month" && state.historyMonthLoading) {
                item {
                    Text(
                        text = stringResource(R.string.loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    SummaryCard(
                        title = if (period == "week") {
                            stringResource(R.string.this_week)
                        } else {
                            formatMonthLabel(state.historyMonth)
                        },
                        summary = summary,
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.category_distribution),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            CategoryDonutChart(
                                items = summary?.byCategory ?: emptyList(),
                                total = summary?.total ?: 0L,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.expense_history),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (displayedExpenses.isEmpty() && !(period == "month" && state.historyMonthLoading)) {
                item {
                    Text(
                        text = stringResource(R.string.no_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                grouped.forEach { (date, expenses) ->
                    item(key = "header-$date") {
                        Text(
                            text = formatDateLabel(
                                if (date is java.time.LocalDate) date else null,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(expenses, key = { it.id }) { expense ->
                        ExpenseRow(
                            expense = expense,
                            onClick = { editing = expense },
                            onDelete = { pendingDelete = expense },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            val hasMore = if (period == "week") weekExpenses.size >= 200 else state.historyMonthHasMore
            val loadingMore = if (period == "week") {
                state.loadingMoreHistory
            } else {
                state.historyMonthLoadingMore
            }

            if (hasMore) {
                item {
                    Button(
                        onClick = {
                            haptics.tick()
                            if (period == "week") {
                                viewModel.loadMoreHistory()
                            } else {
                                viewModel.loadMoreHistoryMonth()
                            }
                        },
                        enabled = !loadingMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (loadingMore) {
                                stringResource(R.string.loading)
                            } else {
                                stringResource(R.string.load_more)
                            },
                        )
                    }
                }
            }
        }

        pendingDelete?.let { expense ->
            DeleteExpenseDialog(
                expense = expense,
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    viewModel.deleteExpense(expense.id)
                    pendingDelete = null
                },
            )
        }

        editing?.let { expense ->
            EditExpenseDialog(
                expense = expense,
                categories = state.categories,
                onDismiss = { editing = null },
                onSave = { amountText, slug, noteText, dateMillis ->
                    viewModel.updateExpense(expense.id, amountText, slug, noteText, dateMillis)
                    editing = null
                },
            )
        }
    }
}
