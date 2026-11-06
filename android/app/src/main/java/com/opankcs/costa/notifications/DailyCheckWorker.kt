package com.opankcs.costa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opankcs.costa.data.CostaRepository
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.data.Summary
import com.opankcs.costa.ui.formatRupiah
import java.time.LocalDate
import java.time.YearMonth

class DailyCheckWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(context)

        try {
            if (prefs.token.isNotBlank()) {
                val repository = CostaRepository(prefs)
                val today = repository.summary("today")
                val month = repository.summary("month")

                if (prefs.notifyDailyReminder && today.count == 0) {
                    CostaNotifications.show(
                        context = context,
                        channel = CostaNotifications.CHANNEL_REMINDER,
                        id = 1001,
                        title = "Belum ada catatan hari ini",
                        text = "Sempatkan catat pengeluaran hari ini sebelum tidur. Ketuk untuk membuka Costa.",
                    )
                }

                if (prefs.notifyBudget) {
                    checkDailyBudget(prefs, today)
                    checkMonthlyBudget(prefs, month)
                    checkFoodBudget(today)
                }
            }
        } catch (_: Exception) {
            // Server might be unreachable at this hour; ignore and try again tomorrow.
        } finally {
            NotificationScheduler.scheduleDailyNext(context)
        }

        return Result.success()
    }

    private fun checkFoodBudget(today: Summary) {
        val food = today.foodBudget ?: return

        if (food.base <= 0L || food.debt <= 0L) return

        CostaNotifications.show(
            context = context,
            channel = CostaNotifications.CHANNEL_BUDGET,
            id = 2101,
            title = "Jatah makan besok turun",
            text = "Kelebihan makan ${formatRupiah(food.debt)}. Jatah besok " +
                "${formatRupiah(food.tomorrowAllowance)} (normal ${formatRupiah(food.base)}/hari).",
        )
    }

    private fun checkDailyBudget(prefs: Prefs, today: Summary) {
        val limit = today.budget.limit ?: 0L
        if (limit <= 0L) return

        val percent = today.total * 100.0 / limit
        val level = when {
            percent >= 100 -> 100
            percent >= 80 -> 80
            else -> 0
        }

        val key = LocalDate.now().toString()
        val previous = prefs.dailyWarningState
        val previousLevel = if (previous.startsWith("$key:")) {
            previous.substringAfter(":").toIntOrNull() ?: 0
        } else {
            0
        }

        if (level > previousLevel) {
            val title = if (level >= 100) {
                "Budget harian terlampaui"
            } else {
                "Budget harian 80% terpakai"
            }
            val text = "Hari ini ${formatRupiah(today.total)} dari ${formatRupiah(limit)} " +
                "(${percent.toInt()}%). Sisa ${formatRupiah(today.budget.remaining)}."

            CostaNotifications.show(
                context = context,
                channel = CostaNotifications.CHANNEL_BUDGET,
                id = 2001,
                title = title,
                text = text,
            )
        }

        prefs.dailyWarningState = "$key:$level"
    }

    private fun checkMonthlyBudget(prefs: Prefs, month: Summary) {
        val limit = month.budget.limit ?: 0L
        if (limit <= 0L) return

        val percent = month.total * 100.0 / limit
        val level = when {
            percent >= 100 -> 100
            percent >= 80 -> 80
            else -> 0
        }

        val key = YearMonth.now().toString()
        val previous = prefs.monthlyWarningState
        val previousLevel = if (previous.startsWith("$key:")) {
            previous.substringAfter(":").toIntOrNull() ?: 0
        } else {
            0
        }

        if (level > previousLevel) {
            val title = if (level >= 100) {
                "Budget bulanan terlampaui"
            } else {
                "Budget bulanan 80% terpakai"
            }
            val text = "Bulan ini ${formatRupiah(month.total)} dari ${formatRupiah(limit)} " +
                "(${percent.toInt()}%). Sisa aman ${formatRupiah(month.month.safeToSpendPerDay)}/hari."

            CostaNotifications.show(
                context = context,
                channel = CostaNotifications.CHANNEL_BUDGET,
                id = 2002,
                title = title,
                text = text,
            )
        }

        prefs.monthlyWarningState = "$key:$level"
    }
}
