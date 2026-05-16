package fr.olegueyan.algomix.ui.home

import fr.olegueyan.algomix.application.di.AppContainer

interface AppContainerOwner {
    val appContainer: AppContainer
}
