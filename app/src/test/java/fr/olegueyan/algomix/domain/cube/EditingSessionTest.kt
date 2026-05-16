package fr.olegueyan.algomix.domain.cube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditingSessionTest {
    @Test
    fun addMoveAppendsToSequence() {
        val session = EditingSession()
            .addMove(MoveParser.parseMove("R"))
            .addMove(MoveParser.parseMove("U"))

        assertEquals("R U", session.sequence.normalizedNotation)
    }

    @Test
    fun undoAndRedoRestoreSequenceChanges() {
        val session = EditingSession()
            .addMove(MoveParser.parseMove("R"))
            .addMove(MoveParser.parseMove("U"))

        val undone = session.undo()
        val redone = undone.redo()

        assertEquals("R", undone.sequence.normalizedNotation)
        assertEquals("R U", redone.sequence.normalizedNotation)
    }

    @Test
    fun suppressLastMoveRemovesLastMoveAndSupportsUndo() {
        val session = EditingSession()
            .addMove(MoveParser.parseMove("R"))
            .addMove(MoveParser.parseMove("U"))

        val suppressed = session.suppressLastMove()

        assertEquals("R", suppressed.sequence.normalizedNotation)
        assertEquals("R U", suppressed.undo().sequence.normalizedNotation)
    }

    @Test
    fun deleteAllClearsSequenceAndSupportsUndo() {
        val session = EditingSession()
            .addMove(MoveParser.parseMove("R"))
            .addMove(MoveParser.parseMove("U"))

        val cleared = session.deleteAll()

        assertTrue(cleared.sequence.isEmpty)
        assertEquals("R U", cleared.undo().sequence.normalizedNotation)
    }
}
