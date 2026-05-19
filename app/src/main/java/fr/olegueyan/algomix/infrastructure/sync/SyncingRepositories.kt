@file:Suppress("TooManyFunctions")

package fr.olegueyan.algomix.infrastructure.sync

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.application.port.TimerRepository
import fr.olegueyan.algomix.domain.library.AlgorithmEntry
import fr.olegueyan.algomix.domain.library.AlgorithmId
import fr.olegueyan.algomix.domain.library.AlgorithmSheet
import fr.olegueyan.algomix.domain.library.CollectionId
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.library.Scramble
import fr.olegueyan.algomix.domain.library.ScrambleId
import fr.olegueyan.algomix.domain.library.SheetId
import fr.olegueyan.algomix.domain.library.Tag
import fr.olegueyan.algomix.domain.library.TagId
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId

class SyncingLibraryRepository(
    private val delegate: LibraryRepository,
    private val scheduler: CloudSyncScheduler,
) : LibraryRepository {
    override suspend fun listCollections(): AppResult<List<LibraryCollection>> = delegate.listCollections()
    override suspend fun listSheets(collectionId: CollectionId?): AppResult<List<AlgorithmSheet>> =
        delegate.listSheets(collectionId)
    override suspend fun listAlgorithms(sheetId: SheetId): AppResult<List<AlgorithmEntry>> =
        delegate.listAlgorithms(sheetId)
    override suspend fun listScrambles(collectionId: CollectionId?): AppResult<List<Scramble>> =
        delegate.listScrambles(collectionId)
    override suspend fun listTags(): AppResult<List<Tag>> = delegate.listTags()
    override suspend fun listSheetTagIds(sheetId: SheetId): AppResult<Set<TagId>> = delegate.listSheetTagIds(sheetId)
    override suspend fun listScrambleTagIds(scrambleId: ScrambleId): AppResult<Set<TagId>> =
        delegate.listScrambleTagIds(scrambleId)

    override suspend fun saveCollection(collection: LibraryCollection): AppResult<LibraryCollection> =
        delegate.saveCollection(collection).alsoSchedule()
    override suspend fun deleteCollection(
        id: CollectionId,
    ): AppResult<Unit> = delegate.deleteCollection(id).alsoSchedule()
    override suspend fun saveSheet(
        sheet: AlgorithmSheet,
    ): AppResult<AlgorithmSheet> = delegate.saveSheet(sheet).alsoSchedule()
    override suspend fun deleteSheet(id: SheetId): AppResult<Unit> = delegate.deleteSheet(id).alsoSchedule()
    override suspend fun saveAlgorithm(algorithm: AlgorithmEntry): AppResult<AlgorithmEntry> =
        delegate.saveAlgorithm(algorithm).alsoSchedule()
    override suspend fun deleteAlgorithm(id: AlgorithmId): AppResult<Unit> = delegate.deleteAlgorithm(id).alsoSchedule()
    override suspend fun saveScramble(scramble: Scramble): AppResult<Scramble> =
        delegate.saveScramble(scramble).alsoSchedule()
    override suspend fun deleteScramble(id: ScrambleId): AppResult<Unit> = delegate.deleteScramble(id).alsoSchedule()
    override suspend fun saveTag(tag: Tag): AppResult<Tag> = delegate.saveTag(tag).alsoSchedule()
    override suspend fun deleteTag(id: TagId): AppResult<Unit> = delegate.deleteTag(id).alsoSchedule()
    override suspend fun setSheetTags(sheetId: SheetId, tagIds: Set<TagId>): AppResult<Unit> =
        delegate.setSheetTags(sheetId, tagIds).alsoSchedule()
    override suspend fun setScrambleTags(scrambleId: ScrambleId, tagIds: Set<TagId>): AppResult<Unit> =
        delegate.setScrambleTags(scrambleId, tagIds).alsoSchedule()

    private fun <T> AppResult<T>.alsoSchedule(): AppResult<T> {
        if (isSuccess) scheduler.schedulePush()
        return this
    }
}

class SyncingTimerRepository(
    private val delegate: TimerRepository,
    private val scheduler: CloudSyncScheduler,
) : TimerRepository {
    override suspend fun listTimerEntries(): AppResult<List<TimerEntry>> = delegate.listTimerEntries()
    override suspend fun saveTimerEntry(entry: TimerEntry): AppResult<TimerEntry> =
        delegate.saveTimerEntry(entry).alsoSchedule()
    override suspend fun deleteTimerEntry(id: TimerEntryId): AppResult<Unit> =
        delegate.deleteTimerEntry(id).alsoSchedule()
    override suspend fun clearTimerHistory(): AppResult<Unit> = delegate.clearTimerHistory().alsoSchedule()

    private fun <T> AppResult<T>.alsoSchedule(): AppResult<T> {
        if (isSuccess) scheduler.schedulePush()
        return this
    }
}

class SyncingSettingsRepository(
    private val delegate: SettingsRepository,
    private val scheduler: CloudSyncScheduler,
) : SettingsRepository {
    override suspend fun loadPreferences(): AppResult<UserPreferences> = delegate.loadPreferences()
    override suspend fun savePreferences(preferences: UserPreferences): AppResult<Unit> =
        delegate.savePreferences(preferences).also {
            if (it.isSuccess) scheduler.schedulePush()
        }
}
