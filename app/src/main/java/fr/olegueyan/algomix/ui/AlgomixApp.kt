package fr.olegueyan.algomix.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.olegueyan.algomix.ui.home.HomeScreen
import fr.olegueyan.algomix.ui.library.LibraryScreen
import fr.olegueyan.algomix.ui.scan.ScanRoute
import fr.olegueyan.algomix.ui.scan.ScanScreen
import fr.olegueyan.algomix.ui.settings.SettingsScreen
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.timer.TimerScreen

@Composable
fun AlgomixApp() {
    val container = LocalAppContainer.current
    val sharedCubeViewModel = rememberSharedCubeViewModel(container)
    val uiState by sharedCubeViewModel.uiState.collectAsStateWithLifecycle()
    val scanRoute = ScanRoute.rememberState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AlgomixBottomBar(
                    selected = uiState.activeRoute,
                    onSelect = sharedCubeViewModel::setRoute,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (uiState.activeRoute) {
                    MainRoute.HOME -> HomeScreen(
                        viewModel = sharedCubeViewModel,
                        onRequestScan = { scanRoute.show() },
                    )
                    MainRoute.LIBRARY -> LibraryScreen()
                    MainRoute.TIMER -> TimerScreen()
                    MainRoute.SETTINGS -> SettingsScreen()
                }
            }
        }
    }

    if (scanRoute.isVisible) {
        ScanScreen(
            sharedCubeViewModel = sharedCubeViewModel,
            onDismiss = { scanRoute.hide() },
        )
    }
}
