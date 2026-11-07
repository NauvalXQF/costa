package com.opankcs.costa.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opankcs.costa.data.CostaRepository
import com.opankcs.costa.R
import com.opankcs.costa.data.Prefs

class DailyCheckWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(context)

        try {
            if (prefs.token.isNotBlank() && prefs.notifyDailyReminder) {
                val today = CostaRepository(prefs).summary("today")

                if (today.count == 0) {
                    CostaNotifications.show(
                        context = context,
                        channel = CostaNotifications.CHANNEL_REMINDER,
                        id = 1001,
                        title = context.getString(R.string.notif_daily_title),
                        text = context.getString(R.string.notif_daily_text),
                    )
                }
            }
        } catch (_: Exception) {
            // Server might be unreachable; try again at the next reminder slot.
        } finally {
            NotificationScheduler.scheduleDailyNext(context)
        }

        return Result.success()
    }
}
