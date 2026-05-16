package fr.olegueyan.algomix.domain.cube

object MoveParser {
    private val definitions = mapOf(
        "U" to MoveDefinition(MoveAxis.Y, MoveKind.FACE, setOf(1), "U", 1),
        "D" to MoveDefinition(MoveAxis.Y, MoveKind.FACE, setOf(-1), "D", -1),
        "R" to MoveDefinition(MoveAxis.X, MoveKind.FACE, setOf(1), "R", 1),
        "L" to MoveDefinition(MoveAxis.X, MoveKind.FACE, setOf(-1), "L", -1),
        "F" to MoveDefinition(MoveAxis.Z, MoveKind.FACE, setOf(1), "F", 1),
        "B" to MoveDefinition(MoveAxis.Z, MoveKind.FACE, setOf(-1), "B", -1),
        "Rw" to MoveDefinition(MoveAxis.X, MoveKind.WIDE, setOf(0, 1), "Rw", 1),
        "Lw" to MoveDefinition(MoveAxis.X, MoveKind.WIDE, setOf(-1, 0), "Lw", -1),
        "Uw" to MoveDefinition(MoveAxis.Y, MoveKind.WIDE, setOf(0, 1), "Uw", 1),
        "Dw" to MoveDefinition(MoveAxis.Y, MoveKind.WIDE, setOf(-1, 0), "Dw", -1),
        "Fw" to MoveDefinition(MoveAxis.Z, MoveKind.WIDE, setOf(0, 1), "Fw", 1),
        "Bw" to MoveDefinition(MoveAxis.Z, MoveKind.WIDE, setOf(-1, 0), "Bw", -1),
        "M" to MoveDefinition(MoveAxis.X, MoveKind.SLICE, setOf(0), "M", -1),
        "E" to MoveDefinition(MoveAxis.Y, MoveKind.SLICE, setOf(0), "E", -1),
        "S" to MoveDefinition(MoveAxis.Z, MoveKind.SLICE, setOf(0), "S", 1),
        "x" to MoveDefinition(MoveAxis.X, MoveKind.ROTATION, setOf(-1, 0, 1), "x", 1),
        "y" to MoveDefinition(MoveAxis.Y, MoveKind.ROTATION, setOf(-1, 0, 1), "y", 1),
        "z" to MoveDefinition(MoveAxis.Z, MoveKind.ROTATION, setOf(-1, 0, 1), "z", 1),
    )

    private val aliases = mapOf(
        "r" to "Rw",
        "l" to "Lw",
        "u" to "Uw",
        "d" to "Dw",
        "f" to "Fw",
        "b" to "Bw",
    )

    fun parse(notation: String): MoveSequence {
        val trimmed = notation.trim()
        if (trimmed.isEmpty()) {
            return MoveSequence.EMPTY
        }
        return MoveSequence(trimmed.split(Regex("\\s+")).map(::parseMove))
    }

    fun parseMove(token: String): Move {
        val suffix = token.takeLast(1).takeIf { it == "'" || it == "2" }.orEmpty()
        val rawBase = if (suffix.isEmpty()) token else token.dropLast(1)
        val normalizedBase = aliases[rawBase] ?: rawBase
        val definition = definitions[normalizedBase] ?: throw MoveParseException(token)
        return Move(
            axis = definition.axis,
            kind = definition.kind,
            layers = definition.layers,
            turn = suffix.toTurn(),
            normalizedBase = definition.normalizedBase,
            baseDirection = definition.baseDirection,
        )
    }

    private fun String.toTurn(): MoveTurn =
        when (this) {
            "" -> MoveTurn.CLOCKWISE
            "'" -> MoveTurn.COUNTER_CLOCKWISE
            "2" -> MoveTurn.HALF_TURN
            else -> throw MoveParseException(this)
        }

    private data class MoveDefinition(
        val axis: MoveAxis,
        val kind: MoveKind,
        val layers: Set<Int>,
        val normalizedBase: String,
        val baseDirection: Int,
    )
}

class MoveParseException(token: String) : IllegalArgumentException("Invalid Rubik move token: $token")
