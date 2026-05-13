package fr.olegueyan.algomix.domain.cloud

import java.time.Instant

data class CloudUser(
    val id: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
)

data class CloudSession(
    val user: CloudUser,
    val authenticatedAt: Instant,
    val expiresAt: Instant? = null,
)

data class SyncSummary(
    val pulledItems: Int = 0,
    val pushedItems: Int = 0,
    val deletedRemoteItems: Int = 0,
    val conflictCount: Int = 0,
    val completedAt: Instant? = null,
)
