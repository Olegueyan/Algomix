package fr.olegueyan.algomix.ui.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.ui.theme.AlgomixPalette

@Composable
fun <T> AlgomixSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, AlgomixPalette.Divider, shape)
            .background(AlgomixPalette.SurfaceWhite, shape)
            .padding(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container = if (isSelected) AlgomixPalette.OrangeSoftBg else AlgomixPalette.SurfaceWhite
            val content = if (isSelected) AlgomixPalette.OrangeOnSoft else AlgomixPalette.TextMuted
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(container, RoundedCornerShape(8.dp))
                    .clickable { onSelect(option) }
                    .padding(PaddingValues(horizontal = 6.dp, vertical = 4.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label(option), color = content, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
