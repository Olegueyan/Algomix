package fr.olegueyan.algomix.domain.timer

import java.time.Instant

@JvmInline
value class TimerEntryId(val value: String)

data class TimerEntry(
    val id: TimerEntryId,
    val durationMillis: Long,
    val solvedAt: Instant,
)
