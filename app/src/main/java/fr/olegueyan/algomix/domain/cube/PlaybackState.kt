package fr.olegueyan.algomix.domain.cube

data class PlaybackState(
    val sequence: MoveSequence,
    val currentIndex: Int = 0,
    val loop: Boolean = false,
    val speedMultiplier: Float = DEFAULT_SPEED_MULTIPLIER,
) {
    fun next(): PlaybackState {
        val nextIndex = currentIndex + 1
        return when {
            nextIndex <= sequence.moves.size -> copy(currentIndex = nextIndex)
            loop -> copy(currentIndex = 0)
            else -> this
        }
    }

    fun previous(): PlaybackState =
        copy(currentIndex = (currentIndex - 1).coerceAtLeast(0))

    fun reset(): PlaybackState = copy(currentIndex = 0)

    companion object {
        const val DEFAULT_SPEED_MULTIPLIER = 1f
    }
}
