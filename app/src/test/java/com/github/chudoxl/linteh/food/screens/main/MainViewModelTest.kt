package com.github.chudoxl.linteh.food.screens.main

import app.cash.turbine.test
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.data.StudentRepository
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.testutil.MainDispatcherRule
import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * prod.md §3.1, §3.5, §3.6 — поведение списка учеников на главном экране.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    /** §3.6 — при инициализации VM обновляет баланс всех учеников, независимо от состояния, по одному разу. */
    @Test
    fun init_auto_refreshes_all_students_once() = runTest {
        val s1 = student(cardNumber = "001", balanceState = BalanceState.Unknown)
        val s2 = student(cardNumber = "002", balanceState = BalanceState.Loaded)
        val s3 = student(cardNumber = "003", balanceState = BalanceState.Error("x"))
        val s4 = student(cardNumber = "004", balanceState = BalanceState.Refreshing)

        val (vm, repo) = buildVm(initialItems = listOf(s1, s2, s3, s4))
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "001" }) }
        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "002" }) }
        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "003" }) }
        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "004" }) }
    }

    /** §3.6 — повторный emit того же списка не запускает автообновление дважды. */
    @Test
    fun second_emit_of_same_items_does_not_trigger_auto_refresh_again() = runTest {
        val s1 = student(cardNumber = "001", balanceState = BalanceState.Unknown)

        val items = MutableStateFlow(listOf(s1))
        val repo = mockk<StudentRepository>(relaxed = true).also {
            every { it.getAllItems() } returns items
            coEvery { it.updateBalance(any()) } returns s1.copy(balanceState = BalanceState.Loaded)
        }
        buildVmFrom(repo)
        advanceUntilIdle()

        // Обновление балансов не трогает Flow напрямую — эмитим ту же версию ещё раз.
        items.value = listOf(s1)
        advanceUntilIdle()

        // prod.md §3.6: повторное автообновление при возврате не выполняется.
        coVerify(exactly = 1) { repo.updateBalance(any()) }
    }

    /** §3.6 — после удаления ученика и повторного добавления его баланс автоматически обновляется. */
    @Test
    fun student_readded_after_deletion_is_auto_refreshed() = runTest {
        val s1 = student(cardNumber = "001", balanceState = BalanceState.Unknown)
        val items = MutableStateFlow(listOf(s1))
        val repo = mockk<StudentRepository>(relaxed = true).also {
            every { it.getAllItems() } returns items
            coEvery { it.updateBalance(any()) } answers { firstArg<Student>().copy(balanceState = BalanceState.Loaded) }
        }
        buildVmFrom(repo)
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "001" }) }

        // Удаление ученика — Flow эмитит пустой список.
        items.value = emptyList()
        advanceUntilIdle()

        // Повторное добавление через экран «Добавить»: Student.balanceState сохраняется в состоянии Unknown
        // (см. EditScreen.kt — Student.balanceState использует BalanceState.Unknown, insert не меняет статус).
        items.value = listOf(s1)
        advanceUntilIdle()

        // Ожидаем 2 вызова: один при init, второй — после повторного появления ученика в списке.
        coVerify(exactly = 2) { repo.updateBalance(match { it.cardNumber == "001" }) }
    }

    /** §3.6 — при добавлении нового ученика в список его баланс автоматически обновляется, старые — не повторно. */
    @Test
    fun new_student_appearing_after_init_is_auto_refreshed() = runTest {
        val s1 = student(cardNumber = "001", balanceState = BalanceState.Loaded)
        val s2 = student(cardNumber = "002", balanceState = BalanceState.Unknown)

        val items = MutableStateFlow(listOf(s1))
        val repo = mockk<StudentRepository>(relaxed = true).also {
            every { it.getAllItems() } returns items
            coEvery { it.updateBalance(any()) } answers { firstArg<Student>().copy(balanceState = BalanceState.Loaded) }
        }
        buildVmFrom(repo)
        advanceUntilIdle()

        // Добавляем нового ученика (как после возврата с экрана редактирования).
        items.value = listOf(s1, s2)
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "001" }) }
        coVerify(exactly = 1) { repo.updateBalance(match { it.cardNumber == "002" }) }
    }

    /** §3.5 — pull-to-refresh запускает updateBalance для всех учеников независимо от флага «Напоминать». */
    @Test
    fun refresh_balances_calls_update_for_all_students_regardless_of_flag() = runTest {
        val s1 = student(cardNumber = "001", balanceState = BalanceState.Loaded, notificationEnabled = true)
        val s2 = student(cardNumber = "002", balanceState = BalanceState.Loaded, notificationEnabled = false)

        val (vm, repo) = buildVm(listOf(s1, s2))
        advanceUntilIdle()
        // После init у каждого ученика уже 1 вызов updateBalance (prod.md §3.6 — обновление всех).

        vm.refreshBalances()
        advanceUntilIdle()

        // prod.md §3.5: pull-to-refresh обновляет всех учеников параллельно, независимо от флага.
        // Итого 2 вызова на ученика: init + refreshBalances.
        coVerify(exactly = 2) { repo.updateBalance(match { it.cardNumber == "001" }) }
        coVerify(exactly = 2) { repo.updateBalance(match { it.cardNumber == "002" }) }
    }

    /** VM оборачивает Student в StudentState для presentation-слоя. */
    @Test
    fun students_state_flow_exposes_student_state_wrapped_items() = runTest {
        val s1 = student(cardNumber = "001", name = "Анна")
        val (vm, _) = buildVm(listOf(s1))
        vm.students.test {
            val list = awaitItem()
            // initial empty then list:
            val actual = if (list.isEmpty()) awaitItem() else list
            assertThat(actual).hasSize(1)
            assertThat(actual.single().student.cardNumber).isEqualTo("001")
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- helpers ---------------------------------------------------------

    private fun buildVm(initialItems: List<Student>): Pair<MainViewModel, StudentRepository> {
        val repo = mockk<StudentRepository>(relaxed = true)
        every { repo.getAllItems() } returns MutableStateFlow(initialItems)
        coEvery { repo.updateBalance(any()) } answers { firstArg<Student>().copy(balanceState = BalanceState.Loaded) }
        return buildVmFrom(repo) to repo
    }

    private fun buildVmFrom(repo: StudentRepository): MainViewModel {
        val app = mockk<App>(relaxed = true)
        every { app.repository } returns repo
        // AndroidViewModel хранит application. Каст в MainViewModel (application as App) будет успешен,
        // так как mockk<App>() — истинный потомок App.
        return MainViewModel(app)
    }
}
