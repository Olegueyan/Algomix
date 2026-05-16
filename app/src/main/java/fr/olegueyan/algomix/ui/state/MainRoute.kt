package fr.olegueyan.algomix.ui.state

enum class MainRoute {
    HOME,
    LIBRARY,
    TIMER,
    SETTINGS,
    ;

    companion object {
        fun fromStoredName(value: String): MainRoute? =
            entries.firstOrNull { route -> route.name == value }
    }
}
