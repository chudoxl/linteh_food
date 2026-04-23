package com.github.chudoxl.linteh.food.model

import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalTime
import org.junit.Test
import java.math.BigDecimal

/**
 * prod.md §4.4, §5 — значения по умолчанию для новых учеников:
 * - напоминания включены,
 * - время — 07:00,
 * - порог — 200 ₽,
 * - состояние баланса — неизвестно, баланс 0.
 */
class StudentDefaultsTest {

    /** §4.4 / §5 — Student.Empty соответствует дефолтам: 07:00, 200 ₽, напоминания включены. */
    @Test
    fun empty_student_has_spec_defaults() {
        val empty = Student.Empty

        assertThat(empty.name).isEmpty()
        assertThat(empty.cardNumber).isEmpty()
        assertThat(empty.password).isEmpty()
        assertThat(empty.notificationEnabled).isTrue()
        assertThat(empty.notificationTime).isEqualTo(LocalTime(hour = 7, minute = 0))
        assertThat(empty.notificationThreshold).isEqualTo(BigDecimal(200))
        assertThat(empty.balance).isEqualTo(BigDecimal.ZERO)
        assertThat(empty.balanceState).isEqualTo(BalanceState.Unknown)
    }
}
