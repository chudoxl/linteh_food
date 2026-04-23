package com.github.chudoxl.linteh.food.ui.components

import ThemeDarknessPreviewParamProvider
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.github.chudoxl.linteh.food.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun DotsLoader(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    stepDelay: Duration = 500.milliseconds,
) {
    var step by remember { mutableIntStateOf(1) }
    LaunchedEffect(stepDelay) {
        while (true) {
            delay(stepDelay)
            step = if (step >= 3) 1 else step + 1
        }
    }
    Text(
        modifier = modifier,
        text = ".".repeat(step),
        style = style,
        color = color,
    )
}

@Preview(showBackground = true)
@Composable
private fun DotsLoaderPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean,
) {
    AppTheme(darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Запрос баланса",
                    style = MaterialTheme.typography.bodyMedium,
                )
                DotsLoader(style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
