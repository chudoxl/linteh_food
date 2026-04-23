package com.github.chudoxl.linteh.food.screens.main

import ThemeDarknessPreviewParamProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.github.chudoxl.linteh.food.R
import com.github.chudoxl.linteh.food.model.BalanceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ErrorBalance(state: BalanceState.Error, balanceValue: String?) {
    val lineHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodyLarge.lineHeight.toDp()
    }
    var showAlertDialog by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            modifier = Modifier
                .size(lineHeight)
                .testTag("balance_error_icon")
                .clickable(
                    onClick = { showAlertDialog = true }
                ),
            imageVector = ImageVector.vectorResource(R.drawable.ic_error_24),
            contentDescription = state.errMsg,
            tint = MaterialTheme.colorScheme.error
        )
        balanceValue?.let { lastBalance ->
            Text(
                modifier = Modifier
                    .defaultMinSize(minWidth = 64.dp)
                    .testTag("student_balance"),
                text = lastBalance,
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    if (showAlertDialog)  state.errMsg?.let { errMsg ->
        AlertDialog(
            modifier = Modifier.testTag("balance_error_dialog"),
            onDismissRequest = { showAlertDialog = false },
            title = { Text("Ошибка проверки баланса") },
            text = { Text(text = errMsg) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("balance_error_ok"),
                    onClick = { showAlertDialog = false },
                    content = { Text("ОК") }
                )
            }
        )
    }
}

@Preview
@Composable
private fun ErrorBalancePreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    ErrorBalance(
        state = BalanceState.Error(null),
        balanceValue = if (darkTheme) "600,89 ₽" else null,
    )
}