package fr.olegueyan.algomix.ui.components.common

import androidx.annotation.DrawableRes
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.domain.cube.Move
import fr.olegueyan.algomix.domain.cube.MoveKind
import fr.olegueyan.algomix.domain.cube.MoveTurn

object MoveIconRegistry {

    @DrawableRes
    fun iconFor(move: Move): Int {
        val base = move.normalizedBase
        return when (move.kind) {
            MoveKind.FACE -> faceIcon(base, move.turn)
            MoveKind.WIDE -> wideIcon(base)
            MoveKind.SLICE -> sliceIcon(base)
            MoveKind.ROTATION -> rotationIcon(base)
        }
    }

    @DrawableRes
    fun iconForNotation(notation: String): Int {
        val suffix = notation.takeLast(1).takeIf { it == "'" || it == "2" }.orEmpty()
        val base = if (suffix.isEmpty()) notation else notation.dropLast(1)
        val turn = when (suffix) {
            "'" -> MoveTurn.COUNTER_CLOCKWISE
            "2" -> MoveTurn.HALF_TURN
            else -> MoveTurn.CLOCKWISE
        }
        return when {
            base in faceBases -> faceIcon(base, turn)
            base in wideBases -> wideIcon(base)
            base in sliceBases -> sliceIcon(base)
            base in rotationBases -> rotationIcon(base)
            else -> R.drawable.notation_rot_none
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun faceIcon(base: String, turn: MoveTurn): Int = when (base) {
        "R" -> when (turn) {
            MoveTurn.CLOCKWISE -> R.drawable.notation_r
            MoveTurn.COUNTER_CLOCKWISE -> R.drawable.notation_r_prime
            MoveTurn.HALF_TURN -> R.drawable.notation_r2
        }
        "L" -> when (turn) {
            MoveTurn.CLOCKWISE -> R.drawable.notation_l
            MoveTurn.COUNTER_CLOCKWISE -> R.drawable.notation_l_prime
            MoveTurn.HALF_TURN -> R.drawable.notation_l2
        }
        "U" -> when (turn) {
            MoveTurn.CLOCKWISE -> R.drawable.notation_u
            MoveTurn.COUNTER_CLOCKWISE -> R.drawable.notation_u_prime
            MoveTurn.HALF_TURN -> R.drawable.notation_u2
        }
        "D" -> when (turn) {
            MoveTurn.CLOCKWISE -> R.drawable.notation_d
            MoveTurn.COUNTER_CLOCKWISE -> R.drawable.notation_d_prime
            MoveTurn.HALF_TURN -> R.drawable.notation_d2
        }
        "F" -> when (turn) {
            MoveTurn.CLOCKWISE -> R.drawable.notation_f
            MoveTurn.COUNTER_CLOCKWISE -> R.drawable.notation_f_prime
            MoveTurn.HALF_TURN -> R.drawable.notation_f2
        }
        "B" -> when (turn) {
            MoveTurn.CLOCKWISE -> R.drawable.notation_b
            MoveTurn.COUNTER_CLOCKWISE -> R.drawable.notation_b_prime
            MoveTurn.HALF_TURN -> R.drawable.notation_b2
        }
        else -> R.drawable.notation_rot_none
    }

    private fun wideIcon(base: String): Int = when (base) {
        "Rw" -> R.drawable.notation_wide_rw
        "Lw" -> R.drawable.notation_wide_lw
        "Uw" -> R.drawable.notation_wide_uw
        "Dw" -> R.drawable.notation_wide_dw
        "Fw" -> R.drawable.notation_wide_fw
        "Bw" -> R.drawable.notation_wide_bw
        else -> R.drawable.notation_rot_none
    }

    private fun sliceIcon(base: String): Int = when (base) {
        "M" -> R.drawable.notation_slice_m
        "E" -> R.drawable.notation_slice_e
        "S" -> R.drawable.notation_slice_s
        else -> R.drawable.notation_rot_none
    }

    private fun rotationIcon(base: String): Int = when (base) {
        "x" -> R.drawable.notation_rot_x
        "y" -> R.drawable.notation_rot_y
        "z" -> R.drawable.notation_rot_z
        else -> R.drawable.notation_rot_none
    }

    private val faceBases = setOf("R", "L", "U", "D", "F", "B")
    private val wideBases = setOf("Rw", "Lw", "Uw", "Dw", "Fw", "Bw")
    private val sliceBases = setOf("M", "E", "S")
    private val rotationBases = setOf("x", "y", "z")
}
