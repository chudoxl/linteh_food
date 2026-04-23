package com.github.chudoxl.linteh.food.worker

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * prod.md §6 — BalanceAlarmReceiver ставит OneTimeWorkRequest с уникальным именем
 * `balance_check_<cardNumber>` и политикой REPLACE.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BalanceAlarmReceiverTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    /** §6 — при получении алярма Receiver ставит работу с уникальным именем `balance_check_<cardNumber>`. */
    @Test
    fun receiver_enqueues_work_with_unique_name() {
        val intent = Intent().putExtra(BalanceCheckScheduler.KEY_CARD_NUMBER, "001")
        BalanceAlarmReceiver().onReceive(context, intent)

        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("balance_check_001")
            .get()

        assertThat(infos).isNotEmpty()
    }

    /** §6 — без cardNumber в Intent Receiver ничего не планирует. */
    @Test
    fun receiver_noops_without_card_number() {
        BalanceAlarmReceiver().onReceive(context, Intent())
        // Нет уникальных задач — имя без cardNumber не должно существовать.
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("balance_check_")
            .get()
        assertThat(infos).isEmpty()
    }
}
