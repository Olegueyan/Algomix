package fr.olegueyan.algomix.domain.cube

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackStateTest {
    @Test
    fun nextAdvancesUntilEndWithoutLooping() {
        val state = PlaybackState(sequence = MoveParser.parse("R U"))

        val atEnd = state.next().next()
        val stillAtEnd = atEnd.next()

        assertEquals(2, atEnd.currentIndex)
        assertEquals(2, stillAtEnd.currentIndex)
    }

    @Test
    fun nextLoopsToStartWhenLoopIsEnabled() {
        val state = PlaybackState(
            sequence = MoveParser.parse("R U"),
            currentIndex = 2,
            loop = true,
        )

        assertEquals(0, state.next().currentIndex)
    }

    @Test
    fun previousDoesNotGoBelowStart() {
        val state = PlaybackState(sequence = MoveParser.parse("R U"))

        assertEquals(0, state.previous().currentIndex)
    }

    @Test
    fun previousMovesBackOneStep() {
        val state = PlaybackState(sequence = MoveParser.parse("R U"), currentIndex = 2)

        assertEquals(1, state.previous().currentIndex)
    }

    @Test
    fun resetReturnsToStart() {
        val state = PlaybackState(sequence = MoveParser.parse("R U"), currentIndex = 2)

        assertEquals(0, state.reset().currentIndex)
    }
}
