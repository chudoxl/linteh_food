package com.github.chudoxl.linteh.food.screens.edit.data

import androidx.compose.runtime.Immutable
import com.github.chudoxl.linteh.food.model.Student
import kotlinx.datetime.LocalTime

@Immutable
sealed class EditScreenState {

    data object Loading : EditScreenState()

    data class Ready(
        val current: Student,
        private val initial: Student? = null,
        val thresholdInput: String = current.notificationThreshold.toString(),
        val checkState: BalanceCheckState = BalanceCheckState.Idle,
        val showDuplicateDialog: Boolean = false,
    ) : EditScreenState() {

        val isEditing: Boolean = initial != null

        val canCheck: Boolean
            get() = current.cardNumber.isNotBlank() && current.password.isNotBlank()

        val canSave: Boolean
            get() {
                val credentialsUnchanged = initial != null &&
                    current.cardNumber == initial.cardNumber &&
                    current.password == initial.password
                return credentialsUnchanged || checkState is BalanceCheckState.Success
            }

        val hasUnsavedChanges: Boolean
            get() = if (initial != null) {
                current != initial
            } else {
                current.name.isNotBlank() || current.cardNumber.isNotBlank() || current.password.isNotBlank()
            }

        //current
        val name: String
            get() = current.name
        val cardNumber: String
            get() = current.cardNumber
        val password: String
            get() = current.password
        val notificationEnabled: Boolean
            get() = current.notificationEnabled
        val notificationTime: LocalTime
            get() = current.notificationTime
        val notificationThreshold: String
            get() = thresholdInput
        //initial
        val initialCardNumber: String?
            get() = initial?.cardNumber
        val initialTime: LocalTime?
            get() = initial?.notificationTime
    }
}
