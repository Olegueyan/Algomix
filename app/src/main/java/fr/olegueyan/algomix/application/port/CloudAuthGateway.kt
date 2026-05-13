package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.cloud.CloudSession

interface CloudAuthGateway {
    suspend fun currentSession(): AppResult<CloudSession?>

    suspend fun signIn(email: String, password: String): AppResult<CloudSession>

    suspend fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): AppResult<CloudSession>

    suspend fun signOut(): AppResult<Unit>

    suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit>
}
