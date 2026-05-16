package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.domain.settings.UserPreferences
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalDataStoreRepositoriesTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun settingsRepositoryReturnsDefaultsWhenEmpty() = runBlocking {
        val repository = LocalSettingsRepository(dataStore("settings-defaults"))

        assertEquals(UserPreferences(), repository.loadPreferences().getOrNull())
    }

    @Test
    fun settingsRepositorySavesAndReloadsPreferences() = runBlocking {
        val repository = LocalSettingsRepository(dataStore("settings-save"))
        val preferences = UserPreferences(
            appAppearance = AppAppearance.DARK,
            cubeTheme = CubeTheme.CARBON,
            localCubeCacheEnabled = false,
            sessionPersistenceEnabled = false,
        )

        repository.savePreferences(preferences)

        assertEquals(preferences, repository.loadPreferences().getOrNull())
    }

    @Test
    fun sessionRepositorySavesReloadsAndClearsSnapshot() = runBlocking {
        val repository = LocalCubeSessionRepository(dataStore("session-save"))
        val snapshot = LocalSessionSnapshot(
            serializedCubeState = "cube",
            activeRoute = "home",
            activeHomeMode = "edit",
            activeSequence = "R U",
            playbackIndex = 2,
            updatedAt = Instant.ofEpochMilli(1_700_000_000_000),
        )

        repository.saveSession(snapshot)
        val loaded = repository.loadSession().getOrNull()
        repository.clearSession()

        assertEquals(snapshot, loaded)
        assertNull(repository.loadSession().getOrNull())
    }

    private fun dataStore(name: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "$name.preferences_pb") },
        )
}
