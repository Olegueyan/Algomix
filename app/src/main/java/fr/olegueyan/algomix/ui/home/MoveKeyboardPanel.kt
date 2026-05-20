package fr.olegueyan.algomix.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.olegueyan.algomix.ui.components.common.AlgomixPill
import fr.olegueyan.algomix.ui.components.common.MoveIconRegistry
import fr.olegueyan.algomix.ui.state.MoveKeyboardCategory
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun MoveKeyboardPanel(
    selectedCategory: MoveKeyboardCategory,
    onCategorySelected: (MoveKeyboardCategory) -> Unit,
    onMoveSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlgomixPalette.SurfaceWhite, RoundedCornerShape(12.dp))
            .border(1.dp, AlgomixPalette.Divider, RoundedCornerShape(12.dp))
            .padding(AlgomixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs, Alignment.CenterHorizontally),
        ) {
            MoveKeyboardCategory.entries.forEach { category ->
                AlgomixPill(
                    text = category.label,
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                )
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(KEYBOARD_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
            modifier = Modifier.fillMaxWidth().height(MOVE_GRID_HEIGHT),
            userScrollEnabled = false,
        ) {
            items(selectedCategory.moves) { notation ->
                MoveKey(notation = notation, onClick = { onMoveSelected(notation) })
            }
        }
    }
}

@Composable
private fun MoveKey(notation: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, AlgomixPalette.Divider, RoundedCornerShape(10.dp))
            .background(AlgomixPalette.SurfaceWhite, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 4.dp, vertical = 4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(MoveIconRegistry.iconForNotation(notation)),
            contentDescription = notation,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = notation,
            color = AlgomixPalette.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private const val KEYBOARD_COLUMNS = 6
private val MOVE_GRID_HEIGHT = 168.dp
