package fr.olegueyan.algomix.application.port

import fr.olegueyan.algomix.application.core.AppResult
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

interface LibraryRepository {
    suspend fun listCollections(): AppResult<List<LibraryCollection>>

    suspend fun saveCollection(collection: LibraryCollection): AppResult<LibraryCollection>

    suspend fun deleteCollection(id: CollectionId): AppResult<Unit>

    suspend fun listSheets(collectionId: CollectionId? = null): AppResult<List<AlgorithmSheet>>

    suspend fun saveSheet(sheet: AlgorithmSheet): AppResult<AlgorithmSheet>

    suspend fun deleteSheet(id: SheetId): AppResult<Unit>

    suspend fun listAlgorithms(sheetId: SheetId): AppResult<List<AlgorithmEntry>>

    suspend fun saveAlgorithm(algorithm: AlgorithmEntry): AppResult<AlgorithmEntry>

    suspend fun deleteAlgorithm(id: AlgorithmId): AppResult<Unit>

    suspend fun listScrambles(collectionId: CollectionId? = null): AppResult<List<Scramble>>

    suspend fun saveScramble(scramble: Scramble): AppResult<Scramble>

    suspend fun deleteScramble(id: ScrambleId): AppResult<Unit>

    suspend fun listTags(): AppResult<List<Tag>>

    suspend fun saveTag(tag: Tag): AppResult<Tag>

    suspend fun deleteTag(id: TagId): AppResult<Unit>

    suspend fun setSheetTags(sheetId: SheetId, tagIds: Set<TagId>): AppResult<Unit>

    suspend fun setScrambleTags(scrambleId: ScrambleId, tagIds: Set<TagId>): AppResult<Unit>

    suspend fun listSheetTagIds(sheetId: SheetId): AppResult<Set<TagId>>

    suspend fun listScrambleTagIds(scrambleId: ScrambleId): AppResult<Set<TagId>>
}
