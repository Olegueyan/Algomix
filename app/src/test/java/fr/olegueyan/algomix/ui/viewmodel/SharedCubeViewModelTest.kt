package fr.olegueyan.algomix.ui.viewmodel

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MainRoute
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
