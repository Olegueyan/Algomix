package fr.olegueyan.algomix.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import fr.olegueyan.algomix.ui.components.common.AlgomixIconButton
import fr.olegueyan.algomix.ui.components.rubik.RubikCubeView
import fr.olegueyan.algomix.ui.state.SharedCubeUiState
import fr.olegueyan.algomix.ui.theme.AlgomixPalette
import fr.olegueyan.algomix.ui.theme.AlgomixSpacing
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel

@Composable
fun RubikCubeStage(
    state: SharedCubeUiState,
    viewModel: SharedCubeViewModel,
    modifier: Modifier = Modifier,
) {
    val viewRef = remember { mutableStateOf<RubikCubeView?>(null) }

    LaunchedEffect(viewRef.value) {
        val view = viewRef.value ?: return@LaunchedEffect
        viewModel.animationEvents.collect { event ->
            view.playMove(event.move, event.finalState)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                RubikCubeView(context).also { view ->
                    view.onDoubleTapResetListener = {
                        val target = viewModel.computeResetTargetQuaternion()
                        view.resetRotation(target)
                    }
                    viewRef.value = view
                }
            },
            update = { view ->
                view.setRotationLocked(state.rotationLocked)
                view.renderCube(state.cubeState)
            },
            modifier = Modifier.fillMaxSize(),
        )
        AlgomixIconButton(
            icon = if (state.rotationLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
            contentDescription = if (state.rotationLocked) "Unlock" else "Lock",
            onClick = viewModel::toggleRotationLock,
            tint = if (state.rotationLocked) AlgomixPalette.OrangePrimary else AlgomixPalette.TextMuted,
            container = AlgomixPalette.SurfaceWhite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AlgomixSpacing.sm),
        )
    }
}
