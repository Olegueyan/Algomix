package fr.olegueyan.algomix.domain.cube

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CubeValidatorTest {
    @Test
    fun solvedCubeIsValidAndSolved() {
        val solved = CubeState.solved()

        val validation = CubeValidator.validate(solved.faceletCube)

        assertTrue(validation.isValid)
        assertTrue(CubeValidator.isSolved(solved))
    }

    @Test
    fun invalidStickerCountIsRejected() {
        val stickers = CubeState.solved().faceletCube.stickers.toMutableList()
        stickers[0] = FaceColor.RED

        val validation = CubeValidator.validate(FaceletCube(stickers))

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("must appear 9 times") })
    }

    @Test
    fun missingFaceletIsRejected() {
        val stickers = CubeState.solved().faceletCube.stickers.toMutableList()
        stickers[0] = null

        val validation = CubeValidator.validate(FaceletCube(stickers))

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("missing stickers") })
    }

    @Test
    fun invalidStickerListSizeIsRejected() {
        val stickers = CubeState.solved().faceletCube.stickers.dropLast(1)

        val validation = CubeValidator.validate(FaceletCube(stickers))

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("must contain 54 stickers") })
    }
}
