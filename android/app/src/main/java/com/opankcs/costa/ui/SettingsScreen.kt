package com.opankcs.costa.ui

import android.app.LocaleManager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import com.opankcs.costa.BuildConfig
import com.opankcs.costa.R
import com.opankcs.costa.data.Category
import com.opankcs.costa.ui.theme.CostaAmber
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.notifications.CostaNotifications

@Composable
fun SettingsScreen(state: CostaUiState, viewModel: CostaViewModel) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val haptics = rememberCostaHaptics()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val passwordUpdatedNotice = context.getString(R.string.notice_password_updated)
    val currentLanguageCode = context.getSystemService(LocaleManager::class.java)
        ?.applicationLocales?.get(0)?.language
    val languageLabel = when (currentLanguageCode) {
        "in" -> stringResource(R.string.language_indonesian)
        "en" -> stringResource(R.string.language_english)
        else -> stringResource(R.string.language_system)
    }


    var monthly by rememberSaveable(state.settings.monthlyBudget) {
        mutableStateOf(if (state.settings.monthlyBudget > 0) state.settings.monthlyBudget.toString() else "")
    }
    var food by rememberSaveable(state.settings.foodDailyBudget) {
        mutableStateOf(if (state.settings.foodDailyBudget > 0) state.settings.foodDailyBudget.toString() else "")
    }
    var startDateMillis by rememberSaveable(state.settings.periodStart) {
        mutableStateOf(
            localDateFromIsoDate(state.settings.periodStart)?.let(::localDateToUtcMillis)
                ?: localDateToUtcMillis(LocalDate.now()),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val previewStart = utcMillisToLocalDate(startDateMillis)
    val previewPeriodEnd = previewStart.withDayOfMonth(previewStart.lengthOfMonth())
    val previewPeriodDays = ChronoUnit.DAYS.between(previewStart, previewPeriodEnd).toInt() + 1
    val monthlyValue = monthly.toLongOrNull() ?: 0L
    val previewDaily = if (monthlyValue > 0) monthlyValue / previewPeriodDays else 0L
    val todayDate = LocalDate.now()
    val previewWeekStart = todayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val previewWeekEnd = previewWeekStart.plusDays(6)
    val previewEffectiveStart = if (previewWeekStart.isAfter(previewStart)) previewWeekStart else previewStart
    val previewEffectiveEnd = if (previewWeekEnd.isAfter(previewPeriodEnd)) previewPeriodEnd else previewWeekEnd
    val previewWeekDays = if (!previewEffectiveStart.isAfter(previewEffectiveEnd)) {
        ChronoUnit.DAYS.between(previewEffectiveStart, previewEffectiveEnd).toInt() + 1
    } else {
        0
    }
    val previewWeekly = previewDaily * previewWeekDays

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(state.userName) }

    var showAddCategory by remember { mutableStateOf(false) }
    var showOrderDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("") }

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editCategoryName by remember { mutableStateOf("") }
    var editCategoryIcon by remember { mutableStateOf("") }

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var reminderEnabled by remember { mutableStateOf(prefs.notifyDailyReminder) }
    var budgetEnabled by remember { mutableStateOf(prefs.notifyBudget) }
    var weeklyEnabled by remember { mutableStateOf(prefs.notifyWeeklyRecap) }
    var monthlyEnabled by remember { mutableStateOf(prefs.notifyMonthlyReset) }
    var hapticsEnabled by remember { mutableStateOf(prefs.hapticsEnabled) }

    LaunchedEffect(state.notice) {
        if (state.notice == passwordUpdatedNotice) {
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.account),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.userName.ifBlank { "-" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = state.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            nameInput = state.userName
                            showNameDialog = true
                        },
                    ) {
                        Text(stringResource(R.string.change_name))
                    }
                    OutlinedButton(
                        onClick = { haptics.tick(); viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.budget_period),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = monthly,
                        onValueChange = { monthly = it },
                        label = { Text(stringResource(R.string.monthly_budget)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { haptics.tick(); showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.period_start_button, formatDateLong(previewStart)))
                    }
                    OutlinedTextField(
                        value = food,
                        onValueChange = { food = it },
                        label = { Text(stringResource(R.string.food_daily_budget)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(R.string.food_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (monthlyValue > 0) {
                        Text(
                            text = stringResource(R.string.period_summary, formatDateShort(previewStart), formatDateShort(previewPeriodEnd), previewPeriodDays),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.daily_auto, formatRupiah(previewDaily)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.weekly_this, formatDateShort(previewEffectiveStart), formatDateShort(previewEffectiveEnd), formatRupiah(previewWeekly)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.fill_monthly),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = { haptics.confirm(); viewModel.saveBudget(monthly, food, startDateMillis) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.save_budget))
                    }
                }
            }
        }


        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.categories),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { haptics.tick(); showAddCategory = true }) {
                            Text(stringResource(R.string.add_with_plus))
                        }
                    }
                    Text(
                        text = stringResource(R.string.category_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { haptics.tick(); showOrderDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.reorder_drag))
                    }
                    if (state.hiddenCategories.isNotEmpty()) {
                        TextButton(onClick = { haptics.tick(); viewModel.restoreCategories() }) {
                            Text(stringResource(R.string.restore_defaults, state.hiddenCategories.size))
                        }
                    }

                    state.categories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${category.icon ?: ""} ${localizedCategoryName(context, category)}".trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (category.userId != null) {
                                Text(
                                    text = "✎",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            editCategoryName = category.name
                                            editCategoryIcon = category.icon ?: ""
                                            editingCategory = category
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                            }
                            Text(
                                text = "✕",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { haptics.tick(); viewModel.deleteCategory(category.id) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.notifications),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    NotificationRow(
                        title = stringResource(R.string.daily_reminder),
                        subtitle = stringResource(R.string.daily_reminder_sub),
                        checked = reminderEnabled,
                        onCheckedChange = {
                            haptics.toggle()
                            reminderEnabled = it
                            prefs.notifyDailyReminder = it
                        },
                    )
                    NotificationRow(
                        title = stringResource(R.string.budget_alert),
                        subtitle = stringResource(R.string.budget_alert_sub),
                        checked = budgetEnabled,
                        onCheckedChange = {
                            haptics.toggle()
                            budgetEnabled = it
                            prefs.notifyBudget = it
                        },
                    )
                    NotificationRow(
                        title = stringResource(R.string.weekly_recap),
                        subtitle = stringResource(R.string.weekly_recap_sub),
                        checked = weeklyEnabled,
                        onCheckedChange = {
                            haptics.toggle()
                            weeklyEnabled = it
                            prefs.notifyWeeklyRecap = it
                        },
                    )
                    NotificationRow(
                        title = stringResource(R.string.monthly_reset),
                        subtitle = stringResource(R.string.monthly_reset_sub),
                        checked = monthlyEnabled,
                        onCheckedChange = {
                            haptics.toggle()
                            monthlyEnabled = it
                            prefs.notifyMonthlyReset = it
                        },
                    )
                    TextButton(
                        onClick = {
                            CostaNotifications.show(
                                context = context,
                                channel = CostaNotifications.CHANNEL_REMINDER,
                                id = 9001,
                                title = context.getString(R.string.test_notification_title),
                                text = context.getString(R.string.test_notification_text),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.send_test_notification))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.feedback),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    NotificationRow(
                        title = stringResource(R.string.haptics),
                        subtitle = stringResource(R.string.haptics_sub),
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            haptics.toggle()
                            hapticsEnabled = it
                            prefs.hapticsEnabled = it
                        },
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.security),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(stringResource(R.string.current_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.repeat_new_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { haptics.confirm(); viewModel.changePassword(currentPassword, newPassword, confirmPassword) },
                        enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.change_password))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.language_current, languageLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { haptics.tick(); showLanguageDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.change_language))
                    }
                }
            }
        }


        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.app_update),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.installed_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { haptics.tick(); viewModel.checkForUpdate() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.check_update))
                    }
                    state.availableUpdate?.let { update ->
                        Text(
                            text = stringResource(R.string.version_available, update.versionName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Button(
                            onClick = { haptics.confirm(); viewModel.downloadAndInstallUpdate() },
                            enabled = !state.updateDownloading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (state.updateDownloading) {
                                    stringResource(R.string.downloading)
                                } else {
                                    stringResource(R.string.download_install)
                                },
                            )
                        }
                    }
                }
            }
        }


        item {
            Text(
                text = stringResource(R.string.data_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text(
                text = buildAnnotatedString {
                    append("by ")
                    append("opankcs")
                    withStyle(SpanStyle(color = CostaAmber)) { append(".") }
                    append("com")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 20.dp),
            )
        }

    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.change_name)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.confirm()
                        viewModel.updateName(nameInput)
                        showNameDialog = false
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text(stringResource(R.string.new_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newCategoryIcon,
                        onValueChange = { newCategoryIcon = it },
                        label = { Text(stringResource(R.string.emoji_example)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.confirm()
                        viewModel.addCategory(newCategoryName, newCategoryIcon)
                        newCategoryName = ""
                        newCategoryIcon = ""
                        showAddCategory = false
                    },
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showOrderDialog) {
        CategoryOrderDialog(
            categories = state.categories,
            onDismiss = { showOrderDialog = false },
            onSave = { ordered ->
                viewModel.reorderCategories(ordered)
                showOrderDialog = false
            },
        )
    }

    editingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text(stringResource(R.string.edit_category)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editCategoryIcon,
                        onValueChange = { editCategoryIcon = it },
                        label = { Text(stringResource(R.string.emoji_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.confirm()
                        viewModel.updateCategory(category.id, editCategoryName, editCategoryIcon)
                        editingCategory = null
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showDatePicker) {
        CostaDatePickerDialog(
            initialMillis = startDateMillis,
            onDismiss = { showDatePicker = false },
            onSelected = { startDateMillis = it },
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "system" to R.string.language_system,
                        "in" to R.string.language_indonesian,
                        "en" to R.string.language_english,
                    ).forEach { (code, label) ->
                        TextButton(
                            onClick = {
                                haptics.tick()
                                showLanguageDialog = false
                                applyAppLanguage(context, code)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(label))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

}

@Composable
private fun NotificationRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

