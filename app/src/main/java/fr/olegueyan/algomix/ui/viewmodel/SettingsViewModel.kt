package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.CloudAuthGateway
import fr.olegueyan.algomix.application.port.CloudSyncGateway
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.ui.state.SettingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val cubeSessionRepository: CubeSessionRepository? = null,
    private val cloudAuthGateway: CloudAuthGateway? = null,
    private val cloudSyncGateway: CloudSyncGateway? = null,
    private val taskLauncher: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = mutableUiState.asStateFlow()
    private val mutableAppRefreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = REFRESH_EVENT_BUFFER)
    val appRefreshEvents: SharedFlow<Unit> = mutableAppRefreshEvents.asSharedFlow()

    init {
        launchTask {
            loadPreferences()
            refreshSession()
        }
    }

    fun setAppAppearance(appearance: AppAppearance) {
        savePreferences(mutableUiState.value.preferences.copy(appAppearance = appearance))
    }

    fun setCubeTheme(theme: CubeTheme) {
        savePreferences(mutableUiState.value.preferences.copy(cubeTheme = theme))
    }

    fun setLocalCubeCacheEnabled(enabled: Boolean) {
        savePreferences(mutableUiState.value.preferences.copy(localCubeCacheEnabled = enabled))
        if (!enabled) {
            launchTask { removeSerializedCubeFromSnapshot() }
        }
    }

    fun setSessionPersistenceEnabled(enabled: Boolean) {
        savePreferences(mutableUiState.value.preferences.copy(sessionPersistenceEnabled = enabled))
        if (!enabled) {
            launchTask { cubeSessionRepository?.clearSession() }
        }
    }

    fun signIn(email: String, password: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.isBlank()) {
            setError("Email et mot de passe requis")
            return
        }
        val gateway = cloudAuthGateway ?: return setError("Backend cloud non configuré")
        launchTask {
            when (val result = gateway.signIn(normalizedEmail, password)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    cloudSession = result.value,
                    feedbackMessage = "Connexion réussie",
                    isError = false,
                )
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun createAccount(
        lastName: String,
        firstName: String,
        email: String,
        password: String,
        passwordConfirmation: String,
    ) {
        val normalizedEmail = email.trim()
        if (lastName.trim().isBlank() || firstName.trim().isBlank() || normalizedEmail.isBlank()) {
            setError("Nom, prénom et email requis")
            return
        }
        if (password.isBlank()) {
            setError("Mot de passe requis")
            return
        }
        if (password != passwordConfirmation) {
            setError("Confirmation différente")
            return
        }
        val gateway = cloudAuthGateway ?: return setError("Backend cloud non configuré")
        launchTask {
            when (
                val result = gateway.createAccount(
                    email = normalizedEmail,
                    password = password,
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                )
            ) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    cloudSession = result.value,
                    feedbackMessage = "Compte créé",
                    isError = false,
                )
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun signOut() {
        val gateway = cloudAuthGateway ?: return setError("Backend cloud non configuré")
        launchTask {
            when (val result = gateway.signOut()) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    cloudSession = null,
                    feedbackMessage = "Déconnecté",
                    isError = false,
                )
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmation: String,
    ) {
        if (mutableUiState.value.cloudSession == null) {
            setError("Connexion cloud requise")
            return
        }
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            setError("Mot de passe requis")
            return
        }
        if (newPassword != confirmation) {
            setError("Confirmation différente")
            return
        }
        val gateway = cloudAuthGateway ?: return setError("Backend cloud non configuré")
        launchTask {
            when (val result = gateway.changePassword(currentPassword, newPassword)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    feedbackMessage = "Mot de passe mis à jour",
                    isError = false,
                )
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    fun syncCloud() {
        if (mutableUiState.value.cloudSession == null) {
            setError("Connexion cloud requise")
            return
        }
        val gateway = cloudSyncGateway ?: return setError("Backend cloud non configuré")
        if (mutableUiState.value.isSyncing) return
        launchTask {
            mutableUiState.value = mutableUiState.value.copy(isSyncing = true)
            try {
                val pushResult = gateway.pushPendingChanges()
                val pullResult = gateway.recover()
                when {
                    pushResult is AppResult.Failure -> setError(pushResult.error)
                    pullResult is AppResult.Failure -> setError(pullResult.error)
                    else -> {
                        val pushed = (pushResult as AppResult.Success).value.pushedItems
                        val pulled = (pullResult as AppResult.Success).value.pulledItems
                        loadPreferences()
                        mutableUiState.value = mutableUiState.value.copy(
                            feedbackMessage = "Sync terminé: $pushed envoyés, $pulled reçus",
                            isError = false,
                        )
                        mutableAppRefreshEvents.tryEmit(Unit)
                    }
                }
            } finally {
                mutableUiState.value = mutableUiState.value.copy(isSyncing = false)
            }
        }
    }

    fun purgeCloud() {
        if (mutableUiState.value.cloudSession == null) {
            setError("Connexion cloud requise")
            return
        }
        val gateway = cloudSyncGateway ?: return setError("Backend cloud non configuré")
        launchTask {
            mutableUiState.value = mutableUiState.value.copy(isSyncing = true)
            when (val result = gateway.purgeRemoteOnly()) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    isSyncing = false,
                    feedbackMessage = "Cloud vidé: ${result.value.deletedRemoteItems} suppressions",
                    isError = false,
                ).also { mutableAppRefreshEvents.tryEmit(Unit) }
                is AppResult.Failure -> {
                    mutableUiState.value = mutableUiState.value.copy(isSyncing = false)
                    setError(result.error)
                }
            }
        }
    }

    fun recoverCloud() {
        syncCloud()
    }

    fun refreshVisibleData() {
        launchTask {
            loadPreferences()
            refreshSession()
        }
    }

    fun consumeFeedback() {
        mutableUiState.value = mutableUiState.value.copy(feedbackMessage = null, isError = false)
    }

    private suspend fun loadPreferences() {
        mutableUiState.value = mutableUiState.value.copy(isLoading = true)
        when (val result = settingsRepository.loadPreferences()) {
            is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                preferences = result.value,
                isLoading = false,
            )
            is AppResult.Failure -> setError(result.error)
        }
    }

    private suspend fun refreshSession() {
        val gateway = cloudAuthGateway ?: return
        when (val result = gateway.currentSession()) {
            is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                cloudSession = result.value,
                isLoading = false,
            )
            is AppResult.Failure -> setError(result.error)
        }
    }

    private fun savePreferences(preferences: UserPreferences) {
        launchTask {
            when (val result = settingsRepository.savePreferences(preferences)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    preferences = preferences,
                    feedbackMessage = "Préférences sauvegardées",
                    isError = false,
                    isLoading = false,
                )
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    private suspend fun removeSerializedCubeFromSnapshot() {
        val repository = cubeSessionRepository ?: return
        val snapshot = repository.loadSession().getOrNull() ?: return
        repository.saveSession(snapshot.copy(serializedCubeState = null))
    }

    private fun setError(message: String) {
        mutableUiState.value = mutableUiState.value.copy(
            feedbackMessage = message,
            isError = true,
            isLoading = false,
        )
    }

    private fun setError(error: AppError) {
        setError(error.message)
    }

    private fun launchTask(block: suspend () -> Unit) {
        val launcher = taskLauncher
        if (launcher != null) {
            launcher(block)
        } else {
            viewModelScope.launch { block() }
        }
    }

    private companion object {
        const val REFRESH_EVENT_BUFFER = 8
    }
}
