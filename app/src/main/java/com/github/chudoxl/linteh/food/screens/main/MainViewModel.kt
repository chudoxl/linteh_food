package com.github.chudoxl.linteh.food.screens.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.screens.main.data.StudentState
import com.github.chudoxl.linteh.food.worker.BalanceCheckScheduler
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private val repository = app.repository

    private val _students = MutableStateFlow<ImmutableList<StudentState>>(persistentListOf())
    val students: StateFlow<ImmutableList<StudentState>> = _students

    private val autoRefreshedCardNumbers: MutableSet<String> = mutableSetOf()

    init {
        viewModelScope.launch {
            repository.getAllItems().collect { itemList ->
                _students.value = itemList
                    .map { item -> StudentState(student = item) }
                    .toImmutableList()

                itemList.filter { !autoRefreshedCardNumbers.contains(it.cardNumber) || it.balanceState == BalanceState.Unknown }
                    .also { pending -> autoRefreshedCardNumbers.addAll(pending.map { it.cardNumber }) }
                    .map { async { repository.updateBalance(student = it) } }
                    .awaitAll()
            }
        }
    }

    fun refreshBalances() {
        viewModelScope.launch {
            students.value.map { studentState ->
                async { repository.updateBalance(studentState.student) }
            }.awaitAll()
        }
    }

    fun deleteStudent(cardNumber: String) {
        BalanceCheckScheduler.cancel(app, cardNumber)
        viewModelScope.launch {
            repository.deleteByCardNumber(cardNumber)
        }
    }
}
