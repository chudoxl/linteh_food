package com.github.chudoxl.linteh.food.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentsDao {
    @Query("SELECT * FROM students")
    fun getAll(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students")
    suspend fun getAllOnce(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE cardNumber = :cardNumber")
    suspend fun getByCardNumber(cardNumber: String): StudentEntity?

    @Insert
    suspend fun insert(item: StudentEntity): Long

    @Update
    suspend fun update(item: StudentEntity)

    @Delete
    suspend fun delete(item: StudentEntity)

    @Query("DELETE FROM students WHERE cardNumber = :cardNumber")
    suspend fun deleteByCardNumber(cardNumber: String)

    @Query("UPDATE students SET balance = :balance, balanceState = :state, balanceError = :error WHERE cardNumber = :cardNumber")
    suspend fun updateBalanceState(cardNumber: String, balance: String, state: String, error: String?)

    @Query("UPDATE students SET balanceState = :state, balanceError = :error WHERE cardNumber = :cardNumber")
    suspend fun updateBalanceState(cardNumber: String, state: String, error: String?)
}
