package com.github.chudoxl.linteh.food.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * prod.md §5 — cardNumber первичный ключ; §3.1 — порядок записей по времени добавления.
 */
@RunWith(AndroidJUnit4::class)
class StudentsDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: StudentsDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.studentsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    /** §5 — insert/getByCardNumber сохраняет все поля сущности без искажения. */
    @Test
    fun insert_then_getByCardNumber_roundtrips_all_fields() = runBlocking {
        val entity = entity("001", name = "Иван", balance = "85.02", time = "07:30")
        dao.insert(entity)

        val loaded = dao.getByCardNumber("001")
        assertThat(loaded).isEqualTo(entity)
    }

    /** §5 — повторный insert с тем же cardNumber бросает SQLiteConstraintException. */
    @Test
    fun duplicate_primary_key_throws() {
        runBlocking { dao.insert(entity("001")) }
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { dao.insert(entity("001", name = "дубль")) }
        }
    }

    /** update по @PrimaryKey меняет существующую запись. */
    @Test
    fun update_changes_existing_row() = runBlocking {
        dao.insert(entity("001", name = "старое"))
        dao.update(entity("001", name = "новое"))

        assertThat(dao.getByCardNumber("001")?.name).isEqualTo("новое")
    }

    /** delete убирает запись из таблицы. */
    @Test
    fun delete_removes_row() = runBlocking {
        val e = entity("001")
        dao.insert(e)
        dao.delete(e)

        assertThat(dao.getByCardNumber("001")).isNull()
    }

    /** getAll возвращает Flow, который реактивно обновляется при insert/delete. */
    @Test
    fun getAll_flow_emits_on_changes() = runBlocking {
        dao.getAll().test {
            assertThat(awaitItem()).isEmpty()
            dao.insert(entity("001"))
            assertThat(awaitItem()).hasSize(1)
            dao.insert(entity("002"))
            assertThat(awaitItem()).hasSize(2)
            dao.delete(entity("001"))
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Точечный UPDATE баланса+состояния не затирает остальные поля записи. */
    @Test
    fun updateBalanceState_full_variant_preserves_other_fields() = runBlocking {
        dao.insert(entity("001", name = "Иван", balance = "100.00", time = "07:30"))

        dao.updateBalanceState("001", balance = "250.00", state = "loaded", error = null)

        val after = dao.getByCardNumber("001")!!
        assertThat(after.name).isEqualTo("Иван")
        assertThat(after.notificationTime).isEqualTo("07:30")
        assertThat(after.balance).isEqualTo("250.00")
        assertThat(after.balanceState).isEqualTo("loaded")
        assertThat(after.balanceError).isNull()
    }

    /** §5 — при ошибке запрос обновляет только state и error, оставляя последний известный баланс. */
    @Test
    fun updateBalanceState_short_variant_preserves_balance() = runBlocking {
        dao.insert(entity("001", balance = "77.00"))

        dao.updateBalanceState("001", state = "error", error = "boom")

        val after = dao.getByCardNumber("001")!!
        assertThat(after.balance).isEqualTo("77.00") // баланс не трогаем при ошибке (prod.md §5)
        assertThat(after.balanceState).isEqualTo("error")
        assertThat(after.balanceError).isEqualTo("boom")
    }

    /** §3.1 — getAll возвращает записи в порядке вставки (старые сверху). */
    @Test
    fun getAll_returns_items_in_insertion_order() = runBlocking {
        dao.insert(entity("c", name = "1-й"))
        dao.insert(entity("a", name = "2-й"))
        dao.insert(entity("b", name = "3-й"))

        // prod.md §3.1: «Порядок — по времени добавления (старые сверху, новые снизу)».
        val names = dao.getAll().first().map { it.name }
        assertThat(names).containsExactly("1-й", "2-й", "3-й").inOrder()
    }

    private fun entity(
        cardNumber: String,
        name: String = "имя",
        balance: String = "0.00",
        time: String = "07:00",
    ) = StudentEntity(
        cardNumber = cardNumber,
        password = "pwd",
        name = name,
        balance = balance,
        balanceState = "unknown",
        balanceError = null,
        notificationEnabled = true,
        notificationTime = time,
        notificationThreshold = 200.0,
    )
}
