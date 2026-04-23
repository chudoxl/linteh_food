package com.github.chudoxl.linteh.food.screens.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * prod.md §3.3 — иконка ошибки + последний известный баланс красным + диалог с текстом ошибки.
 */
@RunWith(AndroidJUnit4::class)
class ErrorBalanceTest {

    @get:Rule
    val compose = createComposeRule()

    /** §3.3 — клик по иконке ошибки открывает диалог с заголовком, текстом причины и кнопкой «ОК». */
    @Test
    fun clicking_error_icon_shows_dialog_with_message_and_ok_button() {
        compose.setContent {
            AppTheme {
                ErrorBalance(state = BalanceState.Error("no network"), balanceValue = "500.00 ₽")
            }
        }
        compose.onNodeWithTag("balance_error_icon").performClick()

        compose.onNodeWithTag("balance_error_dialog").assertIsDisplayed()
        compose.onNodeWithText("no network").assertIsDisplayed()
        // prod.md §3.3: «Ошибка проверки баланса» / кнопка «ОК».
        compose.onNodeWithText("Ошибка проверки баланса").assertIsDisplayed()
        compose.onNodeWithText("ОК").assertIsDisplayed()
    }

    /** §3.3 / §5 — в состоянии Error рядом с иконкой отображается последний известный баланс. */
    @Test
    fun last_known_balance_is_displayed_next_to_error_icon() {
        compose.setContent {
            AppTheme {
                ErrorBalance(state = BalanceState.Error("boom"), balanceValue = "500.00 ₽")
            }
        }
        compose.onNodeWithText("500.00 ₽").assertIsDisplayed()
    }
}
