package com.github.chudoxl.linteh.food.screens.edit

import kotlinx.datetime.LocalTime

interface EditScreenUiListener {
    fun updateName(value: String) {}
    fun updateCardNumber(value: String) {}
    fun updatePassword(value: String) {}
    fun updateNotificationEnabled(value: Boolean) {}
    fun updateBalanceThreshold(value: String) {}
    fun updateNotificationTime(value: LocalTime) {}
    fun checkBalance() {}
    fun save(onDone: () -> Unit) {}
    fun onDuplicateDialogDismissed() {}
}
