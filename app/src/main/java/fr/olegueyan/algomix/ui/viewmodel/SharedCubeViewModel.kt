package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.domain.cube.PlaybackState
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.state.SharedCubeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SharedCubeViewModel(
    private val cubeSessionRepository: CubeSessionRepository,
    private val clockProvider: ClockProvider = SystemClockProvider,
    autoLoadSession: Boolean = true,
    private val taskLauncher: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SharedCubeUiState())
    val uiState: StateFlow<SharedCubeUiState> = mutableUiState.asStateFlow()

    init {
        if (autoLoadSession) {
            launchTask { restoreSession() }
        }
    }

    suspend fun restoreSession() {
        val snapshot = cubeSessionRepository.loadSession().getOrNull() ?: return
        val restoredRoute = MainRoute.fromStoredName(snapshot.activeRoute) ?: MainRoute.HOME
        val restoredHomeMode = HomeMode.fromStoredName(snapshot.activeHomeMode) ?: HomeMode.VISUALIZATION
        mutableUiState.value = mutableUiState.value.copy(
            activeRoute = restoredRoute,
            homeMode = restoredHomeMode,
            playbackState = mutableUiState.value.playbackState.copy(currentIndex = snapshot.playbackIndex),
        )
    }

    fun setRoute(route: MainRoute) {
        updateAndPersist { current -> current.copy(activeRoute = route) }
    }

    fun setHomeMode(mode: HomeMode) {
        updateAndPersist { current -> current.copy(homeMode = mode) }
    }

    fun setPlaybackState(playbackState: PlaybackState) {
        updateAndPersist { current -> current.copy(playbackState = playbackState) }
    }

    private fun updateAndPersist(update: (SharedCubeUiState) -> SharedCubeUiState) {
        val previous = mutableUiState.value
        val next = update(previous)
        if (next == previous) {
            return
        }
        mutableUiState.value = next
        launchTask { persistSession(next) }
    }

    private suspend fun persistSession(state: SharedCubeUiState) {
        cubeSessionRepository.saveSession(
            LocalSessionSnapshot(
                serializedCubeState = null,
                activeRoute = state.activeRoute.name,
                activeHomeMode = state.homeMode.name,
                activeSequence = state.playbackState.sequence.normalizedNotation.ifBlank { null },
                playbackIndex = state.playbackState.currentIndex,
                updatedAt = clockProvider.now(),
            ),
        )
    }

    private fun launchTask(block: suspend () -> Unit) {
        val launcher = taskLauncher
        if (launcher != null) {
            launcher(block)
        } else {
            viewModelScope.launch { block() }
        }
    }

    class Factory(
        private val cubeSessionRepository: CubeSessionRepository,
        private val clockProvider: ClockProvider = SystemClockProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SharedCubeViewModel::class.java)) {
                return SharedCubeViewModel(cubeSessionRepository, clockProvider) as T
            }
            throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
        }
    }
}
