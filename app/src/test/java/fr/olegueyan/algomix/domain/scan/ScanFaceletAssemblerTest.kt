package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.cube.FaceletFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFaceletAssemblerTest {
    @Test
    fun assemblesSolvedFacesIntoSolvedCubeState() {
        val session = solvedScanSession()

        val result = ScanFaceletAssembler.assemble(session)

        assertTrue(result.isValid)
        assertEquals(CubeState.solved(), CubeState.fromFaceletCube(requireNotNull(result.faceletCube)))
    }

    @Test
    fun incompleteScanIsRejected() {
        val session = ScanSessionDraft(faces = solvedScanSession().faces.dropLast(1))

        val result = ScanFaceletAssembler.assemble(session)

        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun invalidColorCountIsRejected() {
        val faces = solvedScanSession().faces.toMutableList()
        faces[0] = faces[0].copy(
            stickers = List(ScanFaceDraft.STICKER_COUNT) { FaceColor.WHITE },
        )

        val result = ScanFaceletAssembler.assemble(ScanSessionDraft(faces))

        assertTrue(result.errors.joinToString().contains("must appear 9 times"))
    }

    private fun solvedScanSession(): ScanSessionDraft =
        ScanSessionDraft(
            faces = FaceletFace.entries.map { face ->
                ScanFaceDraft(
                    faceIndex = face.ordinal,
                    stickers = CubeState.solved().faceletCube.stickers
                        .drop(face.ordinal * ScanFaceDraft.STICKER_COUNT)
                        .take(ScanFaceDraft.STICKER_COUNT),
                )
            },
        )
}
