package fr.olegueyan.algomix.infrastructure.persistence.local

import android.database.SQLException
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import java.io.IOException

internal const val OUTBOX_OPERATION_DELETE = "DELETE"
internal const val OUTBOX_OPERATION_TAGS = "TAGS"
internal const val OUTBOX_OPERATION_UPSERT = "UPSERT"

internal fun ClockProvider.nowMillis(): Long = now().toEpochMilli()

internal suspend fun <T> storageResult(block: suspend () -> AppResult<T>): AppResult<T> =
    try {
        block()
    } catch (error: SQLException) {
        AppResult.failure(AppError.Storage(cause = error))
    } catch (error: IOException) {
        AppResult.failure(AppError.Storage(cause = error))
    } catch (error: IllegalStateException) {
        AppResult.failure(AppError.Storage(cause = error))
    } catch (error: SecurityException) {
        AppResult.failure(AppError.Storage(cause = error))
    }

internal fun validateName(name: String, label: String): AppError? =
    if (name.isBlank()) {
        AppError.Validation("$label name must not be blank")
    } else {
        null
    }

internal fun notFound(label: String): AppResult<Nothing> =
    AppResult.failure(AppError.NotFound("$label not found"))

internal fun outbox(
    entityType: String,
    entityId: String,
    operation: String,
    createdAt: Long,
): OutboxEntity =
    OutboxEntity(
        entityType = entityType,
        entityId = entityId,
        operation = operation,
        createdAt = createdAt,
    )

internal suspend fun LocalPersistenceDao.markCloudEligible(
    entityType: String,
    entityId: String,
    updatedAt: Long,
    deletedAt: Long? = null,
) {
    upsertSyncMetadata(
        SyncMetadataEntity(
            id = "$entityType:$entityId",
            entityType = entityType,
            entityId = entityId,
            updatedAt = updatedAt,
            deletedAt = deletedAt,
            cloudEligible = true,
        ),
    )
}

internal suspend fun LocalPersistenceDao.enqueueCloudMutation(
    entityType: String,
    entityId: String,
    operation: String,
    updatedAt: Long,
    deletedAt: Long? = null,
) {
    markCloudEligible(entityType, entityId, updatedAt, deletedAt)
    insertOutbox(outbox(entityType, entityId, operation, updatedAt))
}
