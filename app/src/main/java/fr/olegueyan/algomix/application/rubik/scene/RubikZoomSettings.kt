package fr.olegueyan.algomix.application.rubik.scene

internal data class RubikZoomSettings(
    val minZoomFactor: Float = RubikSceneConfiguration.MIN_ZOOM_FACTOR,
    val initialZoomFactor: Float = RubikSceneConfiguration.INITIAL_ZOOM_FACTOR,
    val maxZoomFactor: Float = RubikSceneConfiguration.MAX_ZOOM_FACTOR,
) {
    fun normalized(): RubikZoomSettings {
        val minFactor = minZoomFactor.sanitizedOr(RubikSceneConfiguration.MIN_ZOOM_FACTOR)
            .coerceAtLeast(MIN_ZOOM_FACTOR)
        val maxFactor = maxZoomFactor.sanitizedOr(RubikSceneConfiguration.MAX_ZOOM_FACTOR)
            .coerceAtLeast(minFactor)
        val initialFactor = initialZoomFactor.sanitizedOr(RubikSceneConfiguration.INITIAL_ZOOM_FACTOR)
            .coerceIn(minFactor, maxFactor)

        return RubikZoomSettings(
            minZoomFactor = minFactor,
            initialZoomFactor = initialFactor,
            maxZoomFactor = maxFactor,
        )
    }

    private fun Float.sanitizedOr(defaultValue: Float): Float =
        if (isFinite()) this else defaultValue

    private companion object {
        const val MIN_ZOOM_FACTOR = 0.1f
    }
}
