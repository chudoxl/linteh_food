package com.github.chudoxl.linteh.food.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * prod.md §5 — состояния баланса: неизвестно / загружен / обновляется / ошибка (с опц. сообщением).
 */
class BalanceStateTest {

    /** §5 — BalanceState.Error() без параметра имеет null-сообщение. */
    @Test
    fun error_defaults_to_null_message() {
        val error = BalanceState.Error()
        assertThat(error.errMsg).isNull()
    }

    /** §5 — BalanceState.Error сохраняет переданный текст причины. */
    @Test
    fun error_retains_message() {
        val error = BalanceState.Error("no network")
        assertThat(error.errMsg).isEqualTo("no network")
    }

    /** §5 — четыре состояния Unknown/Loaded/Refreshing/Error различимы в set. */
    @Test
    fun four_states_are_distinct() {
        val states = setOf<BalanceState>(
            BalanceState.Unknown,
            BalanceState.Loaded,
            BalanceState.Refreshing,
            BalanceState.Error("x"),
        )
        assertThat(states).hasSize(4)
    }

    /** Error — data class, равенство по тексту сообщения. */
    @Test
    fun error_data_class_equality_by_message() {
        assertThat(BalanceState.Error("msg")).isEqualTo(BalanceState.Error("msg"))
        assertThat(BalanceState.Error("a")).isNotEqualTo(BalanceState.Error("b"))
    }

    /** Unknown, Loaded, Refreshing — object-синглетоны. */
    @Test
    fun unknown_loaded_refreshing_are_singletons() {
        assertThat(BalanceState.Unknown).isSameInstanceAs(BalanceState.Unknown)
        assertThat(BalanceState.Loaded).isSameInstanceAs(BalanceState.Loaded)
        assertThat(BalanceState.Refreshing).isSameInstanceAs(BalanceState.Refreshing)
    }
}
