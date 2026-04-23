package com.github.chudoxl.linteh.food.screens.main

import ThemeDarknessPreviewParamProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.github.chudoxl.linteh.food.ui.theme.AppTheme

@Composable
fun EmptyWidget(
    modifier: Modifier = Modifier,
    onAddItemClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Добавьте данные ученика, чтобы начать отслеживать баланс на его счёте питания",
            textAlign = TextAlign.Center
        )
        OutlinedButton(
            modifier = Modifier
                .height(36.dp)
                .testTag("empty_add_button"),
            onClick = onAddItemClick,
            shape = MaterialTheme.shapes.medium,
            content = { Text("Добавить")}
        )
    }
}

@Preview
@Composable
private fun EmptyWidgetPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme) { EmptyWidget() }
}