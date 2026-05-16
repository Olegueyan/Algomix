package fr.olegueyan.algomix.domain.cube

enum class MoveAxis {
    X,
    Y,
    Z,
}

enum class MoveKind {
    FACE,
    WIDE,
    SLICE,
    ROTATION,
}

enum class MoveTurn(val quarterTurns: Int, val suffix: String) {
    CLOCKWISE(1, ""),
    COUNTER_CLOCKWISE(-1, "'"),
    HALF_TURN(2, "2"),
}

data class Move(
    val axis: MoveAxis,
    val kind: MoveKind,
    val layers: Set<Int>,
    val turn: MoveTurn,
    val normalizedBase: String,
    val baseDirection: Int,
) {
    val normalizedNotation: String = normalizedBase + turn.suffix

    val effectiveQuarterTurns: Int = baseDirection * turn.quarterTurns
}

data class MoveSequence(
    val moves: List<Move>,
) {
    val isEmpty: Boolean = moves.isEmpty()

    val normalizedNotation: String = moves.joinToString(separator = " ") { it.normalizedNotation }

    fun append(move: Move): MoveSequence = copy(moves = moves + move)

    fun removeLast(): MoveSequence = copy(moves = moves.dropLast(1))

    companion object {
        val EMPTY = MoveSequence(emptyList())
    }
}
