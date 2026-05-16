package fr.olegueyan.algomix.domain.cube

data class EditingSession(
    val sequence: MoveSequence = MoveSequence.EMPTY,
    private val undoStack: List<MoveSequence> = emptyList(),
    private val redoStack: List<MoveSequence> = emptyList(),
) {
    fun addMove(move: Move): EditingSession =
        copy(
            sequence = sequence.append(move),
            undoStack = undoStack + sequence,
            redoStack = emptyList(),
        )

    fun suppressLastMove(): EditingSession {
        if (sequence.isEmpty) {
            return this
        }
        return copy(
            sequence = sequence.removeLast(),
            undoStack = undoStack + sequence,
            redoStack = emptyList(),
        )
    }

    fun deleteAll(): EditingSession =
        copy(
            sequence = MoveSequence.EMPTY,
            undoStack = undoStack + sequence,
            redoStack = emptyList(),
        )

    fun undo(): EditingSession {
        val previous = undoStack.lastOrNull() ?: return this
        return copy(
            sequence = previous,
            undoStack = undoStack.dropLast(1),
            redoStack = redoStack + sequence,
        )
    }

    fun redo(): EditingSession {
        val next = redoStack.lastOrNull() ?: return this
        return copy(
            sequence = next,
            undoStack = undoStack + sequence,
            redoStack = redoStack.dropLast(1),
        )
    }
}
