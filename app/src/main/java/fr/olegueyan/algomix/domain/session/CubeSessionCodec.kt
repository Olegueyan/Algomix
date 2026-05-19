package fr.olegueyan.algomix.domain.session

import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.cube.FaceletCube

object CubeSessionCodec {
    fun encode(cubeState: CubeState): String =
        cubeState.faceletCube.stickers.joinToString(separator = SEPARATOR) { sticker ->
            requireNotNull(sticker).name
        }

    fun decode(serializedCubeState: String?): CubeState? {
        if (serializedCubeState.isNullOrBlank()) {
            return null
        }
        return runCatching {
            val stickers = serializedCubeState
                .split(SEPARATOR)
                .takeIf { values -> values.size == FaceletCube.STICKER_COUNT }
                ?.map { value -> FaceColor.valueOf(value) }
                ?: return null
            CubeState.fromFaceletCube(FaceletCube(stickers))
        }.getOrNull()
    }

    private const val SEPARATOR = ","
}
