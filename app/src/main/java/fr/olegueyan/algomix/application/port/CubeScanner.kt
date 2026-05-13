package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.scan.ScanFaceDraft
import fr.olegueyan.algomix.domain.scan.ScanSessionDraft

interface CubeScanner {
    suspend fun startSession(): AppResult<ScanSessionDraft>

    suspend fun saveFace(session: ScanSessionDraft, face: ScanFaceDraft): AppResult<ScanSessionDraft>

    suspend fun validateSession(session: ScanSessionDraft): AppResult<Unit>
}
