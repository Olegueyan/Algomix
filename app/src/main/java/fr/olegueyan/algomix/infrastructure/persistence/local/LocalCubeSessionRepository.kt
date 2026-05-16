package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot
import kotlinx.coroutines.flow.first
import java.time.Instant

class LocalCubeSessionRepository(
    private val dataStore: DataStore<Preferences>,
) : CubeSessionRepository {
    override suspend fun loadSession(): AppResult<LocalSessionSnapshot?> =
        storageResult {
            val preferences = dataStore.data.first()
            val activeRoute = preferences[Keys.ACTIVE_ROUTE] ?: return@storageResult AppResult.success(null)
            val activeHomeMode = preferences[Keys.ACTIVE_HOME_MODE] ?: return@storageResult AppResult.success(null)
            val updatedAt = preferences[Keys.UPDATED_AT] ?: return@storageResult AppResult.success(null)
            AppResult.success(
                LocalSessionSnapshot(
                    serializedCubeState = preferences[Keys.SERIALIZED_CUBE_STATE],
                    activeRoute = activeRoute,
                    activeHomeMode = activeHomeMode,
                    activeSequence = preferences[Keys.ACTIVE_SEQUENCE],
                    playbackIndex = preferences[Keys.PLAYBACK_INDEX] ?: 0,
                    updatedAt = Instant.ofEpochMilli(updatedAt),
                ),
            )
        }

    override suspend fun saveSession(snapshot: LocalSessionSnapshot): AppResult<Unit> =
        storageResult {
            dataStore.edit { mutablePreferences ->
                mutablePreferences[Keys.ACTIVE_ROUTE] = snapshot.activeRoute
                mutablePreferences[Keys.ACTIVE_HOME_MODE] = snapshot.activeHomeMode
                mutablePreferences[Keys.PLAYBACK_INDEX] = snapshot.playbackIndex
                mutablePreferences[Keys.UPDATED_AT] = snapshot.updatedAt.toEpochMilli()
                snapshot.serializedCubeState.writeOptional(mutablePreferences, Keys.SERIALIZED_CUBE_STATE)
                snapshot.activeSequence.writeOptional(mutablePreferences, Keys.ACTIVE_SEQUENCE)
            }
            AppResult.success(Unit)
        }

    override suspend fun clearSession(): AppResult<Unit> =
        storageResult {
            dataStore.edit { mutablePreferences -> mutablePreferences.clear() }
            AppResult.success(Unit)
        }

    private fun String?.writeOptional(
        preferences: MutablePreferences,
        key: Preferences.Key<String>,
    ) {
        if (this == null) {
            preferences.remove(key)
        } else {
            preferences[key] = this
        }
    }

    private object Keys {
        val SERIALIZED_CUBE_STATE = stringPreferencesKey("serialized_cube_state")
        val ACTIVE_ROUTE = stringPreferencesKey("active_route")
        val ACTIVE_HOME_MODE = stringPreferencesKey("active_home_mode")
        val ACTIVE_SEQUENCE = stringPreferencesKey("active_sequence")
        val PLAYBACK_INDEX = intPreferencesKey("playback_index")
        val UPDATED_AT = longPreferencesKey("updated_at")
    }
}
