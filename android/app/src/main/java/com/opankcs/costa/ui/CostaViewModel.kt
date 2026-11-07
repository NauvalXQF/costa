package com.opankcs.costa.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opankcs.costa.BuildConfig
import com.opankcs.costa.data.AppVersionResponse
import com.opankcs.costa.data.Category
import com.opankcs.costa.data.CostaRepository
import com.opankcs.costa.data.DownloadSaver
import com.opankcs.costa.data.Expense
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.data.Settings
import com.opankcs.costa.data.Summary
import com.opankcs.costa.data.UpdateExpenseRequest
import com.opankcs.costa.data.UpdateSettingsRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.time.LocalDate

data class CostaUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val isLoggedIn: Boolean = false,
    val userName: String = "",
    val userEmail: String = "",
    val baseUrl: String = "",
    val categories: List<Category> = emptyList(),
    val hiddenCategories: List<String> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val today: Summary? = null,
    val week: Summary? = null,
    val month: Summary? = null,
    val settings: Settings = Settings(),
    val availableUpdate: AppVersionResponse? = null,
    val updateDownloading: Boolean = false,
    val historyHasMore: Boolean = false,
    val loadingMoreHistory: Boolean = false,
)

class CostaViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val PAGE_SIZE = 200
    }

    private val prefs = Prefs(application)
    private val repository = CostaRepository(prefs)

    private val _state = MutableStateFlow(
        CostaUiState(
            loading = prefs.token.isNotBlank(),
            isLoggedIn = prefs.token.isNotBlank(),
            baseUrl = prefs.baseUrl,
            userName = prefs.userName,
            userEmail = prefs.userEmail,
        ),
    )
    val state = _state.asStateFlow()

    init {
        refresh()
        checkForUpdate(silent = true)
    }

    fun login(email: String, password: String, baseUrl: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            try {
                prefs.baseUrl = baseUrl
                val response = repository.login(email.trim(), password)
                prefs.token = response.token
                prefs.userName = response.user?.name ?: ""
                prefs.userEmail = response.user?.email ?: ""

                _state.update {
                    it.copy(
                        isLoggedIn = true,
                        userName = prefs.userName,
                        userEmail = prefs.userEmail,
                        baseUrl = prefs.baseUrl,
                    )
                }
                refresh()
            } catch (e: HttpException) {
                val message = when (e.code()) {
                    401, 422 -> "Email atau password salah."
                    else -> "Server error (${e.code()})."
                }
                _state.update { it.copy(loading = false, isLoggedIn = false, error = message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        isLoggedIn = false,
                        error = "Tidak bisa terhubung ke server. Cek Base URL.",
                    )
                }
            }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirmation: String,
        inviteCode: String,
        baseUrl: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            try {
                prefs.baseUrl = baseUrl
                val response = repository.register(
                    name = name.trim(),
                    email = email.trim(),
                    password = password,
                    confirmation = confirmation,
                    inviteCode = inviteCode.trim(),
                )
                prefs.token = response.token
                prefs.userName = response.user?.name ?: name.trim()
                prefs.userEmail = response.user?.email ?: email.trim()

                _state.update {
                    it.copy(
                        isLoggedIn = true,
                        userName = prefs.userName,
                        userEmail = prefs.userEmail,
                        baseUrl = prefs.baseUrl,
                    )
                }
                refresh()
            } catch (e: HttpException) {
                val message = if (e.code() == 422) {
                    "Pendaftaran gagal: data tidak valid atau kode undangan salah."
                } else {
                    "Server error (${e.code()})."
                }
                _state.update { it.copy(loading = false, isLoggedIn = false, error = message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        isLoggedIn = false,
                        error = "Tidak bisa terhubung ke server. Cek Base URL.",
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            prefs.clearSession()
            _state.value = CostaUiState(loading = false, baseUrl = prefs.baseUrl)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }

            if (prefs.token.isBlank()) {
                _state.update {
                    it.copy(loading = false, isLoggedIn = false, baseUrl = prefs.baseUrl)
                }
                return@launch
            }

            try {
                val me = runCatching { repository.me() }.getOrNull()
                if (me != null) {
                    prefs.userName = me.name
                    prefs.userEmail = me.email
                }

                val categoryList = repository.categoryList()
                val categories = categoryList.data
                val expenses = repository.expenses(limit = PAGE_SIZE)
                val hasMore = expenses.size >= PAGE_SIZE
                val today = repository.summary("today")
                val week = repository.summary("week")
                val month = repository.summary("month")
                val settings = runCatching { repository.settings() }.getOrDefault(Settings())

                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        isLoggedIn = true,
                        baseUrl = prefs.baseUrl,
                        userName = prefs.userName,
                        userEmail = prefs.userEmail,
                        categories = categories,
                        hiddenCategories = categoryList.hidden,
                        expenses = expenses,
                        historyHasMore = hasMore,
                        today = today,
                        week = week,
                        month = month,
                        settings = settings,
                    )
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    prefs.clearSession()
                    _state.update {
                        it.copy(
                            loading = false,
                            isLoggedIn = false,
                            error = "Sesi berakhir, silakan masuk lagi.",
                            baseUrl = prefs.baseUrl,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(loading = false, error = "Server error (${e.code()}).")
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Tidak bisa terhubung ke server. Cek Base URL.",
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Nama tidak boleh kosong") }
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.updateProfile(name.trim())
                prefs.userName = user?.name ?: name.trim()
                _state.update { it.copy(notice = "Nama diperbarui", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal memperbarui nama") }
            }
        }
    }

    fun addExpense(amountText: String, categorySlug: String, note: String, dateMillis: Long? = null) {
        val amount = parseAmount(amountText)
        if (amount == null || amount <= 0L) {
            _state.update { it.copy(error = "Nominal tidak valid") }
            return
        }

        viewModelScope.launch {
            try {
                repository.addExpense(
                    amount = amount,
                    category = categorySlug,
                    note = note.ifBlank { null },
                    spentAt = spentAtIsoFromMillis(dateMillis),
                )
                _state.update { it.copy(notice = "Tercatat: ${formatRupiah(amount)}", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal menyimpan pengeluaran") }
            }
        }
    }

    fun updateExpense(
        id: Int,
        amountText: String,
        categorySlug: String,
        note: String,
        dateMillis: Long? = null,
    ) {
        val amount = parseAmount(amountText)
        if (amount == null || amount <= 0L) {
            _state.update { it.copy(error = "Nominal tidak valid") }
            return
        }

        viewModelScope.launch {
            try {
                repository.updateExpense(
                    id = id,
                    body = UpdateExpenseRequest(
                        amount = amount,
                        category = categorySlug,
                        note = note.ifBlank { null },
                        spentAt = spentAtIsoForEdit(dateMillis),
                    ),
                )
                _state.update { it.copy(notice = "Pengeluaran diperbarui", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal memperbarui pengeluaran") }
            }
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteExpense(id) }
            _state.update { it.copy(notice = "Pengeluaran dihapus") }
            refresh()
        }
    }

    fun addCategory(name: String, icon: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Nama kategori wajib diisi") }
            return
        }

        viewModelScope.launch {
            try {
                repository.createCategory(name.trim(), icon.trim().ifBlank { null })
                _state.update { it.copy(notice = "Kategori ditambahkan", error = null) }
                refresh()
            } catch (e: HttpException) {
                val message = if (e.code() == 422) "Kategori sudah ada atau tidak valid." else "Gagal menambah kategori (${e.code()})"
                _state.update { it.copy(error = message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal menambah kategori") }
            }
        }
    }

    fun updateCategory(id: Int, name: String, icon: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = "Nama kategori wajib diisi") }
            return
        }

        viewModelScope.launch {
            try {
                repository.updateCategory(id, name.trim(), icon.trim().ifBlank { null })
                _state.update { it.copy(notice = "Kategori diperbarui", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal memperbarui kategori") }
            }
        }
    }

    fun reorderCategories(ordered: List<Category>) {
        viewModelScope.launch {
            try {
                repository.reorderCategories(ordered.map { it.slug })
                _state.update { it.copy(notice = "Urutan kategori disimpan", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal mengubah urutan kategori") }
            }
        }
    }

    fun moveCategory(category: Category, direction: Int) {
        val list = _state.value.categories.toMutableList()
        val index = list.indexOfFirst { it.id == category.id }
        val target = index + direction

        if (index < 0 || target < 0 || target >= list.size) return

        val moved = list[index]
        list[index] = list[target]
        list[target] = moved

        viewModelScope.launch {
            try {
                repository.reorderCategories(list.map { it.slug })
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal mengubah urutan kategori") }
            }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(id)
                _state.update { it.copy(notice = "Kategori dihapus", error = null) }
                refresh()
            } catch (e: HttpException) {
                val message = if (e.code() == 422) "Kategori masih dipakai transaksi." else "Gagal menghapus kategori (${e.code()})"
                _state.update { it.copy(error = message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal menghapus kategori") }
            }
        }
    }

    fun exportData(format: String) {
        viewModelScope.launch {
            try {
                val from = LocalDate.now().withDayOfMonth(1).toString()
                val to = LocalDate.now().toString()

                if (format == "pdf") {
                    val body = repository.exportPdf(from, to)
                    val saved = DownloadSaver.save(
                        getApplication(),
                        "costa-mutasi-$from-$to.pdf",
                        "application/pdf",
                        body,
                    )
                    _state.update {
                        it.copy(
                            notice = if (saved) "PDF disimpan di folder Downloads" else null,
                            error = if (saved) null else "Gagal menyimpan PDF",
                        )
                    }
                } else {
                    val body = repository.exportCsv(from, to)
                    val saved = DownloadSaver.save(
                        getApplication(),
                        "costa-mutasi-$from-$to.csv",
                        "text/csv",
                        body,
                    )
                    _state.update {
                        it.copy(
                            notice = if (saved) "CSV disimpan di folder Downloads" else null,
                            error = if (saved) null else "Gagal menyimpan CSV",
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal export data") }
            }
        }
    }

    fun restoreCategories() {
        viewModelScope.launch {
            try {
                repository.restoreCategories()
                _state.update { it.copy(notice = "Kategori bawaan dipulihkan", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal memulihkan kategori") }
            }
        }
    }

    fun changePassword(current: String, new: String, confirmation: String) {
        if (current.isBlank() || new.length < 8) {
            _state.update { it.copy(error = "Password baru minimal 8 karakter") }
            return
        }
        if (new != confirmation) {
            _state.update { it.copy(error = "Konfirmasi password tidak cocok") }
            return
        }

        viewModelScope.launch {
            try {
                repository.changePassword(current, new, confirmation)
                _state.update { it.copy(notice = "Password diperbarui", error = null) }
            } catch (e: HttpException) {
                val message = if (e.code() == 422) "Password lama salah." else "Gagal mengganti password (${e.code()})"
                _state.update { it.copy(error = message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal mengganti password") }
            }
        }
    }

    fun saveBaseUrl(baseUrl: String) {
        prefs.baseUrl = baseUrl
        _state.update { it.copy(notice = "Server disimpan", error = null) }
        refresh()
    }

    fun saveBudget(monthly: String, food: String, startDateMillis: Long?) {
        viewModelScope.launch {
            try {
                repository.saveSettings(
                    UpdateSettingsRequest(
                        monthlyBudget = monthly.toLongOrNull() ?: 0L,
                        foodDailyBudget = food.toLongOrNull() ?: 0L,
                        budgetStartDate = startDateMillis?.let { utcMillisToLocalDate(it).toString() },
                    ),
                )
                _state.update { it.copy(notice = "Budget disimpan", error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = "Gagal menyimpan budget") }
            }
        }
    }

    fun checkForUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            try {
                val info = repository.appVersion()

                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    _state.update { it.copy(availableUpdate = info) }
                } else if (!silent) {
                    _state.update {
                        it.copy(
                            notice = "Aplikasi sudah versi terbaru (${BuildConfig.VERSION_NAME})",
                            error = null,
                        )
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _state.update { it.copy(error = "Gagal memeriksa update") }
                }
            }
        }
    }

    fun dismissUpdate() {
        _state.update { it.copy(availableUpdate = null) }
    }

    fun downloadAndInstallUpdate() {
        val update = _state.value.availableUpdate ?: return

        if (_state.value.updateDownloading) return

        viewModelScope.launch {
            _state.update { it.copy(updateDownloading = true, error = null) }

            try {
                val body = repository.downloadApk(update.apkUrl)
                val context = getApplication<Application>()
                val file = File(context.cacheDir, "costa-update.apk")

                file.outputStream().use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                context.startActivity(intent)

                _state.update {
                    it.copy(
                        updateDownloading = false,
                        availableUpdate = null,
                        notice = "Ikuti petunjuk pemasangan untuk menyelesaikan update",
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(updateDownloading = false, error = "Gagal mengunduh update")
                }
            }
        }
    }

    fun loadMoreHistory() {
        val current = _state.value.expenses

        if (_state.value.loadingMoreHistory || !_state.value.historyHasMore || current.isEmpty()) return

        val last = current.last()
        val before = last.spentAt ?: return

        viewModelScope.launch {
            _state.update { it.copy(loadingMoreHistory = true, error = null) }

            try {
                val page = repository.expenses(
                    limit = PAGE_SIZE,
                    before = before,
                    beforeId = last.id,
                )
                val existingIds = _state.value.expenses.map { it.id }.toSet()
                val merged = _state.value.expenses + page.filterNot { it.id in existingIds }

                _state.update {
                    it.copy(
                        expenses = merged,
                        historyHasMore = page.size >= PAGE_SIZE,
                        loadingMoreHistory = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loadingMoreHistory = false,
                        error = "Gagal memuat riwayat lama",
                    )
                }
            }
        }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }
}
