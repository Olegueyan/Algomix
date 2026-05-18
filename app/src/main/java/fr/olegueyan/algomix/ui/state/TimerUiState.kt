package fr.olegueyan.algomix.ui.state

import fr.olegueyan.algomix.domain.timer.TimerEntryId

enum class TimerRunState {
    IDLE,
    RUNNING,
    PAUSED,
}

data class TimerDisplayEntry(
    val id: TimerEntryId,
    val durationLabel: String,
    val solvedAtLabel: String,
)

data class TimerUiState(
    val runState: TimerRunState = TimerRunState.IDLE,
    val durationMillis: Long = 0,
    val durationLabel: String = "00:00.00",
    val history: List<TimerDisplayEntry> = emptyList(),
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
    val isLoading: Boolean = false,
)
