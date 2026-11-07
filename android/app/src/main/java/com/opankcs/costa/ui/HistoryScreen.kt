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
import java.time.YearMonth

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
    val currentMonth = YearMonth.now().toString()
    val summary = if (period == "week") state.week else state.historyMonthSummary
    val displayedExpenses = if (period == "week") state.expenses else state.historyMonthExpenses

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodChip(stringResource(R.string.this_week), period == "week") { period = "week" }
                    PeriodChip(stringResource(R.string.this_month), period == "month") { period = "month" }
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
                        )
                    }
                }
            }

            val hasMore = if (period == "week") state.historyHasMore else state.historyMonthHasMore
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
