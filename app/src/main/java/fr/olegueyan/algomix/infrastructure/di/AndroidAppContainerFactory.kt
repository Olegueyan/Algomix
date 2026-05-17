package fr.olegueyan.algomix.infrastructure.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.di.AppContainer
import fr.olegueyan.algomix.infrastructure.export.LocalPdfExporter
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgomixDatabase
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalCubeSessionRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalLibraryRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalSettingsRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalTimerRepository
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
        val libraryRepository = LocalLibraryRepository(dao, clockProvider)
        val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val settingsDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { appContext.preferencesDataStoreFile("user_preferences") },
        )
        val sessionDataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { appContext.preferencesDataStoreFile("local_session") },
        )

        return AppContainer(
            clockProvider = clockProvider,
            configuredCubeSessionRepository = LocalCubeSessionRepository(sessionDataStore),
            configuredLibraryRepository = libraryRepository,
            configuredTimerRepository = LocalTimerRepository(dao, clockProvider),
            configuredSettingsRepository = LocalSettingsRepository(settingsDataStore),
            configuredPdfExporter = LocalPdfExporter(appContext, libraryRepository),
        )
    }
}
