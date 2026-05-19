package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmSheetEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.CollectionEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.ScrambleEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.ScrambleTagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.SheetTagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TimerEntryEntity

data class CloudDataset(
    val collections: List<CollectionEntity> = emptyList(),
    val sheets: List<AlgorithmSheetEntity> = emptyList(),
    val algorithms: List<AlgorithmEntity> = emptyList(),
    val scrambles: List<ScrambleEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val sheetTags: List<CloudSheetTag> = emptyList(),
    val scrambleTags: List<CloudScrambleTag> = emptyList(),
    val timerEntries: List<TimerEntryEntity> = emptyList(),
    val userPreferences: CloudUserPreferences? = null,
)

data class CloudSheetTag(
    val sheetId: String,
    val tagId: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
) {
    fun toEntity(): SheetTagEntity = SheetTagEntity(sheetId, tagId)
}

data class CloudScrambleTag(
    val scrambleId: String,
    val tagId: String,
    val updatedAt: Long,
    val deletedAt: Long? = null,
) {
    fun toEntity(): ScrambleTagEntity = ScrambleTagEntity(scrambleId, tagId)
}

data class CloudUserPreferences(
    val preferences: UserPreferences,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
