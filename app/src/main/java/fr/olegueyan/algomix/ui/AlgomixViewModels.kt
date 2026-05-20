package fr.olegueyan.algomix.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.olegueyan.algomix.application.di.AppContainer
import fr.olegueyan.algomix.ui.home.SingleViewModelFactory
import fr.olegueyan.algomix.ui.viewmodel.LibraryViewModel
import fr.olegueyan.algomix.ui.viewmodel.SettingsViewModel
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import fr.olegueyan.algomix.ui.viewmodel.TimerViewModel

@Composable
fun rememberSharedCubeViewModel(container: AppContainer): SharedCubeViewModel {
    val repository = container.cubeSessionRepository().getOrNull()
        ?: error("CubeSessionRepository is not configured")
    return viewModel(factory = SharedCubeViewModel.Factory(repository, container.clockProvider))
}

@Composable
fun rememberLibraryViewModel(container: AppContainer): LibraryViewModel {
    val repository = container.libraryRepository().getOrNull()
        ?: error("LibraryRepository is not configured")
    return viewModel(factory = SingleViewModelFactory { LibraryViewModel(repository) })
}

@Composable
fun rememberTimerViewModel(container: AppContainer): TimerViewModel {
    val repository = container.timerRepository().getOrNull()
        ?: error("TimerRepository is not configured")
    return viewModel(
        factory = SingleViewModelFactory {
            TimerViewModel(
                timerRepository = repository,
                clockProvider = container.clockProvider,
            )
        },
    )
}

@Composable
fun rememberSettingsViewModel(container: AppContainer): SettingsViewModel {
    val settingsRepository = container.settingsRepository().getOrNull()
        ?: error("SettingsRepository is not configured")
    return viewModel(
        factory = SingleViewModelFactory {
            SettingsViewModel(
                settingsRepository = settingsRepository,
                cloudAuthGateway = container.cloudAuthGateway().getOrNull(),
                cloudSyncGateway = container.cloudSyncGateway().getOrNull(),
            )
        },
    )
}
