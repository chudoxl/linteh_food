package com.github.chudoxl.linteh.food.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.github.chudoxl.linteh.food.data.db.AppDatabase
import com.github.chudoxl.linteh.food.data.network.BalanceApi
import com.github.chudoxl.linteh.food.data.network.BalanceResponse
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.math.BigDecimal

/**
 * prod.md §5, §3.3 — интеграционный тест: Repository + in-memory Room + фейковый API.
 */
@RunWith(AndroidJUnit4::class)
class StudentRepositoryIntegrationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** §5 — изменение баланса через Repository эмитится в Flow getAllItems. */
    @Test
    fun updateBalance_then_getAllItems_propagates_change() = runBlocking {
        val api = StaticApi(BalanceResponse("Иван", BigDecimal("321.50")))
        val repo = StudentRepository(db, api)
        repo.insert(makeStudent("001"))

        repo.getAllItems().test {
            val initial = awaitItem()
            assertThat(initial.single().balanceState).isEqualTo(BalanceState.Unknown)

            repo.updateBalance(initial.single())

            // Ожидаем переход в Loaded с новым значением.
            val updated = awaitItem()
            if (updated.single().balanceState == BalanceState.Refreshing) {
                val finalState = awaitItem()
                assertThat(finalState.single().balanceState).isEqualTo(BalanceState.Loaded)
                assertThat(finalState.single().balance).isEqualTo(BigDecimal("321.50"))
            } else {
                assertThat(updated.single().balanceState).isEqualTo(BalanceState.Loaded)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** §5 — при ошибке интеграция сохраняет последний известный баланс, выдавая state=Error. */
    @Test
    fun updateBalance_error_preserves_last_known_balance_across_reads() = runBlocking {
        val api = ThrowingApi(IOException("no network"))
        val okApi = StaticApi(BalanceResponse("Иван", BigDecimal("500.00")))

        // Первый успешный вызов, чтобы проставить известный баланс.
        val repoOk = StudentRepository(db, okApi)
        repoOk.insert(makeStudent("001"))
        val loaded = repoOk.updateBalance(repoOk.getByCardNumber("001")!!)
        assertThat(loaded.balance).isEqualTo(BigDecimal("500.00"))

        // Теперь API падает — проверяем, что баланс в БД не обнуляется.
        val repoErr = StudentRepository(db, api)
        val afterErr = repoErr.updateBalance(loaded)

        assertThat(afterErr.balance).isEqualTo(BigDecimal("500.00"))
        assertThat(afterErr.balanceState).isInstanceOf(BalanceState.Error::class.java)
        assertThat((afterErr.balanceState as BalanceState.Error).errMsg).isEqualTo("no network")
    }

    private fun makeStudent(cardNumber: String): Student = Student(
        name = "",
        cardNumber = cardNumber,
        password = "pwd",
        balance = BigDecimal.ZERO,
        balanceState = BalanceState.Unknown,
        notificationEnabled = true,
        notificationTime = LocalTime(hour = 7, minute = 0),
        notificationThreshold = BigDecimal(200),
    )

    private class StaticApi(private val response: BalanceResponse) : BalanceApi {
        override suspend fun getBalance(login: String, password: String): BalanceResponse = response
    }

    private class ThrowingApi(private val ex: Exception) : BalanceApi {
        override suspend fun getBalance(login: String, password: String): BalanceResponse = throw ex
    }
}
