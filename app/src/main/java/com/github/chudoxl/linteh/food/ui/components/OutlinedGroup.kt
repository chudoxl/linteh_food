package com.github.chudoxl.linteh.food.ui.components

import ThemeDarknessPreviewParamProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.github.chudoxl.linteh.food.ui.theme.AppTheme

private val LabelOffset = 8.dp

@Composable
fun OutlinedGroup(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    shape: Shape = RoundedCornerShape(4.dp),
    borderStroke: BorderStroke = BorderStroke(
        width = 1.dp,
        color = if (enabled) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    ),
    titleStyle: TextStyle = MaterialTheme.typography.bodySmall,
    titleColor: Color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val contentColor = if (enabled) LocalContentColor.current
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LabelOffset)
                .border(border = borderStroke, shape = shape)
                .padding(contentPadding),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
        Text(
            text = title,
            style = titleStyle,
            color = titleColor,
            modifier = Modifier
                .offset(x = 12.dp)
                .background(containerColor)
                .padding(horizontal = 4.dp),
        )
    }
}

@Preview(widthDp = 320, heightDp = 260, name = "OutlinedGroup")
@Composable
private fun OutlinedGroupPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean,
) {
    AppTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedGroup(
                    title = "Номер карты",
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("33333")
                }
                OutlinedGroup(
                    title = "Номер карты",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                ) {
                    Text("4444")
                }
            }
        }
    }
}
