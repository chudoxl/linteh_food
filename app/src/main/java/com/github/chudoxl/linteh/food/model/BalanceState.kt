package com.github.chudoxl.linteh.food.model

import androidx.compose.runtime.Immutable

@Immutable
sealed class BalanceState {
    data object Unknown : BalanceState()                            //состояние баланса неизвестно
    data object Loaded : BalanceState()                             //значение баланса успешно загружено
    data object Refreshing : BalanceState()                         //значение баланса в процессе загрузки
    data class Error(val errMsg: String? = null): BalanceState()    //ошибка загрузки баланса
}