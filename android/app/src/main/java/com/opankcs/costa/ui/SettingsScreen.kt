package com.opankcs.costa.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.opankcs.costa.data.Category
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.notifications.CostaNotifications

@Composable
fun SettingsScreen(state: CostaUiState, viewModel: CostaViewModel) {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }

    var baseUrl by rememberSaveable(state.baseUrl) { mutableStateOf(state.baseUrl) }

    var daily by rememberSaveable(state.settings.dailyBudget) {
        mutableStateOf(if (state.settings.dailyBudget > 0) state.settings.dailyBudget.toString() else "")
    }
    var weekly by rememberSaveable(state.settings.weeklyBudget) {
        mutableStateOf(if (state.settings.weeklyBudget > 0) state.settings.weeklyBudget.toString() else "")
    }
    var monthly by rememberSaveable(state.settings.monthlyBudget) {
        mutableStateOf(if (state.settings.monthlyBudget > 0) state.settings.monthlyBudget.toString() else "")
    }
    var food by rememberSaveable(state.settings.foodDailyBudget) {
        mutableStateOf(if (state.settings.foodDailyBudget > 0) state.settings.foodDailyBudget.toString() else "")
    }

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(state.userName) }

    var showAddCategory by remember { mutableStateOf(false) }
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

    LaunchedEffect(state.notice) {
        if (state.notice == "Password diperbarui") {
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
                        text = "Akun",
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
                        Text("Ubah nama")
                    }
                    OutlinedButton(
                        onClick = viewModel::logout,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Keluar", color = MaterialTheme.colorScheme.error)
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
                            text = "Kategori",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showAddCategory = true }) {
                            Text("+ Tambah")
                        }
                    }
                    Text(
                        text = "Atur urutan dengan ▲▼. ✎ mengedit kategori buatanmu; ✕ menyembunyikan kategori bawaan atau menghapus kategori buatanmu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.hiddenCategories.isNotEmpty()) {
                        TextButton(onClick = { viewModel.restoreCategories() }) {
                            Text("Pulihkan ${state.hiddenCategories.size} kategori bawaan")
                        }
                    }

                    state.categories.forEach { category ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${category.icon ?: ""} ${category.name}".trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "▲",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.moveCategory(category, -1) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            )
                            Text(
                                text = "▼",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.moveCategory(category, 1) }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
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
                                    .clickable { viewModel.deleteCategory(category.id) }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Export data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Mutasi bulan ini (tanggal 1 sampai hari ini) disimpan ke folder Downloads HP.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportData("csv") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Excel (CSV)")
                        }
                        Button(
                            onClick = { viewModel.exportData("pdf") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("PDF")
                        }
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
                        text = "Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { viewModel.saveBaseUrl(baseUrl) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Simpan server")
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
                        text = "Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = daily,
                        onValueChange = { daily = it },
                        label = { Text("Budget harian (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = weekly,
                        onValueChange = { weekly = it },
                        label = { Text("Budget mingguan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = monthly,
                        onValueChange = { monthly = it },
                        label = { Text("Budget bulanan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = food,
                        onValueChange = { food = it },
                        label = { Text("Jatah makan harian (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Jatah makan dihitung per hari dan saldonya berjalan otomatis (bonus/hutang ikut ke hari berikutnya). Kosongkan untuk menonaktifkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { viewModel.saveBudgets(daily, weekly, monthly, food) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Simpan budget")
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
                        text = "Notifikasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    NotificationRow(
                        title = "Pengingat malam",
                        subtitle = "21:00 — hanya muncul kalau belum ada catatan hari ini",
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it
                            prefs.notifyDailyReminder = it
                        },
                    )
                    NotificationRow(
                        title = "Peringatan budget",
                        subtitle = "Saat budget harian/bulanan 80% dan 100%",
                        checked = budgetEnabled,
                        onCheckedChange = {
                            budgetEnabled = it
                            prefs.notifyBudget = it
                        },
                    )
                    NotificationRow(
                        title = "Rekap mingguan",
                        subtitle = "Minggu 21:30 — total minggu & kategori terbesar",
                        checked = weeklyEnabled,
                        onCheckedChange = {
                            weeklyEnabled = it
                            prefs.notifyWeeklyRecap = it
                        },
                    )
                    NotificationRow(
                        title = "Pengingat awal bulan",
                        subtitle = "Tanggal 1 pukul 08:00 — saatnya atur budget bulan ini",
                        checked = monthlyEnabled,
                        onCheckedChange = {
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
                                title = "Tes notifikasi Costa",
                                text = "Kalau kamu melihat ini, notifikasi Costa sudah aktif.",
                            )
                        },
                    ) {
                        Text("Kirim tes notifikasi")
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
                        text = "Keamanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Password saat ini") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password baru (min. 8 karakter)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Ulangi password baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { viewModel.changePassword(currentPassword, newPassword, confirmPassword) },
                        enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ganti password")
                    }
                }
            }
        }

        item {
            Text(
                text = "Data pengeluaran disimpan di server pribadi Costa dan terpisah untuk setiap akun.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Ubah nama") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama tampilan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateName(nameInput)
                        showNameDialog = false
                    },
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Batal")
                }
            },
        )
    }

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text("Kategori baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nama kategori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = newCategoryIcon,
                        onValueChange = { newCategoryIcon = it },
                        label = { Text("Emoji (opsional, mis. 🎁)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCategory(newCategoryName, newCategoryIcon)
                        newCategoryName = ""
                        newCategoryIcon = ""
                        showAddCategory = false
                    },
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategory = false }) {
                    Text("Batal")
                }
            },
        )
    }

    editingCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Edit kategori") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        label = { Text("Nama kategori") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editCategoryIcon,
                        onValueChange = { editCategoryIcon = it },
                        label = { Text("Emoji (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateCategory(category.id, editCategoryName, editCategoryIcon)
                        editingCategory = null
                    },
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("Batal")
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
