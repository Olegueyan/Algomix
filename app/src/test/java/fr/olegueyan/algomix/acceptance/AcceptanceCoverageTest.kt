package fr.olegueyan.algomix.acceptance

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.port.CloudAuthGateway
import fr.olegueyan.algomix.application.port.CloudSyncGateway
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.application.port.TimerRepository
import fr.olegueyan.algomix.domain.cloud.CloudSession
import fr.olegueyan.algomix.domain.cloud.CloudUser
import fr.olegueyan.algomix.domain.cloud.SyncSummary
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.MoveExecutor
import fr.olegueyan.algomix.domain.cube.MoveParser
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
import fr.olegueyan.algomix.domain.scan.ScanFaceDraft
import fr.olegueyan.algomix.domain.scan.ScanFaceletAssembler
import fr.olegueyan.algomix.domain.scan.ScanSessionDraft
import fr.olegueyan.algomix.domain.session.CubeSessionCodec
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.LibraryItemType
import fr.olegueyan.algomix.ui.state.SaveEditTarget
import fr.olegueyan.algomix.ui.timer.TimerTimeSource
import fr.olegueyan.algomix.ui.viewmodel.LibraryViewModel
import fr.olegueyan.algomix.ui.viewmodel.SettingsViewModel
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import fr.olegueyan.algomix.ui.viewmodel.TimerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

@Suppress("LargeClass", "TooManyFunctions")
class AcceptanceCoverageTest {
    @Test
    fun ac01Ac09ScanAppliesCubeAndSessionRestoresSerializedState() = runBlocking {
        val scannedCube = MoveExecutor.apply(CubeState.solved(), MoveParser.parse("R U"))
        val scanSession = scanSessionFrom(scannedCube)
        val faceletCube = requireNotNull(ScanFaceletAssembler.assemble(scanSession).faceletCube)
        val repository = FakeCubeSessionRepository()
        val viewModel = sharedCubeViewModel(repository)

        viewModel.applyScannedCube(CubeState.fromFaceletCube(faceletCube))

        assertEquals(scannedCube, viewModel.uiState.value.cubeState)
        assertEquals(scannedCube, CubeSessionCodec.decode(repository.savedSnapshot?.serializedCubeState))

        val restored = sharedCubeViewModel(
            FakeCubeSessionRepository(loadedSnapshot = repository.savedSnapshot),
        )
        restored.restoreSession()

        assertEquals(scannedCube, restored.uiState.value.cubeState)
        assertEquals(HomeMode.VISUALIZATION, restored.uiState.value.homeMode)
    }

    @Test
    fun ac02ToAc04AndTst01HomeModesKeepSharedCubeCoherent() {
        val viewModel = sharedCubeViewModel()

        viewModel.scramble(length = 6)
        val scrambledCube = viewModel.uiState.value.cubeState
        viewModel.setHomeMode(HomeMode.PLAY)
        viewModel.playPrevious()
        viewModel.setHomeMode(HomeMode.EDIT)
        viewModel.applyMoveToken("R")
        viewModel.undoEditing()
        viewModel.redoEditing()
        viewModel.suppressLastEditingMove()
        viewModel.deleteAllEditing()

        assertNotEquals(CubeState.solved(), scrambledCube)
        assertEquals(HomeMode.EDIT, viewModel.uiState.value.homeMode)
        assertTrue(viewModel.uiState.value.editingSession.sequence.isEmpty)
    }

    @Test
    fun ac05Ac06AndTst02LibraryCrudTagsFilterAndImport() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = LibraryViewModel(repository, taskLauncher = immediateTaskLauncher())

        viewModel.saveEditingSequence(
            target = SaveEditTarget.EXISTING_SHEET,
            collectionId = CollectionId("collection-1"),
            sheetId = SheetId("sheet-1"),
            name = "Saved edit",
            sequence = "R U R' U'",
        )
        viewModel.importAlgorithm(SheetId("sheet-1"), "Invalid", "NOPE")
        viewModel.setSheetTags(SheetId("sheet-1"), listOf("Training"))
        val tag = repository.tags.first { it.name == "Training" }
        viewModel.toggleTagFilter(tag.id)
        viewModel.setItemType(LibraryItemType.SHEETS)

