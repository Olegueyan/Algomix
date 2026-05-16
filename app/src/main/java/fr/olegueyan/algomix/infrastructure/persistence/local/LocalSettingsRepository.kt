package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.domain.settings.UserPreferences
import kotlinx.coroutines.flow.first

class LocalSettingsRepository(
    private val dataStore: DataStore<Preferences>,
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
            dataStore.edit { mutablePreferences ->
                mutablePreferences[Keys.APP_APPEARANCE] = preferences.appAppearance.name
                mutablePreferences[Keys.CUBE_THEME] = preferences.cubeTheme.name
                mutablePreferences[Keys.LOCAL_CUBE_CACHE_ENABLED] = preferences.localCubeCacheEnabled
                mutablePreferences[Keys.SESSION_PERSISTENCE_ENABLED] = preferences.sessionPersistenceEnabled
            }
            AppResult.success(Unit)
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
}
