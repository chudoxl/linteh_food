package com.github.chudoxl.linteh.food

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * prod.md §14.1, §14.2 — точки входа на экраны «О приложении» и «Лицензии».
 */
@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    /** §14.1 — клик по info-кнопке в шапке главного экрана открывает экран «О приложении». */
    @Test
    fun about_screen_shown_when_info_button_clicked() {
        compose.onNodeWithTag("about_button").performClick()

        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithTag("about_screen").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("about_screen").assertIsDisplayed()
        // prod.md §14.1: заголовок шапки — «О приложении».
        compose.onNodeWithText("О приложении").assertIsDisplayed()
    }

    /** §14.2 — из «О приложении» клик по кнопке «Лицензии» открывает экран лицензий. */
    @Test
    fun licenses_screen_shown_when_libraries_button_clicked() {
        compose.onNodeWithTag("about_button").performClick()

        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithTag("about_screen").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("licenses_button").performScrollTo().performClick()

        compose.waitUntil(timeoutMillis = 3_000) {
            compose.onAllNodesWithTag("licenses_screen").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("licenses_screen").assertIsDisplayed()
        // prod.md §14.2: заголовок шапки — «Лицензии».
        compose.onNodeWithText("Лицензии").assertIsDisplayed()
    }
}
