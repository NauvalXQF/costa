package com.opankcs.costa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opankcs.costa.data.Prefs

class MonthlyResetWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(context)

        if (prefs.notifyMonthlyReset) {
            CostaNotifications.show(
                context = context,
                channel = CostaNotifications.CHANNEL_REMINDER,
                id = 4001,
                title = "Awal bulan baru",
                text = "Saatnya atur budget bulan ini di tab Atur. Ketuk untuk membuka Costa.",
            )
        }

        NotificationScheduler.scheduleMonthlyNext(context)

        return Result.success()
    }
}