        assertTrue(repository.algorithms.any { it.name == "Saved edit" })
        assertTrue(repository.algorithms.none { it.name == "Invalid" })
        assertEquals(1, viewModel.uiState.value.visibleSections.single().sheets.size)
    }

    @Test
    fun ac07AndTst03TimerRecordsDatedHistory() {
        val repository = FakeTimerRepository()
        val timeSource = FakeTimeSource()
        val viewModel = TimerViewModel(
            timerRepository = repository,
            clockProvider = FixedClock,
            timeSource = timeSource,
            taskLauncher = immediateTaskLauncher(),
        )

        viewModel.startOrPause()
        timeSource.nowMillis = 2_345
        viewModel.refreshElapsed()
        viewModel.startOrPause()
        viewModel.saveTime()

        assertEquals(1, repository.entries.size)
        assertEquals("Temps sauvegarde", viewModel.uiState.value.feedbackMessage)
        assertTrue(viewModel.uiState.value.history.single().solvedAtLabel.contains("2023"))
    }

    @Test
    fun ac08AndTst04CloudRequiresSessionThenReportsRecoverAndPurge() {
        val authGateway = FakeCloudAuthGateway()
        val syncGateway = FakeCloudSyncGateway()
        val viewModel = SettingsViewModel(
            settingsRepository = FakeSettingsRepository(),
            cloudAuthGateway = authGateway,
            cloudSyncGateway = syncGateway,
            taskLauncher = immediateTaskLauncher(),
        )

        viewModel.recoverCloud()
        assertEquals("Connexion cloud requise", viewModel.uiState.value.feedbackMessage)
        assertEquals(0, syncGateway.recoverCalls)

        viewModel.signIn("alex@example.com", "password")
        viewModel.recoverCloud()
        viewModel.purgeCloud()

        assertEquals(1, syncGateway.recoverCalls)
        assertEquals(1, syncGateway.purgeCalls)
        assertEquals("Cloud vide: 1 suppressions", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun ac10DocumentationPdfAndMockupPairsArePresent() {
        val root = sequenceOf(File("."), File(".."))
            .first { candidate -> File(candidate, "docs/requirements-specification.pdf").isFile }
        val schemas = File(
            root,
            "Blueprint/Documentation/chapters/requirements-specification/07-technical-schemas.typ",
        )
            .readText()

        assertTrue(File(root, "docs/requirements-specification.pdf").isFile)
        assertTrue(File(root, "docs/rubiks-cube-notation-system.pdf").isFile)
        assertTrue(schemas.contains("columns: 2"))
        assertTrue(schemas.split("Assets/Mockups/Exports").size > MIN_MOCKUP_REFERENCES)
    }

    private fun sharedCubeViewModel(
        repository: FakeCubeSessionRepository = FakeCubeSessionRepository(),
    ): SharedCubeViewModel =
        SharedCubeViewModel(
            cubeSessionRepository = repository,
            clockProvider = FixedClock,
            autoLoadSession = false,
            taskLauncher = immediateTaskLauncher(),
        )

    private fun scanSessionFrom(cubeState: CubeState): ScanSessionDraft =
        ScanSessionDraft(
            faces = cubeState.faceletCube.stickers
                .chunked(ScanFaceDraft.STICKER_COUNT)
                .mapIndexed { index, stickers ->
                    ScanFaceDraft(faceIndex = index, stickers = stickers)
                },
        )

    private fun immediateTaskLauncher(): ((suspend () -> Unit) -> Unit) =
        { block -> runBlocking { block() } }

    private class FakeCubeSessionRepository(
        private val loadedSnapshot: LocalSessionSnapshot? = null,
    ) : CubeSessionRepository {
        var savedSnapshot: LocalSessionSnapshot? = null

        override suspend fun loadSession(): AppResult<LocalSessionSnapshot?> =
            AppResult.success(loadedSnapshot)

        override suspend fun saveSession(snapshot: LocalSessionSnapshot): AppResult<Unit> {
            savedSnapshot = snapshot
            return AppResult.success(Unit)
        }

        override suspend fun clearSession(): AppResult<Unit> =
            AppResult.success(Unit)
    }

    private class FakeLibraryRepository : LibraryRepository {
        val collections = mutableListOf<LibraryCollection>()
        val sheets = mutableListOf<AlgorithmSheet>()
        val algorithms = mutableListOf<AlgorithmEntry>()
        val scrambles = mutableListOf<Scramble>()
        val tags = mutableListOf<Tag>()
        val sheetTags = mutableMapOf<SheetId, Set<TagId>>()
        val scrambleTags = mutableMapOf<ScrambleId, Set<TagId>>()

        override suspend fun listCollections(): AppResult<List<LibraryCollection>> =
            AppResult.success(collections)

        override suspend fun saveCollection(collection: LibraryCollection): AppResult<LibraryCollection> {
            collections.removeAll { it.id == collection.id }
            collections += collection
            return AppResult.success(collection)
        }

        override suspend fun deleteCollection(id: CollectionId): AppResult<Unit> {
            collections.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listSheets(collectionId: CollectionId?): AppResult<List<AlgorithmSheet>> =
            AppResult.success(sheets.filter { collectionId == null || it.collectionId == collectionId })

        override suspend fun saveSheet(sheet: AlgorithmSheet): AppResult<AlgorithmSheet> {
            sheets.removeAll { it.id == sheet.id }
            sheets += sheet
            return AppResult.success(sheet)
        }

        override suspend fun deleteSheet(id: SheetId): AppResult<Unit> {
            sheets.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listAlgorithms(sheetId: SheetId): AppResult<List<AlgorithmEntry>> =
            AppResult.success(algorithms.filter { it.sheetId == sheetId }.sortedBy { it.position })

        override suspend fun saveAlgorithm(algorithm: AlgorithmEntry): AppResult<AlgorithmEntry> {
            algorithms.removeAll { it.id == algorithm.id }
            algorithms += algorithm
            return AppResult.success(algorithm)
        }

        override suspend fun deleteAlgorithm(id: AlgorithmId): AppResult<Unit> {
            algorithms.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listScrambles(collectionId: CollectionId?): AppResult<List<Scramble>> =
            AppResult.success(scrambles.filter { collectionId == null || it.collectionId == collectionId })

        override suspend fun saveScramble(scramble: Scramble): AppResult<Scramble> {
            scrambles.removeAll { it.id == scramble.id }
            scrambles += scramble
            return AppResult.success(scramble)
        }

        override suspend fun deleteScramble(id: ScrambleId): AppResult<Unit> {
            scrambles.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listTags(): AppResult<List<Tag>> =
            AppResult.success(tags)

        override suspend fun saveTag(tag: Tag): AppResult<Tag> {
            tags.removeAll { it.id == tag.id }
            tags += tag
            return AppResult.success(tag)
        }

        override suspend fun deleteTag(id: TagId): AppResult<Unit> {
            tags.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun setSheetTags(sheetId: SheetId, tagIds: Set<TagId>): AppResult<Unit> {
            sheetTags[sheetId] = tagIds
            return AppResult.success(Unit)
        }

        override suspend fun setScrambleTags(scrambleId: ScrambleId, tagIds: Set<TagId>): AppResult<Unit> {
            scrambleTags[scrambleId] = tagIds
            return AppResult.success(Unit)
        }

        override suspend fun listSheetTagIds(sheetId: SheetId): AppResult<Set<TagId>> =
            AppResult.success(sheetTags[sheetId].orEmpty())

        override suspend fun listScrambleTagIds(scrambleId: ScrambleId): AppResult<Set<TagId>> =
            AppResult.success(scrambleTags[scrambleId].orEmpty())

        companion object {
            fun seeded(): FakeLibraryRepository =
                FakeLibraryRepository().apply {
                    collections += LibraryCollection(CollectionId("collection-1"), "Training")
                    sheets += AlgorithmSheet(SheetId("sheet-1"), CollectionId("collection-1"), "OLL")
                }
        }
    }

    private class FakeTimerRepository : TimerRepository {
        val entries = mutableListOf<TimerEntry>()

        override suspend fun listTimerEntries(): AppResult<List<TimerEntry>> =
            AppResult.success(entries.sortedByDescending { it.solvedAt })

        override suspend fun saveTimerEntry(entry: TimerEntry): AppResult<TimerEntry> {
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

    private class FakeTimeSource : TimerTimeSource {
        var nowMillis: Long = 0

        override fun elapsedRealtimeMillis(): Long = nowMillis
    }

    private class FakeSettingsRepository : SettingsRepository {
        override suspend fun loadPreferences(): AppResult<UserPreferences> =
            AppResult.success(UserPreferences())

        override suspend fun savePreferences(preferences: UserPreferences): AppResult<Unit> =
            AppResult.success(Unit)
    }

    private class FakeCloudAuthGateway : CloudAuthGateway {
        private var session: CloudSession? = null

        override suspend fun currentSession(): AppResult<CloudSession?> =
            AppResult.success(session)

        override suspend fun signIn(email: String, password: String): AppResult<CloudSession> {
            session = CloudSession(
                user = CloudUser(id = "user-1", email = email),
                authenticatedAt = FixedClock.now(),
            )
            return AppResult.success(requireNotNull(session))
        }

        override suspend fun createAccount(
            email: String,
            password: String,
            firstName: String,
            lastName: String,
        ): AppResult<CloudSession> =
            signIn(email, password)

        override suspend fun signOut(): AppResult<Unit> {
            session = null
            return AppResult.success(Unit)
        }

        override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> =
            AppResult.success(Unit)
    }

    private class FakeCloudSyncGateway : CloudSyncGateway {
        var recoverCalls = 0
        var purgeCalls = 0

        override suspend fun recover(): AppResult<SyncSummary> {
            recoverCalls += 1
            return AppResult.success(SyncSummary(pulledItems = 1))
        }

        override suspend fun pushPendingChanges(): AppResult<SyncSummary> =
            AppResult.success(SyncSummary(pushedItems = 1))

        override suspend fun purgeRemoteOnly(): AppResult<SyncSummary> {
            purgeCalls += 1
            return AppResult.success(SyncSummary(deletedRemoteItems = 1))
        }
    }

    private object FixedClock : ClockProvider {
        override fun now(): Instant = Instant.parse("2023-05-19T10:15:30Z")
    }

    private companion object {
        const val MIN_MOCKUP_REFERENCES = 10
    }
}
