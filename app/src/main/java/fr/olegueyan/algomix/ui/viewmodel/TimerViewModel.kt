package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.TimerRepository
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId
import fr.olegueyan.algomix.ui.state.TimerDisplayEntry
import fr.olegueyan.algomix.ui.state.TimerRunState
import fr.olegueyan.algomix.ui.state.TimerUiState
import fr.olegueyan.algomix.ui.timer.SystemTimerTimeSource
import fr.olegueyan.algomix.ui.timer.TimerTimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Suppress("TooManyFunctions")
class TimerViewModel(
    private val timerRepository: TimerRepository,
    private val clockProvider: ClockProvider = SystemClockProvider,
    private val timeSource: TimerTimeSource = SystemTimerTimeSource,
    private val taskLauncher: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = mutableUiState.asStateFlow()
    private var accumulatedMillis: Long = 0
    private var runningStartedAtMillis: Long? = null
    private var tickerJob: Job? = null

    init {
        launchTask { refreshHistory() }
    }

    fun startOrPause() {
        when (mutableUiState.value.runState) {
            TimerRunState.IDLE -> startFromIdle()
            TimerRunState.PAUSED -> resume()
            TimerRunState.RUNNING -> pause()
        }
    }

    fun refreshElapsed() {
        val durationMillis = currentDurationMillis()
        mutableUiState.value = mutableUiState.value.copy(
            durationMillis = durationMillis,
            durationLabel = formatDuration(durationMillis),
        )
    }

    fun reset() {
        stopTicker()
        accumulatedMillis = 0
        runningStartedAtMillis = null
        mutableUiState.value = mutableUiState.value.copy(
            runState = TimerRunState.IDLE,
            durationMillis = 0,
            durationLabel = formatDuration(0),
            feedbackMessage = "Timer remis à zéro",
            isError = false,
        )
    }

    fun saveTime() {
        val durationMillis = currentDurationMillis()
        if (durationMillis <= 0) {
            mutableUiState.value = mutableUiState.value.copy(
                feedbackMessage = "Temps invalide",
                isError = true,
            )
            return
        }
        launchTask {
            val entry = TimerEntry(
                id = TimerEntryId(UUID.randomUUID().toString()),
                durationMillis = durationMillis,
                solvedAt = clockProvider.now(),
            )
            when (val result = timerRepository.saveTimerEntry(entry)) {
                is AppResult.Success -> {
                    stopTicker()
                    accumulatedMillis = 0
                    runningStartedAtMillis = null
                    refreshHistory()
                    mutableUiState.value = mutableUiState.value.copy(
                        runState = TimerRunState.IDLE,
                        durationMillis = 0,
                        durationLabel = formatDuration(0),
                        feedbackMessage = "Temps sauvegardé",
                        isError = false,
                    )
                }
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun deleteEntry(id: TimerEntryId) {
        launchTask {
            when (val result = timerRepository.deleteTimerEntry(id)) {
                is AppResult.Success -> {
                    refreshHistory()
                    mutableUiState.value = mutableUiState.value.copy(
                        feedbackMessage = "Temps supprimé",
                        isError = false,
                    )
                }
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun clearHistory() {
        launchTask {
            when (val result = timerRepository.clearTimerHistory()) {
                is AppResult.Success -> {
                    refreshHistory()
                    mutableUiState.value = mutableUiState.value.copy(
                        feedbackMessage = "Historique vidé",
                        isError = false,
                    )
                }
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun consumeFeedback() {
        mutableUiState.value = mutableUiState.value.copy(feedbackMessage = null, isError = false)
    }

    override fun onCleared() {
        stopTicker()
        super.onCleared()
    }

    private fun startFromIdle() {
        accumulatedMillis = 0
        runningStartedAtMillis = timeSource.elapsedRealtimeMillis()
        mutableUiState.value = mutableUiState.value.copy(
            runState = TimerRunState.RUNNING,
            feedbackMessage = null,
            isError = false,
        )
        startTicker()
        refreshElapsed()
    }

    private fun resume() {
        runningStartedAtMillis = timeSource.elapsedRealtimeMillis()
        mutableUiState.value = mutableUiState.value.copy(
            runState = TimerRunState.RUNNING,
            feedbackMessage = null,
            isError = false,
        )
        startTicker()
        refreshElapsed()
    }

    private fun pause() {
        accumulatedMillis = currentDurationMillis()
        runningStartedAtMillis = null
        stopTicker()
        mutableUiState.value = mutableUiState.value.copy(
            runState = TimerRunState.PAUSED,
            durationMillis = accumulatedMillis,
            durationLabel = formatDuration(accumulatedMillis),
            feedbackMessage = "Timer en pause",
            isError = false,
        )
    }

    private suspend fun refreshHistory() {
        mutableUiState.value = mutableUiState.value.copy(isLoading = true)
        when (val result = timerRepository.listTimerEntries()) {
            is AppResult.Success -> {
                mutableUiState.value = mutableUiState.value.copy(
                    history = result.value.map(::toDisplayEntry),
                    isLoading = false,
                )
            }
            is AppResult.Failure -> setError(result.error)
        }
    }

    private fun currentDurationMillis(): Long {
        val startedAt = runningStartedAtMillis ?: return accumulatedMillis
        return accumulatedMillis + (timeSource.elapsedRealtimeMillis() - startedAt).coerceAtLeast(0)
    }

    private fun startTicker() {
        if (taskLauncher != null) {
            return
        }
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (mutableUiState.value.runState == TimerRunState.RUNNING) {
                refreshElapsed()
                delay(TICK_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun toDisplayEntry(entry: TimerEntry): TimerDisplayEntry =
        TimerDisplayEntry(
            id = entry.id,
            durationLabel = formatDuration(entry.durationMillis),
            solvedAtLabel = DATE_FORMATTER.format(entry.solvedAt.atZone(ZoneId.systemDefault())),
        )

    private fun setError(error: AppError) {
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = false,
            feedbackMessage = error.message,
            isError = true,
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

    companion object {
        private const val TICK_INTERVAL_MILLIS = 40L
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm")

        fun formatDuration(durationMillis: Long): String {
            val minutes = durationMillis / MILLIS_PER_MINUTE
            val seconds = (durationMillis % MILLIS_PER_MINUTE) / MILLIS_PER_SECOND
            val centiseconds = (durationMillis % MILLIS_PER_SECOND) / MILLIS_PER_CENTISECOND
            return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
        }

        private const val MILLIS_PER_CENTISECOND = 10L
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
