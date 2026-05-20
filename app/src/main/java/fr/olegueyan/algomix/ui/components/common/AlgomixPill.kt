package fr.olegueyan.algomix.ui.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.PillShape

@Composable
fun AlgomixPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (selected) AlgomixPalette.OrangeSoftBg else AlgomixPalette.SurfaceWhite
    val borderColor = if (selected) AlgomixPalette.OrangeSoftBorder else AlgomixPalette.Divider
    val contentColor: Color = if (selected) AlgomixPalette.OrangeOnSoft else AlgomixPalette.TextPrimary
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .border(BorderStroke(1.dp, borderColor), PillShape)
            .background(container, PillShape)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 10.dp, vertical = 4.dp)),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(text = text, color = contentColor, style = MaterialTheme.typography.labelMedium)
    }
}
