@file:Suppress("CyclomaticComplexMethod", "TooGenericExceptionCaught", "TooManyFunctions")

package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CloudSyncGateway
import fr.olegueyan.algomix.domain.cloud.SyncSummary
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmSheetEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.CollectionEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalPersistenceDao
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalSettingsRepository
import fr.olegueyan.algomix.infrastructure.persistence.local.OUTBOX_OPERATION_DELETE
import fr.olegueyan.algomix.infrastructure.persistence.local.OUTBOX_OPERATION_TAGS
import fr.olegueyan.algomix.infrastructure.persistence.local.OUTBOX_OPERATION_UPSERT
import fr.olegueyan.algomix.infrastructure.persistence.local.OutboxEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.ScrambleEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.SyncMetadataEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TimerEntryEntity

class SupabaseCloudSyncGateway(
    private val dao: LocalPersistenceDao,
    private val settingsRepository: LocalSettingsRepository,
    private val remoteDataSource: CloudRemoteDataSource,
    private val clockProvider: ClockProvider = SystemClockProvider,
) : CloudSyncGateway {
    override suspend fun pushPendingChanges(): AppResult<SyncSummary> =
        syncResult {
            var pushedItems = 0
            dao.listOutbox().forEach { item ->
                val metadata = dao.findSyncMetadata(item.entityType, item.entityId)
                if (metadata?.cloudEligible == false) {
                    dao.deleteOutbox(item.id)
                    return@forEach
                }
                pushOutboxItem(item).errorOrNull()?.let { return@syncResult AppResult.failure(it) }
                dao.deleteOutbox(item.id)
                pushedItems += 1
            }
            AppResult.success(SyncSummary(pushedItems = pushedItems, completedAt = clockProvider.now()))
        }

    override suspend fun recover(): AppResult<SyncSummary> =
        syncResult {
            val remote = remoteDataSource.fetchDataset().getOrReport() ?: return@syncResult AppResult.failure(
                AppError.Network(),
            )
            val metadata = dao.listSyncMetadata().associateBy { it.key }
            var pulled = 0
            var conflicts = 0
            remote.collections.forEach { entity ->
                mergeEntity(
                    entity,
                    metadata,
                    dao::findCollectionIncludingDeleted,
                    dao::upsertCollection,
                )?.let { result ->
                    pulled += result.pulled
                    conflicts += result.conflicts
                }
            }
            remote.sheets.forEach { entity ->
                mergeEntity(
                    entity,
                    metadata,
                    dao::findSheetIncludingDeleted,
                    dao::upsertSheet,
                )?.let { result ->
                    pulled += result.pulled
                    conflicts += result.conflicts
                }
            }
            remote.algorithms.forEach { entity ->
                mergeEntity(
                    entity,
                    metadata,
                    dao::findAlgorithmIncludingDeleted,
                    dao::upsertAlgorithm,
                )?.let { result ->
                    pulled += result.pulled
                    conflicts += result.conflicts
                }
            }
            remote.scrambles.forEach { entity ->
                mergeEntity(
                    entity,
                    metadata,
                    dao::findScrambleIncludingDeleted,
                    dao::upsertScramble,
                )?.let { result ->
                    pulled += result.pulled
                    conflicts += result.conflicts
                }
            }
            remote.tags.forEach { entity ->
                mergeEntity(
                    entity,
                    metadata,
                    dao::findTagIncludingDeleted,
                    dao::upsertTag,
                )?.let { result ->
                    pulled += result.pulled
                    conflicts += result.conflicts
                }
            }
            remote.timerEntries.forEach { entity ->
                mergeEntity(
                    entity,
                    metadata,
                    dao::findTimerEntryIncludingDeleted,
                    dao::upsertTimerEntry,
                )?.let { result ->
                    pulled += result.pulled
                    conflicts += result.conflicts
                }
            }
            pulled += mergeSheetTags(remote.sheetTags)
            pulled += mergeScrambleTags(remote.scrambleTags)
            remote.userPreferences?.let { preferences ->
                val currentMetadata = metadata[metadataKey(USER_PREFERENCES_ENTITY, USER_PREFERENCES_ID)]
                if (currentMetadata == null || preferences.updatedAt >= currentMetadata.updatedAt) {
                    settingsRepository.savePreferencesFromCloud(preferences.preferences, preferences.updatedAt)
                    pulled += 1
                } else {
                    conflicts += 1
                }
            }
            AppResult.success(
                SyncSummary(
                    pulledItems = pulled,
                    conflictCount = conflicts,
                    completedAt = clockProvider.now(),
                ),
            )
        }

    override suspend fun purgeRemoteOnly(): AppResult<SyncSummary> =
        syncResult {
            val now = clockProvider.now().toEpochMilli()
            val deleted = remoteDataSource.purgeRemoteOnly(now).getOrReport() ?: return@syncResult AppResult.failure(
                AppError.Network(),
            )
            dao.markAllCloudIneligible()
            dao.clearOutbox()
            AppResult.success(SyncSummary(deletedRemoteItems = deleted, completedAt = clockProvider.now()))
        }

    private suspend fun pushOutboxItem(item: OutboxEntity): AppResult<Unit> =
        when (item.operation) {
            OUTBOX_OPERATION_UPSERT -> pushUpsert(item)
            OUTBOX_OPERATION_DELETE -> remoteDataSource.tombstone(item.entityType, item.entityId, item.createdAt)
            OUTBOX_OPERATION_TAGS -> pushTags(item)
            else -> AppResult.failure(AppError.Conflict("Unknown outbox operation ${item.operation}"))
        }

    private suspend fun pushUpsert(item: OutboxEntity): AppResult<Unit> =
        when (item.entityType) {
            "collection" -> dao.findCollectionIncludingDeleted(item.entityId)
                ?.let { remoteDataSource.upsertCollection(it) }
            "algorithm_sheet" -> dao.findSheetIncludingDeleted(item.entityId)
                ?.let { remoteDataSource.upsertSheet(it) }
            "algorithm" -> dao.findAlgorithmIncludingDeleted(item.entityId)
                ?.let { remoteDataSource.upsertAlgorithm(it) }
            "scramble" -> dao.findScrambleIncludingDeleted(item.entityId)
                ?.let { remoteDataSource.upsertScramble(it) }
            "tag" -> dao.findTagIncludingDeleted(item.entityId)
                ?.let { remoteDataSource.upsertTag(it) }
            "timer_entry" -> dao.findTimerEntryIncludingDeleted(item.entityId)
                ?.let { remoteDataSource.upsertTimerEntry(it) }
            USER_PREFERENCES_ENTITY -> settingsRepository.loadPreferences().getOrNull()
                ?.let { remoteDataSource.upsertUserPreferences(it, item.createdAt) }
            else -> null
        } ?: AppResult.failure(AppError.NotFound("${item.entityType} not found locally"))

    private suspend fun pushTags(item: OutboxEntity): AppResult<Unit> =
        when (item.entityType) {
            "algorithm_sheet" -> remoteDataSource.replaceSheetTags(
                item.entityId,
                dao.listSheetTagIds(item.entityId).toSet(),
                item.createdAt,
            )
            "scramble" -> remoteDataSource.replaceScrambleTags(
                item.entityId,
                dao.listScrambleTagIds(item.entityId).toSet(),
                item.createdAt,
            )
            else -> AppResult.failure(AppError.Conflict("Tags are not supported for ${item.entityType}"))
        }

    private suspend fun mergeSheetTags(remoteTags: List<CloudSheetTag>): Int {
        val activeBySheet = remoteTags.filter { it.deletedAt == null }.groupBy { it.sheetId }
        activeBySheet.forEach { (sheetId, tags) ->
            dao.clearSheetTags(sheetId)
            dao.insertSheetTags(tags.map { it.toEntity() })
        }
        return remoteTags.size
    }

    private suspend fun mergeScrambleTags(remoteTags: List<CloudScrambleTag>): Int {
        val activeByScramble = remoteTags.filter { it.deletedAt == null }.groupBy { it.scrambleId }
        activeByScramble.forEach { (scrambleId, tags) ->
            dao.clearScrambleTags(scrambleId)
            dao.insertScrambleTags(tags.map { it.toEntity() })
        }
        return remoteTags.size
    }

    private suspend fun <T : Any> mergeEntity(
        remote: T,
        metadata: Map<String, SyncMetadataEntity>,
        findLocal: suspend (String) -> T?,
        upsertLocal: suspend (T) -> Unit,
    ): MergeResult? {
        val descriptor = remote.descriptor() ?: return null
        val local = findLocal(descriptor.id)
        val currentMetadata = metadata[metadataKey(descriptor.entityType, descriptor.id)]
        val localUpdatedAt = local?.updatedAt() ?: Long.MIN_VALUE
        val conflict = currentMetadata != null &&
            localUpdatedAt > currentMetadata.updatedAt &&
            descriptor.updatedAt > currentMetadata.updatedAt &&
            localUpdatedAt != descriptor.updatedAt
        return if (local == null || descriptor.updatedAt >= localUpdatedAt) {
            upsertLocal(remote)
            dao.upsertSyncMetadata(
                SyncMetadataEntity(
                    id = metadataKey(descriptor.entityType, descriptor.id),
                    entityType = descriptor.entityType,
                    entityId = descriptor.id,
                    updatedAt = descriptor.updatedAt,
                    deletedAt = descriptor.deletedAt,
                    cloudEligible = true,
                ),
            )
            MergeResult(pulled = 1, conflicts = if (conflict) 1 else 0)
        } else {
            MergeResult(pulled = 0, conflicts = if (conflict) 1 else 0)
        }
    }

    private fun Any.descriptor(): EntityDescriptor? =
        when (this) {
            is CollectionEntity -> EntityDescriptor("collection", id, updatedAt, deletedAt)
            is AlgorithmSheetEntity -> EntityDescriptor("algorithm_sheet", id, updatedAt, deletedAt)
            is AlgorithmEntity -> EntityDescriptor("algorithm", id, updatedAt, deletedAt)
            is ScrambleEntity -> EntityDescriptor("scramble", id, updatedAt, deletedAt)
            is TagEntity -> EntityDescriptor("tag", id, updatedAt, deletedAt)
            is TimerEntryEntity -> EntityDescriptor("timer_entry", id, updatedAt, deletedAt)
            else -> null
        }

    private fun Any.updatedAt(): Long =
        when (this) {
            is CollectionEntity -> updatedAt
            is AlgorithmSheetEntity -> updatedAt
            is AlgorithmEntity -> updatedAt
            is ScrambleEntity -> updatedAt
            is TagEntity -> updatedAt
            is TimerEntryEntity -> updatedAt
            else -> Long.MIN_VALUE
        }

    private suspend fun <T> AppResult<T>.getOrReport(): T? =
        when (this) {
            is AppResult.Success -> value
            is AppResult.Failure -> null
        }

    private suspend fun <T> syncResult(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (error: Exception) {
            AppResult.failure(AppError.Network(cause = error))
        }

    private data class EntityDescriptor(
        val entityType: String,
        val id: String,
        val updatedAt: Long,
        val deletedAt: Long?,
    )

    private data class MergeResult(
        val pulled: Int,
        val conflicts: Int,
    )

    private val SyncMetadataEntity.key: String
        get() = metadataKey(entityType, entityId)

    private fun metadataKey(entityType: String, entityId: String): String = "$entityType:$entityId"

    private companion object {
        const val USER_PREFERENCES_ENTITY = LocalSettingsRepository.USER_PREFERENCES_ENTITY
        const val USER_PREFERENCES_ID = LocalSettingsRepository.USER_PREFERENCES_ID
    }
}
