package fr.olegueyan.algomix.ui.viewmodel

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.CloudAuthGateway
import fr.olegueyan.algomix.application.port.CloudSyncGateway
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.domain.cloud.CloudSession
import fr.olegueyan.algomix.domain.cloud.CloudUser
import fr.olegueyan.algomix.domain.cloud.SyncSummary
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.domain.settings.UserPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SettingsViewModelTest {
    @Test
    fun initialStateLoadsPreferencesAndSession() {
        val session = testSession()
        val repository = FakeSettingsRepository(UserPreferences(appAppearance = AppAppearance.DARK))
        val authGateway = FakeCloudAuthGateway(session = session)

        val viewModel = createViewModel(repository, authGateway)

        assertEquals(AppAppearance.DARK, viewModel.uiState.value.preferences.appAppearance)
        assertEquals(session, viewModel.uiState.value.cloudSession)
    }

    @Test
    fun preferenceChangesArePersisted() {
        val repository = FakeSettingsRepository()
        val viewModel = createViewModel(repository)

        viewModel.setAppAppearance(AppAppearance.DARK)
        viewModel.setCubeTheme(CubeTheme.CARBON)
        viewModel.setLocalCubeCacheEnabled(false)
        viewModel.setSessionPersistenceEnabled(false)

        assertEquals(AppAppearance.DARK, repository.preferences.appAppearance)
        assertEquals(CubeTheme.CARBON, repository.preferences.cubeTheme)
        assertFalse(repository.preferences.localCubeCacheEnabled)
        assertFalse(repository.preferences.sessionPersistenceEnabled)
        assertEquals("Préférences sauvegardées", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun persistenceTogglesClearSessionOrSerializedCube() {
        val cubeSessionRepository = FakeCubeSessionRepository(
            loadedSnapshot = LocalSessionSnapshot(
                serializedCubeState = "cube",
                activeRoute = "HOME",
                activeHomeMode = "VISUALIZATION",
                activeSequence = null,
                playbackIndex = 0,
                updatedAt = Instant.EPOCH,
            ),
        )
        val viewModel = createViewModel(cubeSessionRepository = cubeSessionRepository)

        viewModel.setLocalCubeCacheEnabled(false)

        assertNull(cubeSessionRepository.savedSnapshot?.serializedCubeState)

        viewModel.setSessionPersistenceEnabled(false)

        assertEquals(1, cubeSessionRepository.clearCalls)
    }

    @Test
    fun loginCreateAccountAndLogoutUseAuthGateway() {
        val authGateway = FakeCloudAuthGateway()
        val viewModel = createViewModel(authGateway = authGateway)

        viewModel.signIn("alex@example.com", "password")
        assertEquals("alex@example.com", viewModel.uiState.value.cloudSession?.user?.email)

        viewModel.createAccount("Martin", "Alex", "new@example.com", "secret", "secret")
        assertEquals("new@example.com", viewModel.uiState.value.cloudSession?.user?.email)

        viewModel.signOut()
        assertNull(viewModel.uiState.value.cloudSession)
        assertTrue(authGateway.signedOut)
    }

    @Test
    fun invalidFormsAreRejectedBeforeCallingGateway() {
        val authGateway = FakeCloudAuthGateway()
        val viewModel = createViewModel(authGateway = authGateway)

        viewModel.signIn("", "")
        assertEquals("Email et mot de passe requis", viewModel.uiState.value.feedbackMessage)
        assertEquals(0, authGateway.signInCalls)

        viewModel.createAccount("Martin", "Alex", "new@example.com", "secret", "different")
        assertEquals("Confirmation différente", viewModel.uiState.value.feedbackMessage)
        assertEquals(0, authGateway.createAccountCalls)
    }

    @Test
    fun missingCloudGatewayReportsNotConfigured() {
        val viewModel = createViewModel()

        viewModel.signIn("alex@example.com", "password")

        assertTrue(viewModel.uiState.value.isError)
        assertEquals("Backend cloud non configuré", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun recoverAndPurgeRequireSession() {
        val syncGateway = FakeCloudSyncGateway()
        val viewModel = createViewModel(syncGateway = syncGateway)

        viewModel.recoverCloud()
        viewModel.purgeCloud()

        assertEquals("Connexion cloud requise", viewModel.uiState.value.feedbackMessage)
        assertEquals(0, syncGateway.recoverCalls)
        assertEquals(0, syncGateway.purgeCalls)
    }

    @Test
    fun recoverAndPurgeUseSyncGatewayWithSession() {
        val syncGateway = FakeCloudSyncGateway()
        val viewModel = createViewModel(
            authGateway = FakeCloudAuthGateway(session = testSession()),
            syncGateway = syncGateway,
        )

        viewModel.recoverCloud()
        viewModel.purgeCloud()

        assertEquals(1, syncGateway.recoverCalls)
        assertEquals(1, syncGateway.purgeCalls)
        assertEquals("Cloud vidé: 3 suppressions", viewModel.uiState.value.feedbackMessage)
    }

    @Test
    fun changePasswordRequiresSessionAndMatchingConfirmation() {
        val authGateway = FakeCloudAuthGateway(session = testSession())
        val viewModel = createViewModel(authGateway = authGateway)

        viewModel.changePassword("old", "new", "different")
        assertEquals("Confirmation différente", viewModel.uiState.value.feedbackMessage)
        assertEquals(0, authGateway.changePasswordCalls)

        viewModel.changePassword("old", "new", "new")

        assertEquals(1, authGateway.changePasswordCalls)
        assertEquals("Mot de passe mis à jour", viewModel.uiState.value.feedbackMessage)
    }

    private fun createViewModel(
        repository: FakeSettingsRepository = FakeSettingsRepository(),
        authGateway: CloudAuthGateway? = null,
        syncGateway: CloudSyncGateway? = null,
        cubeSessionRepository: CubeSessionRepository? = null,
    ): SettingsViewModel =
        SettingsViewModel(
            settingsRepository = repository,
            cubeSessionRepository = cubeSessionRepository,
            cloudAuthGateway = authGateway,
            cloudSyncGateway = syncGateway,
            taskLauncher = { block -> runBlocking { block() } },
        )

    private class FakeSettingsRepository(
        var preferences: UserPreferences = UserPreferences(),
    ) : SettingsRepository {
        override suspend fun loadPreferences(): AppResult<UserPreferences> =
            AppResult.success(preferences)

        override suspend fun savePreferences(preferences: UserPreferences): AppResult<Unit> {
            this.preferences = preferences
            return AppResult.success(Unit)
        }
    }

    private class FakeCloudAuthGateway(
        private var session: CloudSession? = null,
    ) : CloudAuthGateway {
        var signInCalls = 0
        var createAccountCalls = 0
        var changePasswordCalls = 0
        var signedOut = false

        override suspend fun currentSession(): AppResult<CloudSession?> =
            AppResult.success(session)

        override suspend fun signIn(email: String, password: String): AppResult<CloudSession> {
            signInCalls += 1
            session = testSession(email)
            return AppResult.success(session ?: return AppResult.failure(AppError.Unknown()))
        }

        override suspend fun createAccount(
            email: String,
            password: String,
            firstName: String,
            lastName: String,
        ): AppResult<CloudSession> {
            createAccountCalls += 1
            session = testSession(email, firstName, lastName)
            return AppResult.success(session ?: return AppResult.failure(AppError.Unknown()))
        }

        override suspend fun signOut(): AppResult<Unit> {
            signedOut = true
            session = null
            return AppResult.success(Unit)
        }

        override suspend fun changePassword(
            currentPassword: String,
            newPassword: String,
        ): AppResult<Unit> {
            changePasswordCalls += 1
            return AppResult.success(Unit)
        }
    }

    private class FakeCloudSyncGateway : CloudSyncGateway {
        var recoverCalls = 0
        var purgeCalls = 0

        override suspend fun recover(): AppResult<SyncSummary> {
            recoverCalls += 1
            return AppResult.success(SyncSummary(pulledItems = 2))
        }

        override suspend fun pushPendingChanges(): AppResult<SyncSummary> =
            AppResult.success(SyncSummary(pushedItems = 1))

        override suspend fun purgeRemoteOnly(): AppResult<SyncSummary> {
            purgeCalls += 1
            return AppResult.success(SyncSummary(deletedRemoteItems = 3))
        }
    }

    private class FakeCubeSessionRepository(
        private val loadedSnapshot: LocalSessionSnapshot? = null,
    ) : CubeSessionRepository {
        var savedSnapshot: LocalSessionSnapshot? = null
        var clearCalls = 0

        override suspend fun loadSession(): AppResult<LocalSessionSnapshot?> =
            AppResult.success(loadedSnapshot)

        override suspend fun saveSession(snapshot: LocalSessionSnapshot): AppResult<Unit> {
            savedSnapshot = snapshot
            return AppResult.success(Unit)
        }

        override suspend fun clearSession(): AppResult<Unit> {
            clearCalls += 1
            return AppResult.success(Unit)
        }
    }

    companion object {
        private fun testSession(
            email: String = "alex@example.com",
            firstName: String = "Alex",
            lastName: String = "Martin",
        ): CloudSession =
            CloudSession(
                user = CloudUser(
                    id = "user-1",
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                ),
                authenticatedAt = Instant.EPOCH,
            )
    }
}
