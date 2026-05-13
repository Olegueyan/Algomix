package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId

interface TimerRepository {
    suspend fun listTimerEntries(): AppResult<List<TimerEntry>>

    suspend fun saveTimerEntry(entry: TimerEntry): AppResult<TimerEntry>

    suspend fun deleteTimerEntry(id: TimerEntryId): AppResult<Unit>

    suspend fun clearTimerHistory(): AppResult<Unit>
}
