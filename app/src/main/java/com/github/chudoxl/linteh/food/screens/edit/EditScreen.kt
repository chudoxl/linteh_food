package com.github.chudoxl.linteh.food.screens.edit

import ThemeDarknessPreviewParamProvider
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.chudoxl.linteh.food.App
import com.github.chudoxl.linteh.food.R
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.screens.edit.data.EditScreenState
import com.github.chudoxl.linteh.food.ui.components.InfoButton
import com.github.chudoxl.linteh.food.ui.components.OutlinedGroup
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import dev.darkokoa.datetimewheelpicker.WheelTimePicker
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import kotlin.time.Clock

@Composable
fun EditScreen(
    cardNumber: String?,
    onBack: () -> Unit,
) {
    val viewModel: EditViewModel = viewModel(
        factory = EditViewModel.Companion.Factory(
            app = LocalContext.current.applicationContext as App,
            cardNumber = cardNumber
        ),
    )
    val state by viewModel.state.collectAsState()
    EditScreenContent(
        state = state,
        uiListener = viewModel.uiListener,
        onBack = onBack,
    )
}

@Composable
internal fun EditScreenContent(
    state: EditScreenState,
    uiListener: EditScreenUiListener,
    onBack: () -> Unit,
) {
    when (state) {
        is EditScreenState.Loading -> LoadingContent(onBack = onBack)
        is EditScreenState.Ready -> ReadyContent(
            state = state,
            uiListener = uiListener,
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingContent(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier
            .testTag("edit_loading"),
        topBar = { EditTopBar(title = "Данные ученика", onBack = onBack) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyContent(
    state: EditScreenState.Ready,
    uiListener: EditScreenUiListener,
    onBack: () -> Unit,
) {
    var showExitDialog by remember { mutableStateOf(false) }
    val onBackRequested: () -> Unit = {
        if (state.hasUnsavedChanges) showExitDialog = true else onBack()
    }

    BackHandler(enabled = state.hasUnsavedChanges) {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitDialog(onConfirmExit = onBack, onDismiss = { showExitDialog = false })
    }

    if (state.showDuplicateDialog) {
        DuplicateDialog(
            cardNumber = state.cardNumber,
            onDismiss = uiListener::onDuplicateDialogDismissed,
        )
    }

    val passwordFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val thresholdFocus = remember { FocusRequester() }

    var passwordFv by remember {
        mutableStateOf(TextFieldValue(state.password, selection = TextRange(index = state.password.length)))
    }
    LaunchedEffect(state.password) {
        if (passwordFv.text != state.password) {
            passwordFv = TextFieldValue(state.password, selection = TextRange(index = state.password.length))
        }
    }

    var nameFv by remember {
        mutableStateOf(TextFieldValue(state.name, selection = TextRange(index = state.name.length)))
    }
    LaunchedEffect(state.name) {
        if (nameFv.text != state.name) {
            nameFv = TextFieldValue(state.name, selection = TextRange(index = state.name.length))
        }
    }

    var thresholdFv by remember {
        mutableStateOf(TextFieldValue(state.thresholdInput, selection = TextRange(index = state.thresholdInput.length)))
    }
    LaunchedEffect(state.thresholdInput) {
        if (thresholdFv.text != state.thresholdInput) {
            thresholdFv = TextFieldValue(state.thresholdInput, selection = TextRange(index = state.thresholdInput.length))
        }
    }

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .testTag("edit_screen"),
        topBar = {
            EditTopBar(
                title = if (state.isEditing) "Данные ученика" else "Новый ученик",
                onBack = onBackRequested,
            )
        },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                HorizontalDivider()
                Button(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .testTag("btn_save"),
                    enabled = state.canSave,
                    onClick = { uiListener.save(onBack) }
                ) {
                    Text("Сохранить")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_card_number"),
                value = state.cardNumber,
                onValueChange = uiListener::updateCardNumber,
                label = { Text("Номер карты") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    passwordFv = passwordFv.copy(selection = TextRange(passwordFv.text.length))
                    passwordFocus.requestFocus()
                }),
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus)
                    .testTag("input_password"),
                value = passwordFv,
                onValueChange = {
                    passwordFv = it
                    if (it.text != state.password) uiListener.updatePassword(it.text)
                },
                label = { Text("Пароль") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = if (state.canSave) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = if (state.canSave) KeyboardActions.Default else KeyboardActions(
                    onNext = {
                        nameFv = nameFv.copy(selection = TextRange(nameFv.text.length))
                        nameFocus.requestFocus()
                    }
                ),
            )

            Spacer(Modifier.height(8.dp))

            BalanceCheckWidget(
                checkState = state.checkState,
                canCheck = state.canCheck,
                onCheckClick = uiListener::checkBalance,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus)
                    .testTag("input_name"),
                value = state.name,
                onValueChange = uiListener::updateName,
                label = { Text("Имя") },
                enabled = state.canSave,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = if (state.canSave) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = if (state.canSave) KeyboardActions.Default else KeyboardActions(
                    onNext = {
                        thresholdFv = thresholdFv.copy(selection = TextRange(thresholdFv.text.length))
                        thresholdFocus.requestFocus()
                    }
                ),
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Напоминать",
                    color = if (state.canSave) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    modifier = Modifier.testTag("switch_notifications"),
                    enabled = state.canSave,
                    checked = state.notificationEnabled,
                    onCheckedChange = uiListener::updateNotificationEnabled,
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(thresholdFocus)
                    .testTag("input_threshold"),
                value = state.notificationThreshold,
                onValueChange = uiListener::updateBalanceThreshold,
                label = { Text("если баланс меньше, ₽") },
                singleLine = true,
                enabled = state.notificationEnabled && state.canSave,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
            )

            Spacer(Modifier.height(8.dp))

            WheelTime(
                enabled = state.notificationEnabled && state.canSave,
                initialTime = state.initialTime,
                updateTime = uiListener::updateNotificationTime,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}


@Composable
private fun WheelTime(
    enabled: Boolean,
    initialTime: LocalTime?,
    updateTime: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) = OutlinedGroup(
    enabled = enabled,
    title = "каждый день в",
    modifier = modifier,
) {
    val startTime: LocalTime by remember {
        mutableStateOf(
            initialTime
                ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
        )
    }
    WheelTimePicker(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .nestedScroll(ConsumeVerticalScroll)
            .alpha(if (enabled) 1f else 0.38f)
            .testTag("wheel_time")
            .then(
                if (enabled) Modifier
                else Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                                .changes.forEach { it.consume() }
                        }
                    }
                }
            ),
        startTime = startTime,
        size = DpSize(width = 200.dp, height = 128.dp),
        onSnappedTime = updateTime,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTopBar(title: String, onBack: () -> Unit) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.arrow_back_24),
                    contentDescription = "Назад",
                )
            }
        },
        title = { Text(text = title) },
        actions = {
            InfoButton(
                onClick = { showHelp = true },
                modifier = Modifier.testTag("help_button"),
                contentDescription = "Справка по полям",
            )
        },
    )
    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun DuplicateDialog(cardNumber: String, onDismiss: () -> Unit) {
    val pretty = cardNumber.chunked(3).joinToString(separator = " ")
    AlertDialog(
        modifier = Modifier.testTag("duplicate_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Ученик уже добавлен") },
        text = {
            Text(
                "Ученик с номером карты $pretty уже есть в списке. " +
                    "Измените номер карты или вернитесь к существующей записи на главном экране."
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("duplicate_dialog_ok"),
                onClick = onDismiss,
            ) {
                Text("ОК")
            }
        },
    )
}

