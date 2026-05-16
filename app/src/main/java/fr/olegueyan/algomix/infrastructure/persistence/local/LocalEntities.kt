package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "algorithm_sheets",
    indices = [Index("collection_id")],
)
data class AlgorithmSheetEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "collection_id") val collectionId: String,
    val name: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "algorithms",
    indices = [Index("sheet_id")],
)
data class AlgorithmEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "sheet_id") val sheetId: String,
    val name: String,
    val sequence: String,
    val position: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "scrambles",
    indices = [Index("collection_id")],
)
data class ScrambleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "collection_id") val collectionId: String,
    val name: String,
    val sequence: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "sheet_tags",
    primaryKeys = ["sheet_id", "tag_id"],
    indices = [Index("tag_id")],
)
data class SheetTagEntity(
    @ColumnInfo(name = "sheet_id") val sheetId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
)

@Entity(
    tableName = "scramble_tags",
    primaryKeys = ["scramble_id", "tag_id"],
    indices = [Index("tag_id")],
)
data class ScrambleTagEntity(
    @ColumnInfo(name = "scramble_id") val scrambleId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
)

@Entity(tableName = "timer_entries")
data class TimerEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "duration_millis") val durationMillis: Long,
    @ColumnInfo(name = "solved_at") val solvedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "cloud_eligible") val cloudEligible: Boolean = true,
)

@Entity(
    tableName = "outbox",
    indices = [Index("entity_type"), Index("entity_id")],
)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val operation: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
