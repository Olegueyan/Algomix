package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.CubeValidator
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.cube.FaceletCube
import fr.olegueyan.algomix.domain.cube.FaceletFace

object ScanFaceletAssembler {
    fun assemble(session: ScanSessionDraft): ScanFaceletAssembly {
        if (!session.isComplete) {
            return ScanFaceletAssembly.failure("Scan incomplet: 6 faces completes requises")
        }
        val faceByIndex = session.faces.associateBy { face -> face.faceIndex }
        val stickers = mutableListOf<FaceColor?>()
        FaceletFace.entries.indices.forEach { faceIndex ->
            val face = faceByIndex[faceIndex]
                ?: return ScanFaceletAssembly.failure("Face $faceIndex manquante")
            if (!face.isComplete) {
                return ScanFaceletAssembly.failure("Face $faceIndex incomplete")
            }
            stickers += face.stickers
        }
        val cube = FaceletCube(stickers)
        val validation = CubeValidator.validate(cube)
        return if (validation.isValid) {
            ScanFaceletAssembly(faceletCube = cube)
        } else {
            ScanFaceletAssembly(errors = validation.errors)
        }
    }
}

data class ScanFaceletAssembly(
    val faceletCube: FaceletCube? = null,
    val errors: List<String> = emptyList(),
) {
    val isValid: Boolean
        get() = faceletCube != null && errors.isEmpty()

    val errorMessage: String
        get() = errors.joinToString("; ")

    companion object {
        fun failure(message: String): ScanFaceletAssembly =
            ScanFaceletAssembly(errors = listOf(message))
    }
}
