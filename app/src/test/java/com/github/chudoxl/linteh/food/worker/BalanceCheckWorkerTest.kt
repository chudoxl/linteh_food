package com.github.chudoxl.linteh.food.worker

import android.Manifest
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.github.chudoxl.linteh.food.data.StudentRepository
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.math.BigDecimal

/**
 * prod.md §7.1, §7.5, §6.3 — поведение автоматической проверки баланса.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class BalanceCheckWorkerTest {

    private val app: TestApp get() = ApplicationProvider.getApplicationContext()
    private val manager: NotificationManager
        get() = app.getSystemService(NotificationManager::class.java)

    private lateinit var repo: StudentRepository

    @Before
    fun setUp() {
        repo = mockk(relaxed = true)
        app.inject(repo)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        mockkObject(BalanceCheckScheduler)
        every { BalanceCheckScheduler.schedule(any(), any()) } just Runs
        every { BalanceCheckScheduler.cancel(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(BalanceCheckScheduler)
    }

    /** §7.1 п.1 — баланс меньше порога → показывается уведомление. */
    @Test
    fun low_balance_triggers_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(balance = BigDecimal("150.00"), balanceState = BalanceState.Loaded)

        val result = buildWorker("001").doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(manager.activeNotifications).hasLength(1)
    }

    /** §7.1 — баланс ≥ порога и без ошибок → уведомление не показывается. */
    @Test
    fun healthy_balance_does_not_show_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(balance = BigDecimal("500.00"), balanceState = BalanceState.Loaded)

        buildWorker("001").doWork()

        // prod.md §7.1: баланс ≥ порога и нет ошибок → уведомление не показывается.
        assertThat(manager.activeNotifications).isEmpty()
    }

    /** §7.1 п.2 / §7.2 — ошибка запроса → уведомление «Не удалось проверить баланс: …». */
    @Test
    fun error_balance_shows_error_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(
            balance = BigDecimal("300.00"),
            balanceState = BalanceState.Error("no network"),
        )

        buildWorker("001").doWork()

        // prod.md §7.1 п.2: ошибка запроса → уведомление.
        assertThat(manager.activeNotifications).hasLength(1)
        // prod.md §7.2: заголовок «Не удалось проверить баланс…».
        val title = manager.activeNotifications.single().notification.extras.getString("android.title")
        assertThat(title).contains("Не удалось")
    }

    /** §7.1 — при выключенных напоминаниях уведомления не показываются ни в каком случае. */
    @Test
    fun disabled_notifications_never_show_even_for_error() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = false)
        coEvery { repo.getByCardNumber("001") } returns s

        val result = buildWorker("001").doWork()

        // prod.md §7.1 финал: если напоминания выключены — уведомления не показываются ни в каком случае.
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(manager.activeNotifications).isEmpty()
    }

    /** §4.4 + §7.1 — при пороге 0 уведомление приходит, если баланс строго отрицательный. */
    @Test
    fun zero_threshold_negative_balance_triggers_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal.ZERO)
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(
            balance = BigDecimal("-5.00"),
            balanceState = BalanceState.Loaded,
        )

        buildWorker("001").doWork()

        // prod.md §4.4 + §7.1: порог 0 ⇒ уведомление приходит только при отрицательном балансе.
        assertThat(manager.activeNotifications).hasLength(1)
    }

    /** §7.1 — сравнение строгое: при balance == threshold уведомление не шлётся (граница, threshold=0). */
    @Test
    fun zero_threshold_zero_balance_does_not_trigger_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal.ZERO)
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(balance = BigDecimal.ZERO, balanceState = BalanceState.Loaded)

        buildWorker("001").doWork()

        // prod.md §7.1: строгое `balance < threshold` ⇒ при 0 == 0 уведомление не отправляется.
        assertThat(manager.activeNotifications).isEmpty()
    }

    /** §4.4 + §7.1 — отрицательный порог работает по тому же правилу: balance строго меньше threshold ⇒ уведомление. */
    @Test
    fun negative_threshold_balance_below_triggers_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal("-5"))
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(
            balance = BigDecimal("-10.00"),
            balanceState = BalanceState.Loaded,
        )

        buildWorker("001").doWork()

        // prod.md §7.1: единообразное правило `balance < threshold` для любых значений порога.
        assertThat(manager.activeNotifications).hasLength(1)
    }

    /** §7.1 — строгое сравнение: отрицательный порог при равном балансе не триггерит уведомление. */
    @Test
    fun negative_threshold_balance_equal_does_not_trigger_notification() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal("-5"))
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(
            balance = BigDecimal("-5.00"),
            balanceState = BalanceState.Loaded,
        )

        buildWorker("001").doWork()

        // prod.md §7.1: строгое `<` ⇒ при balance == threshold уведомление не шлётся.
        assertThat(manager.activeNotifications).isEmpty()
    }

    /** §6.3 — после выполнения проверки Worker перепланирует расписание на следующий день. */
    @Test
    fun after_check_next_alarm_is_rescheduled() = runBlocking {
        val s = student(cardNumber = "001", notificationEnabled = true)
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(balance = BigDecimal("500.00"), balanceState = BalanceState.Loaded)

        buildWorker("001").doWork()

        // prod.md §6.3: после проверки расписание переводится на следующий день.
        io.mockk.verify { BalanceCheckScheduler.schedule(any(), match { it.cardNumber == "001" }) }
    }

    /** §7.5 — если очередная проверка вернула баланс ≥ порога, активное уведомление отменяется. */
    @Test
    fun healthy_balance_cancels_previous_active_notification() = runBlocking {
        NotificationHelper.createChannel(app)
        NotificationHelper.showLowBalanceNotification(app, notificationId = "001".hashCode(), studentName = "A", balance = "0")
        assertThat(manager.activeNotifications).hasLength(1)

        val s = student(cardNumber = "001", notificationEnabled = true, notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("001") } returns s
        coEvery { repo.updateBalance(any()) } returns s.copy(balance = BigDecimal("500.00"), balanceState = BalanceState.Loaded)

        buildWorker("001").doWork()

        // prod.md §7.5: если очередная проверка вернула баланс ≥ порога и без ошибок —
        // активное уведомление автоматически отменяется.
        assertThat(manager.activeNotifications).isEmpty()
    }

    // --- helpers ---------------------------------------------------------

    private fun buildWorker(cardNumber: String): BalanceCheckWorker =
        TestListenableWorkerBuilder<BalanceCheckWorker>(
            context = app,
            inputData = workDataOf(BalanceCheckScheduler.KEY_CARD_NUMBER to cardNumber),
        )
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: android.content.Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker? = BalanceCheckWorker(appContext, workerParameters)
            })
            .build()
}
