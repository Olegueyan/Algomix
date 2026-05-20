package fr.olegueyan.algomix.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import fr.olegueyan.algomix.application.di.AppContainer
import fr.olegueyan.algomix.infrastructure.di.AndroidAppContainerFactory
import fr.olegueyan.algomix.ui.home.AppContainerOwner
import fr.olegueyan.algomix.ui.theme.AlgomixTheme

class AlgomixActivity : ComponentActivity(), AppContainerOwner {

    override val appContainer: AppContainer by lazy {
        AndroidAppContainerFactory.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppContainer provides appContainer) {
                AlgomixTheme {
                    AlgomixApp()
                }
            }
        }
    }
}
