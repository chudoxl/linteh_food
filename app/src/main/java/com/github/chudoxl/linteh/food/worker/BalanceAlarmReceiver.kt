package com.github.chudoxl.linteh.food.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class BalanceAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cardNumber = intent.getStringExtra(BalanceCheckScheduler.KEY_CARD_NUMBER) ?: return

        val request = OneTimeWorkRequestBuilder<BalanceCheckWorker>()
            .setInputData(workDataOf(BalanceCheckScheduler.KEY_CARD_NUMBER to cardNumber))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "balance_check_$cardNumber",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
