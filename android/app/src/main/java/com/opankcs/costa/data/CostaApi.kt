package com.opankcs.costa.data

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface CostaApi {
    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @GET("api/app-version")
    suspend fun appVersion(): AppVersionResponse

    @Streaming
    @GET
    suspend fun downloadApk(@Url url: String): ResponseBody

    @POST("api/logout")
    suspend fun logout()

    @GET("api/me")
    suspend fun me(): MeResponse

    @PUT("api/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): UserResponse

    @POST("api/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @GET("api/categories")
    suspend fun categories(): CategoryListResponse

    @POST("api/categories")
    suspend fun createCategory(@Body body: CreateCategoryRequest): SingleCategoryResponse

    @PUT("api/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body body: UpdateCategoryRequest,
    ): SingleCategoryResponse

    @PUT("api/categories/order")
    suspend fun reorderCategories(@Body body: CategoryOrderRequest): CategoryOrderResponse

    @POST("api/categories/restore")
    suspend fun restoreCategories()

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int)

    @GET("api/expenses")
    suspend fun expenses(
        @Query("limit") limit: Int = 200,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("before") before: String? = null,
        @Query("before_id") beforeId: Int? = null,
    ): ExpenseListResponse

    @POST("api/expenses")
    suspend fun createExpense(@Body body: CreateExpenseRequest): SingleExpenseResponse

    @PATCH("api/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") id: Int,
        @Body body: UpdateExpenseRequest,
    ): SingleExpenseResponse

    @DELETE("api/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: Int)

    @GET("api/summary")
    suspend fun summary(@Query("period") period: String = "today"): Summary

    @GET("api/settings")
    suspend fun settings(): SettingsResponse

    @PUT("api/settings")
    suspend fun updateSettings(@Body body: UpdateSettingsRequest): SettingsResponse

    @Streaming
    @GET("api/export/csv")
    suspend fun exportCsv(
        @Query("from") from: String,
        @Query("to") to: String,
    ): ResponseBody

    @Streaming
    @GET("api/export/pdf")
    suspend fun exportPdf(
        @Query("from") from: String,
        @Query("to") to: String,
    ): ResponseBody
}
