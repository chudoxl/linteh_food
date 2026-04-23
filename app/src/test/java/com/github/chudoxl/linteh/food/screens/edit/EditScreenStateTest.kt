package com.github.chudoxl.linteh.food.screens.edit

import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.screens.edit.data.EditScreenState.Ready
import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

/**
 * prod.md §4.5 — «сохраняются только те номер карты и пароль, с которыми была успешно
 * выполнена проверка баланса». Из этого же условия выводится доступность остальных
 * полей формы (Имя, Время, Порог): они активны ровно тогда, когда активна кнопка
 * «Сохранить» — т.е. `canSave == true`.
 */
class EditScreenStateTest {

    private val successCheck = BalanceCheckState.Success("Иванов Иван", "100.00 ₽")
    private val errorCheck = BalanceCheckState.Error("boom")

    /** §4.5 — в режиме добавления без проверки баланса форма заблокирована. */
    @Test
    fun add_mode_idle_blocks_other_fields() {
        val state = Ready(current = Student.Empty, checkState = BalanceCheckState.Idle)
        assertThat(state.canSave).isFalse()
    }

    /** §4.5 — пока запрос проверки выполняется, форма остаётся заблокированной. */
    @Test
    fun add_mode_loading_blocks_other_fields() {
        val state = Ready(current = filledNewStudent(), checkState = BalanceCheckState.Loading)
        assertThat(state.canSave).isFalse()
    }

    /** §4.5 — при ошибке проверки баланса форма остаётся заблокированной. */
    @Test
    fun add_mode_error_blocks_other_fields() {
        val state = Ready(current = filledNewStudent(), checkState = errorCheck)
        assertThat(state.canSave).isFalse()
    }

    /** §4.5 — в режиме добавления успешная проверка разблокирует форму. */
    @Test
    fun add_mode_success_enables_other_fields() {
        val state = Ready(current = filledNewStudent(), checkState = successCheck)
        assertThat(state.canSave).isTrue()
    }

    /** §4.5 — в режиме редактирования без смены credentials форма доступна без повторной проверки. */
    @Test
    fun edit_mode_credentials_unchanged_enables_other_fields() {
        val initial = student(cardNumber = "999", password = "pwd")
        val state = Ready(current = initial, initial = initial, checkState = BalanceCheckState.Idle)
        assertThat(state.canSave).isTrue()
    }

    /** §4.5 — смена пароля в edit-режиме блокирует форму до новой успешной проверки. */
    @Test
    fun edit_mode_password_changed_without_recheck_blocks_other_fields() {
        val initial = student(cardNumber = "999", password = "old")
        val state = Ready(
            current = initial.copy(password = "new"),
            initial = initial,
            checkState = BalanceCheckState.Idle,
        )
        assertThat(state.canSave).isFalse()
    }

    /** §4.5 — успешная проверка с новым паролем снова разблокирует форму в edit-режиме. */
    @Test
    fun edit_mode_password_changed_and_rechecked_enables_other_fields() {
        val initial = student(cardNumber = "999", password = "old")
        val state = Ready(
            current = initial.copy(password = "new"),
            initial = initial,
            checkState = successCheck,
        )
        assertThat(state.canSave).isTrue()
    }

    /** §4.5 п.3 — сброс `checkState` после смены credentials возвращает форму в заблокированное состояние. */
    @Test
    fun add_mode_success_then_invalidation_blocks_other_fields() {
        // prod.md §4.5 п.3: после изменения номера/пароля вслед за успехом — блок возвращается.
        // ViewModel сбрасывает checkState в Idle при смене credentials; здесь проверяется
        // формальный контракт состояния.
        val state = Ready(
            current = filledNewStudent(cardNumber = "new"),
            checkState = BalanceCheckState.Idle,
        )
        assertThat(state.canSave).isFalse()
    }

    /** §4.4 — по умолчанию thresholdInput в Ready равен current.notificationThreshold.toString(). */
    @Test
    fun ready_default_threshold_input_matches_current_threshold_string() {
        val state = Ready(current = Student.Empty.copy(notificationThreshold = BigDecimal(150)))
        assertThat(state.notificationThreshold).isEqualTo("150")
    }

    /** §4.4 — thresholdInput как явный параметр переопределяет отображаемое значение (raw-ввод). */
    @Test
    fun ready_threshold_input_overrides_current_threshold_string() {
        val state = Ready(
            current = Student.Empty.copy(notificationThreshold = BigDecimal.ZERO),
            thresholdInput = "",
        )
        assertThat(state.notificationThreshold).isEqualTo("")
    }

    private fun filledNewStudent(cardNumber: String = "001") = Student.Empty.copy(
        cardNumber = cardNumber,
        password = "pwd",
        notificationThreshold = BigDecimal(200),
    )
}
