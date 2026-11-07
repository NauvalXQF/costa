package com.opankcs.costa.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val DAILY_WORK = "costa_daily_check"
    private const val WEEKLY_WORK = "costa_weekly_recap"
    private const val MONTHLY_WORK = "costa_monthly_reset"
    private val reminderHours = listOf(9, 12, 21)

    fun schedule(context: Context) {
        CostaNotifications.ensureChannels(context.applicationContext)
        scheduleDailyNext(context)
        scheduleWeeklyNext(context)
        scheduleMonthlyNext(context)
    }

    fun scheduleDailyNext(context: Context) {
        val delay = nextReminderDelay()
        val request = OneTimeWorkRequestBuilder<DailyCheckWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(DAILY_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleWeeklyNext(context: Context) {
        val delay = nextWeeklyDelay(DayOfWeek.SUNDAY, 21, 30)
        val request = OneTimeWorkRequestBuilder<WeeklyRecapWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(WEEKLY_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleMonthlyNext(context: Context) {
        val delay = nextMonthlyDelay(8, 0)
        val request = OneTimeWorkRequestBuilder<MonthlyResetWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(MONTHLY_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    private fun nextReminderDelay(): Long {
        val now = LocalDateTime.now()

        for (hour in reminderHours) {
            val target = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
            if (target.isAfter(now)) {
                return Duration.between(now, target).toMillis()
            }
        }

        val tomorrow = now.toLocalDate().plusDays(1).atTime(9, 0)
        return Duration.between(now, tomorrow).toMillis()
    }

    private fun nextWeeklyDelay(day: DayOfWeek, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.with(TemporalAdjusters.nextOrSame(day))
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusWeeks(1)
        }
        return Duration.between(now, target).toMillis()
    }

    private fun nextMonthlyDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withDayOfMonth(1)
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusMonths(1).withDayOfMonth(1)
        }
        return Duration.between(now, target).toMillis()
    }
}
