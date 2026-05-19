package fr.olegueyan.algomix.domain.scan

import fr.olegueyan.algomix.domain.cube.FaceColor
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanColorClassifierTest {
    @Test
    fun classifiesReferenceRgbToEachFaceColor() {
        val references = mapOf(
            FaceColor.WHITE to RgbColor(255, 255, 255),
            FaceColor.YELLOW to RgbColor(255, 214, 0),
            FaceColor.RED to RgbColor(196, 31, 59),
            FaceColor.ORANGE to RgbColor(255, 120, 0),
            FaceColor.BLUE to RgbColor(0, 81, 186),
            FaceColor.GREEN to RgbColor(0, 158, 97),
        )

        references.forEach { (expectedColor, rgb) ->
            assertEquals(expectedColor, ScanColorClassifier.classify(rgb))
        }
    }
}
