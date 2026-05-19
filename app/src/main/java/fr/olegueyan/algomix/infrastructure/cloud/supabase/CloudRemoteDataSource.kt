@file:Suppress("TooManyFunctions")

package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmSheetEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.CollectionEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.ScrambleEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TimerEntryEntity

interface CloudRemoteDataSource {
    suspend fun fetchDataset(): AppResult<CloudDataset>

    suspend fun upsertCollection(entity: CollectionEntity): AppResult<Unit>

    suspend fun upsertSheet(entity: AlgorithmSheetEntity): AppResult<Unit>

    suspend fun upsertAlgorithm(entity: AlgorithmEntity): AppResult<Unit>

    suspend fun upsertScramble(entity: ScrambleEntity): AppResult<Unit>

    suspend fun upsertTag(entity: TagEntity): AppResult<Unit>

    suspend fun replaceSheetTags(sheetId: String, tagIds: Set<String>, updatedAt: Long): AppResult<Unit>

    suspend fun replaceScrambleTags(scrambleId: String, tagIds: Set<String>, updatedAt: Long): AppResult<Unit>

    suspend fun upsertTimerEntry(entity: TimerEntryEntity): AppResult<Unit>

    suspend fun upsertUserPreferences(preferences: UserPreferences, updatedAt: Long): AppResult<Unit>

    suspend fun tombstone(entityType: String, entityId: String, deletedAt: Long): AppResult<Unit>

    suspend fun purgeRemoteOnly(deletedAt: Long): AppResult<Int>
}
