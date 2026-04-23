package com.github.chudoxl.linteh.food.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.model.Student
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object BalanceCheckScheduler {

    const val KEY_CARD_NUMBER = "card_number"

    fun schedule(context: Context, student: Student) {
        if (!student.notificationEnabled) return

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        val now = System.currentTimeMillis()
        val triggerAtMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, student.notificationTime.hour)
            set(Calendar.MINUTE, student.notificationTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        val pendingIntent = createPendingIntent(context, student.cardNumber)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(context: Context, cardNumber: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = existingPendingIntent(context, cardNumber) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleAll(context: Context) {
        val app = context.applicationContext as App
        CoroutineScope(Dispatchers.IO).launch {
            app.repository.getAllOnce()
                .filter { it.notificationEnabled }
                .forEach { schedule(context, it) }
        }
    }

    private fun createPendingIntent(context: Context, cardNumber: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            cardNumber.hashCode(),
            buildIntent(context, cardNumber),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun existingPendingIntent(context: Context, cardNumber: String): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            cardNumber.hashCode(),
            buildIntent(context, cardNumber),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )

    private fun buildIntent(context: Context, cardNumber: String): Intent =
        Intent(context, BalanceAlarmReceiver::class.java).apply {
            putExtra(KEY_CARD_NUMBER, cardNumber)
        }
}
