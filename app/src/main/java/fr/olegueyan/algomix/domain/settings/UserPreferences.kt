package fr.olegueyan.algomix.domain.settings

enum class AppAppearance {
    LIGHT,
    DARK,
}

enum class CubeTheme {
    FILLED,
    STICKER_ON_BLACK,
    CARBON,
}

data class UserPreferences(
    val appAppearance: AppAppearance = AppAppearance.LIGHT,
    val cubeTheme: CubeTheme = CubeTheme.FILLED,
    val localCubeCacheEnabled: Boolean = true,
    val sessionPersistenceEnabled: Boolean = true,
)
