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
import fr.olegueyan.algomix.domain.session.CubeSessionCodec
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
        val restoredCubeState = CubeSessionCodec.decode(snapshot.serializedCubeState) ?: CubeState.solved()
        val restoredSequence = parseStoredSequence(snapshot.activeSequence)
        val restoredIndex = snapshot.playbackIndex.coerceIn(0, restoredSequence.moves.size)
        val restoredBase = restoredCubeState.baseBefore(restoredSequence, restoredIndex)
        val restoredEditingSession = if (restoredHomeMode == HomeMode.EDIT) {
            EditingSession(sequence = restoredSequence)
        } else {
            mutableUiState.value.editingSession
        }
        mutableUiState.value = mutableUiState.value.copy(
            cubeState = restoredCubeState,
            activeRoute = restoredRoute,
            homeMode = restoredHomeMode,
            playbackState = PlaybackState(
                sequence = restoredSequence,
                currentIndex = restoredIndex,
            ),
            playbackBaseCubeState = restoredBase,
            editingSession = restoredEditingSession,
            editingBaseCubeState = restoredBase,
            homeUiState = mutableUiState.value.homeUiState.copy(
                freeSequenceNotation = if (restoredHomeMode == HomeMode.FREE) {
                    restoredSequence.normalizedNotation
                } else {
                    mutableUiState.value.homeUiState.freeSequenceNotation
                },
            ),
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
        updateEditingSession(
            feedback = "Undo applique",
            unchangedFeedback = "Aucun undo disponible",
        ) { current -> current.undo() }
    }

    fun redoEditing() {
        updateEditingSession(
            feedback = "Redo applique",
            unchangedFeedback = "Aucun redo disponible",
        ) { current -> current.redo() }
    }

    fun suppressLastEditingMove() {
        updateEditingSession(
            feedback = "Dernier move supprime",
            unchangedFeedback = "Aucun move a supprimer",
        ) { current -> current.suppressLastMove() }
    }

    fun deleteAllEditing() {
        updateEditingSession(
            feedback = "Sequence videe",
            unchangedFeedback = "Sequence deja vide",
        ) { current -> current.deleteAll() }
    }

    fun requestScan() {
        updateFeedback("Ouverture du scan")
    }

    fun applyScannedCube(cubeState: CubeState) {
        updateAndPersist { current ->
            current.copy(
                cubeState = cubeState,
                homeMode = HomeMode.VISUALIZATION,
                playbackBaseCubeState = cubeState,
                editingBaseCubeState = cubeState,
                homeUiState = current.homeUiState.copy(feedbackMessage = "Cube scanne applique"),
            )
        }
    }

    fun requestLoadAlgorithm() {
        updateFeedback("Utilisez Charger algo pour ouvrir la selection")
    }

    fun requestLoadScramble() {
        updateFeedback("Utilisez Charger melange pour ouvrir la selection")
    }

    fun requestSaveEditing() {
        updateFeedback("Utilisez Save en edition pour sauvegarder")
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
        unchangedFeedback: String = feedback,
        update: (EditingSession) -> EditingSession,
    ) {
        updateAndPersist { current ->
            val nextSession = update(current.editingSession)
            if (nextSession == current.editingSession) {
                return@updateAndPersist current.copy(
                    homeUiState = current.homeUiState.copy(feedbackMessage = unchangedFeedback),
                )
            }
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
                serializedCubeState = CubeSessionCodec.encode(state.cubeState),
                activeRoute = state.activeRoute.name,
                activeHomeMode = state.homeMode.name,
                activeSequence = state.activeSequenceForSnapshot().normalizedNotation.ifBlank { null },
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

private fun parseStoredSequence(activeSequence: String?): MoveSequence =
    activeSequence
        ?.takeIf(String::isNotBlank)
        ?.let { sequence -> runCatching { MoveParser.parse(sequence) }.getOrNull() }
        ?: MoveSequence.EMPTY

private fun CubeState.baseBefore(sequence: MoveSequence, index: Int): CubeState {
    val inversePrefix = MoveSequence(
        sequence.moves
            .take(index)
            .asReversed()
            .map { move -> move.inverse() },
    )
    return MoveExecutor.apply(this, inversePrefix)
}

private fun fr.olegueyan.algomix.domain.cube.Move.inverse(): fr.olegueyan.algomix.domain.cube.Move =
    copy(
        turn = when (turn) {
            fr.olegueyan.algomix.domain.cube.MoveTurn.CLOCKWISE ->
                fr.olegueyan.algomix.domain.cube.MoveTurn.COUNTER_CLOCKWISE
            fr.olegueyan.algomix.domain.cube.MoveTurn.COUNTER_CLOCKWISE ->
                fr.olegueyan.algomix.domain.cube.MoveTurn.CLOCKWISE
            fr.olegueyan.algomix.domain.cube.MoveTurn.HALF_TURN ->
                fr.olegueyan.algomix.domain.cube.MoveTurn.HALF_TURN
        },
    )

private fun SharedCubeUiState.activeSequenceForSnapshot(): MoveSequence =
    when (homeMode) {
        HomeMode.EDIT -> editingSession.sequence
        HomeMode.FREE -> parseStoredSequence(homeUiState.freeSequenceNotation)
        else -> playbackState.sequence
    }
