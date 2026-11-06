package com.opankcs.costa.data

import com.google.gson.annotations.SerializedName

data class Category(
    val id: Int,
    @SerializedName("user_id") val userId: Int? = null,
    val name: String,
    val slug: String,
    val icon: String? = null,
    val color: String? = null,
)

data class CategoryListResponse(
    val data: List<Category> = emptyList(),
    val hidden: List<String> = emptyList(),
)

data class SingleCategoryResponse(
    val data: Category? = null,
)

data class CreateCategoryRequest(
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val aliases: List<String>? = null,
)

data class UpdateCategoryRequest(
    val name: String? = null,
    val icon: String? = null,
)

data class CategoryOrderRequest(
    val order: List<String>,
)

data class CategoryOrderResponse(
    val data: List<String> = emptyList(),
)

data class Expense(
    val id: Int,
    val amount: Long,
    val note: String? = null,
    @SerializedName("spent_at") val spentAt: String? = null,
    val source: String? = null,
    val category: Category? = null,
)

data class ExpenseListResponse(
    val data: List<Expense> = emptyList(),
)

data class SingleExpenseResponse(
    val data: Expense? = null,
)

data class CreateExpenseRequest(
    val amount: Long,
    val category: String,
    val note: String? = null,
    @SerializedName("spent_at") val spentAt: String? = null,
    val source: String = "android",
)

data class UpdateExpenseRequest(
    val amount: Long? = null,
    val category: String? = null,
    val note: String? = null,
    @SerializedName("spent_at") val spentAt: String? = null,
)

data class CategoryTotal(
    val slug: String? = null,
    val name: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val total: Long = 0,
    val count: Int = 0,
)

data class BudgetStatus(
    val limit: Long? = null,
    val spent: Long = 0,
    val remaining: Long? = null,
    val percent: Double? = null,
    val exceeded: Boolean = false,
)

data class MonthStatus(
    val budget: Long? = null,
    val spent: Long = 0,
    val remaining: Long? = null,
    @SerializedName("safe_to_spend_per_day") val safeToSpendPerDay: Long? = null,
)

data class Summary(
    val total: Long = 0,
    val count: Int = 0,
    @SerializedName("by_category") val byCategory: List<CategoryTotal> = emptyList(),
    val budget: BudgetStatus = BudgetStatus(),
    val month: MonthStatus = MonthStatus(),
)

data class Settings(
    @SerializedName("daily_budget") val dailyBudget: Long = 0,
    @SerializedName("weekly_budget") val weeklyBudget: Long = 0,
    @SerializedName("monthly_budget") val monthlyBudget: Long = 0,
)

data class SettingsResponse(
    val data: Settings = Settings(),
)

data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "android",
)

data class LoginResponse(
    val token: String,
    val user: UserDto? = null,
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("invite_code") val inviteCode: String,
    @SerializedName("device_name") val deviceName: String = "android",
)

data class MeResponse(
    val user: UserDto? = null,
)

data class UserResponse(
    val user: UserDto? = null,
)

data class UpdateProfileRequest(
    val name: String,
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
    @SerializedName("new_password_confirmation") val newPasswordConfirmation: String,
)
