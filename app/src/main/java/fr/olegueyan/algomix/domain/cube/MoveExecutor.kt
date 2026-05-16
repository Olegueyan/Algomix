package fr.olegueyan.algomix.domain.cube

object MoveExecutor {
    fun apply(state: CubeState, sequence: MoveSequence): CubeState =
        sequence.moves.fold(state) { current, move -> apply(current, move) }

    fun apply(state: CubeState, move: Move): CubeState {
        val turns = move.effectiveQuarterTurns.floorMod(4)
        return (0 until turns).fold(state) { current, _ -> rotateQuarterTurn(current, move) }
    }

    private fun rotateQuarterTurn(state: CubeState, move: Move): CubeState =
        CubeState.fromCubelets(
            state.cubelets.map { cubelet ->
                if (!move.affects(cubelet.position)) {
                    cubelet
                } else {
                    cubelet.rotate(move.axis)
                }
            },
        )

    private fun Move.affects(position: CubeVector): Boolean =
        layers.contains(
            when (axis) {
                MoveAxis.X -> position.x
                MoveAxis.Y -> position.y
                MoveAxis.Z -> position.z
            },
        )

    private fun Cubelet.rotate(axis: MoveAxis): Cubelet =
        copy(
            position = position.rotate(axis),
            stickers = stickers.mapKeys { entry -> entry.key.rotate(axis) },
        )

    private fun CubeVector.rotate(axis: MoveAxis): CubeVector =
        when (axis) {
            MoveAxis.X -> CubeVector(x, -z, y)
            MoveAxis.Y -> CubeVector(z, y, -x)
            MoveAxis.Z -> CubeVector(-y, x, z)
        }

    private fun Int.floorMod(modulus: Int): Int =
        ((this % modulus) + modulus) % modulus
}