@Composable
private fun ExitDialog(onConfirmExit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag("exit_dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Выйти без сохранения?") },
        text = { Text("Введённые данные не будут сохранены") },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("exit_dialog_confirm"),
                onClick = { onDismiss(); onConfirmExit() },
            ) {
                Text("Выйти")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag("exit_dialog_cancel"),
                onClick = onDismiss,
            ) {
                Text("Отмена")
            }
        },
    )
}

private val ConsumeVerticalScroll = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = Offset(x = 0f, y = available.y)

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity = Velocity(x = 0f, y = available.y)
}

private val noOpActions = object : EditScreenUiListener {}

@Preview(widthDp = 360, heightDp = 640, name = "Добавление")
@Composable
private fun EditScreenAddPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) {
        EditScreenContent(
            state = EditScreenState.Ready(current = Student.Empty),
            uiListener = noOpActions,
            onBack = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 840, name = "Редактирование")
@Composable
private fun EditScreenEditPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) {
        EditScreenContent(
            state = EditScreenState.Ready(
                current = Student(
                    name = "Анна",
                    cardNumber = "001002003",
                    password = "secret",
                    balanceState = BalanceState.Unknown,
                    balance = BigDecimal.ZERO,
                    notificationEnabled = true,
                    notificationThreshold = BigDecimal(200),
                    notificationTime = LocalTime(hour = 7, minute = 0),
                ),
                initial = Student.Empty,
                checkState = BalanceCheckState.Success(
                    studentName = "Иванова Анна",
                    balance = "1 250,00 ₽",
                )
            ),
            uiListener = noOpActions,
            onBack = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 640, name = "Загрузка")
@Composable
private fun EditScreenLoadingPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) {
        EditScreenContent(
            state = EditScreenState.Loading,
            uiListener = noOpActions,
            onBack = {},
        )
    }
}
