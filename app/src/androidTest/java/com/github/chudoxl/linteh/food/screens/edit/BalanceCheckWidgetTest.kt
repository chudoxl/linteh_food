package com.github.chudoxl.linteh.food.screens.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * prod.md §4.2 — состояния виджета проверки баланса.
 */
@RunWith(AndroidJUnit4::class)
class BalanceCheckWidgetTest {

    @get:Rule
    val compose = createComposeRule()

    /** §4.2 — в Idle показывается только кнопка «Проверить», без блока статуса. */
    @Test
    fun idle_shows_only_button_without_status() {
        compose.setContent {
            AppTheme {
                BalanceCheckWidget(
                    checkState = BalanceCheckState.Idle,
                    canCheck = true,
                    onCheckClick = {},
                )
            }
        }

        compose.onNodeWithTag("btn_check_balance").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("balance_check_status").assertIsNotDisplayed()
    }

    /** §4.2 — кнопка «Проверить» неактивна, пока не заполнены номер карты и пароль. */
    @Test
    fun check_button_disabled_when_not_can_check() {
        compose.setContent {
            AppTheme {
                BalanceCheckWidget(
                    checkState = BalanceCheckState.Idle,
                    canCheck = false,
                    onCheckClick = {},
                )
            }
        }
        compose.onNodeWithTag("btn_check_balance").assertIsNotEnabled()
    }

    /** §4.2 — в Loading показывается «Запрос баланса» и анимация 1-2-3 точки. */
    @Test
    fun loading_shows_status_text_and_indicator() {
        compose.setContent {
            AppTheme {
                BalanceCheckWidget(
                    checkState = BalanceCheckState.Loading,
                    canCheck = true,
                    onCheckClick = {},
                )
            }
        }
        compose.onNodeWithTag("balance_check_status").assertIsDisplayed()
        compose.onNodeWithText("Запрос баланса").assertIsDisplayed()
        compose.onNodeWithTag("dots_loader").assertIsDisplayed()
    }

    /** §4.2 — в Success показываются три строки: «ОК», «ФИО: …», «Баланс: …». */
    @Test
    fun success_shows_ok_and_name_and_balance_in_three_lines() {
        compose.setContent {
            AppTheme {
                BalanceCheckWidget(
                    checkState = BalanceCheckState.Success("Иванов Иван", "1 250.50 ₽"),
                    canCheck = true,
                    onCheckClick = {},
                )
            }
        }

        // prod.md §4.2: «ОК», затем «ФИО: XXX», затем «Баланс: XXX.XX ₽» — три отдельные строки.
        compose.onNodeWithText("ОК").assertIsDisplayed()
        compose.onNodeWithText("ФИО: Иванов Иван").assertIsDisplayed()
        compose.onNodeWithText("Баланс: 1 250.50 ₽").assertIsDisplayed()
    }

    /** §4.2 — в Error показывается слово «Ошибка» и текст причины. */
    @Test
    fun error_shows_error_word_and_message() {
        compose.setContent {
            AppTheme {
                BalanceCheckWidget(
                    checkState = BalanceCheckState.Error("no network"),
                    canCheck = true,
                    onCheckClick = {},
                )
            }
        }
        compose.onNodeWithText("Ошибка").assertIsDisplayed()
        compose.onNodeWithText("no network").assertIsDisplayed()
    }
}
