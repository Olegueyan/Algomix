package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.cloud.SyncSummary

interface CloudSyncGateway {
    suspend fun recover(): AppResult<SyncSummary>

    suspend fun pushPendingChanges(): AppResult<SyncSummary>

    suspend fun purgeRemoteOnly(): AppResult<SyncSummary>
}
