package com.github.chudoxl.linteh.food.screens.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.screens.edit.data.EditScreenState
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import kotlinx.datetime.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

/**
 * prod.md §4.1 — заголовок «Новый ученик» в режиме добавления, «Данные ученика» в редактировании.
 * prod.md §4.5 — поля Имя, Порог, Время неактивны, пока не выполнена успешная проверка
 * (в режиме добавления; в редактировании — после смены пароля до повторной успешной проверки).
 */
@RunWith(AndroidJUnit4::class)
class EditScreenContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val noOpListener = object : EditScreenUiListener {}

    private val existing = Student(
        name = "Иван",
        cardNumber = "001002003",
        password = "pwd",
        balanceState = BalanceState.Loaded,
        balance = BigDecimal("100.00"),
        notificationEnabled = true,
        notificationTime = LocalTime(hour = 7, minute = 0),
        notificationThreshold = BigDecimal(200),
    )

    private val successCheck = BalanceCheckState.Success("Иванов Иван", "100.00 ₽")

    // --- §4.1 titles ---------------------------------------------------------

    /** §4.1 — в режиме добавления заголовок «Новый ученик». */
    @Test
    fun add_mode_title_is_new_student() {
        compose.setContent {
            AppTheme {
                EditScreenContent(
                    state = EditScreenState.Ready(current = Student.Empty),
                    uiListener = noOpListener,
                    onBack = {},
                )
            }
        }

        // prod.md §4.1: в режиме добавления «в шапке показывается "Новый ученик"».
        compose.onNodeWithText("Новый ученик").assertIsDisplayed()
    }

    /** §4.1 — в режиме редактирования заголовок «Данные ученика». */
    @Test
    fun edit_mode_title_is_student_data() {
        compose.setContent {
            AppTheme {
                EditScreenContent(
                    state = EditScreenState.Ready(current = existing, initial = existing),
                    uiListener = noOpListener,
                    onBack = {},
                )
            }
        }

        // prod.md §4.1: в режиме редактирования — «Данные ученика».
        compose.onNodeWithText("Данные ученика").assertIsDisplayed()
    }

    /** §4.1 — состояние Loading (только edit-режим) показывает круговой индикатор с заголовком «Данные ученика». */
    @Test
    fun loading_state_keeps_student_data_title() {
        compose.setContent {
            AppTheme {
                EditScreenContent(
                    state = EditScreenState.Loading,
                    uiListener = noOpListener,
                    onBack = {},
                )
            }
        }
        // Loading возникает только в режиме редактирования (см. EditViewModel.init).
        compose.onNodeWithText("Данные ученика").assertIsDisplayed()
        compose.onNodeWithTag("edit_loading").assertIsDisplayed()
    }

    // --- §4.5 fields enabled -------------------------------------------------

    /** §4.5 — в режиме добавления без проверки поле «Имя» неактивно. */
    @Test
    fun add_mode_idle_name_field_is_disabled() {
        renderReady(current = filledNewStudent(), checkState = BalanceCheckState.Idle)
        // prod.md §4.5 п.1: «остальные поля формы остаются неактивными, пока пользователь
        // не выполнит успешную проверку баланса».
        compose.onNodeWithTag("input_name").assertIsNotEnabled()
    }

    /** §4.5 — в режиме добавления без проверки поле «Порог» неактивно. */
    @Test
    fun add_mode_idle_threshold_field_is_disabled() {
        renderReady(current = filledNewStudent(), checkState = BalanceCheckState.Idle)
        compose.onNodeWithTag("input_threshold").assertIsNotEnabled()
    }

    /** §4.5 — успешная проверка в режиме добавления делает Имя и Порог активными. */
    @Test
    fun add_mode_success_enables_other_fields() {
        renderReady(current = filledNewStudent(), checkState = successCheck)
        compose.onNodeWithTag("input_name").assertIsEnabled()
        compose.onNodeWithTag("input_threshold").assertIsEnabled()
    }

    /** §4.5 — в режиме редактирования без смены credentials Имя и Порог активны без повторной проверки. */
    @Test
    fun edit_mode_credentials_unchanged_enables_other_fields() {
        renderReady(current = existing, initial = existing, checkState = BalanceCheckState.Idle)
        // prod.md §4.5: если credentials не менялись, поля доступны без новой проверки.
        compose.onNodeWithTag("input_name").assertIsEnabled()
        compose.onNodeWithTag("input_threshold").assertIsEnabled()
    }

    /** §4.5 п.2 — смена пароля в edit-режиме делает Имя и Порог неактивными до новой проверки. */
    @Test
    fun edit_mode_password_changed_disables_other_fields() {
        renderReady(
            current = existing.copy(password = "new-pwd"),
            initial = existing,
            checkState = BalanceCheckState.Idle,
        )
        // prod.md §4.5 п.2: «если пользователь изменяет пароль, то остальные поля формы
        // становятся неактивными, пока не будет выполнена успешная проверка с новым паролем».
        compose.onNodeWithTag("input_name").assertIsNotEnabled()
        compose.onNodeWithTag("input_threshold").assertIsNotEnabled()
    }

    /** §4.5 — успешная перепроверка с новым паролем снова разблокирует поля в edit-режиме. */
    @Test
    fun edit_mode_password_changed_and_rechecked_enables_other_fields() {
        renderReady(
            current = existing.copy(password = "new-pwd"),
            initial = existing,
            checkState = successCheck,
        )
        compose.onNodeWithTag("input_name").assertIsEnabled()
        compose.onNodeWithTag("input_threshold").assertIsEnabled()
    }

    private fun filledNewStudent(cardNumber: String = "001") = Student.Empty.copy(
        cardNumber = cardNumber,
        password = "pwd",
    )

    private fun renderReady(
        current: Student,
        initial: Student? = null,
        checkState: BalanceCheckState = BalanceCheckState.Idle,
    ) {
        compose.setContent {
            AppTheme {
                EditScreenContent(
                    state = EditScreenState.Ready(
                        current = current,
                        initial = initial,
                        checkState = checkState,
                    ),
                    uiListener = noOpListener,
                    onBack = {},
                )
            }
        }
    }

    /** Кнопка «ⓘ» в шапке открывает диалог-справку по полям. */
    @Test
    fun help_button_opens_dialog() {
        compose.setContent {
            AppTheme {
                EditScreenContent(
                    state = EditScreenState.Ready(current = Student.Empty),
                    uiListener = noOpListener,
                    onBack = {},
                )
            }
        }
        compose.onNodeWithTag("help_button").assertIsDisplayed().performClick()
        compose.onNodeWithTag("help_dialog").assertIsDisplayed()
        compose.onNodeWithText("Справка по полям").assertIsDisplayed()
    }
}
