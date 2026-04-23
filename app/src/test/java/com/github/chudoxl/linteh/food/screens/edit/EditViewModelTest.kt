package com.github.chudoxl.linteh.food.screens.edit

import app.cash.turbine.test
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.data.StudentRepository
import com.github.chudoxl.linteh.food.data.network.BalanceResponse
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.screens.edit.data.EditScreenState
import com.github.chudoxl.linteh.food.testutil.MainDispatcherRule
import com.github.chudoxl.linteh.food.testutil.student
import com.github.chudoxl.linteh.food.worker.BalanceCheckScheduler
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/**
 * prod.md §4.1—§4.6 — экран добавления/редактирования.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var app: App
    private lateinit var repo: StudentRepository

    @Before
    fun setUp() {
        app = mockk<App>(relaxed = true)
        repo = mockk<StudentRepository>(relaxed = true)
        every { app.repository } returns repo
        mockkObject(BalanceCheckScheduler)
        every { BalanceCheckScheduler.schedule(any(), any()) } just Runs
        every { BalanceCheckScheduler.cancel(any(), any()) } just Runs
        every { BalanceCheckScheduler.scheduleAll(any()) } just Runs
        // По умолчанию локальная БД пустая — чтобы guard §4.2 не срабатывал
        // в тестах, не касающихся сценария дубликата.
        coEvery { repo.getByCardNumber(any()) } returns null
    }

    /**
     * В режиме редактирования init ViewModel'и запускает загрузку на Dispatchers.IO
     * (не на test-диспатчере), поэтому advanceUntilIdle не ждёт этого запуска.
     * Ждём явно первого Ready-состояния.
     */
    private suspend fun EditViewModel.awaitReady(): EditScreenState.Ready =
        state.first { it is EditScreenState.Ready } as EditScreenState.Ready

    @After
    fun tearDown() {
        unmockkObject(BalanceCheckScheduler)
    }

    // --- add mode -------------------------------------------------------

    /** §4.1 — в режиме добавления VM сразу в состоянии Ready с пустым Student. */
    @Test
    fun add_mode_state_is_ready_with_empty_student() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.state.test {
            val initial = awaitItem()
            assertThat(initial).isInstanceOf(EditScreenState.Ready::class.java)
            val ready = initial as EditScreenState.Ready
            assertThat(ready.current).isEqualTo(Student.Empty)
            assertThat(ready.isEditing).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** §4.2 — кнопка «Проверить» активна только если номер карты и пароль заполнены. */
    @Test
    fun add_mode_can_check_only_when_card_and_password_filled() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).canCheck).isFalse()

        vm.uiListener.updatePassword("pwd")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).canCheck).isTrue()
    }

    /** §4.5 — canSave остаётся false до успешной проверки; после Success становится true. */
    @Test
    fun add_mode_can_save_false_until_successful_check() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).canSave).isFalse()

        coEvery { repo.checkBalance("001", "pwd") } returns BalanceResponse("Иванов", BigDecimal("123.45"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.canSave).isTrue()
        assertThat(ready.checkState).isInstanceOf(BalanceCheckState.Success::class.java)
    }

    /** §4.3 — при пустом имени и ФИО из трёх слов автозаполняется вторым словом. */
    @Test
    fun successful_check_autofills_blank_name_with_second_word_if_3_words() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иванов Иван Иванович", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        // prod.md §4.3: имя заполняется вторым словом ФИО при пустом поле.
        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.name).isEqualTo("Иван")
    }

    /** §4.3 — при пустом имени и ФИО из двух слов автозаполняется вторым словом. */
    @Test
    fun successful_check_autofills_blank_name_with_second_word_if_2_words() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иванов Иван", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        // prod.md §4.3: имя заполняется вторым словом ФИО при пустом поле.
        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.name).isEqualTo("Иван")
    }

    /** §4.3 — при пустом имени и ФИО из одного слова автозаполняется этим словом. */
    @Test
    fun successful_check_autofills_blank_name_with_first_word_if_1_words() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иван", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        // prod.md §4.3: имя заполняется первым словом ФИО при пустом поле и единственным словом в ФИО.
        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.name).isEqualTo("Иван")
    }

    /** §4.3 — если пользователь уже ввёл имя, автозаполнение его не перезаписывает. */
    @Test
    fun successful_check_does_not_overwrite_user_entered_name() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateName("Анна")
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иванов Иван", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.name).isEqualTo("Анна")
    }

    /** §4.2 — при ошибке сети checkState становится Error с текстом сообщения, canSave остаётся false. */
    @Test
    fun check_error_sets_error_state_with_message() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } throws IOException("no network")
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.checkState).isEqualTo(BalanceCheckState.Error("no network"))
        assertThat(ready.canSave).isFalse()
    }

    /** §4.5 п.3 — смена пароля после успешной проверки сбрасывает canSave в false. */
    @Test
    fun editing_card_or_password_after_success_invalidates_check() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иванов", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).canSave).isTrue()

        // prod.md §4.5, п. 3: после изменения номера/пароля canSave должен стать false.
        vm.uiListener.updatePassword("new-pwd")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).canSave).isFalse()
    }

    /** §4.5 / §6.1 — save() в add-режиме делает insert и планирует проверку при включённых напоминаниях. */
    @Test
    fun save_in_add_mode_inserts_and_schedules_when_enabled() = runTest {
        coEvery { repo.insert(any()) } returns 1L
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иванов", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        var done = false
        vm.uiListener.save { done = true }
        advanceUntilIdle()

        assertThat(done).isTrue()
        coVerify { repo.insert(match { it.cardNumber == "001" }) }
        io.mockk.verify { BalanceCheckScheduler.schedule(app, match { it.cardNumber == "001" }) }
    }

    /** §6.1 — при выключенных напоминаниях save() не создаёт расписание. */
    @Test
    fun save_in_add_mode_does_not_schedule_when_notifications_disabled() = runTest {
        coEvery { repo.insert(any()) } returns 1L
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        vm.uiListener.updateNotificationEnabled(false)
        coEvery { repo.checkBalance(any(), any()) } returns BalanceResponse("Иванов", BigDecimal("1.00"))
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        vm.uiListener.save {}
        advanceUntilIdle()

        io.mockk.verify(exactly = 0) { BalanceCheckScheduler.schedule(any(), any()) }
    }

    /** §4.6 — в режиме добавления любое заполненное поле делает hasUnsavedChanges=true. */
    @Test
    fun has_unsaved_changes_in_add_mode_follows_any_text_entry() = runTest {
        val vm = EditViewModel(app, cardNumber = null)
        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isFalse()

        vm.uiListener.updateName("Анна")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isTrue()

        vm.uiListener.updateName("")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isFalse()
    }

    // --- duplicate card guard -------------------------------------------
    // prod.md §4.2: «Проверка уникальности номера карты (только режим добавления)».

    /** §4.2 — в add-режиме при существующем номере карты диалог «дубликат» показывается без похода в сеть. */
    @Test
    fun add_mode_check_with_existing_card_shows_duplicate_dialog_without_network() = runTest {
        val existing = student(cardNumber = "001", name = "Иван")
        coEvery { repo.getByCardNumber("001") } returns existing
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        advanceUntilIdle()

        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.showDuplicateDialog).isTrue()
        assertThat(ready.checkState).isEqualTo(BalanceCheckState.Idle)
        coVerify(exactly = 0) { repo.checkBalance(any(), any()) }
    }

    /** §4.2 — после диалога дубликата форма остаётся в Idle и canSave=false. */
    @Test
    fun add_mode_check_with_existing_card_keeps_form_locked() = runTest {
        val existing = student(cardNumber = "001")
        coEvery { repo.getByCardNumber("001") } returns existing
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.canSave).isFalse()
        assertThat(ready.checkState).isEqualTo(BalanceCheckState.Idle)
    }

    /** §4.2 — закрытие диалога дубликата сбрасывает флаг и возвращает форму к исходному состоянию. */
    @Test
    fun add_mode_dismiss_duplicate_dialog_resets_flag() = runTest {
        val existing = student(cardNumber = "001")
        coEvery { repo.getByCardNumber("001") } returns existing
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")
        vm.uiListener.checkBalance()
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).showDuplicateDialog).isTrue()

        vm.uiListener.onDuplicateDialogDismissed()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.showDuplicateDialog).isFalse()
        assertThat(ready.checkState).isEqualTo(BalanceCheckState.Idle)
        assertThat(ready.current.cardNumber).isEqualTo("001")
        assertThat(ready.current.password).isEqualTo("pwd")
    }

    /** §4.2 — если дубликата нет, проверка в add-режиме идёт в сеть и приходит Success. */
    @Test
    fun add_mode_check_without_duplicate_proceeds_to_network() = runTest {
        coEvery { repo.getByCardNumber("001") } returns null
        coEvery { repo.checkBalance("001", "pwd") } returns BalanceResponse("Иванов Иван", BigDecimal("123.45"))
        val vm = EditViewModel(app, cardNumber = null)
        vm.uiListener.updateCardNumber("001")
        vm.uiListener.updatePassword("pwd")

        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.showDuplicateDialog).isFalse()
        assertThat(ready.checkState).isInstanceOf(BalanceCheckState.Success::class.java)
        coVerify(exactly = 1) { repo.checkBalance("001", "pwd") }
    }

    /** §4.2 — в edit-режиме проверка не проверяет на дубликат и идёт прямо в сеть. */
    @Test
    fun edit_mode_check_does_not_trigger_duplicate_dialog() = runTest {
        val existing = student(cardNumber = "999", password = "old")
        coEvery { repo.getByCardNumber("999") } returns existing
        coEvery { repo.checkBalance("999", "new") } returns BalanceResponse("Иванов Иван", BigDecimal("1.00"))
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updatePassword("new")
        vm.uiListener.checkBalance()
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.showDuplicateDialog).isFalse()
        assertThat(ready.checkState).isInstanceOf(BalanceCheckState.Success::class.java)
        coVerify(atLeast = 1) { repo.checkBalance("999", "new") }
    }

    // --- edit mode ------------------------------------------------------

    /** §4.1 — в edit-режиме VM сначала в Loading, затем Ready с загруженным Student. */
    @Test
    fun edit_mode_starts_loading_then_ready_with_fetched_student() = runTest {
        val existing = student(cardNumber = "999", name = "Пётр")
        coEvery { repo.getByCardNumber("999") } returns existing

        val vm = EditViewModel(app, cardNumber = "999")
        vm.state.test {
            // Инициализируется в Loading при cardNumber != null, затем Ready(fetched).
            val first = awaitItem()
            if (first is EditScreenState.Loading) {
                val ready = awaitItem() as EditScreenState.Ready
                assertThat(ready.isEditing).isTrue()
                assertThat(ready.current).isEqualTo(existing)
            } else {
                val ready = first as EditScreenState.Ready
                assertThat(ready.isEditing).isTrue()
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** §4.2 — в edit-режиме попытка изменить номер карты игнорируется. */
    @Test
    fun edit_mode_card_number_cannot_be_changed() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing

        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateCardNumber("000")
        advanceUntilIdle()

        // prod.md §4.2: В режиме редактирования номер карты не может быть изменён.
        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.cardNumber).isEqualTo("999")
    }

    /** §4.5 п.2 — смена пароля в edit-режиме делает canSave=false до повторной проверки. */
    @Test
    fun edit_mode_password_change_invalidates_save_until_recheck() = runTest {
        val existing = student(cardNumber = "999", password = "old")
        coEvery { repo.getByCardNumber("999") } returns existing

        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updatePassword("new")
        advanceUntilIdle()

        // prod.md §4.5 п.2: «изменение пароля → блокировка до успешной проверки с новым паролем».
        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.canSave).isFalse()
    }

    /** §6.1 — save() в edit-режиме отменяет старое расписание и создаёт новое при включённых напоминаниях. */
    @Test
    fun edit_mode_save_cancels_previous_and_schedules_new_when_enabled() = runTest {
        val existing = student(cardNumber = "999", notificationEnabled = true)
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateName("Анна")
        advanceUntilIdle()
        vm.uiListener.save {}
        advanceUntilIdle()

        io.mockk.verify { BalanceCheckScheduler.cancel(app, "999") }
        io.mockk.verify { BalanceCheckScheduler.schedule(app, match { it.cardNumber == "999" }) }
        coVerify { repo.update(match { it.cardNumber == "999" && it.name == "Анна" }) }
    }

    /** §6.1 — при выключении напоминаний save() отменяет старое расписание и не создаёт нового. */
    @Test
    fun edit_mode_save_cancels_but_does_not_reschedule_when_disabled() = runTest {
        val existing = student(cardNumber = "999", notificationEnabled = true)
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateNotificationEnabled(false)
        advanceUntilIdle()
        vm.uiListener.save {}
        advanceUntilIdle()

        io.mockk.verify { BalanceCheckScheduler.cancel(app, "999") }
        io.mockk.verify(exactly = 0) { BalanceCheckScheduler.schedule(any(), any()) }
    }

    /** §4.6 — в edit-режиме hasUnsavedChanges отражает отличие current от исходного Student. */
    @Test
    fun has_unsaved_changes_in_edit_mode_compares_against_initial() = runTest {
        val existing = student(cardNumber = "999", name = "Иван")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isFalse()

        vm.uiListener.updateName("Пётр")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isTrue()

        vm.uiListener.updateName("Иван")
        advanceUntilIdle()
        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isFalse()
    }

    /** §4.4 — порог 0 принимается как валидное значение и не блокирует сохранение. */
    @Test
    fun threshold_zero_is_accepted_and_does_not_block_save() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("0")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(0))
        assertThat(ready.canSave).isTrue()
    }

    /** §4.4 — стирание последней цифры в поле порога делает его визуально пустым (bug fix). */
    @Test
    fun threshold_erase_last_digit_clears_input_field() = runTest {
        val existing = student(cardNumber = "999", notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("20")
        advanceUntilIdle()
        vm.uiListener.updateBalanceThreshold("2")
        advanceUntilIdle()
        vm.uiListener.updateBalanceThreshold("")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("")
    }

    /** §4.4 — пустой raw-input трактуется как BigDecimal.ZERO в current Student. */
    @Test
    fun threshold_empty_input_sets_current_to_zero() = runTest {
        val existing = student(cardNumber = "999", notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal.ZERO)
        assertThat(ready.notificationThreshold).isEqualTo("")
    }

    /** §4.4 — после стирания и повторного ввода порог в current корректно обновляется. */
    @Test
    fun threshold_200_then_empty_then_50_sets_current_correctly() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("200")
        advanceUntilIdle()
        vm.uiListener.updateBalanceThreshold("")
        advanceUntilIdle()
        vm.uiListener.updateBalanceThreshold("50")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(50))
        assertThat(ready.notificationThreshold).isEqualTo("50")
    }

    /** §4.4 — минус запрещён: "-50" фильтруется до "50". */
    @Test
    fun threshold_filters_minus_sign() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("-50")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("50")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(50))
    }

    /** §4.4 — точка запрещена: "1.5" фильтруется до "15". */
    @Test
    fun threshold_filters_dot_separator() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("1.5")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("15")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(15))
    }

    /** §4.4 — запятая запрещена (десятичный разделитель RU): "1,5" фильтруется до "15". */
    @Test
    fun threshold_filters_comma_separator() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("1,5")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("15")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(15))
    }

    /** §4.4 — буквы (из вставки/физ. клавиатуры) фильтруются: "abc123" → "123". */
    @Test
    fun threshold_filters_letters_from_paste() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("abc123")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("123")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(123))
    }

    /** §4.4 — ввод, состоящий только из не-цифр, эквивалентен пустому: current → 0, input → "". */
    @Test
    fun threshold_filters_only_letters_results_in_empty() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("abc")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal.ZERO)
    }

    /** §4.6 — в edit-режиме стирание поля порога (initial=200, input="") делает hasUnsavedChanges=true. */
    @Test
    fun threshold_erased_in_edit_mode_flags_unsaved_changes() = runTest {
        val existing = student(cardNumber = "999", notificationThreshold = BigDecimal(200))
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()
        assertThat((vm.state.value as EditScreenState.Ready).hasUnsavedChanges).isFalse()

        vm.uiListener.updateBalanceThreshold("")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.hasUnsavedChanges).isTrue()
    }

    /** §4.4 — нет верхней границы: 15-значные цифры принимаются. */
    @Test
    fun threshold_large_number_accepted_without_upper_bound() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("999999999999999")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("999999999999999")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal("999999999999999"))
    }

    /** §4.4 — ведущие нули оставляем в raw-input; current хранит нормализованный BigDecimal. */
    @Test
    fun threshold_leading_zeros_are_preserved_in_raw_input_but_normalized_in_current() = runTest {
        val existing = student(cardNumber = "999")
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateBalanceThreshold("007")
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.notificationThreshold).isEqualTo("007")
        assertThat(ready.current.notificationThreshold).isEqualTo(BigDecimal(7))
    }

    /** §4.4 — изменение времени через колесо сохраняется в текущем Student. */
    @Test
    fun notification_time_update_propagates_to_current() = runTest {
        val existing = student(cardNumber = "999", notificationTime = LocalTime(7, 0))
        coEvery { repo.getByCardNumber("999") } returns existing
        val vm = EditViewModel(app, cardNumber = "999")
        vm.awaitReady()

        vm.uiListener.updateNotificationTime(LocalTime(8, 30))
        advanceUntilIdle()

        val ready = vm.state.value as EditScreenState.Ready
        assertThat(ready.current.notificationTime).isEqualTo(LocalTime(8, 30))
    }
}
