package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmSheetEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.CollectionEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.ScrambleEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TimerEntryEntity

class FakeCloudRemoteDataSource : CloudRemoteDataSource {
    var dataset = CloudDataset()
    var failNextMutation = false

    override suspend fun fetchDataset(): AppResult<CloudDataset> = AppResult.success(dataset)

    override suspend fun upsertCollection(entity: CollectionEntity): AppResult<Unit> =
        mutate { dataset = dataset.copy(collections = dataset.collections.upsert(entity, CollectionEntity::id)) }

    override suspend fun upsertSheet(entity: AlgorithmSheetEntity): AppResult<Unit> =
        mutate { dataset = dataset.copy(sheets = dataset.sheets.upsert(entity, AlgorithmSheetEntity::id)) }

    override suspend fun upsertAlgorithm(entity: AlgorithmEntity): AppResult<Unit> =
        mutate { dataset = dataset.copy(algorithms = dataset.algorithms.upsert(entity, AlgorithmEntity::id)) }

    override suspend fun upsertScramble(entity: ScrambleEntity): AppResult<Unit> =
        mutate { dataset = dataset.copy(scrambles = dataset.scrambles.upsert(entity, ScrambleEntity::id)) }

    override suspend fun upsertTag(entity: TagEntity): AppResult<Unit> =
        mutate { dataset = dataset.copy(tags = dataset.tags.upsert(entity, TagEntity::id)) }

    override suspend fun replaceSheetTags(
        sheetId: String,
        tagIds: Set<String>,
        updatedAt: Long,
    ): AppResult<Unit> =
        mutate {
            val tombstoned = dataset.sheetTags.map { tag ->
                if (tag.sheetId == sheetId) tag.copy(updatedAt = updatedAt, deletedAt = updatedAt) else tag
            }
            val next = tagIds.map { tagId -> CloudSheetTag(sheetId, tagId, updatedAt) }
            dataset = dataset.copy(sheetTags = tombstoned.mergeSheetTags(next))
        }

    override suspend fun replaceScrambleTags(
        scrambleId: String,
        tagIds: Set<String>,
        updatedAt: Long,
    ): AppResult<Unit> =
        mutate {
            val tombstoned = dataset.scrambleTags.map { tag ->
                if (tag.scrambleId == scrambleId) tag.copy(updatedAt = updatedAt, deletedAt = updatedAt) else tag
            }
            val next = tagIds.map { tagId -> CloudScrambleTag(scrambleId, tagId, updatedAt) }
            dataset = dataset.copy(scrambleTags = tombstoned.mergeScrambleTags(next))
        }

    override suspend fun upsertTimerEntry(entity: TimerEntryEntity): AppResult<Unit> =
        mutate { dataset = dataset.copy(timerEntries = dataset.timerEntries.upsert(entity, TimerEntryEntity::id)) }

    override suspend fun upsertUserPreferences(
        preferences: UserPreferences,
        updatedAt: Long,
    ): AppResult<Unit> =
        mutate { dataset = dataset.copy(userPreferences = CloudUserPreferences(preferences, updatedAt)) }

    override suspend fun tombstone(entityType: String, entityId: String, deletedAt: Long): AppResult<Unit> =
        mutate {
            dataset = when (entityType) {
                "collection" -> dataset.copy(
                    collections = dataset.collections.map {
                        if (it.id == entityId) it.copy(updatedAt = deletedAt, deletedAt = deletedAt) else it
                    },
                )
                else -> dataset
            }
        }

    override suspend fun purgeRemoteOnly(deletedAt: Long): AppResult<Int> =
        mutateWithValue {
            val activeCount = dataset.collections.count { it.deletedAt == null }
            dataset = dataset.copy(
                collections = dataset.collections.map { it.copy(updatedAt = deletedAt, deletedAt = deletedAt) },
                sheets = dataset.sheets.map { it.copy(updatedAt = deletedAt, deletedAt = deletedAt) },
                algorithms = dataset.algorithms.map { it.copy(updatedAt = deletedAt, deletedAt = deletedAt) },
                scrambles = dataset.scrambles.map { it.copy(updatedAt = deletedAt, deletedAt = deletedAt) },
                tags = dataset.tags.map { it.copy(updatedAt = deletedAt, deletedAt = deletedAt) },
                timerEntries = dataset.timerEntries.map { it.copy(updatedAt = deletedAt, deletedAt = deletedAt) },
                userPreferences = dataset.userPreferences?.copy(updatedAt = deletedAt, deletedAt = deletedAt),
            )
            activeCount
        }

    private fun mutate(block: () -> Unit): AppResult<Unit> =
        mutateWithValue {
            block()
            Unit
        }

    private fun <T> mutateWithValue(block: () -> T): AppResult<T> {
        if (failNextMutation) {
            failNextMutation = false
            return AppResult.failure(AppError.Network("Fake remote failure"))
        }
        return AppResult.success(block())
    }

    private fun <T> List<T>.upsert(value: T, id: (T) -> String): List<T> =
        filterNot { id(it) == id(value) } + value

    private fun List<CloudSheetTag>.mergeSheetTags(values: List<CloudSheetTag>): List<CloudSheetTag> =
        filterNot { existing -> values.any { it.sheetId == existing.sheetId && it.tagId == existing.tagId } } + values

    private fun List<CloudScrambleTag>.mergeScrambleTags(values: List<CloudScrambleTag>): List<CloudScrambleTag> =
        filterNot { existing -> values.any { it.scrambleId == existing.scrambleId && it.tagId == existing.tagId } } +
            values
}
