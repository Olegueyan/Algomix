package fr.olegueyan.algomix.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.components.common.AlgomixDialog
import fr.olegueyan.algomix.ui.components.common.AlgomixGhostButton
import fr.olegueyan.algomix.ui.components.common.AlgomixOutlinedButton
import fr.olegueyan.algomix.ui.components.common.AlgomixPrimaryButton
import fr.olegueyan.algomix.ui.components.common.AlgomixSegmented
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel

@Composable
fun HomeScreen(
    viewModel: SharedCubeViewModel,
    onRequestScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.homeUiState.feedbackMessage) {
        // Feedback messages currently rendered inline; consumption could be added later.
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(PaddingValues(horizontal = AlgomixSpacing.lg)),
        verticalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = AlgomixPalette.BrandBlue,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = AlgomixSpacing.md),
        )

        val labels = mapOf(
            HomeMode.VISUALIZATION to stringResource(R.string.home_mode_visualization),
            HomeMode.FREE to stringResource(R.string.home_mode_free),
            HomeMode.PLAY to stringResource(R.string.home_mode_play),
            HomeMode.EDIT to stringResource(R.string.home_mode_edit),
        )
        AlgomixSegmented(
            options = HomeMode.entries,
            selected = state.homeMode,
            onSelect = viewModel::setHomeMode,
            label = { mode -> labels.getValue(mode) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AlgomixSpacing.sm),
        ) {
            AlgomixGhostButton(
                text = stringResource(R.string.home_scan_cube),
                onClick = {
                    viewModel.requestScan()
                    onRequestScan()
                },
                modifier = Modifier.weight(1f),
            )
            AlgomixGhostButton(
                text = stringResource(R.string.home_scramble),
                onClick = { viewModel.scramble() },
                modifier = Modifier.weight(1f),
            )
        }

        state.homeUiState.feedbackMessage?.let { feedback ->
            Text(
                text = feedback,
                color = AlgomixPalette.OrangeOnSoft,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AlgomixSpacing.xs),
            )
        }

        RubikCubeStage(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.weight(1f, fill = true),
        )

        when (state.homeMode) {
            HomeMode.VISUALIZATION -> Unit
            HomeMode.FREE -> MoveKeyboardPanel(
                selectedCategory = state.homeUiState.keyboardCategory,
                onCategorySelected = viewModel::setKeyboardCategory,
                onMoveSelected = viewModel::applyMoveToken,
            )
            HomeMode.PLAY -> PlaybackPanel(
                playbackState = state.playbackState,
                autoPlayEnabled = state.homeUiState.autoPlayEnabled,
                onPrev = viewModel::playPrevious,
                onNext = viewModel::playNext,
                onReset = viewModel::resetPlayback,
                onToggleAuto = viewModel::toggleAutoPlay,
                onToggleLoop = viewModel::toggleLoop,
                onCycleSpeed = viewModel::cyclePlaybackSpeed,
                onLoadAlgorithm = viewModel::requestLoadAlgorithm,
                onLoadScramble = viewModel::requestLoadScramble,
            )
            HomeMode.EDIT -> {
                EditActionsBar(
                    onSave = viewModel::requestSaveEditing,
                    onUndo = viewModel::undoEditing,
                    onRedo = viewModel::redoEditing,
                    onSuppress = viewModel::suppressLastEditingMove,
                    onDeleteAll = { showDeleteConfirm = true },
                )
                MoveKeyboardPanel(
                    selectedCategory = state.homeUiState.keyboardCategory,
                    onCategorySelected = viewModel::setKeyboardCategory,
                    onMoveSelected = viewModel::applyMoveToken,
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlgomixDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = stringResource(R.string.home_delete_all_title),
            actions = {
                AlgomixOutlinedButton(
                    text = stringResource(R.string.home_delete_all_cancel),
                    onClick = { showDeleteConfirm = false },
                )
                AlgomixPrimaryButton(
                    text = stringResource(R.string.home_delete_all_confirm),
                    onClick = {
                        viewModel.deleteAllEditing()
                        showDeleteConfirm = false
                    },
                )
            },
        ) {
            Text(
                text = stringResource(R.string.home_delete_all_message),
                color = AlgomixPalette.TextMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
