package fr.olegueyan.algomix.ui.viewmodel

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.MoveExecutor
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MainRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SharedCubeViewModelTest {
    @Test
    fun initializesWithDefaultState() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value

        assertEquals(CubeState.solved(), state.cubeState)
        assertEquals(MainRoute.HOME, state.activeRoute)
        assertEquals(HomeMode.VISUALIZATION, state.homeMode)
        assertEquals(0, state.playbackState.currentIndex)
    }

    @Test
    fun changingRouteKeepsCubeStateAndSavesSession() {
        val repository = FakeCubeSessionRepository()
        val viewModel = createViewModel(repository)
        val initialCube = viewModel.uiState.value.cubeState

        viewModel.setRoute(MainRoute.TIMER)

        assertEquals(initialCube, viewModel.uiState.value.cubeState)
        assertEquals(MainRoute.TIMER, viewModel.uiState.value.activeRoute)
        assertEquals("TIMER", repository.savedSnapshot?.activeRoute)
        assertNull(repository.savedSnapshot?.serializedCubeState)
    }

    @Test
    fun changingHomeModeUpdatesStateAndSavesSession() {
        val repository = FakeCubeSessionRepository()
        val viewModel = createViewModel(repository)

        viewModel.setHomeMode(HomeMode.EDIT)

        assertEquals(HomeMode.EDIT, viewModel.uiState.value.homeMode)
        assertEquals("EDIT", repository.savedSnapshot?.activeHomeMode)
    }

    @Test
    fun restoresRouteHomeModeAndPlaybackIndexFromSession() = runBlocking {
        val repository = FakeCubeSessionRepository(
            loadedSnapshot = LocalSessionSnapshot(
                serializedCubeState = null,
                activeRoute = "LIBRARY",
                activeHomeMode = "PLAY",
                activeSequence = null,
                playbackIndex = 3,
                updatedAt = Instant.EPOCH,
            ),
        )
        val viewModel = createViewModel(repository)

        viewModel.restoreSession()

        assertEquals(MainRoute.LIBRARY, viewModel.uiState.value.activeRoute)
        assertEquals(HomeMode.PLAY, viewModel.uiState.value.homeMode)
        assertEquals(3, viewModel.uiState.value.playbackState.currentIndex)
    }

    @Test
    fun changingHomeModeKeepsCurrentCubeState() {
        val viewModel = createViewModel()
        viewModel.setHomeMode(HomeMode.FREE)
        viewModel.applyMoveToken("R")
        val movedCube = viewModel.uiState.value.cubeState

        viewModel.setHomeMode(HomeMode.EDIT)

        assertEquals(movedCube, viewModel.uiState.value.cubeState)
    }

    @Test
    fun freeModeAppliesMoveAndUpdatesDisplayedSequence() {
        val viewModel = createViewModel()

        viewModel.setHomeMode(HomeMode.FREE)
        viewModel.applyMoveToken("R")

        assertNotEquals(CubeState.solved(), viewModel.uiState.value.cubeState)
        assertEquals("R", viewModel.uiState.value.homeUiState.freeSequenceNotation)
    }

    @Test
    fun scrambleChangesCubeAndPreparesPlaybackSequence() {
        val viewModel = createViewModel()

        viewModel.scramble(length = 8)

        val state = viewModel.uiState.value
        assertNotEquals(CubeState.solved(), state.cubeState)
        assertEquals(8, state.playbackState.sequence.moves.size)
        assertEquals(8, state.playbackState.currentIndex)
        assertEquals(
            state.playbackState.sequence,
            MoveParser.parse(state.playbackState.sequence.normalizedNotation),
        )
    }

    @Test
    fun playbackNextPreviousResetLoopAndSpeedUpdateState() {
        val viewModel = createViewModel()
        viewModel.loadPlaybackSequence(MoveParser.parse("R U"), CubeState.solved())

        viewModel.playNext()
        assertEquals(1, viewModel.uiState.value.playbackState.currentIndex)
        assertNotEquals(CubeState.solved(), viewModel.uiState.value.cubeState)

        viewModel.playPrevious()
        assertEquals(0, viewModel.uiState.value.playbackState.currentIndex)
        assertEquals(CubeState.solved(), viewModel.uiState.value.cubeState)

        viewModel.playNext()
        viewModel.resetPlayback()
        assertEquals(0, viewModel.uiState.value.playbackState.currentIndex)

        viewModel.toggleLoop()
        viewModel.cyclePlaybackSpeed()
        assertTrue(viewModel.uiState.value.playbackState.loop)
        assertEquals(1.5f, viewModel.uiState.value.playbackState.speedMultiplier)
    }

    @Test
    fun editingAddUndoRedoSuppressAndDeleteAllUpdateSequenceAndCube() {
        val viewModel = createViewModel()
        viewModel.setHomeMode(HomeMode.EDIT)

        viewModel.applyMoveToken("R")
        assertEquals("R", viewModel.uiState.value.editingSession.sequence.normalizedNotation)
        assertNotEquals(CubeState.solved(), viewModel.uiState.value.cubeState)

        viewModel.undoEditing()
        assertTrue(viewModel.uiState.value.editingSession.sequence.isEmpty)
        assertEquals(CubeState.solved(), viewModel.uiState.value.cubeState)

        viewModel.redoEditing()
        assertEquals("R", viewModel.uiState.value.editingSession.sequence.normalizedNotation)

        viewModel.applyMoveToken("U")
        viewModel.suppressLastEditingMove()
        assertEquals("R", viewModel.uiState.value.editingSession.sequence.normalizedNotation)

        viewModel.deleteAllEditing()
        assertTrue(viewModel.uiState.value.editingSession.sequence.isEmpty)
        assertEquals(CubeState.solved(), viewModel.uiState.value.cubeState)
    }

    @Test
    fun placeholderActionsExposeNonBlockingFeedback() {
        val viewModel = createViewModel()

        viewModel.requestLoadAlgorithm()
        assertEquals(
            "Chargement d'algo disponible au batch 7",
            viewModel.uiState.value.homeUiState.feedbackMessage,
        )

        viewModel.requestSaveEditing()
        assertEquals("Sauvegarde disponible au batch 7", viewModel.uiState.value.homeUiState.feedbackMessage)
    }

    @Test
    fun applyScannedCubeReplacesSharedCubeAndReturnsVisualizationMode() {
        val repository = FakeCubeSessionRepository()
        val viewModel = createViewModel(repository)
        val scannedCube = MoveExecutor.apply(CubeState.solved(), MoveParser.parse("R"))

        viewModel.setHomeMode(HomeMode.EDIT)
        viewModel.applyScannedCube(scannedCube)

        assertEquals(scannedCube, viewModel.uiState.value.cubeState)
        assertEquals(HomeMode.VISUALIZATION, viewModel.uiState.value.homeMode)
        assertEquals("Cube scanne applique", viewModel.uiState.value.homeUiState.feedbackMessage)
        assertEquals("VISUALIZATION", repository.savedSnapshot?.activeHomeMode)
    }

    private fun createViewModel(
        repository: FakeCubeSessionRepository = FakeCubeSessionRepository(),
    ): SharedCubeViewModel =
        SharedCubeViewModel(
            cubeSessionRepository = repository,
            clockProvider = FixedClock,
            autoLoadSession = false,
            taskLauncher = { block -> runBlocking { block() } },
        )

    private class FakeCubeSessionRepository(
        private val loadedSnapshot: LocalSessionSnapshot? = null,
    ) : CubeSessionRepository {
        var savedSnapshot: LocalSessionSnapshot? = null

        override suspend fun loadSession(): AppResult<LocalSessionSnapshot?> =
            AppResult.success(loadedSnapshot)

        override suspend fun saveSession(snapshot: LocalSessionSnapshot): AppResult<Unit> {
            savedSnapshot = snapshot
            return AppResult.success(Unit)
        }

        override suspend fun clearSession(): AppResult<Unit> =
            AppResult.success(Unit)
    }

    private object FixedClock : ClockProvider {
        override fun now(): Instant = Instant.ofEpochMilli(1_700_000_000_000)
    }
}
