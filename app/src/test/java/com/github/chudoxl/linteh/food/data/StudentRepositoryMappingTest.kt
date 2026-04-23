package com.github.chudoxl.linteh.food.data

import com.github.chudoxl.linteh.food.data.db.AppDatabase
import com.github.chudoxl.linteh.food.data.db.StudentEntity
import com.github.chudoxl.linteh.food.data.network.BalanceApi
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalTime
import org.junit.Test
import java.math.BigDecimal

/**
 * Маппинг StudentEntity ↔ Student (prod.md §5, §12, §13).
 *
 * Чтобы дотянуться до приватных extension-функций внутри StudentRepository.Companion,
 * мы прогоняем объект через DAO-mock: insert/update/getByCardNumber — единственный
 * публичный контракт репозитория, а внутренний маппинг — его побочный эффект.
 */
class StudentRepositoryMappingTest {

    private fun repo(captured: MutableList<StudentEntity>): StudentRepository {
        val dao = mockk<com.github.chudoxl.linteh.food.data.db.StudentsDao>(relaxed = true)
        val db = mockk<AppDatabase>()
        io.mockk.every { db.studentsDao() } returns dao
        io.mockk.coEvery { dao.insert(any()) } answers {
            captured.add(firstArg()); 1L
        }
        io.mockk.coEvery { dao.update(any()) } answers {
            captured.add(firstArg())
        }
        io.mockk.every { dao.getAll() } returns flowOf(emptyList())
        return StudentRepository(db, mockk<BalanceApi>(relaxed = true))
    }

    /** §5 — при сохранении баланс округляется до двух знаков по HALF_UP. */
    @Test
    fun balance_rounded_half_up_to_two_decimals_on_save() = runBlocking {
        val captured = mutableListOf<StudentEntity>()
        val student = student(balance = BigDecimal("1.005"))
        repo(captured).insert(student)

        // Сохраняем Double — точность зависит от floating-point. Проверяем, что округлено к .01 при обратной прогонке.
        val rehydrated = captured.single().toStudentViaRepository()
        assertThat(rehydrated.balance).isEqualTo(BigDecimal("1.01"))
    }

    /** §13 — LocalTime сериализуется как «HH:MM» и корректно десериализуется обратно. */
    @Test
    fun time_serialized_as_hh_mm_and_parsed_back() = runBlocking {
        val captured = mutableListOf<StudentEntity>()
        val student = student(notificationTime = LocalTime(hour = 7, minute = 5))
        repo(captured).insert(student)

        val entity = captured.single()
        assertThat(entity.notificationTime).isEqualTo("07:05")
        assertThat(entity.toStudentViaRepository().notificationTime).isEqualTo(LocalTime(7, 5))
    }

    /** §5 — BalanceState.Error сериализуется как «error» с сохранением текста в balanceError. */
    @Test
    fun error_state_serializes_with_message() = runBlocking {
        val captured = mutableListOf<StudentEntity>()
        val student = student(balanceState = BalanceState.Error("timeout"))
        repo(captured).insert(student)

        val entity = captured.single()
        assertThat(entity.balanceState).isEqualTo("error")
        assertThat(entity.balanceError).isEqualTo("timeout")
    }

    /** §5 — Loaded сериализуется как «loaded», balanceError сбрасывается в null. */
    @Test
    fun loaded_state_clears_error_on_serialize() = runBlocking {
        val captured = mutableListOf<StudentEntity>()
        val student = student(balanceState = BalanceState.Loaded)
        repo(captured).insert(student)

        val entity = captured.single()
        assertThat(entity.balanceState).isEqualTo("loaded")
        assertThat(entity.balanceError).isNull()
    }

    /** §5 — Unknown сериализуется как «unknown» и корректно восстанавливается обратно. */
    @Test
    fun unknown_state_roundtrip() = runBlocking {
        val captured = mutableListOf<StudentEntity>()
        val student = student(balanceState = BalanceState.Unknown)
        repo(captured).insert(student)

        val entity = captured.single()
        assertThat(entity.balanceState).isEqualTo("unknown")
        assertThat(entity.toStudentViaRepository().balanceState).isEqualTo(BalanceState.Unknown)
    }

    /** §4.4 — notificationThreshold из БД читается как целое (HALF_UP до scale=0). */
    @Test
    fun notification_threshold_rounded_to_zero_scale_on_read() = runBlocking {
        // БД хранит Double. При чтении scale обрезается до 0 через HALF_UP (prod.md §4.4 "целое неотрицательное число").
        val dao = mockk<com.github.chudoxl.linteh.food.data.db.StudentsDao>(relaxed = true)
        val db = mockk<AppDatabase>()
        io.mockk.every { db.studentsDao() } returns dao
        io.mockk.coEvery { dao.getByCardNumber("001") } returns StudentEntity(
            cardNumber = "001",
            password = "p",
            name = "n",
            balance = "0.00",
            balanceState = "loaded",
            balanceError = null,
            notificationEnabled = true,
            notificationTime = "07:00",
            notificationThreshold = 200.4,
        )

        val student = StudentRepository(db, mockk<BalanceApi>(relaxed = true)).getByCardNumber("001")!!
        assertThat(student.notificationThreshold).isEqualTo(BigDecimal(200))
    }

    /**
     * Прогоняет Entity обратно через публичный getByCardNumber, чтобы проверить весь маппинг.
     */
    private fun StudentEntity.toStudentViaRepository(): Student {
        val dao = mockk<com.github.chudoxl.linteh.food.data.db.StudentsDao>(relaxed = true)
        val db = mockk<AppDatabase>()
        io.mockk.every { db.studentsDao() } returns dao
        io.mockk.coEvery { dao.getByCardNumber(cardNumber) } returns this
        val repo = StudentRepository(db, mockk<BalanceApi>(relaxed = true))
        return runBlocking { repo.getByCardNumber(cardNumber) }!!
    }
}
