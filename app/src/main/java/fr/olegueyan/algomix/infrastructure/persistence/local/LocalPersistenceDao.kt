package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Suppress("TooManyFunctions")
@Dao
interface LocalPersistenceDao {
    @Query("SELECT * FROM collections WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    suspend fun listCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collections")
    suspend fun listCollectionsIncludingDeleted(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id AND deleted_at IS NULL")
    suspend fun findCollection(id: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun findCollectionIncludingDeleted(id: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollection(entity: CollectionEntity)

    @Query(
        """
        UPDATE collections
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteCollection(id: String, deletedAt: Long): Int

    @Query(
        """
        SELECT * FROM algorithm_sheets
        WHERE deleted_at IS NULL AND (:collectionId IS NULL OR collection_id = :collectionId)
        ORDER BY name COLLATE NOCASE
        """,
    )
    suspend fun listSheets(collectionId: String?): List<AlgorithmSheetEntity>

    @Query("SELECT * FROM algorithm_sheets")
    suspend fun listSheetsIncludingDeleted(): List<AlgorithmSheetEntity>

    @Query("SELECT * FROM algorithm_sheets WHERE id = :id AND deleted_at IS NULL")
    suspend fun findSheet(id: String): AlgorithmSheetEntity?

    @Query("SELECT * FROM algorithm_sheets WHERE id = :id")
    suspend fun findSheetIncludingDeleted(id: String): AlgorithmSheetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSheet(entity: AlgorithmSheetEntity)

    @Query(
        """
        UPDATE algorithm_sheets
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteSheet(id: String, deletedAt: Long): Int

    @Query(
        """
        SELECT * FROM algorithms
        WHERE deleted_at IS NULL AND sheet_id = :sheetId
        ORDER BY position, name COLLATE NOCASE
        """,
    )
    suspend fun listAlgorithms(sheetId: String): List<AlgorithmEntity>

    @Query("SELECT * FROM algorithms")
    suspend fun listAlgorithmsIncludingDeleted(): List<AlgorithmEntity>

    @Query("SELECT * FROM algorithms WHERE id = :id AND deleted_at IS NULL")
    suspend fun findAlgorithm(id: String): AlgorithmEntity?

    @Query("SELECT * FROM algorithms WHERE id = :id")
    suspend fun findAlgorithmIncludingDeleted(id: String): AlgorithmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlgorithm(entity: AlgorithmEntity)

    @Query(
        """
        UPDATE algorithms
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteAlgorithm(id: String, deletedAt: Long): Int

    @Query(
        """
        SELECT * FROM scrambles
        WHERE deleted_at IS NULL AND (:collectionId IS NULL OR collection_id = :collectionId)
        ORDER BY name COLLATE NOCASE
        """,
    )
    suspend fun listScrambles(collectionId: String?): List<ScrambleEntity>

    @Query("SELECT * FROM scrambles")
    suspend fun listScramblesIncludingDeleted(): List<ScrambleEntity>

    @Query("SELECT * FROM scrambles WHERE id = :id AND deleted_at IS NULL")
    suspend fun findScramble(id: String): ScrambleEntity?

    @Query("SELECT * FROM scrambles WHERE id = :id")
    suspend fun findScrambleIncludingDeleted(id: String): ScrambleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScramble(entity: ScrambleEntity)

    @Query(
        """
        UPDATE scrambles
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteScramble(id: String, deletedAt: Long): Int

    @Query("SELECT * FROM tags WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    suspend fun listTags(): List<TagEntity>

    @Query("SELECT * FROM tags")
    suspend fun listTagsIncludingDeleted(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id AND deleted_at IS NULL")
    suspend fun findTag(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun findTagIncludingDeleted(id: String): TagEntity?

    @Query("SELECT COUNT(*) FROM tags WHERE deleted_at IS NULL AND id IN (:ids)")
    suspend fun countExistingTags(ids: Set<String>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTag(entity: TagEntity)

    @Query("UPDATE tags SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDeleteTag(id: String, deletedAt: Long): Int

    @Query("SELECT tag_id FROM sheet_tags WHERE sheet_id = :sheetId ORDER BY tag_id")
    suspend fun listSheetTagIds(sheetId: String): List<String>

    @Query("SELECT * FROM sheet_tags")
    suspend fun listSheetTags(): List<SheetTagEntity>

    @Query("DELETE FROM sheet_tags WHERE sheet_id = :sheetId")
    suspend fun clearSheetTags(sheetId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheetTags(entities: List<SheetTagEntity>)

    @Query("SELECT tag_id FROM scramble_tags WHERE scramble_id = :scrambleId ORDER BY tag_id")
    suspend fun listScrambleTagIds(scrambleId: String): List<String>

    @Query("SELECT * FROM scramble_tags")
    suspend fun listScrambleTags(): List<ScrambleTagEntity>

    @Query("DELETE FROM scramble_tags WHERE scramble_id = :scrambleId")
    suspend fun clearScrambleTags(scrambleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScrambleTags(entities: List<ScrambleTagEntity>)

    @Query("SELECT * FROM timer_entries WHERE deleted_at IS NULL ORDER BY solved_at DESC")
    suspend fun listTimerEntries(): List<TimerEntryEntity>

    @Query("SELECT * FROM timer_entries")
    suspend fun listTimerEntriesIncludingDeleted(): List<TimerEntryEntity>

    @Query("SELECT * FROM timer_entries WHERE id = :id AND deleted_at IS NULL")
    suspend fun findTimerEntry(id: String): TimerEntryEntity?

    @Query("SELECT * FROM timer_entries WHERE id = :id")
    suspend fun findTimerEntryIncludingDeleted(id: String): TimerEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimerEntry(entity: TimerEntryEntity)

    @Query(
        """
        UPDATE timer_entries
        SET deleted_at = :deletedAt, updated_at = :deletedAt
        WHERE id = :id AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteTimerEntry(id: String, deletedAt: Long): Int

    @Query("SELECT id FROM timer_entries WHERE deleted_at IS NULL")
    suspend fun listActiveTimerEntryIds(): List<String>

    @Query("UPDATE timer_entries SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE deleted_at IS NULL")
    suspend fun softDeleteAllTimerEntries(deletedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMetadata(entity: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun findSyncMetadata(entityType: String, entityId: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata")
    suspend fun listSyncMetadata(): List<SyncMetadataEntity>

    @Query(
        """
        UPDATE sync_metadata
        SET cloud_eligible = :cloudEligible
        WHERE entity_type = :entityType AND entity_id = :entityId
        """,
    )
    suspend fun markCloudEligible(entityType: String, entityId: String, cloudEligible: Boolean)

    @Query("UPDATE sync_metadata SET cloud_eligible = 0")
    suspend fun markAllCloudIneligible()

    @Insert
    suspend fun insertOutbox(entity: OutboxEntity)

    @Query("SELECT * FROM outbox ORDER BY id")
    suspend fun listOutbox(): List<OutboxEntity>

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun deleteOutbox(id: Long)

    @Query("DELETE FROM outbox WHERE entity_type = :entityType AND entity_id = :entityId")
    suspend fun clearOutboxForEntity(entityType: String, entityId: String)

    @Query("DELETE FROM outbox")
    suspend fun clearOutbox()
}
