package com.opankcs.costa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opankcs.costa.data.CostaRepository
import com.opankcs.costa.R
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.ui.formatRupiah
import com.opankcs.costa.ui.localizedCategoryName

class WeeklyRecapWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(context)

        try {
            if (prefs.token.isNotBlank() && prefs.notifyWeeklyRecap) {
                val week = CostaRepository(prefs).summary("week")
                val top = week.byCategory.maxByOrNull { it.total }

                val topName = top?.let { localizedCategoryName(context, it) }
                val text = if (top != null) {
                    context.getString(
                        R.string.notif_weekly_text,
                        formatRupiah(week.total),
                        week.count,
                        topName,
                        formatRupiah(top.total),
                    )
                } else {
                    context.getString(
                        R.string.notif_weekly_text_no_top,
                        formatRupiah(week.total),
                        week.count,
                    )
                }

                CostaNotifications.show(
                    context = context,
                    channel = CostaNotifications.CHANNEL_RECAP,
                    id = 3001,
                    title = context.getString(R.string.notif_weekly_title),
                    text = text,
                )
            }
        } catch (_: Exception) {
            // Ignore network issues and try again next week.
        } finally {
            NotificationScheduler.scheduleWeeklyNext(context)
        }

        return Result.success()
    }
}
