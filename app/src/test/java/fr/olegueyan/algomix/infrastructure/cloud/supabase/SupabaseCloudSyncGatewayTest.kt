package fr.olegueyan.algomix.infrastructure.cloud.supabase

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.domain.library.CollectionId
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgomixDatabase
import fr.olegueyan.algomix.infrastructure.persistence.local.CollectionEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalLibraryRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalPersistenceDao
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalSettingsRepository
import fr.olegueyan.algomix.infrastructure.sync.CloudSyncScheduler
import fr.olegueyan.algomix.infrastructure.sync.SyncingLibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class SupabaseCloudSyncGatewayTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: AlgomixDatabase
    private lateinit var dao: LocalPersistenceDao
    private lateinit var clock: MutableClock
    private lateinit var settingsRepository: LocalSettingsRepository
    private lateinit var libraryRepository: LocalLibraryRepository
    private lateinit var remote: FakeCloudRemoteDataSource
    private lateinit var gateway: SupabaseCloudSyncGateway

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AlgomixDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.localPersistenceDao()
        clock = MutableClock(Instant.ofEpochMilli(100))
        settingsRepository = LocalSettingsRepository(dataStore("settings"), dao, clock)
        libraryRepository = LocalLibraryRepository(dao, clock)
        remote = FakeCloudRemoteDataSource()
        gateway = SupabaseCloudSyncGateway(dao, settingsRepository, remote, clock)
    }

    @After
    fun tearDown() {
        database.close()
        dataStoreScope.cancel()
    }

    @Test
    fun pushPendingChangesUploadsOutboxAndClearsIt() = runBlocking {
        libraryRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "OLL"))

        val summary = gateway.pushPendingChanges().getOrNull()

        assertEquals(1, summary?.pushedItems)
        assertEquals("OLL", remote.dataset.collections.single().name)
        assertTrue(dao.listOutbox().isEmpty())
    }

    @Test
    fun failedPushKeepsOutbox() = runBlocking {
        libraryRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "OLL"))
        remote.failNextMutation = true

        val result = gateway.pushPendingChanges()

        assertTrue(result.errorOrNull() is AppError.Network)
        assertEquals(1, dao.listOutbox().size)
    }

    @Test
    fun recoverMergesRemoteNewerValueAndCountsConflict() = runBlocking {
        libraryRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "Local"))
        clock.now = Instant.ofEpochMilli(150)
        dao.upsertCollection(CollectionEntity("collection-1", "Local newer", 150))
        remote.dataset = CloudDataset(
            collections = listOf(CollectionEntity("collection-1", "Remote newest", 200)),
        )

        val summary = gateway.recover().getOrNull()

        assertEquals(1, summary?.pulledItems)
        assertEquals(1, summary?.conflictCount)
        assertEquals("Remote newest", libraryRepository.listCollections().getOrNull()?.single()?.name)
    }

    @Test
    fun remoteTombstoneMasksLocalRows() = runBlocking {
        libraryRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "Local"))
        remote.dataset = CloudDataset(
            collections = listOf(CollectionEntity("collection-1", "Local", updatedAt = 200, deletedAt = 200)),
        )

        gateway.recover()

        assertTrue(libraryRepository.listCollections().getOrNull().orEmpty().isEmpty())
    }

    @Test
    fun purgeRemoteOnlyKeepsLocalDataAndPreventsImmediateReupload() = runBlocking {
        libraryRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "Local"))
        gateway.pushPendingChanges()
        libraryRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "Local changed"))

        val summary = gateway.purgeRemoteOnly().getOrNull()
        gateway.pushPendingChanges()

        assertEquals(1, summary?.deletedRemoteItems)
        assertEquals("Local changed", libraryRepository.listCollections().getOrNull()?.single()?.name)
        assertTrue(dao.listOutbox().isEmpty())
        assertTrue(remote.dataset.collections.single().deletedAt != null)
    }

    @Test
    fun settingsPreferencesCreateCloudOutbox() = runBlocking {
        settingsRepository.savePreferences(
            UserPreferences(
                appAppearance = AppAppearance.DARK,
                cubeTheme = CubeTheme.CARBON,
            ),
        )

        assertEquals(listOf(LocalSettingsRepository.USER_PREFERENCES_ENTITY), dao.listOutbox().map { it.entityType })
    }

    @Test
    fun syncingRepositorySchedulesPushAfterSuccessfulMutation() = runBlocking {
        val scheduler = FakeScheduler()
        val syncingRepository = SyncingLibraryRepository(libraryRepository, scheduler)

        syncingRepository.saveCollection(LibraryCollection(CollectionId("collection-1"), "PLL"))

        assertEquals(1, scheduler.scheduleCount)
    }

    @Test
    fun missingSupabaseConfigDoesNotCreateGateways() {
        val gateways = SupabaseClientFactory.createGateways(
            config = SupabaseConfig(url = "", publishableKey = ""),
            dao = dao,
            settingsRepository = settingsRepository,
            clockProvider = clock,
        )

        assertEquals(null, gateways)
    }

    private fun dataStore(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(temporaryFolder.root, "$name.preferences_pb") },
        )

    private class MutableClock(
        var now: Instant,
    ) : ClockProvider {
        override fun now(): Instant = now
    }

    private class FakeScheduler : CloudSyncScheduler {
        var scheduleCount = 0

        override fun schedulePush() {
            scheduleCount += 1
        }
    }
}
