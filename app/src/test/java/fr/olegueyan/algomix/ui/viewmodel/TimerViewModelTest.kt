package fr.olegueyan.algomix.ui.viewmodel

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.port.TimerRepository
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId
import fr.olegueyan.algomix.ui.state.TimerRunState
import fr.olegueyan.algomix.ui.timer.TimerTimeSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TimerViewModelTest {
    @Test
    fun initialStateLoadsHistory() {
        val repository = FakeTimerRepository(
            entries = mutableListOf(TimerEntry(TimerEntryId("entry-1"), 24_910, Instant.EPOCH)),
        )

        val viewModel = createViewModel(repository)

        assertEquals(listOf("00:24.91"), viewModel.uiState.value.history.map { it.durationLabel })
    }

    @Test
    fun startIncrementsDurationFromFakeTime() {
        val timeSource = FakeTimerTimeSource()
        val viewModel = createViewModel(timeSource = timeSource)

        viewModel.startOrPause()
        timeSource.advanceBy(23_410)
        viewModel.refreshElapsed()

        assertEquals(TimerRunState.RUNNING, viewModel.uiState.value.runState)
        assertEquals("00:23.41", viewModel.uiState.value.durationLabel)
    }

    @Test
    fun pauseFreezesDurationAndResumeContinuesAccumulatedTime() {
        val timeSource = FakeTimerTimeSource()
        val viewModel = createViewModel(timeSource = timeSource)

        viewModel.startOrPause()
        timeSource.advanceBy(10_000)
        viewModel.startOrPause()
        timeSource.advanceBy(5_000)
        viewModel.refreshElapsed()
        assertEquals("00:10.00", viewModel.uiState.value.durationLabel)

        viewModel.startOrPause()
        timeSource.advanceBy(2_500)
        viewModel.refreshElapsed()

        assertEquals(TimerRunState.RUNNING, viewModel.uiState.value.runState)
        assertEquals("00:12.50", viewModel.uiState.value.durationLabel)
    }

    @Test
    fun resetClearsCurrentDurationWithoutDeletingHistory() {
        val repository = FakeTimerRepository(
            entries = mutableListOf(TimerEntry(TimerEntryId("entry-1"), 1_000, Instant.EPOCH)),
        )
        val timeSource = FakeTimerTimeSource()
        val viewModel = createViewModel(repository, timeSource)

        viewModel.startOrPause()
        timeSource.advanceBy(3_000)
        viewModel.reset()

        assertEquals(TimerRunState.IDLE, viewModel.uiState.value.runState)
        assertEquals("00:00.00", viewModel.uiState.value.durationLabel)
        assertEquals(1, viewModel.uiState.value.history.size)
    }

    @Test
    fun saveZeroDurationIsRejected() {
        val repository = FakeTimerRepository()
        val viewModel = createViewModel(repository)

        viewModel.saveTime()

        assertTrue(repository.entries.isEmpty())
        assertTrue(viewModel.uiState.value.isError)
        assertEquals("Temps invalide", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun saveValidDurationStoresEntryReloadsHistoryAndResetsTimer() {
        val repository = FakeTimerRepository()
        val clock = FakeClock(Instant.ofEpochMilli(1_700_000_000_000))
        val timeSource = FakeTimerTimeSource()
        val viewModel = createViewModel(repository, timeSource, clock)

        viewModel.startOrPause()
        timeSource.advanceBy(9_876)
        viewModel.saveTime()

        assertEquals(1, repository.entries.size)
        assertEquals(9_876, repository.entries.single().durationMillis)
        assertEquals(clock.now(), repository.entries.single().solvedAt)
        assertEquals(TimerRunState.IDLE, viewModel.uiState.value.runState)
        assertEquals("00:00.00", viewModel.uiState.value.durationLabel)
        assertEquals(listOf("00:09.87"), viewModel.uiState.value.history.map { it.durationLabel })
    }

    @Test
    fun deleteAndClearHistoryReloadState() {
        val repository = FakeTimerRepository(
            entries = mutableListOf(
                TimerEntry(TimerEntryId("entry-1"), 1_000, Instant.EPOCH),
                TimerEntry(TimerEntryId("entry-2"), 2_000, Instant.EPOCH.plusMillis(1)),
            ),
        )
        val viewModel = createViewModel(repository)

        viewModel.deleteEntry(TimerEntryId("entry-1"))
        assertEquals(listOf(TimerEntryId("entry-2")), viewModel.uiState.value.history.map { it.id })

        viewModel.clearHistory()
        assertTrue(viewModel.uiState.value.history.isEmpty())
    }

    private fun createViewModel(
        repository: FakeTimerRepository = FakeTimerRepository(),
        timeSource: FakeTimerTimeSource = FakeTimerTimeSource(),
        clockProvider: ClockProvider = FakeClock(Instant.EPOCH),
    ): TimerViewModel =
        TimerViewModel(
            timerRepository = repository,
            clockProvider = clockProvider,
            timeSource = timeSource,
            taskLauncher = { block -> runBlocking { block() } },
        )

    private class FakeTimerRepository(
        val entries: MutableList<TimerEntry> = mutableListOf(),
    ) : TimerRepository {
        override suspend fun listTimerEntries(): AppResult<List<TimerEntry>> =
            AppResult.success(entries.sortedByDescending { it.solvedAt })

        override suspend fun saveTimerEntry(entry: TimerEntry): AppResult<TimerEntry> {
            entries.removeAll { it.id == entry.id }
            entries += entry
            return AppResult.success(entry)
        }

        override suspend fun deleteTimerEntry(id: TimerEntryId): AppResult<Unit> {
            entries.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun clearTimerHistory(): AppResult<Unit> {
            entries.clear()
            return AppResult.success(Unit)
        }
    }

    private class FakeTimerTimeSource : TimerTimeSource {
        private var nowMillis: Long = 0

        override fun elapsedRealtimeMillis(): Long = nowMillis

        fun advanceBy(durationMillis: Long) {
            nowMillis += durationMillis
        }
    }

    private class FakeClock(
        private val now: Instant,
    ) : ClockProvider {
        override fun now(): Instant = now
    }
}
