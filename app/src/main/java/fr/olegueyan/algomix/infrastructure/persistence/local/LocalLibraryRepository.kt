package fr.olegueyan.algomix.infrastructure.persistence.local

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.domain.cube.MoveParseException
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.library.AlgorithmEntry
import fr.olegueyan.algomix.domain.library.AlgorithmId
import fr.olegueyan.algomix.domain.library.AlgorithmSheet
import fr.olegueyan.algomix.domain.library.CollectionId
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.library.Scramble
import fr.olegueyan.algomix.domain.library.ScrambleId
import fr.olegueyan.algomix.domain.library.SheetId
import fr.olegueyan.algomix.domain.library.Tag
import fr.olegueyan.algomix.domain.library.TagId

class LocalLibraryRepository(
    private val dao: LocalPersistenceDao,
    private val clockProvider: ClockProvider = SystemClockProvider,
) : LibraryRepository {
    override suspend fun listCollections(): AppResult<List<LibraryCollection>> =
        storageResult { AppResult.success(dao.listCollections().map { it.toDomain() }) }

    override suspend fun saveCollection(collection: LibraryCollection): AppResult<LibraryCollection> =
        storageResult {
            validateName(collection.name, "Collection")?.let { return@storageResult AppResult.failure(it) }
            val now = clockProvider.nowMillis()
            dao.upsertCollection(collection.toEntity(updatedAt = now))
            dao.insertOutbox(outbox("collection", collection.id.value, OUTBOX_OPERATION_UPSERT, now))
            AppResult.success(collection.copy(name = collection.name.trim()))
        }

    override suspend fun deleteCollection(id: CollectionId): AppResult<Unit> =
        softDelete(id.value, "collection") { entityId, deletedAt -> dao.softDeleteCollection(entityId, deletedAt) }

    override suspend fun listSheets(collectionId: CollectionId?): AppResult<List<AlgorithmSheet>> =
        storageResult { AppResult.success(dao.listSheets(collectionId?.value).map { it.toDomain() }) }

    override suspend fun saveSheet(sheet: AlgorithmSheet): AppResult<AlgorithmSheet> =
        storageResult {
            validateName(sheet.name, "Sheet")?.let { return@storageResult AppResult.failure(it) }
            if (dao.findCollection(sheet.collectionId.value) == null) {
                return@storageResult notFound("Collection")
            }
            val now = clockProvider.nowMillis()
            dao.upsertSheet(sheet.toEntity(updatedAt = now))
            dao.insertOutbox(outbox("algorithm_sheet", sheet.id.value, OUTBOX_OPERATION_UPSERT, now))
            AppResult.success(sheet.copy(name = sheet.name.trim()))
        }

    override suspend fun deleteSheet(id: SheetId): AppResult<Unit> =
        softDelete(id.value, "algorithm_sheet") { entityId, deletedAt -> dao.softDeleteSheet(entityId, deletedAt) }

    override suspend fun listAlgorithms(sheetId: SheetId): AppResult<List<AlgorithmEntry>> =
        storageResult {
            if (dao.findSheet(sheetId.value) == null) {
                return@storageResult notFound("Sheet")
            }
            AppResult.success(dao.listAlgorithms(sheetId.value).map { it.toDomain() })
        }

    override suspend fun saveAlgorithm(algorithm: AlgorithmEntry): AppResult<AlgorithmEntry> =
        storageResult {
            validateName(algorithm.name, "Algorithm")?.let { return@storageResult AppResult.failure(it) }
            if (dao.findSheet(algorithm.sheetId.value) == null) {
                return@storageResult notFound("Sheet")
            }
            val sequence = normalizeSequence(algorithm.sequence).getOrNull()
                ?: return@storageResult AppResult.failure(AppError.Validation("Algorithm sequence is invalid"))
            val now = clockProvider.nowMillis()
            dao.upsertAlgorithm(algorithm.toEntity(sequence = sequence, updatedAt = now))
            dao.insertOutbox(outbox("algorithm", algorithm.id.value, OUTBOX_OPERATION_UPSERT, now))
            AppResult.success(algorithm.copy(name = algorithm.name.trim(), sequence = sequence))
        }

    override suspend fun deleteAlgorithm(id: AlgorithmId): AppResult<Unit> =
        softDelete(id.value, "algorithm") { entityId, deletedAt -> dao.softDeleteAlgorithm(entityId, deletedAt) }

    override suspend fun listScrambles(collectionId: CollectionId?): AppResult<List<Scramble>> =
        storageResult { AppResult.success(dao.listScrambles(collectionId?.value).map { it.toDomain() }) }

    override suspend fun saveScramble(scramble: Scramble): AppResult<Scramble> =
        storageResult {
            validateName(scramble.name, "Scramble")?.let { return@storageResult AppResult.failure(it) }
            if (dao.findCollection(scramble.collectionId.value) == null) {
                return@storageResult notFound("Collection")
            }
            val sequence = normalizeSequence(scramble.sequence).getOrNull()
                ?: return@storageResult AppResult.failure(AppError.Validation("Scramble sequence is invalid"))
            val now = clockProvider.nowMillis()
            dao.upsertScramble(scramble.toEntity(sequence = sequence, updatedAt = now))
            dao.insertOutbox(outbox("scramble", scramble.id.value, OUTBOX_OPERATION_UPSERT, now))
            AppResult.success(scramble.copy(name = scramble.name.trim(), sequence = sequence))
        }

    override suspend fun deleteScramble(id: ScrambleId): AppResult<Unit> =
        softDelete(id.value, "scramble") { entityId, deletedAt -> dao.softDeleteScramble(entityId, deletedAt) }

    override suspend fun listTags(): AppResult<List<Tag>> =
        storageResult { AppResult.success(dao.listTags().map { it.toDomain() }) }

    override suspend fun saveTag(tag: Tag): AppResult<Tag> =
        storageResult {
            validateName(tag.name, "Tag")?.let { return@storageResult AppResult.failure(it) }
            val now = clockProvider.nowMillis()
            dao.upsertTag(tag.toEntity(updatedAt = now))
            dao.insertOutbox(outbox("tag", tag.id.value, OUTBOX_OPERATION_UPSERT, now))
            AppResult.success(tag.copy(name = tag.name.trim()))
        }

    override suspend fun deleteTag(id: TagId): AppResult<Unit> =
        softDelete(id.value, "tag") { entityId, deletedAt -> dao.softDeleteTag(entityId, deletedAt) }

    override suspend fun setSheetTags(sheetId: SheetId, tagIds: Set<TagId>): AppResult<Unit> =
        storageResult {
            if (dao.findSheet(sheetId.value) == null) {
                return@storageResult notFound("Sheet")
            }
            validateTagsExist(tagIds)?.let { return@storageResult it }
            val now = clockProvider.nowMillis()
            val tagValues = tagIds.mapTo(mutableSetOf()) { it.value }
            dao.clearSheetTags(sheetId.value)
            dao.insertSheetTags(tagValues.map { tagId -> SheetTagEntity(sheetId.value, tagId) })
            dao.insertOutbox(outbox("algorithm_sheet", sheetId.value, OUTBOX_OPERATION_TAGS, now))
            AppResult.success(Unit)
        }

    override suspend fun setScrambleTags(scrambleId: ScrambleId, tagIds: Set<TagId>): AppResult<Unit> =
        storageResult {
            if (dao.findScramble(scrambleId.value) == null) {
                return@storageResult notFound("Scramble")
            }
            validateTagsExist(tagIds)?.let { return@storageResult it }
            val now = clockProvider.nowMillis()
            val tagValues = tagIds.mapTo(mutableSetOf()) { it.value }
            dao.clearScrambleTags(scrambleId.value)
            dao.insertScrambleTags(tagValues.map { tagId -> ScrambleTagEntity(scrambleId.value, tagId) })
            dao.insertOutbox(outbox("scramble", scrambleId.value, OUTBOX_OPERATION_TAGS, now))
            AppResult.success(Unit)
        }

    override suspend fun listSheetTagIds(sheetId: SheetId): AppResult<Set<TagId>> =
        storageResult {
            if (dao.findSheet(sheetId.value) == null) {
                return@storageResult notFound("Sheet")
            }
            AppResult.success(dao.listSheetTagIds(sheetId.value).mapTo(mutableSetOf(), ::TagId))
        }

    override suspend fun listScrambleTagIds(scrambleId: ScrambleId): AppResult<Set<TagId>> =
        storageResult {
            if (dao.findScramble(scrambleId.value) == null) {
                return@storageResult notFound("Scramble")
            }
            AppResult.success(dao.listScrambleTagIds(scrambleId.value).mapTo(mutableSetOf(), ::TagId))
        }

    private suspend fun softDelete(
        id: String,
        entityType: String,
        delete: suspend (String, Long) -> Int,
    ): AppResult<Unit> =
        storageResult {
            val now = clockProvider.nowMillis()
            if (delete(id, now) == 0) {
                return@storageResult notFound(entityType)
            }
            dao.insertOutbox(outbox(entityType, id, OUTBOX_OPERATION_DELETE, now))
            AppResult.success(Unit)
        }

    private suspend fun validateTagsExist(tagIds: Set<TagId>): AppResult<Unit>? {
        if (tagIds.isEmpty()) {
            return null
        }
        val existingCount = dao.countExistingTags(tagIds.mapTo(mutableSetOf()) { it.value })
        return if (existingCount == tagIds.size) {
            null
        } else {
            notFound("Tag")
        }
    }

    private fun normalizeSequence(sequence: String): AppResult<String> =
        try {
            AppResult.success(MoveParser.parse(sequence).normalizedNotation)
        } catch (error: MoveParseException) {
            AppResult.failure(AppError.Validation(cause = error))
        }
}
