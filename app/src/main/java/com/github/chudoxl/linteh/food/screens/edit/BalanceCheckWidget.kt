package com.github.chudoxl.linteh.food.screens.edit

import ThemeDarknessPreviewParamProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.github.chudoxl.linteh.food.R
import com.github.chudoxl.linteh.food.screens.edit.data.BalanceCheckState
import com.github.chudoxl.linteh.food.ui.components.DotsLoader
import com.github.chudoxl.linteh.food.ui.components.OutlinedGroup
import com.github.chudoxl.linteh.food.ui.theme.AppTheme

@Composable
fun BalanceCheckWidget(
    checkState: BalanceCheckState,
    canCheck: Boolean,
    onCheckClick: () -> Unit = {},
) = OutlinedGroup (
    title = "Проверить",
    contentPadding = PaddingValues(8.dp),
    modifier = Modifier
        .fillMaxWidth()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            modifier = Modifier
                .size(26.dp)
                .testTag("btn_check_balance"),
            enabled = canCheck && checkState !is BalanceCheckState.Loading,
            onClick = onCheckClick,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.play_24),
                contentDescription = "Начать проверку",
            )
        }

        Column(modifier = Modifier.weight(1f, fill = true)) {
            when (checkState) {
                is BalanceCheckState.Loading -> {
                    Row(
                        modifier = Modifier.testTag("balance_check_status"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Запрос баланса",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        DotsLoader(
                            modifier = Modifier.testTag("dots_loader"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is BalanceCheckState.Success -> {
                    Row(
                        modifier = Modifier.testTag("balance_check_status"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Запрос баланса...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            modifier = Modifier.testTag("balance_check_status"),
                            text = "ОК",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = "ФИО: ${checkState.studentName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Баланс: ${checkState.balance}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                is BalanceCheckState.Error -> {
                    Row(
                        modifier = Modifier.testTag("balance_check_status"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Запрос баланса...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            modifier = Modifier.testTag("balance_check_status"),
                            text = "Ошибка",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                    Text(
                        modifier = Modifier.testTag("balance_check_value"),
                        text = checkState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BalanceCheckState.Idle -> {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckWidgetIdleEnabledPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme) {
        BalanceCheckWidget(
            checkState = BalanceCheckState.Idle,
            canCheck = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckWidgetIdleDisabledPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme) {
        BalanceCheckWidget(
            checkState = BalanceCheckState.Idle,
            canCheck = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckWidgetLoadingPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme) {
        BalanceCheckWidget(
            checkState = BalanceCheckState.Loading,
            canCheck = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckWidgetSuccessPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme) {
        BalanceCheckWidget(
            checkState = BalanceCheckState.Success(
                studentName = "Иванов Иван",
                balance = "1 250,50 ₽",
            ),
            canCheck = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCheckWidgetErrorPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme) {
        BalanceCheckWidget(
            checkState = BalanceCheckState.Error(message = "Нет соединения с сервером"),
            canCheck = true,
        )
    }
}