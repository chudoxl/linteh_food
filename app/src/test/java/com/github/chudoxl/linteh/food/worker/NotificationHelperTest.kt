package com.github.chudoxl.linteh.food.worker

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * prod.md §7.2, §7.3, §7.4 — канал «Напоминания о балансе» и уведомления.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationHelperTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun grantPostNotifications() {
        // На API 33+ показ уведомлений требует runtime-разрешения.
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** §7.3 — канал «Напоминания о балансе» создан с высокой важностью, вибрацией и мигание индикатора. */
    @Test
    fun createChannel_has_spec_parameters() {
        NotificationHelper.createChannel(context)

        val channel = manager.getNotificationChannel("balance_alerts")
        assertThat(channel).isNotNull()
        assertThat(channel.name.toString()).isEqualTo("Напоминания о балансе")
        assertThat(channel.description).isEqualTo("Уведомления о низком балансе школьного питания")
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        // prod.md §7.3: вибрация — короткий двойной паттерн.
        assertThat(channel.vibrationPattern).isEqualTo(longArrayOf(0, 300, 200, 300))
        assertThat(channel.shouldVibrate()).isTrue()
        assertThat(channel.shouldShowLights()).isTrue()
    }

    /** §7.2 — уведомление о низком балансе имеет заголовок «Низкий баланс: …» и тело «Баланс … ₽». */
    @Test
    fun show_low_balance_notification_sets_title_and_body() {
        NotificationHelper.createChannel(context)
        NotificationHelper.showLowBalanceNotification(
            context = context,
            notificationId = 42,
            studentName = "Анна",
            balance = "150.00",
        )

        val active = manager.activeNotifications
        assertThat(active).hasLength(1)
        val notif = active.single().notification
        // prod.md §7.2 для «низкий баланс».
        assertThat(notif.extras.getString("android.title")).isEqualTo("Низкий баланс: Анна")
        assertThat(notif.extras.getString("android.text")).isEqualTo("Баланс 150.00 ₽")
    }

    /** §7.4 — новое уведомление с тем же notificationId замещает предыдущее. */
    @Test
    fun repeated_notification_for_same_id_is_unique() {
        NotificationHelper.createChannel(context)
        NotificationHelper.showLowBalanceNotification(context, 42, "A", "1.00")
        NotificationHelper.showLowBalanceNotification(context, 42, "B", "2.00")

        // prod.md §7.4: одно активное уведомление на ученика.
        val active = manager.activeNotifications
        assertThat(active).hasLength(1)
        assertThat(active.single().notification.extras.getString("android.title")).isEqualTo("Низкий баланс: B")
    }

    /** §3.7 / §7.5 — cancelAll снимает все активные уведомления приложения. */
    @Test
    fun cancelAll_clears_all_active_notifications() {
        NotificationHelper.createChannel(context)
        NotificationHelper.showLowBalanceNotification(context, 1, "A", "1.00")
        NotificationHelper.showLowBalanceNotification(context, 2, "B", "2.00")

        NotificationHelper.cancelAll(context)

        // prod.md §3.7, §7.5: сброс всех уведомлений приложения.
        assertThat(manager.activeNotifications).isEmpty()
    }
}
