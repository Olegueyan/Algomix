package fr.olegueyan.algomix.ui.components.rubik

import androidx.annotation.ColorInt

/**
 * Minimal public styling surface for [RubikCubeView].
 *
 * The cube geometry, camera fit and interaction tuning stay internal so the view remains easy to
 * embed without exposing low-level rendering details.
 */
data class RubikCubeAppearance(
    @param:ColorInt @field:ColorInt val backgroundColor: Int = DEFAULT_BACKGROUND_COLOR,
    @param:ColorInt @field:ColorInt val bodyColor: Int = DEFAULT_BODY_COLOR,
) {
    /** Exposes the default appearance values shared by XML and code paths. */
    companion object {
        const val DEFAULT_BACKGROUND_COLOR = 0xFFF4F1EA.toInt()
        const val DEFAULT_BODY_COLOR = 0xFF000000.toInt()
    }
}
