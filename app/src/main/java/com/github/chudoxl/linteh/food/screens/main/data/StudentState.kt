package com.github.chudoxl.linteh.food.screens.main.data

import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.utils.formatAsRubles
import com.github.chudoxl.linteh.food.utils.formatAsRublesInteger

data class StudentState(
    val student: Student
) {
    val cardNumber: String
        get() = student.cardNumber

    val password: String
        get() = student.password

    val cardNumberPretty: String = student.cardNumber.chunked(3).joinToString(separator = " ")

    val name: String = student.name.ifBlank { cardNumberPretty }

    val balanceState: BalanceState = student.balanceState

    val balanceValue: String = student.balance.formatAsRubles()

    val notificationEnabled: Boolean
        get() = student.notificationEnabled

    val notificationDescription: String =
        if (notificationEnabled) "Напоминать в ${student.notificationTime}, если баланс < ${student.notificationThreshold.formatAsRublesInteger()}"
        else "Не напоминать о низком балансе"
}
