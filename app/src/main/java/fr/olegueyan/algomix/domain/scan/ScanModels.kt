package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.FaceColor
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
) {
    val completedFaceCount: Int
        get() = faces.count { it.isComplete }
}
