package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.EditingSession
import fr.olegueyan.algomix.domain.cube.MoveExecutor
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.cube.MoveSequence
import fr.olegueyan.algomix.domain.cube.PlaybackState
import fr.olegueyan.algomix.domain.cube.ScrambleGenerator
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.state.MoveKeyboardCategory
import fr.olegueyan.algomix.ui.state.SharedCubeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class SharedCubeViewModel(
    private val cubeSessionRepository: CubeSessionRepository,
    private val clockProvider: ClockProvider = SystemClockProvider,
    autoLoadSession: Boolean = true,
    private val taskLauncher: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SharedCubeUiState())
    val uiState: StateFlow<SharedCubeUiState> = mutableUiState.asStateFlow()
    private var autoPlayJob: Job? = null

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
        updateAndPersist { current ->
            if (mode == HomeMode.EDIT && current.editingSession.sequence.isEmpty) {
                current.copy(homeMode = mode, editingBaseCubeState = current.cubeState)
            } else {
                current.copy(homeMode = mode)
            }
        }
    }

    fun setPlaybackState(playbackState: PlaybackState) {
        updateAndPersist { current -> current.copy(playbackState = playbackState) }
    }

    fun loadPlaybackSequence(
        sequence: MoveSequence,
        baseCubeState: CubeState = mutableUiState.value.cubeState,
    ) {
        updateAndPersist { current ->
            current.copy(
                playbackBaseCubeState = baseCubeState,
                playbackState = PlaybackState(sequence = sequence),
                cubeState = baseCubeState,
            )
        }
    }

    fun setKeyboardCategory(category: MoveKeyboardCategory) {
        updateAndPersist { current ->
            current.copy(homeUiState = current.homeUiState.copy(keyboardCategory = category))
        }
    }

    fun applyMoveToken(token: String) {
        val move = MoveParser.parseMove(token)
        when (mutableUiState.value.homeMode) {
            HomeMode.EDIT -> applyEditingMove(token)
            else -> updateAndPersist { current ->
                val sequence = MoveParser.parse(current.homeUiState.freeSequenceNotation).append(move)
                current.copy(
                    cubeState = MoveExecutor.apply(current.cubeState, move),
                    homeUiState = current.homeUiState.copy(
                        freeSequenceNotation = sequence.normalizedNotation,
                        feedbackMessage = "Move $token applique",
                    ),
                )
            }
        }
    }

    fun scramble(length: Int = DEFAULT_SCRAMBLE_LENGTH) {
        val sequence = ScrambleGenerator.generate(length)
        updateAndPersist { current ->
            current.copy(
                cubeState = MoveExecutor.apply(current.cubeState, sequence),
                playbackBaseCubeState = current.cubeState,
                playbackState = PlaybackState(sequence = sequence, currentIndex = sequence.moves.size),
                homeUiState = current.homeUiState.copy(
                    freeSequenceNotation = sequence.normalizedNotation,
                    feedbackMessage = "Melange genere",
                ),
            )
        }
    }

    fun playNext() {
        updatePlaybackIndex { current ->
            val playbackState = mutableUiState.value.playbackState
            if (current == playbackState.sequence.moves.size && playbackState.loop) {
                0
            } else {
                (current + 1).coerceAtMost(playbackState.sequence.moves.size)
            }
        }
    }

    fun playPrevious() {
        updatePlaybackIndex { current -> (current - 1).coerceAtLeast(0) }
    }

    fun resetPlayback() {
        updatePlaybackIndex { 0 }
    }

    fun toggleLoop() {
        updateAndPersist { current ->
            current.copy(playbackState = current.playbackState.copy(loop = !current.playbackState.loop))
        }
    }

    fun cyclePlaybackSpeed() {
        updateAndPersist { current ->
            val nextSpeed = when (current.playbackState.speedMultiplier) {
                1f -> 1.5f
                1.5f -> 2f
                else -> 1f
            }
            current.copy(playbackState = current.playbackState.copy(speedMultiplier = nextSpeed))
        }
    }

    fun toggleAutoPlay() {
        val enabled = !mutableUiState.value.homeUiState.autoPlayEnabled
        updateAndPersist { current ->
            current.copy(homeUiState = current.homeUiState.copy(autoPlayEnabled = enabled))
        }
        if (enabled) {
            startAutoPlayLoop()
        } else {
            stopAutoPlayLoop()
        }
    }

    fun undoEditing() {
        updateEditingSession { current -> current.undo() }
    }

    fun redoEditing() {
        updateEditingSession { current -> current.redo() }
    }

    fun suppressLastEditingMove() {
        updateEditingSession { current -> current.suppressLastMove() }
    }

    fun deleteAllEditing() {
        updateEditingSession { current -> current.deleteAll() }
    }

    fun requestScan() {
        updateFeedback("Scan disponible au batch 11")
    }

    fun requestLoadAlgorithm() {
        updateFeedback("Chargement d'algo disponible au batch 7")
    }

    fun requestLoadScramble() {
        updateFeedback("Chargement de melange disponible au batch 7")
    }

    fun requestSaveEditing() {
        updateFeedback("Sauvegarde disponible au batch 7")
    }

    fun showFeedback(message: String) {
        updateFeedback(message)
    }

    fun consumeFeedback() {
        updateAndPersist { current -> current.copy(homeUiState = current.homeUiState.copy(feedbackMessage = null)) }
    }

    private fun applyEditingMove(token: String) {
        val move = MoveParser.parseMove(token)
        updateEditingSession(
            feedback = "Move $token ajoute",
        ) { current -> current.addMove(move) }
    }

    private fun updateEditingSession(
        feedback: String = "Sequence d'edition mise a jour",
        update: (EditingSession) -> EditingSession,
    ) {
        updateAndPersist { current ->
            val nextSession = update(current.editingSession)
            current.copy(
                cubeState = MoveExecutor.apply(current.editingBaseCubeState, nextSession.sequence),
                editingSession = nextSession,
                homeUiState = current.homeUiState.copy(feedbackMessage = feedback),
            )
        }
    }

    private fun updatePlaybackIndex(index: (Int) -> Int) {
        updateAndPersist { current ->
            val sequence = current.playbackState.sequence
            val nextIndex = index(current.playbackState.currentIndex).coerceIn(0, sequence.moves.size)
            current.copy(
                cubeState = MoveExecutor.apply(
                    current.playbackBaseCubeState,
                    MoveSequence(sequence.moves.take(nextIndex)),
                ),
                playbackState = current.playbackState.copy(currentIndex = nextIndex),
            )
        }
    }

    private fun updateFeedback(message: String) {
        updateAndPersist { current ->
            current.copy(homeUiState = current.homeUiState.copy(feedbackMessage = message))
        }
    }

    private fun startAutoPlayLoop() {
        if (taskLauncher != null) {
            return
        }
        autoPlayJob?.cancel()
        autoPlayJob = viewModelScope.launch {
            while (mutableUiState.value.homeUiState.autoPlayEnabled) {
                val delayMillis = (1_000L / mutableUiState.value.playbackState.speedMultiplier).toLong()
                delay(delayMillis.coerceAtLeast(250L))
                val before = mutableUiState.value.playbackState.currentIndex
                playNext()
                val after = mutableUiState.value.playbackState.currentIndex
                val atEnd = after == mutableUiState.value.playbackState.sequence.moves.size
                val shouldStop = !mutableUiState.value.playbackState.loop && (before == after || atEnd)
                if (shouldStop) {
                    updateAndPersist { current ->
                        current.copy(homeUiState = current.homeUiState.copy(autoPlayEnabled = false))
                    }
                }
            }
        }
    }

    private fun stopAutoPlayLoop() {
        autoPlayJob?.cancel()
        autoPlayJob = null
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

    companion object {
        private const val DEFAULT_SCRAMBLE_LENGTH = 20
    }
}
