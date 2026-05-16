package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.application.port.TimerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaceholderUiState(
    val titleResId: Int,
    val bodyResId: Int,
)

class HomeViewModel : ViewModel()

class LibraryViewModel(
    @Suppress("unused") private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        PlaceholderUiState(R.string.placeholder_library_title, R.string.placeholder_library_body),
    )
    val uiState: StateFlow<PlaceholderUiState> = mutableUiState.asStateFlow()
}

class TimerViewModel(
    @Suppress("unused") private val timerRepository: TimerRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        PlaceholderUiState(R.string.placeholder_timer_title, R.string.placeholder_timer_body),
    )
    val uiState: StateFlow<PlaceholderUiState> = mutableUiState.asStateFlow()
}

class SettingsViewModel(
    @Suppress("unused") private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(
        PlaceholderUiState(R.string.placeholder_settings_title, R.string.placeholder_settings_body),
    )
    val uiState: StateFlow<PlaceholderUiState> = mutableUiState.asStateFlow()
}
