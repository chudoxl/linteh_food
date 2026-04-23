package com.github.chudoxl.linteh.food.screens.main

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.main.data.StudentState
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class StudentItemTest {

    @get:Rule
    val compose = createComposeRule()

    /** §3.3 / §13 — номер карты в карточке отображается группами по три цифры. */
    @Test
    fun card_number_is_grouped_by_three_in_ui() {
        val s = demoStudent(cardNumber = "001002003")
        compose.setContent { AppTheme { StudentItem(state = StudentState(s)) } }

        // prod.md §3.3: номер карты «001 002 003».
        compose.onNodeWithTag("student_card_number").assertIsDisplayed()
        compose.onNodeWithText("001 002 003").assertIsDisplayed()
    }

    /** §3.3 — при включённых напоминаниях показывается «Напоминать в HH:MM, если баланс < XXX ₽». */
    @Test
    fun notification_line_shows_enabled_text() {
        val s = demoStudent(notificationEnabled = true, notificationTime = LocalTime(7, 0), notificationThreshold = BigDecimal(200))
        compose.setContent { AppTheme { StudentItem(state = StudentState(s)) } }

        compose.onNodeWithText("Напоминать в 07:00, если баланс < 200 ₽").assertIsDisplayed()
    }

    /** §3.3 — при выключенных напоминаниях показывается «Не напоминать о низком балансе». */
    @Test
    fun notification_line_shows_disabled_text() {
        val s = demoStudent(notificationEnabled = false)
        compose.setContent { AppTheme { StudentItem(state = StudentState(s)) } }

        compose.onNodeWithText("Не напоминать о низком балансе").assertIsDisplayed()
    }

    /** §3.4 — «Скопировать №» помещает номер карты в системный буфер обмена. */
    @Test
    fun copy_button_copies_card_number_to_clipboard() {
        val s = demoStudent(cardNumber = "001002003")
        var capturedContext: Context? = null
        compose.setContent {
            capturedContext = LocalContext.current
            AppTheme { StudentItem(state = StudentState(s)) }
        }
        compose.onNodeWithTag("btn_copy").performClick()
        compose.waitForIdle()

        val cm = capturedContext!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // prod.md §3.4: «Скопировать №».
        assertThat(cm.primaryClip?.getItemAt(0)?.text?.toString()).isEqualTo("001002003")
    }

    /** §3.4 — клик по «Редактировать» вызывает onEditClick. */
    @Test
    fun edit_button_invokes_on_edit_click() {
        val s = demoStudent()
        var called = false
        compose.setContent {
            AppTheme {
                StudentItem(state = StudentState(s), onEditClick = { called = true })
            }
        }
        compose.onNodeWithTag("btn_edit").performClick()
        compose.waitForIdle()

        // prod.md §3.4: «Редактировать» открывает экран редактирования.
        assertThat(called).isTrue()
    }

    /** §3.4 — «Удалить» показывает диалог подтверждения с именем и кнопками. */
    @Test
    fun delete_button_shows_confirmation_dialog() {
        val s = demoStudent(name = "Иван")
        compose.setContent { AppTheme { StudentItem(state = StudentState(s)) } }

        compose.onNodeWithTag("btn_delete").performClick()
        compose.waitForIdle()

        // prod.md §3.4: диалог подтверждения удаления.
        compose.onNodeWithText("Удалить ученика?").assertIsDisplayed()
        compose.onNodeWithText("Данные ученика Иван и его напоминания будут удалены").assertIsDisplayed()
        compose.onNodeWithTag("btn_delete_confirm").assertIsDisplayed()
        compose.onNodeWithTag("btn_delete_cancel").assertIsDisplayed()
    }

    /** §3.4 — при пустом имени в теле диалога удаления используется отформатированный номер карты. */
    @Test
    fun delete_dialog_body_uses_card_number_when_name_empty() {
        val s = demoStudent(name = "", cardNumber = "001002003")
        compose.setContent { AppTheme { StudentItem(state = StudentState(s)) } }

        compose.onNodeWithTag("btn_delete").performClick()
        compose.waitForIdle()

        // prod.md §3.4: при пустом имени — отформатированный номер карты.
        compose.onNodeWithText(
            "Данные ученика 001 002 003 и его напоминания будут удалены"
        ).assertIsDisplayed()
    }

    /** §3.4 — «Удалить» в диалоге подтверждения вызывает onDeleteClick. */
    @Test
    fun delete_dialog_confirm_invokes_on_delete_click() {
        val s = demoStudent()
        var called = false
        compose.setContent {
            AppTheme {
                StudentItem(state = StudentState(s), onDeleteClick = { called = true })
            }
        }
        compose.onNodeWithTag("btn_delete").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("btn_delete_confirm").performClick()
        compose.waitForIdle()

        assertThat(called).isTrue()
    }

    /** §3.4 — «Отмена» в диалоге удаления закрывает его без вызова onDeleteClick. */
    @Test
    fun delete_dialog_cancel_does_not_invoke_on_delete_click() {
        val s = demoStudent()
        var called = false
        compose.setContent {
            AppTheme {
                StudentItem(state = StudentState(s), onDeleteClick = { called = true })
            }
        }
        compose.onNodeWithTag("btn_delete").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("btn_delete_cancel").performClick()
        compose.waitForIdle()

        assertThat(called).isFalse()
        compose.onNodeWithText("Удалить ученика?").assertDoesNotExist()
    }

    /** §3.4 / §8 — кнопка «Пополнить» вызывает onTopUpClick (запуск Хлынов Банка). */
    @Test
    fun topup_button_invokes_on_topup_click() {
        val s = demoStudent()
        var called = false
        compose.setContent {
            AppTheme {
                StudentItem(state = StudentState(s), onTopUpClick = { called = true })
            }
        }
        compose.onNodeWithTag("btn_topup").performClick()
        compose.waitForIdle()

        // prod.md §3.4: «Пополнить» запускает Хлынов Банк — запуск инициируется через callback.
        assertThat(called).isTrue()
    }

    private fun demoStudent(
        name: String = "Иван",
        cardNumber: String = "001002003",
        notificationEnabled: Boolean = true,
        notificationTime: LocalTime = LocalTime(7, 0),
        notificationThreshold: BigDecimal = BigDecimal(200),
    ) = Student(
        name = name,
        cardNumber = cardNumber,
        password = "pwd",
        balance = BigDecimal("100.00"),
        balanceState = BalanceState.Loaded,
        notificationEnabled = notificationEnabled,
        notificationTime = notificationTime,
        notificationThreshold = notificationThreshold,
    )
}
