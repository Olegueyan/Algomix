package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.domain.settings.UserPreferences
import kotlinx.coroutines.flow.first

class LocalSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val dao: LocalPersistenceDao? = null,
    private val clockProvider: ClockProvider = SystemClockProvider,
) : SettingsRepository {
    override suspend fun loadPreferences(): AppResult<UserPreferences> =
        storageResult {
            val preferences = dataStore.data.first()
            AppResult.success(
                UserPreferences(
                    appAppearance = enumValueOrDefault(
                        preferences[Keys.APP_APPEARANCE],
                        UserPreferences().appAppearance,
                    ),
                    cubeTheme = enumValueOrDefault(
                        preferences[Keys.CUBE_THEME],
                        UserPreferences().cubeTheme,
                    ),
                    localCubeCacheEnabled = preferences[Keys.LOCAL_CUBE_CACHE_ENABLED]
                        ?: UserPreferences().localCubeCacheEnabled,
                    sessionPersistenceEnabled = preferences[Keys.SESSION_PERSISTENCE_ENABLED]
                        ?: UserPreferences().sessionPersistenceEnabled,
                ),
            )
        }

    override suspend fun savePreferences(preferences: UserPreferences): AppResult<Unit> =
        storageResult {
            savePreferencesWithoutOutbox(preferences)
            val now = clockProvider.nowMillis()
            dao?.enqueueCloudMutation(USER_PREFERENCES_ENTITY, USER_PREFERENCES_ID, OUTBOX_OPERATION_UPSERT, now)
            AppResult.success(Unit)
        }

    suspend fun savePreferencesFromCloud(preferences: UserPreferences, updatedAt: Long): AppResult<Unit> =
        storageResult {
            savePreferencesWithoutOutbox(preferences)
            dao?.upsertSyncMetadata(
                SyncMetadataEntity(
                    id = "$USER_PREFERENCES_ENTITY:$USER_PREFERENCES_ID",
                    entityType = USER_PREFERENCES_ENTITY,
                    entityId = USER_PREFERENCES_ID,
                    updatedAt = updatedAt,
                    cloudEligible = true,
                ),
            )
            AppResult.success(Unit)
        }

    private suspend fun savePreferencesWithoutOutbox(preferences: UserPreferences) {
        dataStore.edit { mutablePreferences ->
            mutablePreferences[Keys.APP_APPEARANCE] = preferences.appAppearance.name
            mutablePreferences[Keys.CUBE_THEME] = preferences.cubeTheme.name
            mutablePreferences[Keys.LOCAL_CUBE_CACHE_ENABLED] = preferences.localCubeCacheEnabled
            mutablePreferences[Keys.SESSION_PERSISTENCE_ENABLED] = preferences.sessionPersistenceEnabled
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        if (value == null) {
            default
        } else {
            enumValues<T>().firstOrNull { it.name == value } ?: default
        }

    private object Keys {
        val APP_APPEARANCE = stringPreferencesKey("app_appearance")
        val CUBE_THEME = stringPreferencesKey("cube_theme")
        val LOCAL_CUBE_CACHE_ENABLED = booleanPreferencesKey("local_cube_cache_enabled")
        val SESSION_PERSISTENCE_ENABLED = booleanPreferencesKey("session_persistence_enabled")
    }

    companion object {
        const val USER_PREFERENCES_ENTITY = "user_preferences"
        const val USER_PREFERENCES_ID = "current"
    }
}
