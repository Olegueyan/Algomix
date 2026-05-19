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

    @Test
    fun duplicateCentersAreRejected() {
        val stickers = CubeState.solved().faceletCube.stickers.toMutableList()
        stickers[FaceletCube.FACE_STICKER_COUNT + CENTER_INDEX] = stickers[CENTER_INDEX]

        val validation = CubeValidator.validate(FaceletCube(stickers))

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("six unique colors") })
    }

    @Test
    fun inconsistentEdgeColorSetsAreRejected() {
        val stickers = CubeState.solved().faceletCube.stickers.toMutableList()
        val firstEdgeIndex = 1
        val secondEdgeIndex = FaceletCube.FACE_STICKER_COUNT + 1
        val first = stickers[firstEdgeIndex]
        stickers[firstEdgeIndex] = stickers[secondEdgeIndex]
        stickers[secondEdgeIndex] = first

        val validation = CubeValidator.validate(FaceletCube(stickers))

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("Edge color sets") })
    }

    private companion object {
        const val CENTER_INDEX = 4
    }
}
