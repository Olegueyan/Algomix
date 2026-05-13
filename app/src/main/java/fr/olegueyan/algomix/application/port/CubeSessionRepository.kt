package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.session.LocalSessionSnapshot

interface CubeSessionRepository {
    suspend fun loadSession(): AppResult<LocalSessionSnapshot?>

    suspend fun saveSession(snapshot: LocalSessionSnapshot): AppResult<Unit>

    suspend fun clearSession(): AppResult<Unit>
}
