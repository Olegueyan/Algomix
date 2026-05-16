package fr.olegueyan.algomix.ui.state

import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.EditingSession
import fr.olegueyan.algomix.domain.cube.MoveSequence
import fr.olegueyan.algomix.domain.cube.PlaybackState

data class SharedCubeUiState(
    val cubeState: CubeState = CubeState.solved(),
    val activeRoute: MainRoute = MainRoute.HOME,
    val homeMode: HomeMode = HomeMode.VISUALIZATION,
    val playbackState: PlaybackState = PlaybackState(MoveSequence.EMPTY),
    val editingSession: EditingSession = EditingSession(),
)
