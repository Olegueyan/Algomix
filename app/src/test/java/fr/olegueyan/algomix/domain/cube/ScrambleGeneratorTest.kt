package fr.olegueyan.algomix.domain.cube

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrambleGeneratorTest {
    @Test
    fun generatesRequestedLength() {
        val scramble = ScrambleGenerator.generate(length = 20, random = Random(seed = 42))

        assertEquals(20, scramble.moves.size)
    }

    @Test
    fun generatedScrambleAvoidsImmediateAxisRepetition() {
        val scramble = ScrambleGenerator.generate(length = 50, random = Random(seed = 7))

        scramble.moves.zipWithNext().forEach { (previous, next) ->
            assertNotEquals(previous.axis, next.axis)
        }
    }

    @Test
    fun generatedScrambleIsParsable() {
        val scramble = ScrambleGenerator.generate(length = 25, random = Random(seed = 11))

        val parsedAgain = MoveParser.parse(scramble.normalizedNotation)

        assertEquals(scramble.normalizedNotation, parsedAgain.normalizedNotation)
    }

    @Test
    fun zeroLengthScrambleIsEmpty() {
        assertTrue(ScrambleGenerator.generate(length = 0, random = Random(seed = 1)).isEmpty)
    }
}
