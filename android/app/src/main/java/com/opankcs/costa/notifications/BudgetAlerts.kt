package com.opankcs.costa.notifications

import android.content.Context
import com.opankcs.costa.R
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.data.Summary
import com.opankcs.costa.ui.formatRupiah
import java.time.LocalDate
import java.time.YearMonth

object BudgetAlerts {
    fun check(context: Context, prefs: Prefs, today: Summary, month: Summary) {
        if (!prefs.notifyBudget) return

        checkDailyBudget(context, prefs, today)
        checkMonthlyBudget(context, prefs, month)
        checkFoodBudget(context, prefs, today)
    }

    private fun checkFoodBudget(context: Context, prefs: Prefs, today: Summary) {
        val food = today.foodBudget ?: return

        if (food.base <= 0L || food.debt <= 0L) return

        val key = LocalDate.now().toString()
        if (prefs.foodWarningState == key) return
        prefs.foodWarningState = key

        CostaNotifications.show(
            context = context,
            channel = CostaNotifications.CHANNEL_BUDGET,
            id = 2101,
            title = context.getString(R.string.notif_food_title),
            text = context.getString(
                R.string.notif_food_text,
                formatRupiah(food.debt),
                formatRupiah(food.tomorrowAllowance),
                formatRupiah(food.base),
            ),
        )
    }

    private fun checkDailyBudget(context: Context, prefs: Prefs, today: Summary) {
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
                context.getString(R.string.notif_daily_100_title)
            } else {
                context.getString(R.string.notif_daily_80_title)
            }
            val text = context.getString(
                R.string.notif_daily_budget_text,
                formatRupiah(today.total),
                formatRupiah(limit),
                percent.toInt(),
                formatRupiah(today.budget.remaining),
            )

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

    private fun checkMonthlyBudget(context: Context, prefs: Prefs, month: Summary) {
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
                context.getString(R.string.notif_monthly_100_title)
            } else {
                context.getString(R.string.notif_monthly_80_title)
            }
            val text = context.getString(
                R.string.notif_monthly_budget_text,
                formatRupiah(month.total),
                formatRupiah(limit),
                percent.toInt(),
                formatRupiah(month.month.safeToSpendPerDay),
            )

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
