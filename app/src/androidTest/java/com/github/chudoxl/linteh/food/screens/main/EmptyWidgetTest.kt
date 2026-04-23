package com.github.chudoxl.linteh.food.screens.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * prod.md §3.2 — пустое состояние главного экрана.
 */
@RunWith(AndroidJUnit4::class)
class EmptyWidgetTest {

    @get:Rule
    val compose = createComposeRule()

    /** §3.2 — пустое состояние показывает пояснительный текст и кликабельную кнопку «Добавить». */
    @Test
    fun empty_widget_shows_spec_text_and_add_button() {
        var clicks = 0
        compose.setContent {
            AppTheme { EmptyWidget(onAddItemClick = { clicks++ }) }
        }

        compose.onNodeWithText(
            "Добавьте данные ученика, чтобы начать отслеживать баланс на его счёте питания"
        ).assertIsDisplayed()
        compose.onNodeWithTag("empty_add_button").assertIsDisplayed().performClick()
        assertThat(clicks).isEqualTo(1)
    }
}
