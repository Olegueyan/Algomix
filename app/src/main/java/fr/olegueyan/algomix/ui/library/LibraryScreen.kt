package fr.olegueyan.algomix.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.LocalAppContainer
import fr.olegueyan.algomix.ui.components.common.AlgomixGhostButton
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.components.common.AlgomixPill
import fr.olegueyan.algomix.ui.components.common.AlgomixPrimaryButton
import fr.olegueyan.algomix.ui.rememberLibraryViewModel
import fr.olegueyan.algomix.ui.state.LibraryItemType
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing

@Composable
fun LibraryScreen(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = rememberLibraryViewModel(container)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val typeLabels = mapOf(
        LibraryItemType.ALL to stringResource(R.string.library_filter_all),
        LibraryItemType.SHEETS to stringResource(R.string.library_filter_sheets),
        LibraryItemType.SCRAMBLES to stringResource(R.string.library_filter_scrambles),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(PaddingValues(horizontal = AlgomixSpacing.lg, vertical = AlgomixSpacing.md)),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.library_title),
            color = AlgomixPalette.BrandBlue,
            style = MaterialTheme.typography.headlineMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
        ) {
            AlgomixPrimaryButton(
                text = stringResource(R.string.library_create_collection),
                onClick = { viewModel.createCollection("Nouvelle collection") },
                modifier = Modifier.weight(1f),
            )
            AlgomixGhostButton(
                text = stringResource(R.string.library_create_scramble),
                onClick = viewModel::showScrambleCreate,
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AlgomixPalette.SurfaceWhite, RoundedCornerShape(10.dp))
                .border(1.dp, AlgomixPalette.Divider, RoundedCornerShape(10.dp))
                .padding(AlgomixSpacing.sm),
        ) {
            BasicTextField(
                value = state.filterState.query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                textStyle = TextStyle(
                    color = AlgomixPalette.TextPrimary,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (state.filterState.query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.library_search_hint),
                            color = AlgomixPalette.TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    inner()
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
        ) {
            LibraryItemType.entries.forEach { type ->
                AlgomixPill(
                    text = typeLabels.getValue(type),
                    selected = state.filterState.itemType == type,
                    onClick = { viewModel.setItemType(type) },
                )
            }
        }

        state.feedback?.let { feedback ->
            Text(
                text = feedback.message,
                color = if (feedback.isError) AlgomixPalette.Danger else AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        when {
            state.isLoading -> Text(
                text = stringResource(R.string.library_loading),
                color = AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            state.visibleSections.isEmpty() -> Text(
                text = stringResource(R.string.library_empty),
                color = AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
            ) {
                items(state.visibleSections, key = { it.collection.id.value }) { section ->
                    CollectionSection(
                        name = section.collection.name,
                        sheetCount = section.sheets.size,
                        scrambleCount = section.scrambles.size,
                        onCreateSheet = {
                            viewModel.createSheet(section.collection.id, "Nouvelle fiche")
                        },
                        onDelete = { viewModel.deleteCollection(section.collection.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionSection(
    name: String,
    sheetCount: Int,
    scrambleCount: Int,
    onCreateSheet: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AlgomixPalette.SurfaceWhite, RoundedCornerShape(12.dp))
            .border(1.dp, AlgomixPalette.Divider, RoundedCornerShape(12.dp))
            .padding(AlgomixSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = name, color = AlgomixPalette.BrandBlue, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "$sheetCount fiches · $scrambleCount mélanges",
                color = AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.xs),
        ) {
            AlgomixGhostButton(
                text = stringResource(R.string.library_create_sheet),
                onClick = onCreateSheet,
                modifier = Modifier.weight(1f),
            )
            AlgomixOutlinedButton(
                text = stringResource(R.string.library_delete),
                onClick = onDelete,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
