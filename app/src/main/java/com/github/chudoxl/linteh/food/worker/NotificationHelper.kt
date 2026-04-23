package com.github.chudoxl.linteh.food.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.chudoxl.linteh.food.MainActivity
import com.github.chudoxl.linteh.food.R

object NotificationHelper {
    private const val CHANNEL_ID = "balance_alerts"

    fun createChannel(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания о балансе",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о низком балансе школьного питания"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                audioAttributes,
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 200, 300)
            enableLights(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showLowBalanceNotification(
        context: Context,
        notificationId: Int,
        studentName: String,
        balance: String,
    ) {
        postNotification(
            context = context,
            notificationId = notificationId,
            title = "Низкий баланс: $studentName",
            text = "Баланс $balance ₽",
        )
    }

    fun showErrorNotification(
        context: Context,
        notificationId: Int,
        studentName: String,
        errorMessage: String?,
    ) {
        postNotification(
            context = context,
            notificationId = notificationId,
            title = "Не удалось проверить баланс: $studentName",
            text = errorMessage?.takeIf { it.isNotBlank() } ?: "Неизвестная ошибка",
        )
    }

    private fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(notificationId)
        notificationManager.notify(notificationId, notification)
    }

    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
