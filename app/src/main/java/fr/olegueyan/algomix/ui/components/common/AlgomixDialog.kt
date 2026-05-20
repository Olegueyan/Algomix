package fr.olegueyan.algomix.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun AlgomixDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier.width(320.dp),
            shape = MaterialTheme.shapes.large,
            color = AlgomixPalette.SurfaceWhite,
            border = BorderStroke(1.dp, AlgomixPalette.Divider),
        ) {
            Column(
                modifier = Modifier
                    .background(AlgomixPalette.SurfaceWhite)
                    .padding(AlgomixSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.md),
            ) {
                Text(
                    text = title,
                    color = AlgomixPalette.BrandBlue,
                    style = MaterialTheme.typography.titleMedium,
                )
                content()
                if (actions != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AlgomixSpacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm, alignment = Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}
