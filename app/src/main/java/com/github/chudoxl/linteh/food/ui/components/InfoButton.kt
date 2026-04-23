package com.github.chudoxl.linteh.food.ui.components

import ThemeDarknessPreviewParamProvider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.github.chudoxl.linteh.food.R
import com.github.chudoxl.linteh.food.ui.theme.AppTheme

@Composable
fun InfoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) = IconButton(
    onClick = onClick,
    modifier = modifier,
    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
) {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.info_24),
        contentDescription = contentDescription,
    )
}

@Preview
@Composable
private fun InfoButtonPreview(
    @PreviewParameter(ThemeDarknessPreviewParamProvider::class) darkTheme: Boolean
) {
    AppTheme(darkTheme = darkTheme) { InfoButton(onClick = {}) }
}