package fr.olegueyan.algomix.domain.cube

import kotlin.random.Random

object ScrambleGenerator {
    private val faceMoves = listOf("U", "D", "R", "L", "F", "B")
    private val suffixes = listOf("", "'", "2")

    fun generate(length: Int, random: Random = Random.Default): MoveSequence {
        require(length >= 0) { "Scramble length must be positive or zero" }
        val moves = mutableListOf<Move>()
        var previousAxis: MoveAxis? = null
        repeat(length) {
            val baseMove = faceMoves
                .map { MoveParser.parseMove(it) }
                .filterNot { it.axis == previousAxis }
                .random(random)
            val suffix = suffixes.random(random)
            val nextMove = MoveParser.parseMove(baseMove.normalizedBase + suffix)
            moves += nextMove
            previousAxis = nextMove.axis
        }
        return MoveSequence(moves)
    }
}
