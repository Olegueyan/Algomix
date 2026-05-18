package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.port.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaceholderUiState(
    val titleResId: Int,
    val bodyResId: Int,
)

class HomeViewModel : ViewModel()

class SettingsViewModel(
    @Suppress("unused") private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        PlaceholderUiState(R.string.placeholder_settings_title, R.string.placeholder_settings_body),
    )
    val uiState: StateFlow<PlaceholderUiState> = mutableUiState.asStateFlow()
}
