package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.cube.FaceletFace
import java.time.Instant

data class ScanFaceDraft(
    val faceIndex: Int,
    val stickers: List<FaceColor?> = emptyList(),
    val capturedAt: Instant? = null,
) {
    val isComplete: Boolean
        get() = stickers.size == STICKER_COUNT && stickers.all { it != null }

    companion object {
        const val STICKER_COUNT = 9
    }
}

data class ScanSessionDraft(
    val faces: List<ScanFaceDraft> = emptyList(),
    val validationErrors: List<String> = emptyList(),
) {
    val completedFaceCount: Int
        get() = faces.count { it.isComplete }

    val currentFaceIndex: Int
        get() {
            val capturedIndexes = faces.filter { it.isComplete }.map { it.faceIndex }.toSet()
            return FaceletFace.entries.indices.firstOrNull { index -> index !in capturedIndexes }
                ?: FaceletFace.entries.lastIndex
        }

    val currentFace: FaceletFace
        get() = FaceletFace.entries[currentFaceIndex]

    val isComplete: Boolean
        get() = completedFaceCount == FaceletFace.entries.size

    fun face(faceIndex: Int): ScanFaceDraft? =
        faces.firstOrNull { it.faceIndex == faceIndex }
}
