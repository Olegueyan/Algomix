package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.application.rubik.scene.Quaternion
import fr.olegueyan.algomix.application.rubik.scene.RubikResetTarget
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.EditingSession
import fr.olegueyan.algomix.domain.cube.Move
import fr.olegueyan.algomix.domain.cube.MoveExecutor
import fr.olegueyan.algomix.domain.cube.MoveKind
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.cube.MoveSequence
import fr.olegueyan.algomix.domain.cube.PlaybackState
import fr.olegueyan.algomix.domain.cube.ScrambleGenerator
import fr.olegueyan.algomix.domain.session.CubeSessionCodec
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.ui.state.AnimatedMoveEvent
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.state.MoveKeyboardCategory
import fr.olegueyan.algomix.ui.state.SharedCubeUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class SharedCubeViewModel(
    private val cubeSessionRepository: CubeSessionRepository,
    private val clockProvider: ClockProvider = SystemClockProvider,
    autoLoadSession: Boolean = true,
    private val taskLauncher: (((suspend () -> Unit)) -> Unit)? = null,
    private val settingsRepository: SettingsRepository? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SharedCubeUiState())
    val uiState: StateFlow<SharedCubeUiState> = mutableUiState.asStateFlow()
    private val mutableAnimationEvents = MutableSharedFlow<AnimatedMoveEvent>(extraBufferCapacity = ANIMATION_BUFFER)
    val animationEvents: SharedFlow<AnimatedMoveEvent> = mutableAnimationEvents.asSharedFlow()
    private var animationSequenceCounter = 0L
    private var autoPlayJob: Job? = null

    init {
        if (autoLoadSession) {
            launchTask { restoreSession() }
        }
    }

    suspend fun restoreSession() {
        val preferences = settingsRepository?.loadPreferences()?.getOrNull()
        if (preferences?.sessionPersistenceEnabled == false) {
            mutableUiState.value = SharedCubeUiState()
            return
        }
        val snapshot = cubeSessionRepository.loadSession().getOrNull() ?: return
        val restoredRoute = MainRoute.fromStoredName(snapshot.activeRoute) ?: MainRoute.HOME
        val restoredHomeMode = HomeMode.fromStoredName(snapshot.activeHomeMode) ?: HomeMode.FREE
        val restoredCubeState = if (preferences?.localCubeCacheEnabled == false) {
            CubeState.solved()
        } else {
            CubeSessionCodec.decode(snapshot.serializedCubeState) ?: CubeState.solved()
        }
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
            mainCubeState = if (restoredHomeMode == HomeMode.EDIT) {
                mutableUiState.value.mainCubeState
            } else {
                restoredCubeState
            },
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
            when {
                current.homeMode != HomeMode.EDIT && mode == HomeMode.EDIT -> {
                    val editBase = if (current.editingSession.sequence.isEmpty) {
                        CubeState.solved()
                    } else {
                        current.editingBaseCubeState
                    }
                    current.copy(
                        homeMode = mode,
                        mainCubeState = current.cubeState,
                        editingBaseCubeState = editBase,
                        cubeState = MoveExecutor.apply(editBase, current.editingSession.sequence),
                    )
                }
                current.homeMode == HomeMode.EDIT && mode != HomeMode.EDIT -> {
                    current.copy(homeMode = mode, cubeState = current.mainCubeState)
                }
                else -> current.copy(homeMode = mode)
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
                mainCubeState = if (current.homeMode == HomeMode.EDIT) current.mainCubeState else baseCubeState,
            )
        }
    }

    fun loadEditingSequence(
        sequence: MoveSequence,
        baseCubeState: CubeState = CubeState.solved(),
    ) {
        updateAndPersist { current ->
            current.copy(
                homeMode = HomeMode.EDIT,
                editingBaseCubeState = baseCubeState,
                editingSession = EditingSession(sequence = sequence),
                playbackBaseCubeState = baseCubeState,
                playbackState = current.playbackState.copy(
                    sequence = sequence,
                    currentIndex = 0,
                ),
                cubeState = baseCubeState,
                homeUiState = current.homeUiState.copy(feedbackMessage = "Séquence chargée", feedbackIsError = false),
            )
        }
    }

    fun setKeyboardCategory(category: MoveKeyboardCategory) {
        updateAndPersist { current ->
            val oldCategory = current.homeUiState.keyboardCategory
            if (current.homeMode != HomeMode.FREE || oldCategory == category) {
                return@updateAndPersist current.copy(
                    homeUiState = current.homeUiState.copy(keyboardCategory = category),
                )
            }
            val savedStates = current.homeUiState.categoryCubeStates.toMutableMap()
            savedStates[oldCategory] = current.cubeState
            val restoredState = savedStates[category] ?: CubeState.solved()
            current.copy(
                cubeState = restoredState,
                mainCubeState = restoredState,
                homeUiState = current.homeUiState.copy(
                    keyboardCategory = category,
                    categoryCubeStates = savedStates,
                    freeSequenceNotation = "",
                ),
            )
        }
    }

    fun applyMoveToken(token: String) {
        val move = MoveParser.parseMove(token)
        when (mutableUiState.value.homeMode) {
            HomeMode.EDIT -> applyEditingMove(token)
            else -> {
                updateAndPersist { current ->
                    val newCubeState = MoveExecutor.apply(current.cubeState, move)
                    val sequence = MoveParser.parse(current.homeUiState.freeSequenceNotation).append(move)
                    val category = current.homeUiState.keyboardCategory
                    val updatedCategoryStates = current.homeUiState.categoryCubeStates.toMutableMap()
                    updatedCategoryStates[category] = newCubeState
                    current.copy(
                        cubeState = newCubeState,
                        mainCubeState = newCubeState,
                        homeUiState = current.homeUiState.copy(
                            freeSequenceNotation = sequence.normalizedNotation,
                            categoryCubeStates = updatedCategoryStates,
                        ),
                    )
                }
                emitAnimationEvent(move)
            }
        }
    }

    fun toggleRotationLock() {
        updateAndPersist { current -> current.copy(rotationLocked = !current.rotationLocked) }
    }

    fun computeResetTargetQuaternion(): Quaternion {
        val current = mutableUiState.value
        val rotationMoves = when (current.homeMode) {
            HomeMode.FREE -> parseStoredSequence(current.homeUiState.freeSequenceNotation).moves
                .filter { move -> move.kind == MoveKind.ROTATION }
            HomeMode.PLAY ->
                current.playbackState.sequence.moves
                    .take(current.playbackState.currentIndex)
                    .filter { move -> move.kind == MoveKind.ROTATION }
            HomeMode.EDIT ->
                current.editingSession.sequence.moves
                    .filter { move -> move.kind == MoveKind.ROTATION }
        }
        return RubikResetTarget.composeRotationMoves(rotationMoves)
    }

    private fun emitAnimationEvent(move: Move) {
        animationSequenceCounter += 1
        mutableAnimationEvents.tryEmit(
            AnimatedMoveEvent(
                move = move,
                finalState = mutableUiState.value.cubeState,
                sequence = animationSequenceCounter,
            ),
        )
    }

    fun scramble(length: Int = DEFAULT_SCRAMBLE_LENGTH) {
        val sequence = ScrambleGenerator.generate(length)
        updateAndPersist { current ->
            current.copy(
                cubeState = MoveExecutor.apply(current.cubeState, sequence),
                mainCubeState = MoveExecutor.apply(current.cubeState, sequence),
                playbackBaseCubeState = current.cubeState,
                playbackState = PlaybackState(sequence = sequence, currentIndex = sequence.moves.size),
                homeUiState = current.homeUiState.copy(
                    freeSequenceNotation = sequence.normalizedNotation,
                    feedbackMessage = "Mélange généré",
                ),
            )
        }
    }

    fun playNext() {
        val playbackBefore = mutableUiState.value.playbackState
        val movesSize = playbackBefore.sequence.moves.size
        val previousIndex = playbackBefore.currentIndex
        updatePlaybackIndex { current ->
            if (current == movesSize && playbackBefore.loop) {
                0
            } else {
                (current + 1).coerceAtMost(movesSize)
            }
        }
        val nextIndex = mutableUiState.value.playbackState.currentIndex
        if (nextIndex == previousIndex + 1 && nextIndex <= movesSize) {
            val playedMove = playbackBefore.sequence.moves.getOrNull(previousIndex)
            if (playedMove != null) emitAnimationEvent(playedMove)
        }
    }

    fun playPrevious() {
        val playbackBefore = mutableUiState.value.playbackState
        val previousIndex = playbackBefore.currentIndex
        updatePlaybackIndex { current -> (current - 1).coerceAtLeast(0) }
        val nextIndex = mutableUiState.value.playbackState.currentIndex
        if (nextIndex == previousIndex - 1 && previousIndex > 0) {
            val undoneMove = playbackBefore.sequence.moves.getOrNull(nextIndex)?.inverse()
            if (undoneMove != null) emitAnimationEvent(undoneMove)
        }
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

    fun resetCurrentCubeToSolved() {
        updateAndPersist { current ->
            val solved = CubeState.solved()
            when (current.homeMode) {
                HomeMode.EDIT -> current.copy(
                    cubeState = solved,
                    editingBaseCubeState = solved,
                    editingSession = EditingSession(),
                    playbackBaseCubeState = solved,
                    playbackState = PlaybackState(MoveSequence.EMPTY),
                    homeUiState = current.homeUiState.copy(feedbackMessage = "Cube réinitialisé"),
                )
                else -> current.copy(
                    cubeState = solved,
                    mainCubeState = solved,
                    playbackBaseCubeState = solved,
                    playbackState = PlaybackState(MoveSequence.EMPTY),
                    homeUiState = current.homeUiState.copy(
                        freeSequenceNotation = "",
                        feedbackMessage = "Cube réinitialisé",
                    ),
                )
            }
        }
    }

    fun requestScan() {
        updateFeedback("Ouverture du scan")
    }

    fun applyScannedCube(cubeState: CubeState) {
        updateAndPersist { current ->
            current.copy(
                cubeState = cubeState,
                mainCubeState = cubeState,
                homeMode = HomeMode.FREE,
                playbackBaseCubeState = cubeState,
                editingBaseCubeState = cubeState,
                homeUiState = current.homeUiState.copy(feedbackMessage = "Cube scanné appliqué"),
            )
        }
    }

    fun requestLoadAlgorithm() {
        updateFeedback("Utilisez Charger algo pour ouvrir la sélection")
    }

    fun requestLoadScramble() {
        updateFeedback("Utilisez Charger mélange pour ouvrir la sélection")
    }

    fun requestSaveEditing() {
        updateFeedback("Utilisez Save en édition pour sauvegarder")
    }

    fun showFeedback(message: String, isError: Boolean = false) {
        updateAndPersist { current ->
            current.copy(
                homeUiState = current.homeUiState.copy(
                    feedbackMessage = message,
                    feedbackIsError = isError,
                ),
            )
        }
    }

    fun consumeFeedback() {
        updateAndPersist { current ->
            current.copy(homeUiState = current.homeUiState.copy(feedbackMessage = null, feedbackIsError = false))
        }
    }

    private fun applyEditingMove(token: String) {
        val move = MoveParser.parseMove(token)
        updateEditingSession { current -> current.addMove(move) }
        emitAnimationEvent(move)
    }

    private fun updateEditingSession(
        update: (EditingSession) -> EditingSession,
    ) {
        updateAndPersist { current ->
            val nextSession = update(current.editingSession)
            if (nextSession == current.editingSession) return@updateAndPersist current
            current.copy(
                cubeState = MoveExecutor.apply(current.editingBaseCubeState, nextSession.sequence),
                editingSession = nextSession,
                playbackBaseCubeState = current.editingBaseCubeState,
                playbackState = current.playbackState.copy(
                    sequence = nextSession.sequence,
                    currentIndex = nextSession.sequence.moves.size,
                ),
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
                mainCubeState = if (current.homeMode == HomeMode.EDIT) {
                    current.mainCubeState
                } else {
                    MoveExecutor.apply(
                        current.playbackBaseCubeState,
                        MoveSequence(sequence.moves.take(nextIndex)),
                    )
                },
                playbackState = current.playbackState.copy(currentIndex = nextIndex),
            )
        }
    }

    private fun updateFeedback(message: String, isError: Boolean = false) {
        updateAndPersist { current ->
            current.copy(
                homeUiState = current.homeUiState.copy(
                    feedbackMessage = message,
                    feedbackIsError = isError,
                ),
            )
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
        val preferences = settingsRepository?.loadPreferences()?.getOrNull()
        if (preferences?.sessionPersistenceEnabled == false) {
            cubeSessionRepository.clearSession()
            return
        }
        cubeSessionRepository.saveSession(
            LocalSessionSnapshot(
                serializedCubeState = if (preferences?.localCubeCacheEnabled == false) {
                    null
                } else {
                    CubeSessionCodec.encode(state.cubeState)
                },
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
        private val settingsRepository: SettingsRepository? = null,
        private val clockProvider: ClockProvider = SystemClockProvider,
    ) : ViewModelProvider.Factory {
        constructor(
            cubeSessionRepository: CubeSessionRepository,
            clockProvider: ClockProvider,
        ) : this(cubeSessionRepository, null, clockProvider)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SharedCubeViewModel::class.java)) {
                return SharedCubeViewModel(
                    cubeSessionRepository = cubeSessionRepository,
                    clockProvider = clockProvider,
                    settingsRepository = settingsRepository,
                ) as T
            }
            throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        private const val DEFAULT_SCRAMBLE_LENGTH = 20
        private const val ANIMATION_BUFFER = 32
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
