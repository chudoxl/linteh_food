package com.github.chudoxl.linteh.food.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey
    val cardNumber: String,
    val password: String,
    val name: String,

    val balance: String,
    val balanceState: String,
    val balanceError: String?,

    val notificationEnabled: Boolean,
    val notificationTime: String,
    val notificationThreshold: Double,
)
