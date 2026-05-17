package fr.olegueyan.algomix.infrastructure.export

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.LibraryRepository
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LocalPdfExporterTest {
    @Test
    fun exportsSheetWithAlgorithmsToPdf() = runBlocking {
        val exporter = LocalPdfExporter(RuntimeEnvironment.getApplication(), FakeLibraryRepository())

        val result = exporter.exportSheet(SheetId("sheet-1")).getOrNull()

        requireNotNull(result)
        assertEquals("oll-cas-01-algomix.pdf", result.fileName)
        assertTrue(File(result.localFilePath).exists())
    }

    @Test
    fun missingSheetReturnsNotFound() = runBlocking {
        val exporter = LocalPdfExporter(RuntimeEnvironment.getApplication(), FakeLibraryRepository())

        val error = exporter.exportSheet(SheetId("missing")).errorOrNull()

        assertTrue(error is AppError.NotFound)
    }

    private class FakeLibraryRepository : LibraryRepository {
        override suspend fun listCollections(): AppResult<List<LibraryCollection>> =
            AppResult.success(emptyList())

        override suspend fun saveCollection(collection: LibraryCollection): AppResult<LibraryCollection> =
            AppResult.success(collection)

        override suspend fun deleteCollection(id: CollectionId): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun listSheets(collectionId: CollectionId?): AppResult<List<AlgorithmSheet>> =
            AppResult.success(
                listOf(AlgorithmSheet(SheetId("sheet-1"), CollectionId("collection-1"), "OLL Cas 01")),
            )

        override suspend fun saveSheet(sheet: AlgorithmSheet): AppResult<AlgorithmSheet> =
            AppResult.success(sheet)

        override suspend fun deleteSheet(id: SheetId): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun listAlgorithms(sheetId: SheetId): AppResult<List<AlgorithmEntry>> =
            AppResult.success(
                listOf(
                    AlgorithmEntry(AlgorithmId("algorithm-1"), sheetId, "Algo A", "R U R' U'", 0),
                    AlgorithmEntry(AlgorithmId("algorithm-2"), sheetId, "Algo B", "F R U", 1),
                ),
            )

        override suspend fun saveAlgorithm(algorithm: AlgorithmEntry): AppResult<AlgorithmEntry> =
            AppResult.success(algorithm)

        override suspend fun deleteAlgorithm(id: AlgorithmId): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun listScrambles(collectionId: CollectionId?): AppResult<List<Scramble>> =
            AppResult.success(emptyList())

        override suspend fun saveScramble(scramble: Scramble): AppResult<Scramble> =
            AppResult.success(scramble)

        override suspend fun deleteScramble(id: ScrambleId): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun listTags(): AppResult<List<Tag>> =
            AppResult.success(emptyList())

        override suspend fun saveTag(tag: Tag): AppResult<Tag> =
            AppResult.success(tag)

        override suspend fun deleteTag(id: TagId): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun setSheetTags(sheetId: SheetId, tagIds: Set<TagId>): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun setScrambleTags(scrambleId: ScrambleId, tagIds: Set<TagId>): AppResult<Unit> =
            AppResult.success(Unit)

        override suspend fun listSheetTagIds(sheetId: SheetId): AppResult<Set<TagId>> =
            AppResult.success(emptySet())

        override suspend fun listScrambleTagIds(scrambleId: ScrambleId): AppResult<Set<TagId>> =
            AppResult.success(emptySet())
    }
}
