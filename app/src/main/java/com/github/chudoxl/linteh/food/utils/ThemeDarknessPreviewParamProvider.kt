import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class ThemeDarknessPreviewParamProvider : PreviewParameterProvider<Boolean> {
    override val values: Sequence<Boolean> = sequenceOf(false, true)
}