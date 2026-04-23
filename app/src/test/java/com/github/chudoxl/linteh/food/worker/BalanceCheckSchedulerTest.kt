package com.github.chudoxl.linteh.food.worker

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * prod.md §6.1, §6.2 — расписание автоматических проверок баланса.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BalanceCheckSchedulerTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    /** §6.1 — если указанное время ещё не наступило сегодня, будильник ставится на сегодня. */
    @Test
    fun schedule_plans_for_today_if_time_is_ahead() {
        val future = plusMinutes(fromNow = 5)
        val s = student(
            cardNumber = "001",
            notificationEnabled = true,
            notificationTime = LocalTime(hour = future.first, minute = future.second),
        )
        BalanceCheckScheduler.schedule(context, s)

        val scheduled = shadowOf(alarmManager).scheduledAlarms
        assertThat(scheduled).isNotEmpty()
        // Не далее чем через сутки.
        val delta = scheduled.first().triggerAtTime - System.currentTimeMillis()
        assertThat(delta).isLessThan(24 * 60 * 60 * 1000L)
    }

    /** §6.1 — если время уже прошло, первая проверка планируется на завтра (без внеочередной). */
    @Test
    fun schedule_plans_for_tomorrow_if_time_has_passed() {
        // время, гарантированно прошедшее: минута назад.
        val past = plusMinutes(fromNow = -1)
        val s = student(
            cardNumber = "002",
            notificationEnabled = true,
            notificationTime = LocalTime(hour = past.first, minute = past.second),
        )
        BalanceCheckScheduler.schedule(context, s)

        val scheduled = shadowOf(alarmManager).scheduledAlarms
        val delta = scheduled.last().triggerAtTime - System.currentTimeMillis()
        // prod.md §6.1: если время прошло, планируется на завтра.
        // Проверяем, что trigger > 23 часов впереди.
        assertThat(delta).isGreaterThan(23 * 60 * 60 * 1000L)
    }

    /** §6.1 — если у ученика напоминания выключены, расписание не создаётся. */
    @Test
    fun schedule_skipped_if_notifications_disabled() {
        val s = student(cardNumber = "003", notificationEnabled = false)
        BalanceCheckScheduler.schedule(context, s)

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    /** §6.1 — cancel удаляет ранее запланированный будильник по cardNumber. */
    @Test
    fun cancel_removes_scheduled_alarm() {
        val s = student(
            cardNumber = "cancel-me",
            notificationEnabled = true,
            notificationTime = LocalTime(hour = plusMinutes(5).first, minute = plusMinutes(5).second),
        )
        BalanceCheckScheduler.schedule(context, s)
        assertThat(shadowOf(alarmManager).scheduledAlarms).isNotEmpty()

        BalanceCheckScheduler.cancel(context, "cancel-me")

        assertThat(shadowOf(alarmManager).scheduledAlarms).isEmpty()
    }

    private fun plusMinutes(fromNow: Int): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, fromNow) }
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }
}
