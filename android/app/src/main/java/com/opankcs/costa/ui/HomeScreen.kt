package com.opankcs.costa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.opankcs.costa.R
import com.opankcs.costa.data.Expense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: CostaUiState, viewModel: CostaViewModel) {
    val haptics = rememberCostaHaptics()

    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDateMillis by rememberSaveable { mutableStateOf(0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    var editing by remember { mutableStateOf<Expense?>(null) }

    LaunchedEffect(state.categories) {
        if (selectedCategory == null) {
            selectedCategory = state.categories.firstOrNull()?.slug
        }
    }

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SummaryCard(title = stringResource(R.string.today), summary = state.today) }

            state.today?.foodBudget?.let { food ->
                item { FoodBudgetCard(food) }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.record_expense),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text(stringResource(R.string.amount_hint)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(10_000L, 20_000L, 25_000L, 50_000L, 100_000L).forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .clickable {
                                            haptics.tick()
                                            amount = preset.toString()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${preset / 1000}k",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                        CategoryChips(
                            categories = state.categories,
                            selected = selectedCategory,
                            onSelect = { selectedCategory = it },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { haptics.tick(); showDatePicker = true }) {
                                Text(
                                    if (selectedDateMillis == 0L) {
                                        stringResource(R.string.today)
                                    } else {
                                        formatDateMillisLabel(selectedDateMillis)
                                    },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(stringResource(R.string.note_optional)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                val slug = selectedCategory ?: return@Button
                                haptics.confirm()
                                viewModel.addExpense(
                                    amountText = amount,
                                    categorySlug = slug,
                                    note = note,
                                    dateMillis = if (selectedDateMillis == 0L) null else selectedDateMillis,
                                )
                                amount = ""
                                note = ""
                                selectedDateMillis = 0L
                            },
                            enabled = amount.isNotBlank() && selectedCategory != null && !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.latest_expenses),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }

            if (state.expenses.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_expenses),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.expenses.take(5), key = { it.id }) { expense ->
                    ExpenseRow(
                        expense = expense,
                        onClick = { editing = expense },
                        onDelete = { pendingDelete = expense },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }

        if (showDatePicker) {
            CostaDatePickerDialog(
                initialMillis = null,
                onDismiss = { showDatePicker = false },
                onSelected = { selectedDateMillis = it },
            )
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
