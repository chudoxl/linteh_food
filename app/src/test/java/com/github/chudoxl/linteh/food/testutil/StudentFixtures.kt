package com.github.chudoxl.linteh.food.testutil

import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import kotlinx.datetime.LocalTime
import java.math.BigDecimal

fun student(
    cardNumber: String = "001002003",
    name: String = "Иван",
    password: String = "pwd",
    balance: BigDecimal = BigDecimal("0.00"),
    balanceState: BalanceState = BalanceState.Unknown,
    notificationEnabled: Boolean = true,
    notificationTime: LocalTime = LocalTime(hour = 7, minute = 0),
    notificationThreshold: BigDecimal = BigDecimal(200),
): Student = Student(
    cardNumber = cardNumber,
    name = name,
    password = password,
    balance = balance,
    balanceState = balanceState,
    notificationEnabled = notificationEnabled,
    notificationTime = notificationTime,
    notificationThreshold = notificationThreshold,
)
