package fr.olegueyan.algomix.infrastructure.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import fr.olegueyan.algomix.BuildConfig
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.di.AppContainer
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.application.port.TimerRepository
import fr.olegueyan.algomix.infrastructure.cloud.supabase.SupabaseClientFactory
import fr.olegueyan.algomix.infrastructure.cloud.supabase.SupabaseConfig
import fr.olegueyan.algomix.infrastructure.cloud.supabase.SupabaseTokenStore
import fr.olegueyan.algomix.infrastructure.export.LocalPdfExporter
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgomixDatabase
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalCubeSessionRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalLibraryRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalSettingsRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalTimerRepository
import fr.olegueyan.algomix.infrastructure.scan.CameraXCubeScanner
import fr.olegueyan.algomix.infrastructure.sync.SyncingLibraryRepository
import fr.olegueyan.algomix.infrastructure.sync.SyncingSettingsRepository
import fr.olegueyan.algomix.infrastructure.sync.SyncingTimerRepository
import fr.olegueyan.algomix.infrastructure.sync.WorkManagerCloudSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object AndroidAppContainerFactory {
    fun create(
        context: Context,
        clockProvider: ClockProvider = SystemClockProvider,
    ): AppContainer {
        val appContext = context.applicationContext
        val database = AlgomixDatabase.create(appContext)
        val dao = database.localPersistenceDao()
        val localLibraryRepository = LocalLibraryRepository(dao, clockProvider)
        val localTimerRepository = LocalTimerRepository(dao, clockProvider)
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settingsDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { appContext.preferencesDataStoreFile("user_preferences") },
        )
        val sessionDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { appContext.preferencesDataStoreFile("local_session") },
        )
        val localSettingsRepository = LocalSettingsRepository(settingsDataStore, dao, clockProvider)
        val tokenStore = SupabaseTokenStore(
            appContext.getSharedPreferences("supabase_auth", android.content.Context.MODE_PRIVATE),
        )
        val supabaseGateways = SupabaseClientFactory.createGateways(
            config = SupabaseConfig(
                url = BuildConfig.SUPABASE_URL,
                publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            ),
            dao = dao,
            settingsRepository = localSettingsRepository,
            clockProvider = clockProvider,
            tokenStore = tokenStore,
        )
        val scheduler = supabaseGateways?.let { WorkManagerCloudSyncScheduler(appContext) }
        val libraryRepository: LibraryRepository =
            if (scheduler == null) {
                localLibraryRepository
            } else {
                SyncingLibraryRepository(
                    localLibraryRepository,
                    scheduler,
                )
            }
        val timerRepository: TimerRepository =
            if (scheduler == null) {
                localTimerRepository
            } else {
                SyncingTimerRepository(localTimerRepository, scheduler)
            }
        val settingsRepository: SettingsRepository =
            if (scheduler == null) {
                localSettingsRepository
            } else {
                SyncingSettingsRepository(
                    localSettingsRepository,
                    scheduler,
                )
            }

        return AppContainer(
            clockProvider = clockProvider,
            configuredCubeSessionRepository = LocalCubeSessionRepository(sessionDataStore),
            configuredLibraryRepository = libraryRepository,
            configuredTimerRepository = timerRepository,
            configuredSettingsRepository = settingsRepository,
            configuredCloudAuthGateway = supabaseGateways?.authGateway,
            configuredCloudSyncGateway = supabaseGateways?.syncGateway,
            configuredPdfExporter = LocalPdfExporter(appContext, localLibraryRepository),
            configuredCubeScanner = CameraXCubeScanner(clockProvider),
        )
    }
}
