package com.github.chudoxl.linteh.food.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.model.BalanceState

class BalanceCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val repository = app.repository
        val cardNumber = inputData.getString(BalanceCheckScheduler.KEY_CARD_NUMBER) ?: return Result.failure()

        NotificationHelper.createChannel(applicationContext)

        val student = repository.getByCardNumber(cardNumber) ?: return Result.failure()
        if (!student.notificationEnabled) return Result.success()

        val updated = repository.updateBalance(student = student)
        val errorState = updated.balanceState as? BalanceState.Error
        val studentName = updated.name.ifBlank { "Ученик" }
        val notificationId = student.cardNumber.hashCode()

        when {
            errorState != null -> NotificationHelper.showErrorNotification(
                context = applicationContext,
                notificationId = notificationId,
                studentName = studentName,
                errorMessage = errorState.errMsg,
            )
            // prod.md §4.4 + §7.1: строгое `balance < threshold` без спецслучаев.
            // При threshold == 0 уведомление приходит только для отрицательного баланса;
            // отрицательные пороги обрабатываются этим же правилом.
            updated.balance < student.notificationThreshold -> NotificationHelper.showLowBalanceNotification(
                context = applicationContext,
                notificationId = notificationId,
                studentName = studentName,
                balance = updated.balance.toPlainString(),
            )
            else -> NotificationHelper.cancel(applicationContext, notificationId)
        }

        BalanceCheckScheduler.schedule(applicationContext, student)

        return Result.success()
    }
}
