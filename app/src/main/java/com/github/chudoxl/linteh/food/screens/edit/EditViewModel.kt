package com.github.chudoxl.linteh.food.screens.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.screens.edit.data.EditScreenState
import com.github.chudoxl.linteh.food.screens.edit.data.EditScreenState.Ready
import com.github.chudoxl.linteh.food.utils.formatAsRubles
import com.github.chudoxl.linteh.food.worker.BalanceCheckScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import java.math.BigDecimal

class EditViewModel(
    app: App,
    cardNumber: String?,
) : ViewModel() {

    private val repository = app.repository

    private val _state = MutableStateFlow<EditScreenState>(
        if (cardNumber == null) Ready(current = Student.Empty) else EditScreenState.Loading
    )

    init {
        cardNumber?.let {
            viewModelScope.launch(Dispatchers.IO) {
                val loaded = repository.getByCardNumber(cardNumber)
                _state.value = if (loaded != null) {
                    Ready(current = loaded, initial = loaded)
                } else {
                    Ready(current = Student.Empty)
                }
            }
        }
    }

    val state: StateFlow<EditScreenState> = _state

    val uiListener = object: EditScreenUiListener {
        override fun updateName(value: String) = updateCurrent { copy(name = value) }
        override fun updateCardNumber(value: String) = updateReady {
            if (isEditing) return@updateReady this
            if (current.cardNumber == value) this
            else copy(current = current.copy(cardNumber = value), checkState = BalanceCheckState.Idle)
        }
        override fun updatePassword(value: String) = updateReady {
            if (current.password == value) this
            else copy(current = current.copy(password = value), checkState = BalanceCheckState.Idle)
        }
        override fun updateNotificationEnabled(value: Boolean) = updateCurrent { copy(notificationEnabled = value) }
        override fun updateBalanceThreshold(value: String) {
            val filtered = value.filter { it.isDigit() }
            val parsed = if (filtered.isEmpty()) BigDecimal.ZERO else BigDecimal(filtered)
            updateReady {
                copy(
                    current = current.copy(notificationThreshold = parsed),
                    thresholdInput = filtered,
                )
            }
        }
        override fun updateNotificationTime(value: LocalTime) = updateCurrent { copy(notificationTime = value) }

        override fun checkBalance() {
            val ready = _state.value as? Ready ?: return
            viewModelScope.launch {
                if (!ready.isEditing) {
                    val existing = repository.getByCardNumber(ready.cardNumber)
                    if (existing != null) {
                        updateReady { copy(showDuplicateDialog = true) }
                        return@launch
                    }
                }
                updateReady { copy(checkState = BalanceCheckState.Loading) }
                try {
                    val response = repository.checkBalance(ready.cardNumber, ready.password)
                    updateReady {
                        val nextName = current.name.ifBlank {
                            val split = response.studentName.trim()
                                .split(Regex("\\s+"))
                            split.getOrNull(1)?.takeIf { it.isNotBlank() }
                                ?: split.getOrNull(0)?.takeIf { it.isNotBlank() }
                                ?: current.name
                        }
                        copy(
                            current = current.copy(name = nextName),
                            checkState = BalanceCheckState.Success(
                                studentName = response.studentName,
                                balance = response.balance.formatAsRubles()
                            )
                        )
                    }
                } catch (e: Exception) {
                    updateReady {
                        copy(checkState = BalanceCheckState.Error(e.message ?: "Неизвестная ошибка"))
                    }
                }
            }
        }

        override fun onDuplicateDialogDismissed() {
            updateReady { copy(showDuplicateDialog = false) }
        }

        override fun save(onDone: () -> Unit) {
            val ready = _state.value as? Ready ?: return
            viewModelScope.launch {
                val student = ready.current
                if (ready.isEditing) {
                    //отменяем таску, в случае редактирования
                    ready.initialCardNumber?.let { BalanceCheckScheduler.cancel(app, it) }
                    repository.update(student)
                } else {
                    repository.insert(student)
                }
                if (ready.notificationEnabled) {
                    BalanceCheckScheduler.schedule(app, student)
                }
                onDone()
            }
        }
    }

    private fun updateCurrent(update: Student.() -> Student) {
        updateReady { copy(current = current.update()) }
    }

    private inline fun updateReady(block: Ready.() -> Ready) {
        _state.update { if (it is Ready) it.block() else it }
    }

    companion object {
        class Factory(
            private val app: App,
            private val cardNumber: String?,
        ) : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditViewModel(app, cardNumber) as T
        }
    }
}
