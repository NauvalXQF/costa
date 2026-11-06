package com.opankcs.costa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.opankcs.costa.data.Category
import com.opankcs.costa.data.Expense
import com.opankcs.costa.data.FoodBudget
import com.opankcs.costa.data.Summary

@Composable
fun SummaryCard(title: String, summary: Summary?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatRupiah(summary?.total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            val budget = summary?.budget
            if (budget?.limit != null && budget.limit > 0) {
                val statusColor = budgetProgressColor(budget.percent)
                val statusLabel = budgetStatusLabel(budget.percent)

                LinearProgressIndicator(
                    progress = { ((budget.percent ?: 0.0).toFloat() / 100f).coerceIn(0f, 1f) },
                    color = statusColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )
                Text(
                    text = "Budget ${formatRupiah(budget.limit)} · sisa ${formatRupiah(budget.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (statusLabel.isNotBlank()) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            summary?.month?.safeToSpendPerDay?.let { safe ->
                Text(
                    text = "Aman belanja ${formatRupiah(safe)}/hari (sisa bulan ini)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "${summary?.count ?: 0} transaksi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FoodBudgetCard(food: FoodBudget) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Jatah makan hari ini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatRupiah(food.allowanceToday),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "dari ${formatRupiah(food.base)}/hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            val percentage = if (food.allowanceToday > 0L) {
                food.spentToday.toDouble() / food.allowanceToday.toDouble() * 100.0
            } else if (food.debt > 0L) {
                100.0
            } else {
                0.0
            }
            val statusColor = budgetProgressColor(percentage)
            val statusLabel = budgetStatusLabel(percentage)

            val progress = if (food.allowanceToday > 0L) {
                (food.spentToday.toFloat() / food.allowanceToday.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                color = statusColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Terpakai hari ini ${formatRupiah(food.spentToday)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (statusLabel.isNotBlank()) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                }
            }

            val status = when {
                food.debt > 0 -> "▼ Hutang jajan ${formatRupiah(food.debt)} · besok jatah ${formatRupiah(food.tomorrowAllowance)}"
                food.bonus > 0 -> "▲ Bonus ${formatRupiah(food.bonus)} · besok jatah ${formatRupiah(food.tomorrowAllowance)}"
                else -> "Pas · besok jatah ${formatRupiah(food.tomorrowAllowance)}"
            }
            val statusContentColor = when {
                food.debt > 0 -> MaterialTheme.colorScheme.error
                food.bonus > 0 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (food.debt > 0 || food.bonus > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = statusContentColor,
            )
        }
    }
}

@Composable
fun CategoryChips(
    categories: List<Category>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selected, categories) {
        val index = categories.indexOfFirst { it.slug == selected }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = category.slug == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(50),
                    )
                    .clickable { onSelect(category.slug) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${category.icon ?: ""} ${category.name}".trim(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun ExpenseRow(
    expense: Expense,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = expense.category?.icon ?: "🧾",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = expense.category?.name ?: "-",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            val subtitle = listOfNotNull(
                expense.note,
                formatTime(expense.spentAt).ifBlank { null },
            ).joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = formatRupiah(expense.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DeleteExpenseDialog(
    expense: Expense,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val description = buildString {
        append(expense.category?.icon ?: "")
        append(" ")
        append(expense.category?.name ?: "-")
        append(" · ")
        append(formatRupiah(expense.amount))
        expense.note?.let {
            append("\n")
            append(it)
        }
        append("\n\nData yang dihapus tidak bisa dikembalikan.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hapus pengeluaran?") },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Hapus",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostaDatePickerDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis ?: System.currentTimeMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onSelected)
                    onDismiss()
                },
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun EditExpenseDialog(
    expense: Expense,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (amountText: String, categorySlug: String, note: String, dateMillis: Long?) -> Unit,
) {
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var note by remember { mutableStateOf(expense.note ?: "") }
    var selectedCategory by remember { mutableStateOf(expense.category?.slug) }
    var dateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit pengeluaran") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryChips(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { showDatePicker = true }) {
                    val label = dateMillis?.let { formatDateMillisLabel(it) }
                        ?: formatDateLabel(parseSpentAt(expense.spentAt)?.toLocalDate())
                    Text("Tanggal: $label")
                }
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = parseAmount(amount)
                    val slug = selectedCategory
                    when {
                        parsed == null || parsed <= 0L -> error = "Nominal tidak valid"
                        slug == null -> error = "Pilih kategori"
                        else -> onSave(amount, slug, note, dateMillis)
                    }
                },
            ) {
                Text("Simpan", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
    )

    if (showDatePicker) {
        CostaDatePickerDialog(
            initialMillis = dateMillis ?: dateMillisFromIso(expense.spentAt),
            onDismiss = { showDatePicker = false },
            onSelected = { dateMillis = it },
        )
    }
}
