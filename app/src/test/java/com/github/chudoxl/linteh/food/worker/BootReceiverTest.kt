package com.github.chudoxl.linteh.food.worker

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * prod.md §6.1, §11 — после BOOT_COMPLETED и MY_PACKAGE_REPLACED расписания восстанавливаются.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BootReceiverTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        mockkObject(BalanceCheckScheduler)
        every { BalanceCheckScheduler.scheduleAll(any()) } answers {}
    }

    @After
    fun tearDown() {
        unmockkObject(BalanceCheckScheduler)
    }

    /** §6.1 / §11 — BOOT_COMPLETED пересоздаёт расписания для всех учеников. */
    @Test
    fun boot_completed_reschedules_all() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        BootReceiver().onReceive(context, intent)
        verify { BalanceCheckScheduler.scheduleAll(context) }
    }

    /** §6.1 / §11 — MY_PACKAGE_REPLACED после обновления приложения пересоздаёт расписания. */
    @Test
    fun package_replaced_reschedules_all() {
        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)
        BootReceiver().onReceive(context, intent)
        verify { BalanceCheckScheduler.scheduleAll(context) }
    }

    /** Прочие Intent'ы BootReceiver игнорирует. */
    @Test
    fun unrelated_action_does_not_reschedule() {
        BootReceiver().onReceive(context, Intent("something.else"))
        verify(exactly = 0) { BalanceCheckScheduler.scheduleAll(any()) }
    }
}
