package com.github.chudoxl.linteh.food.screens.edit.data

sealed class BalanceCheckState {
    data object Idle : BalanceCheckState()
    data object Loading : BalanceCheckState()
    data class Success(val studentName: String, val balance: String) : BalanceCheckState()
    data class Error(val message: String) : BalanceCheckState()
}