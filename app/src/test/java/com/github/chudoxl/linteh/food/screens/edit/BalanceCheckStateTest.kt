package com.github.chudoxl.linteh.food.screens.edit

import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * prod.md §4.2 — состояния виджета проверки баланса.
 */
class BalanceCheckStateTest {

    /** §4.2 — sealed-класс содержит ровно 4 различных состояния виджета проверки. */
    @Test
    fun four_states_exist_and_are_distinct() {
        val states = listOf<BalanceCheckState>(
            BalanceCheckState.Idle,
            BalanceCheckState.Loading,
            BalanceCheckState.Success("Иванов", "85.02 ₽"),
            BalanceCheckState.Error("no network"),
        )
        assertThat(states.distinct()).hasSize(4)
    }

    /** Idle и Loading — object-синглетоны, сравниваются по ссылке. */
    @Test
    fun idle_and_loading_are_singletons() {
        assertThat(BalanceCheckState.Idle).isSameInstanceAs(BalanceCheckState.Idle)
        assertThat(BalanceCheckState.Loading).isSameInstanceAs(BalanceCheckState.Loading)
    }

    /** Success — data class, равенство по полям (ФИО, баланс). */
    @Test
    fun success_equality_by_fields() {
        assertThat(BalanceCheckState.Success("A", "1")).isEqualTo(BalanceCheckState.Success("A", "1"))
        assertThat(BalanceCheckState.Success("A", "1")).isNotEqualTo(BalanceCheckState.Success("B", "1"))
    }

    /** Error — data class, равенство по тексту сообщения. */
    @Test
    fun error_equality_by_message() {
        assertThat(BalanceCheckState.Error("x")).isEqualTo(BalanceCheckState.Error("x"))
        assertThat(BalanceCheckState.Error("x")).isNotEqualTo(BalanceCheckState.Error("y"))
    }
}
