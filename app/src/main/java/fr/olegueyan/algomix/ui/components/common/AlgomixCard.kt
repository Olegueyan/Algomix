package fr.olegueyan.algomix.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun AlgomixCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = AlgomixPalette.SurfaceWhite,
        contentColor = AlgomixPalette.TextPrimary,
        border = BorderStroke(1.dp, AlgomixPalette.Divider),
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(AlgomixSpacing.md)) {
            if (title != null) {
                Text(
                    text = title,
                    color = AlgomixPalette.BrandBlue,
                    style = MaterialTheme.typography.titleSmall,
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = AlgomixSpacing.sm))
            }
            content()
        }
    }
}
