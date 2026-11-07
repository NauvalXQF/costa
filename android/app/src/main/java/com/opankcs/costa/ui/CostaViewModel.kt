package com.opankcs.costa.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opankcs.costa.BuildConfig
import com.opankcs.costa.R
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
import com.opankcs.costa.notifications.BudgetAlerts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

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
    val historyMonth: String = YearMonth.now().toString(),
    val historyMonthSummary: Summary? = null,
    val historyMonthExpenses: List<Expense> = emptyList(),
    val historyMonthLoading: Boolean = false,
    val historyMonthHasMore: Boolean = false,
    val historyMonthLoadingMore: Boolean = false,
)

class CostaViewModel(application: Application) : AndroidViewModel(application) {
    private fun msg(id: Int, vararg args: Any): String = getApplication<Application>().getString(id, *args)

    private companion object {
        const val PAGE_SIZE = 200
    }

    private val prefs = Prefs(application)
    private val repository = CostaRepository(prefs)
    private var historyMonthJob: Job? = null

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
                    401, 422 -> msg(R.string.error_login_invalid)
                    else -> msg(R.string.error_server_code, e.code())
                }
                _state.update { it.copy(loading = false, isLoggedIn = false, error = message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        isLoggedIn = false,
                        error = msg(R.string.error_connect),
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
                    msg(R.string.error_register)
                } else {
                    msg(R.string.error_server_code, e.code())
                }
                _state.update { it.copy(loading = false, isLoggedIn = false, error = message) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        isLoggedIn = false,
                        error = msg(R.string.error_connect),
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

    fun refresh(checkBudgetAlerts: Boolean = false) {
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

                if (checkBudgetAlerts) {
                    BudgetAlerts.check(getApplication(), prefs, today, month)
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    prefs.clearSession()
                    _state.update {
                        it.copy(
                            loading = false,
                            isLoggedIn = false,
                            error = msg(R.string.error_session_expired),
                            baseUrl = prefs.baseUrl,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(loading = false, error = msg(R.string.error_server_code, e.code()))
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = msg(R.string.error_connect),
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = msg(R.string.error_name_empty)) }
            return
        }

        viewModelScope.launch {
            try {
                val user = repository.updateProfile(name.trim())
                prefs.userName = user?.name ?: name.trim()
                _state.update { it.copy(notice = msg(R.string.notice_name_updated), error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_name_update)) }
            }
        }
    }

    fun addExpense(amountText: String, categorySlug: String, note: String, dateMillis: Long? = null) {
        val amount = parseAmount(amountText)
        if (amount == null || amount <= 0L) {
            _state.update { it.copy(error = msg(R.string.error_invalid_amount)) }
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
                _state.update { it.copy(notice = msg(R.string.notice_expense_saved, formatRupiah(amount)), error = null) }
                refresh(checkBudgetAlerts = true)
                reloadHistoryMonthIfLoaded()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_expense_save)) }
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
            _state.update { it.copy(error = msg(R.string.error_invalid_amount)) }
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
                _state.update { it.copy(notice = msg(R.string.notice_expense_updated), error = null) }
                refresh(checkBudgetAlerts = true)
                reloadHistoryMonthIfLoaded()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_expense_update)) }
            }
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            runCatching { repository.deleteExpense(id) }
            _state.update { it.copy(notice = msg(R.string.notice_expense_deleted)) }
            refresh(checkBudgetAlerts = true)
            reloadHistoryMonthIfLoaded()
        }
    }

    fun addCategory(name: String, icon: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = msg(R.string.error_category_name_required)) }
            return
        }

        viewModelScope.launch {
            try {
                repository.createCategory(name.trim(), icon.trim().ifBlank { null })
                _state.update { it.copy(notice = msg(R.string.notice_category_added), error = null) }
                refresh()
            } catch (e: HttpException) {
                val message = if (e.code() == 422) msg(R.string.error_category_exists) else msg(R.string.error_category_add_code, e.code())
                _state.update { it.copy(error = message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_category_add)) }
            }
        }
    }

    fun updateCategory(id: Int, name: String, icon: String) {
        if (name.isBlank()) {
            _state.update { it.copy(error = msg(R.string.error_category_name_required)) }
            return
        }

        viewModelScope.launch {
            try {
                repository.updateCategory(id, name.trim(), icon.trim().ifBlank { null })
                _state.update { it.copy(notice = msg(R.string.notice_category_updated), error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_category_update)) }
            }
        }
    }

    fun reorderCategories(ordered: List<Category>) {
        viewModelScope.launch {
            try {
                repository.reorderCategories(ordered.map { it.slug })
                _state.update { it.copy(notice = msg(R.string.notice_category_reordered), error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_category_reorder)) }
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
                _state.update { it.copy(error = msg(R.string.error_category_reorder)) }
            }
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(id)
                _state.update { it.copy(notice = msg(R.string.notice_category_deleted), error = null) }
                refresh()
            } catch (e: HttpException) {
                val message = if (e.code() == 422) msg(R.string.error_category_in_use) else msg(R.string.error_category_delete_code, e.code())
                _state.update { it.copy(error = message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_category_delete)) }
            }
        }
    }

    fun exportData(format: String) {
        viewModelScope.launch {
            try {
                val from = LocalDate.now().withDayOfMonth(1).toString()
                val lang = if (Locale.getDefault().language == "en") "en" else "id"
                val to = LocalDate.now().toString()

                if (format == "pdf") {
                    val body = repository.exportPdf(from, to, lang)
                    val saved = DownloadSaver.save(
                        getApplication(),
                        "costa-mutasi-$from-$to.pdf",
                        "application/pdf",
                        body,
                    )
                    _state.update {
                        it.copy(
                            notice = if (saved) msg(R.string.notice_pdf_saved) else null,
                            error = if (saved) null else msg(R.string.error_pdf_save),
                        )
                    }
                } else {
                    val body = repository.exportCsv(from, to, lang)
                    val saved = DownloadSaver.save(
                        getApplication(),
                        "costa-mutasi-$from-$to.csv",
                        "text/csv",
                        body,
                    )
                    _state.update {
                        it.copy(
                            notice = if (saved) msg(R.string.notice_csv_saved) else null,
                            error = if (saved) null else msg(R.string.error_csv_save),
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_export)) }
            }
        }
    }

    fun restoreCategories() {
        viewModelScope.launch {
            try {
                repository.restoreCategories()
                _state.update { it.copy(notice = msg(R.string.notice_categories_restored), error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_categories_restore)) }
            }
        }
    }

    fun changePassword(current: String, new: String, confirmation: String) {
        if (current.isBlank() || new.length < 8) {
            _state.update { it.copy(error = msg(R.string.error_password_min)) }
            return
        }
        if (new != confirmation) {
            _state.update { it.copy(error = msg(R.string.error_password_mismatch)) }
            return
        }

        viewModelScope.launch {
            try {
                repository.changePassword(current, new, confirmation)
                _state.update { it.copy(notice = msg(R.string.notice_password_updated), error = null) }
            } catch (e: HttpException) {
                val message = if (e.code() == 422) msg(R.string.error_password_old) else msg(R.string.error_password_code, e.code())
                _state.update { it.copy(error = message) }
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_password_change)) }
            }
        }
    }

    fun saveBaseUrl(baseUrl: String) {
        prefs.baseUrl = baseUrl
        _state.update { it.copy(notice = msg(R.string.notice_server_saved), error = null) }
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
                _state.update { it.copy(notice = msg(R.string.notice_budget_saved), error = null) }
                refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = msg(R.string.error_budget_save)) }
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
                            notice = msg(R.string.notice_app_latest, BuildConfig.VERSION_NAME),
                            error = null,
                        )
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _state.update { it.copy(error = msg(R.string.error_update_check)) }
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
                        notice = msg(R.string.notice_update_install),
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(updateDownloading = false, error = msg(R.string.error_update_download))
                }
            }
        }
    }

    fun loadHistoryMonth(month: String, force: Boolean = false) {
        val currentMonth = YearMonth.now().toString()
        val safeMonth = if (month > currentMonth) currentMonth else month
        val current = _state.value

        if (!force && current.historyMonth == safeMonth &&
            (current.historyMonthSummary != null || current.historyMonthLoading)
        ) {
            return
        }

        historyMonthJob?.cancel()
        historyMonthJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    historyMonth = safeMonth,
                    historyMonthLoading = true,
                    historyMonthSummary = null,
                    historyMonthExpenses = emptyList(),
                    historyMonthHasMore = false,
                    historyMonthLoadingMore = false,
                    error = null,
                )
            }

            try {
                val yearMonth = YearMonth.parse(safeMonth)
                val from = yearMonth.atDay(1).toString()
                val to = yearMonth.atEndOfMonth().toString()
                val summary = repository.summary("month", safeMonth)
                val expenses = repository.expenses(limit = PAGE_SIZE, from = from, to = to)

                _state.update {
                    it.copy(
                        historyMonthSummary = summary,
                        historyMonthExpenses = expenses,
                        historyMonthHasMore = expenses.size >= PAGE_SIZE,
                        historyMonthLoading = false,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(historyMonthLoading = false, error = msg(R.string.error_history_load))
                }
            }
        }
    }

    fun moveHistoryMonth(delta: Int) {
        val current = YearMonth.parse(_state.value.historyMonth)
        val target = current.plusMonths(delta.toLong())
        if (target > YearMonth.now()) return
        loadHistoryMonth(target.toString(), force = true)
    }

    fun loadMoreHistoryMonth() {
        val state = _state.value
        if (state.historyMonthLoadingMore || !state.historyMonthHasMore) return

        val last = state.historyMonthExpenses.lastOrNull() ?: return
        val before = last.spentAt ?: return
        val yearMonth = YearMonth.parse(state.historyMonth)

        viewModelScope.launch {
            _state.update { it.copy(historyMonthLoadingMore = true, error = null) }

            try {
                val page = repository.expenses(
                    limit = PAGE_SIZE,
                    from = yearMonth.atDay(1).toString(),
                    to = yearMonth.atEndOfMonth().toString(),
                    before = before,
                    beforeId = last.id,
                )
                val existingIds = _state.value.historyMonthExpenses.map { it.id }.toSet()
                val merged = _state.value.historyMonthExpenses + page.filterNot { it.id in existingIds }

                _state.update {
                    it.copy(
                        historyMonthExpenses = merged,
                        historyMonthHasMore = page.size >= PAGE_SIZE,
                        historyMonthLoadingMore = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        historyMonthLoadingMore = false,
                        error = msg(R.string.error_history_load),
                    )
                }
            }
        }
    }

    private fun reloadHistoryMonthIfLoaded() {
        val state = _state.value
        if (state.historyMonthSummary != null) {
            loadHistoryMonth(state.historyMonth, force = true)
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
                        error = msg(R.string.error_history_load),
                    )
                }
            }
        }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }
}
