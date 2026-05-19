package fr.olegueyan.algomix.infrastructure.persistence.local

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.TimerRepository
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId

class LocalTimerRepository(
    private val dao: LocalPersistenceDao,
    private val clockProvider: ClockProvider = SystemClockProvider,
) : TimerRepository {
    override suspend fun listTimerEntries(): AppResult<List<TimerEntry>> =
        storageResult { AppResult.success(dao.listTimerEntries().map { it.toDomain() }) }

    override suspend fun saveTimerEntry(entry: TimerEntry): AppResult<TimerEntry> =
        storageResult {
            if (entry.durationMillis <= 0) {
                return@storageResult AppResult.failure(AppError.Validation("Timer duration must be greater than zero"))
            }
            val now = clockProvider.nowMillis()
            dao.upsertTimerEntry(entry.toEntity(updatedAt = now))
            dao.enqueueCloudMutation("timer_entry", entry.id.value, OUTBOX_OPERATION_UPSERT, now)
            AppResult.success(entry)
        }

    override suspend fun deleteTimerEntry(id: TimerEntryId): AppResult<Unit> =
        storageResult {
            val now = clockProvider.nowMillis()
            if (dao.softDeleteTimerEntry(id.value, now) == 0) {
                return@storageResult notFound("Timer entry")
            }
            dao.enqueueCloudMutation("timer_entry", id.value, OUTBOX_OPERATION_DELETE, now, deletedAt = now)
            AppResult.success(Unit)
        }

    override suspend fun clearTimerHistory(): AppResult<Unit> =
        storageResult {
            val now = clockProvider.nowMillis()
            val activeIds = dao.listActiveTimerEntryIds()
            dao.softDeleteAllTimerEntries(now)
            activeIds.forEach { id ->
                dao.enqueueCloudMutation("timer_entry", id, OUTBOX_OPERATION_DELETE, now, deletedAt = now)
            }
            AppResult.success(Unit)
        }
}
