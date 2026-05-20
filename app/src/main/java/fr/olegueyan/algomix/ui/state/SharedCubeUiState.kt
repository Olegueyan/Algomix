package fr.olegueyan.algomix.ui.state

import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.EditingSession
import fr.olegueyan.algomix.domain.cube.Move
import fr.olegueyan.algomix.domain.cube.MoveSequence
import fr.olegueyan.algomix.domain.cube.PlaybackState

data class SharedCubeUiState(
    val freeCubeState: CubeState = CubeState.solved(),
    val playCubeState: CubeState = CubeState.solved(),
    val editCubeState: CubeState = CubeState.solved(),
    val activeRoute: MainRoute = MainRoute.HOME,
    val homeMode: HomeMode = HomeMode.FREE,
    val playbackState: PlaybackState = PlaybackState(MoveSequence.EMPTY),
    val playbackBaseCubeState: CubeState = CubeState.solved(),
    val editingSession: EditingSession = EditingSession(),
    val editingBaseCubeState: CubeState = CubeState.solved(),
    val editingPlaybackIndex: Int = 0,
    val homeUiState: HomeUiState = HomeUiState(),
    val freeRotationLocked: Boolean = false,
    val playRotationLocked: Boolean = false,
    val editRotationLocked: Boolean = false,
) {
    val cubeState: CubeState
        get() = cubeStateFor(homeMode)

    val rotationLocked: Boolean
        get() = rotationLockedFor(homeMode)

    fun cubeStateFor(mode: HomeMode): CubeState =
        when (mode) {
            HomeMode.FREE -> freeCubeState
            HomeMode.PLAY -> playCubeState
            HomeMode.EDIT -> editCubeState
        }

    fun rotationLockedFor(mode: HomeMode): Boolean =
        when (mode) {
            HomeMode.FREE -> freeRotationLocked
            HomeMode.PLAY -> playRotationLocked
            HomeMode.EDIT -> editRotationLocked
        }
}

data class AnimatedMoveEvent(
    val move: Move,
    val finalState: CubeState,
    val sequence: Long,
)
