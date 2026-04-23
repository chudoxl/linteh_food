package com.github.chudoxl.linteh.food.data

import com.github.chudoxl.linteh.food.data.db.AppDatabase
import com.github.chudoxl.linteh.food.data.db.StudentEntity
import com.github.chudoxl.linteh.food.data.db.StudentsDao
import com.github.chudoxl.linteh.food.data.network.BalanceApi
import com.github.chudoxl.linteh.food.data.network.BalanceResponse
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

/**
 * prod.md §3.3, §5, §6.3 — поведение StudentRepository.updateBalance:
 * - state → Refreshing → Loaded на успехе;
 * - state → Error(errMsg) на ошибке, последний известный баланс НЕ перезаписывается;
 * - balanceError сбрасывается при следующей успешной проверке.
 */
class StudentRepositoryTest {

    /** §3.3 / §5 — updateBalance переводит состояние Refreshing → Loaded при успешном ответе. */
    @Test
    fun updateBalance_success_sets_refreshing_then_loaded() = runBlocking {
        val initial = baseEntity(balance = "100.00", state = "loaded", error = null)
        val stored = mutableListOf(initial)
        val dao = captureUpdates(stored)
        val db = mockk<AppDatabase>().apply { every { studentsDao() } returns dao }
        val api = mockk<BalanceApi>().apply {
            coEvery { getBalance(any(), any()) } returns BalanceResponse(
                studentName = "Иван", balance = BigDecimal("321.50")
            )
        }
        val repository = StudentRepository(db, api)

        val updated = repository.updateBalance(student(cardNumber = "001"))

        assertThat(updated.balance).isEqualTo(BigDecimal("321.50"))
        assertThat(updated.balanceState).isEqualTo(BalanceState.Loaded)

        coVerifyOrder {
            dao.updateBalanceState("001", state = "refreshing", error = null)
            dao.updateBalanceState("001", balance = "321.50", state = "loaded", error = null)
        }
    }

    /** §5 — при ошибке последний известный баланс сохраняется, state становится Error(msg). */
    @Test
    fun updateBalance_error_preserves_last_known_balance() = runBlocking {
        val initial = baseEntity(balance = "555.55", state = "loaded", error = null)
        val stored = mutableListOf(initial)
        val dao = captureUpdates(stored)
        val db = mockk<AppDatabase>().apply { every { studentsDao() } returns dao }
        val api = mockk<BalanceApi>().apply {
            coEvery { getBalance(any(), any()) } throws IOException("no network")
        }
        val repository = StudentRepository(db, api)

        val updated = repository.updateBalance(student(cardNumber = "001"))

        // prod.md §5, §3.3: последний известный баланс не перезаписывается при ошибке.
        assertThat(updated.balance).isEqualTo(BigDecimal("555.55"))
        assertThat(updated.balanceState).isInstanceOf(BalanceState.Error::class.java)
        assertThat((updated.balanceState as BalanceState.Error).errMsg).isEqualTo("no network")

        // balance НЕ вызван с новым значением — только state flip'ы.
        coVerify(exactly = 1) {
            dao.updateBalanceState("001", state = "refreshing", error = null)
        }
        coVerify(exactly = 1) {
            dao.updateBalanceState("001", state = "error", error = "no network")
        }
        coVerify(exactly = 0) {
            dao.updateBalanceState(any(), balance = any<String>(), state = any(), error = any())
        }
    }

    /** §5 — успешная проверка после ошибки сбрасывает balanceError. */
    @Test
    fun subsequent_success_after_error_clears_error() = runBlocking {
        val stored = mutableListOf(
            baseEntity(balance = "555.55", state = "error", error = "no network")
        )
        val dao = captureUpdates(stored)
        val db = mockk<AppDatabase>().apply { every { studentsDao() } returns dao }
        val api = mockk<BalanceApi>().apply {
            coEvery { getBalance(any(), any()) } returns BalanceResponse("Иван", BigDecimal("700.00"))
        }
        val repository = StudentRepository(db, api)

        val updated = repository.updateBalance(student(cardNumber = "001"))

        assertThat(updated.balanceState).isEqualTo(BalanceState.Loaded)
        assertThat(updated.balance).isEqualTo(BigDecimal("700.00"))
        // prod.md §5: balanceError сбрасывается при следующей успешной проверке.
        coVerify { dao.updateBalanceState("001", balance = "700.00", state = "loaded", error = null) }
    }

    /** getAllItems маппит Entity → Student, включая balanceState. */
    @Test
    fun getAllItems_maps_entities_to_domain() = runBlocking {
        val dao = mockk<StudentsDao>()
        every { dao.getAll() } returns flowOf(listOf(baseEntity(balance = "10.00", state = "loaded", error = null)))
        val db = mockk<AppDatabase>().apply { every { studentsDao() } returns dao }
        val api = mockk<BalanceApi>()

        val first = StudentRepository(db, api).getAllItems().first()

        assertThat(first).hasSize(1)
        assertThat(first.single().cardNumber).isEqualTo("001")
        assertThat(first.single().balanceState).isEqualTo(BalanceState.Loaded)
    }

    // --- helpers ---------------------------------------------------------

    private fun baseEntity(balance: String, state: String, error: String?) = StudentEntity(
        cardNumber = "001",
        password = "pwd",
        name = "Иван",
        balance = balance,
        balanceState = state,
        balanceError = error,
        notificationEnabled = true,
        notificationTime = "07:00",
        notificationThreshold = 200.0,
    )

    /**
     * DAO с in-memory слотом для строки. Выдаёт get/updateBalanceState с состоянием `stored`.
     */
    private fun captureUpdates(stored: MutableList<StudentEntity>): StudentsDao {
        val dao = mockk<StudentsDao>(relaxed = true)
        coEvery { dao.getByCardNumber(any()) } answers {
            stored.firstOrNull { it.cardNumber == firstArg<String>() }
        }
        coEvery {
            dao.updateBalanceState(any(), any<String>(), any<String>(), any())
        } answers {
            val idx = stored.indexOfFirst { it.cardNumber == firstArg<String>() }
            if (idx >= 0) stored[idx] = stored[idx].copy(
                balance = secondArg(),
                balanceState = thirdArg(),
                balanceError = arg(3)
            )
        }
        coEvery {
            dao.updateBalanceState(any<String>(), any<String>(), any())
        } answers {
            val idx = stored.indexOfFirst { it.cardNumber == firstArg<String>() }
            if (idx >= 0) stored[idx] = stored[idx].copy(
                balanceState = secondArg(),
                balanceError = thirdArg()
            )
        }
        return dao
    }
}
