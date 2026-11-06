package com.opankcs.costa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opankcs.costa.data.CostaRepository
import com.opankcs.costa.data.Prefs
import com.opankcs.costa.ui.formatRupiah

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

                val text = buildString {
                    append("Total ${formatRupiah(week.total)} dari ${week.count} transaksi.")
                    if (top != null) {
                        append(" Terbesar: ${top.name ?: "-"} ${formatRupiah(top.total)}.")
                    }
                    append(" Minggu baru sudah bisa dicatat.")
                }

                CostaNotifications.show(
                    context = context,
                    channel = CostaNotifications.CHANNEL_RECAP,
                    id = 3001,
                    title = "Rekap minggu ini",
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
