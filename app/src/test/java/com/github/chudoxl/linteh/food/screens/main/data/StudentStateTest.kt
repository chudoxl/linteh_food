package com.github.chudoxl.linteh.food.screens.main.data

import com.github.chudoxl.linteh.food.testutil.student
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalTime
import org.junit.Test
import java.math.BigDecimal

/**
 * prod.md §3.3, §13 — форматирование отображений:
 * - баланс: число с двумя знаками, пробел, символ ₽; разделитель тысяч — пробел;
 * - номер карты: группы по три цифры через пробел;
 * - время: HH:MM;
 * - строка напоминаний: «Напоминать в HH:MM, если баланс < XXX ₽» или «Не напоминать…».
 *
 * Fallback имени на номер карты (§3.3, §7.2) — тоже обязателен.
 */
class StudentStateTest {

    /** §3.3 / §13 — номер карты форматируется группами по три цифры. */
    @Test
    fun card_number_is_grouped_by_three() {
        val state = StudentState(student(cardNumber = "001002003"))
        assertThat(state.cardNumberPretty).isEqualTo("001 002 003")
    }

    /** §13 — номер карты нечётной длины тоже разбивается по три, хвост остаётся короче. */
    @Test
    fun card_number_non_multiple_of_three_still_groups() {
        val state = StudentState(student(cardNumber = "12345678"))
        // 12 345 678 — по три с головы
        assertThat(state.cardNumberPretty).isEqualTo("123 456 78")
    }

    /** §13 — баланс > 1000 получает пробел-разделитель тысяч и запятую перед копейками. */
    @Test
    fun balance_is_formatted_with_thousand_separator() {
        val state = StudentState(student(balance = BigDecimal("1250.50")))
        // prod.md §3.3 / §13: пример «1 250.50 ₽»
        assertThat(state.balanceValue).isEqualTo("1 250,50 ₽")
    }

    /** §13 — баланс меньше тысячи форматируется с двумя знаками после запятой и символом ₽. */
    @Test
    fun balance_small_is_formatted_with_two_decimals() {
        val state = StudentState(student(balance = BigDecimal("85.02")))
        assertThat(state.balanceValue).isEqualTo("85,02 ₽")
    }

    /** §3.3 — при включённых напоминаниях описание: «Напоминать в HH:MM, если баланс < XXX ₽». */
    @Test
    fun notification_description_when_enabled() {
        val state = StudentState(
            student(
                notificationEnabled = true,
                notificationTime = LocalTime(hour = 7, minute = 0),
                notificationThreshold = BigDecimal(200),
            )
        )
        assertThat(state.notificationDescription)
            .isEqualTo("Напоминать в 07:00, если баланс < 200 ₽")
    }

    /** §3.3 — при выключенных напоминаниях описание: «Не напоминать о низком балансе». */
    @Test
    fun notification_description_when_disabled() {
        val state = StudentState(student(notificationEnabled = false))
        assertThat(state.notificationDescription).isEqualTo("Не напоминать о низком балансе")
    }

    /** §3.3 / §7.2 — при пустом имени StudentState.name возвращает отформатированный номер карты. */
    @Test
    fun display_name_falls_back_to_formatted_card_number_when_empty() {
        // prod.md §3.3: «Если поле имени пустое — вместо имени отображается отформатированный номер карты».
        val state = StudentState(student(name = "", cardNumber = "001002003"))
        assertThat(state.name).isEqualTo("001 002 003")
    }
}
