package fr.olegueyan.algomix.ui.state

enum class HomeMode {
    VISUALIZATION,
    FREE,
    PLAY,
    EDIT,
    ;

    companion object {
        fun fromStoredName(value: String): HomeMode? =
            entries.firstOrNull { mode -> mode.name == value }
    }
}
