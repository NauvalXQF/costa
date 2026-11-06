package com.opankcs.costa.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.opankcs.costa.MainActivity
import com.opankcs.costa.R

object CostaNotifications {
    const val CHANNEL_REMINDER = "costa_reminder"
    const val CHANNEL_BUDGET = "costa_budget"
    const val CHANNEL_RECAP = "costa_recap"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDER,
                    "Pengingat harian",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Pengingat mencatat pengeluaran"
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_BUDGET,
                    "Peringatan budget",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Peringatan saat budget hampir habis atau terlampaui"
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_RECAP,
                    "Rekap mingguan",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Ringkasan pengeluaran mingguan"
                },
            )
        }
    }

    fun show(context: Context, channel: String, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_costa)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
