package com.github.chudoxl.linteh.food.screens.main

import ThemeDarknessPreviewParamProvider
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.github.chudoxl.linteh.food.R
import com.github.chudoxl.linteh.food.model.BalanceState
import com.github.chudoxl.linteh.food.model.Student
import com.github.chudoxl.linteh.food.screens.main.data.StudentState
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import com.github.chudoxl.linteh.food.utils.copyToClipboard
import kotlinx.datetime.LocalTime
import java.math.BigDecimal

@Composable
fun StudentItem(
    state: StudentState,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onTopUpClick: () -> Unit = {},
) = Column (
    modifier = Modifier
        .background(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow)
        .padding(12.dp)
        .fillMaxWidth()
        .testTag("student_item_${state.cardNumber}")
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val lineHeight = with(LocalDensity.current) {
            MaterialTheme.typography.bodyLarge.lineHeight.toDp()
        }
        Text(
            modifier = Modifier.testTag("student_name"),
            text = state.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.weight(1f, fill = true))
        when (state.balanceState) {
            is BalanceState.Error -> ErrorBalance(state.balanceState, state.balanceValue)
            BalanceState.Loaded -> Text(
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp)
                    .testTag("student_balance"),
                text = state.balanceValue,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            BalanceState.Refreshing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(lineHeight).testTag("student_balance"),
                    strokeWidth = 1.dp
                )
            }
            BalanceState.Unknown -> Text(
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp)
                    .testTag("student_balance"),
                text = "n/a",
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Text(
        modifier = Modifier.testTag("student_card_number"),
        text = state.cardNumberPretty,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(
        modifier = Modifier.testTag("student_notification_line"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val lineHeight = with(LocalDensity.current) {
            MaterialTheme.typography.bodySmall.lineHeight.toDp()
        }
        val notificationRes = remember(state.notificationEnabled) {
            if (state.notificationEnabled) R.drawable.ic_notification_on_24 else R.drawable.ic_notification_off_24
        }
        Icon(
            modifier = Modifier.size(lineHeight),
            imageVector = ImageVector.vectorResource(notificationRes),
            contentDescription = state.notificationDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = state.notificationDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        ItemButton(
            modifier = Modifier.testTag("btn_edit"),
            icon = R.drawable.edit_24,
            text = "Редактировать",
            withText = false,
            onClick = onEditClick
        )
        Spacer(Modifier.width(4.dp))
        ItemButton(
            modifier = Modifier.testTag("btn_delete"),
            icon = R.drawable.delete_24,
            text = "Удалить",
            withText = false,
            onClick = { showDeleteDialog = true }
        )
        Spacer(Modifier
            .weight(1f, fill = true)
            .height(28.dp))
        ItemButton(
            modifier = Modifier.testTag("btn_copy"),
            icon = R.drawable.content_copy_24,
            text = "Скопировать №",
            onClick = { context.copyToClipboard(textToCopy = state.cardNumber) }
        )
        Spacer(Modifier.width(8.dp))
        ItemButton(
            modifier = Modifier.testTag("btn_topup"),
            icon = R.drawable.hlynov_24,
            text = "Пополнить",
            onClick = onTopUpClick
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "Удалить ученика?") },
            text = { Text(text = "Данные ученика ${state.name} и его напоминания будут удалены") },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("btn_delete_confirm"),
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text(text = "Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.testTag("btn_delete_cancel"),
                    onClick = { showDeleteDialog = false }
                ) {
                    Text(text = "Отмена")
                }
            },
        )
    }
}

@Composable
private fun ItemButton(
    modifier: Modifier = Modifier,
    text: String = "",
    withText: Boolean = true,
    @DrawableRes icon: Int,
    onClick: () -> Unit
) {
    val lineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodySmall.lineHeight.toDp()
    }
    if (withText) TextButton (
        modifier = modifier.height(28.dp),
        contentPadding = PaddingValues(4.dp, 2.dp, 4.dp, 2.dp),
        shape = MaterialTheme.shapes.small,
        onClick = onClick,
        content = {
            Icon(
                modifier = Modifier.size(lineHeight),
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = text,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    ) else IconButton (
        modifier = modifier.size(28.dp),
        onClick = onClick,
        content = {
            Icon(
                modifier = Modifier.size(lineHeight),
                imageVector = ImageVector.vectorResource( icon),
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}


@Preview(showBackground = true)
@Composable
private fun StudentItemPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) {
        StudentItem(
            state = StudentState(
                student = Student(
                    name = "Терентий",
                    cardNumber = "001234567",
                    password = "XXX",
                    balance = BigDecimal("3600.99"),
                    balanceState = if (darkTheme) BalanceState.Refreshing else BalanceState.Loaded,
                    notificationEnabled = darkTheme,
                    notificationTime = LocalTime(hour = 6, minute = 30),
                    notificationThreshold = BigDecimal(2556)
                )
            )
        )
    }
}

