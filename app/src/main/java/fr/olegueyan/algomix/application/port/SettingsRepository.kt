package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.settings.UserPreferences

interface SettingsRepository {
    suspend fun loadPreferences(): AppResult<UserPreferences>

    suspend fun savePreferences(preferences: UserPreferences): AppResult<Unit>
}
