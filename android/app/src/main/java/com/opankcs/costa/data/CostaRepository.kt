package com.opankcs.costa.data

import okhttp3.ResponseBody

class CostaRepository(private val prefs: Prefs) {
    private val api: CostaApi
        get() = ApiClient.get(prefs)

    suspend fun login(email: String, password: String): LoginResponse =
        api.login(LoginRequest(email = email, password = password))

    suspend fun register(
        name: String,
        email: String,
        password: String,
        confirmation: String,
        inviteCode: String,
    ): LoginResponse = api.register(
        RegisterRequest(
            name = name,
            email = email,
            password = password,
            passwordConfirmation = confirmation,
            inviteCode = inviteCode,
        ),
    )

    suspend fun logout() = api.logout()

    suspend fun me(): UserDto? = api.me().user

    suspend fun updateProfile(name: String): UserDto? = api.updateProfile(UpdateProfileRequest(name)).user

    suspend fun changePassword(current: String, new: String, confirmation: String) =
        api.changePassword(ChangePasswordRequest(current, new, confirmation))

    suspend fun categoryList(): CategoryListResponse = api.categories()

    suspend fun appVersion(): AppVersionResponse = api.appVersion()

    suspend fun downloadApk(url: String): ResponseBody = api.downloadApk(url)

    suspend fun createCategory(name: String, icon: String?): Category? =
        api.createCategory(CreateCategoryRequest(name = name, icon = icon)).data

    suspend fun updateCategory(id: Int, name: String, icon: String?): Category? =
        api.updateCategory(id, UpdateCategoryRequest(name = name, icon = icon)).data

    suspend fun reorderCategories(slugs: List<String>) =
        api.reorderCategories(CategoryOrderRequest(order = slugs))

    suspend fun restoreCategories() = api.restoreCategories()

    suspend fun deleteCategory(id: Int) = api.deleteCategory(id)

    suspend fun expenses(
        limit: Int = 200,
        from: String? = null,
        to: String? = null,
        before: String? = null,
        beforeId: Int? = null,
    ): List<Expense> = api.expenses(
        limit = limit,
        from = from,
        to = to,
        before = before,
        beforeId = beforeId,
    ).data

    suspend fun addExpense(amount: Long, category: String, note: String?, spentAt: String? = null): Expense? =
        api.createExpense(
            CreateExpenseRequest(amount = amount, category = category, note = note, spentAt = spentAt),
        ).data

    suspend fun updateExpense(id: Int, body: UpdateExpenseRequest): Expense? =
        api.updateExpense(id, body).data

    suspend fun deleteExpense(id: Int) = api.deleteExpense(id)

    suspend fun summary(period: String): Summary = api.summary(period)

    suspend fun settings(): Settings = api.settings().data

    suspend fun saveSettings(settings: Settings): Settings = api.updateSettings(settings).data

    suspend fun exportCsv(from: String, to: String): ResponseBody = api.exportCsv(from, to)

    suspend fun exportPdf(from: String, to: String): ResponseBody = api.exportPdf(from, to)
}
