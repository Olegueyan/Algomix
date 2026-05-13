package fr.olegueyan.algomix.domain.session

import java.time.Instant

data class LocalSessionSnapshot(
    val serializedCubeState: String?,
    val activeRoute: String,
    val activeHomeMode: String,
    val activeSequence: String?,
    val playbackIndex: Int,
    val updatedAt: Instant,
)
