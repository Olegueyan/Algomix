package fr.olegueyan.algomix.domain.cube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveParserTest {
    @Test
    fun parsesFaceTurnsAndSuffixes() {
        val sequence = MoveParser.parse("R U R' U'")

        assertEquals("R U R' U'", sequence.normalizedNotation)
        assertFalse(sequence.isEmpty)
        assertEquals(
            listOf(MoveTurn.CLOCKWISE, MoveTurn.CLOCKWISE, MoveTurn.COUNTER_CLOCKWISE),
            sequence.moves.take(3).map { it.turn },
        )
    }

    @Test
    fun parsesHalfTurn() {
        val move = MoveParser.parseMove("R2")

        assertEquals(MoveTurn.HALF_TURN, move.turn)
        assertEquals("R2", move.normalizedNotation)
    }

    @Test
    fun normalizesShortWideMoveAliases() {
        val sequence = MoveParser.parse("r u' f2 l d b")

        assertEquals("Rw Uw' Fw2 Lw Dw Bw", sequence.normalizedNotation)
        assertTrue(sequence.moves.all { it.kind == MoveKind.WIDE })
    }

    @Test
    fun parsesLongWideMoves() {
        val sequence = MoveParser.parse("Rw Uw Fw Lw Dw Bw")

        assertEquals("Rw Uw Fw Lw Dw Bw", sequence.normalizedNotation)
        assertTrue(sequence.moves.all { it.kind == MoveKind.WIDE })
    }

    @Test
    fun parsesSliceMoves() {
        val sequence = MoveParser.parse("M E S")

        assertEquals("M E S", sequence.normalizedNotation)
        assertTrue(sequence.moves.all { it.kind == MoveKind.SLICE })
    }

    @Test
    fun parsesCubeRotations() {
        val sequence = MoveParser.parse("x y z")

        assertEquals("x y z", sequence.normalizedNotation)
        assertTrue(sequence.moves.all { it.kind == MoveKind.ROTATION })
    }

    @Test
    fun emptyNotationReturnsEmptySequence() {
        assertTrue(MoveParser.parse("   ").isEmpty)
    }

    @Test(expected = MoveParseException::class)
    fun invalidTokenIsRejected() {
        MoveParser.parse("R Q")
    }

    @Test(expected = MoveParseException::class)
    fun invalidSuffixIsRejected() {
        MoveParser.parseMove("R3")
    }
}
