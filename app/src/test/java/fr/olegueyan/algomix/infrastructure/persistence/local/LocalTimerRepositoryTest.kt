package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.room.Room
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class LocalTimerRepositoryTest {
    private lateinit var database: AlgomixDatabase
    private lateinit var dao: LocalPersistenceDao
    private lateinit var repository: LocalTimerRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AlgomixDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.localPersistenceDao()
        repository = LocalTimerRepository(dao, FixedClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savesListsAndDeletesTimerEntries() = runBlocking {
        val older = TimerEntry(TimerEntryId("entry-1"), 12_345, Instant.ofEpochMilli(100))
        val newer = TimerEntry(TimerEntryId("entry-2"), 9_876, Instant.ofEpochMilli(200))

        repository.saveTimerEntry(older)
        repository.saveTimerEntry(newer)
        repository.deleteTimerEntry(older.id)

        assertEquals(listOf(newer), repository.listTimerEntries().getOrNull())
        assertEquals(listOf("UPSERT", "UPSERT", "DELETE"), dao.listOutbox().map { it.operation })
    }

    @Test
    fun clearTimerHistorySoftDeletesAllActiveEntries() = runBlocking {
        repository.saveTimerEntry(TimerEntry(TimerEntryId("entry-1"), 12_345, Instant.ofEpochMilli(100)))
        repository.saveTimerEntry(TimerEntry(TimerEntryId("entry-2"), 9_876, Instant.ofEpochMilli(200)))

        repository.clearTimerHistory()

        assertTrue(repository.listTimerEntries().getOrNull().orEmpty().isEmpty())
        assertEquals(4, dao.listOutbox().size)
    }

    @Test
    fun rejectsZeroDuration() = runBlocking {
        val result = repository.saveTimerEntry(TimerEntry(TimerEntryId("entry-1"), 0, Instant.EPOCH))

        assertTrue(result.errorOrNull() is AppError.Validation)
    }

    @Test
    fun deletingUnknownTimerEntryReturnsNotFound() = runBlocking {
        val result = repository.deleteTimerEntry(TimerEntryId("missing"))

        assertTrue(result.errorOrNull() is AppError.NotFound)
    }

    private object FixedClock : ClockProvider {
        override fun now(): Instant = Instant.ofEpochMilli(1_700_000_000_000)
    }
}
