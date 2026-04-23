package com.github.chudoxl.linteh.food.data

import com.github.chudoxl.linteh.food.data.db.AppDatabase
import com.github.chudoxl.linteh.food.data.db.StudentEntity
import com.github.chudoxl.linteh.food.data.network.BalanceApi
import com.github.chudoxl.linteh.food.data.network.BalanceResponse
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalTime
import java.math.BigDecimal

class StudentRepository(
    private val db: AppDatabase,
    private val balanceApi: BalanceApi,
) {
    private val dao = db.studentsDao()

    fun getAllItems(): Flow<List<Student>> = dao.getAll().map { entities ->
        entities.map { it.toStudent() }
    }

    suspend fun getByCardNumber(cardNumber: String): Student? = dao.getByCardNumber(cardNumber)?.toStudent()

    suspend fun insert(student: Student): Long = dao.insert(student.toStudentEntity())

    suspend fun update(student: Student) = dao.update(student.toStudentEntity())

    suspend fun deleteByCardNumber(cardNumber: String) = dao.deleteByCardNumber(cardNumber)

    suspend fun updateBalance(student: Student): Student {
        try {
            dao.updateBalanceState(
                cardNumber = student.cardNumber,
                state = BalanceState.Refreshing.toEntityString(),
                error = null
            )
            val response = checkBalance(student.cardNumber, student.password)
            dao.updateBalanceState(
                cardNumber = student.cardNumber,
                balance = response.balance.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                state = BalanceState.Loaded.toEntityString(),
                error = null
            )

        } catch (e: Exception) {
            dao.updateBalanceState(
                cardNumber = student.cardNumber,
                state = BalanceState.Error().toEntityString(),
                error = e.message
            )
        }
        return getByCardNumber(student.cardNumber)!!
    }

    suspend fun checkBalance(cardNumber: String, password: String): BalanceResponse {
        return balanceApi.getBalance(cardNumber, password)
    }

    suspend fun getAllOnce(): List<Student> = dao.getAllOnce().map { it.toStudent() }

    companion object {
        private fun StudentEntity.toStudent() = Student(
            cardNumber = cardNumber,
            password = password,
            name = name,
            balance = BigDecimal(balance).setScale(2, java.math.RoundingMode.HALF_UP),
            notificationThreshold = BigDecimal(notificationThreshold).setScale(0, java.math.RoundingMode.HALF_UP),
            notificationEnabled = notificationEnabled,
            notificationTime = notificationTime.toLocalTime(),
            balanceState = toBalanceState(),
        )

        private fun Student.toStudentEntity() = StudentEntity(
            cardNumber = cardNumber,
            password = password,
            name = name,
            balance = balance.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
            notificationThreshold = notificationThreshold.toDouble(),
            notificationEnabled = notificationEnabled,
            notificationTime = notificationTime.hhMM(),
            balanceState = balanceState.toEntityString(),
            balanceError = (balanceState as? BalanceState.Error)?.errMsg,
        )

        private fun StudentEntity.toBalanceState(): BalanceState = when (balanceState) {
            "loaded" -> BalanceState.Loaded
            "refreshing" -> BalanceState.Refreshing
            "error" -> BalanceState.Error(balanceError)
            else -> BalanceState.Unknown
        }

        private fun BalanceState.toEntityString() = when(this) {
            is BalanceState.Error -> "error"
            BalanceState.Loaded -> "loaded"
            BalanceState.Refreshing -> "refreshing"
            BalanceState.Unknown -> "unknown"
        }

        fun String.toLocalTime(): LocalTime {
            val splits = this.split(":")
            return LocalTime(hour = splits[0].toInt(), minute = splits[1].toInt())
        }

        fun LocalTime.hhMM(): String = String.format("%02d:%02d", hour, minute)
    }
}