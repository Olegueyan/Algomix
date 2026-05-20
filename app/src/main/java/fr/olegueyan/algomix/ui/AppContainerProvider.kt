package fr.olegueyan.algomix.ui

import androidx.compose.runtime.compositionLocalOf
import fr.olegueyan.algomix.application.di.AppContainer

val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer is not provided in the current composition.")
}
