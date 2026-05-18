package fr.olegueyan.algomix.ui.state

import fr.olegueyan.algomix.domain.cloud.CloudSession
import fr.olegueyan.algomix.domain.settings.UserPreferences

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val cloudSession: CloudSession? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val isError: Boolean = false,
) {
    val isAuthenticated: Boolean
        get() = cloudSession != null
}
