package fr.olegueyan.algomix.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.ui.theme.AlgomixPalette

@Composable
fun AlgomixHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = AlgomixPalette.BrandBlue,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp)),
        )
        HorizontalDivider(thickness = 1.dp, color = AlgomixPalette.Divider)
    }
}
