package fr.olegueyan.algomix.infrastructure.persistence.local

import androidx.room.Room
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.ClockProvider
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class LocalLibraryRepositoryTest {
    private lateinit var database: AlgomixDatabase
    private lateinit var dao: LocalPersistenceDao
    private lateinit var repository: LocalLibraryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AlgomixDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.localPersistenceDao()
        repository = LocalLibraryRepository(dao, FixedClock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun collectionCrudUsesSoftDeleteAndOutbox() = runBlocking {
        val collection = LibraryCollection(CollectionId("collection-1"), " F2L ")

        val saved = repository.saveCollection(collection).getOrNull()
        repository.deleteCollection(collection.id)

        assertEquals(LibraryCollection(CollectionId("collection-1"), "F2L"), saved)
        assertTrue(repository.listCollections().getOrNull().orEmpty().isEmpty())
        assertEquals(listOf("UPSERT", "DELETE"), dao.listOutbox().map { it.operation })
    }

    @Test
    fun sheetAlgorithmAndScrambleCrudNormalizeSequences() = runBlocking {
        saveBaseCollection()
        val sheet = AlgorithmSheet(SheetId("sheet-1"), CollectionId("collection-1"), "OLL")
        val algorithm = AlgorithmEntry(
            id = AlgorithmId("algorithm-1"),
            sheetId = sheet.id,
            name = "Sune",
            sequence = "r u' R2",
            position = 2,
        )
        val firstAlgorithm = algorithm.copy(id = AlgorithmId("algorithm-0"), name = "First", position = 1)
        val scramble = Scramble(
            id = ScrambleId("scramble-1"),
            collectionId = CollectionId("collection-1"),
            name = "Training",
            sequence = "r u'",
        )

        repository.saveSheet(sheet)
        repository.saveAlgorithm(algorithm)
        repository.saveAlgorithm(firstAlgorithm)
        repository.saveScramble(scramble)

        assertEquals(listOf("First", "Sune"), repository.listAlgorithms(sheet.id).getOrNull()?.map { it.name })
        assertEquals("Rw Uw' R2", repository.listAlgorithms(sheet.id).getOrNull()?.last()?.sequence)
        assertEquals("Rw Uw'", repository.listScrambles(null).getOrNull()?.single()?.sequence)
    }

    @Test
    fun tagRelationsCanBePersistedAndRead() = runBlocking {
        saveBaseCollection()
        val sheet = AlgorithmSheet(SheetId("sheet-1"), CollectionId("collection-1"), "PLL")
        val scramble = Scramble(ScrambleId("scramble-1"), CollectionId("collection-1"), "Warmup", "R U")
        val tag = Tag(TagId("tag-1"), "fast")

        repository.saveSheet(sheet)
        repository.saveScramble(scramble)
        repository.saveTag(tag)
        repository.setSheetTags(sheet.id, setOf(tag.id))
        repository.setScrambleTags(scramble.id, setOf(tag.id))

        assertEquals(setOf(tag.id), repository.listSheetTagIds(sheet.id).getOrNull())
        assertEquals(setOf(tag.id), repository.listScrambleTagIds(scramble.id).getOrNull())
    }

    @Test
    fun invalidSequenceReturnsValidationError() = runBlocking {
        saveBaseCollection()
        val sheet = AlgorithmSheet(SheetId("sheet-1"), CollectionId("collection-1"), "OLL")
        repository.saveSheet(sheet)

        val result = repository.saveAlgorithm(
            AlgorithmEntry(AlgorithmId("algorithm-1"), sheet.id, "Bad", "R X", position = 0),
        )

        assertTrue(result.errorOrNull() is AppError.Validation)
    }

    @Test
    fun blankNameReturnsValidationError() = runBlocking {
        val result = repository.saveCollection(LibraryCollection(CollectionId("collection-1"), " "))

        assertTrue(result.errorOrNull() is AppError.Validation)
    }

    @Test
    fun deletingUnknownIdReturnsNotFound() = runBlocking {
        val result = repository.deleteCollection(CollectionId("missing"))

        assertTrue(result.errorOrNull() is AppError.NotFound)
    }

    private suspend fun saveBaseCollection() {
        repository.saveCollection(LibraryCollection(CollectionId("collection-1"), "Algorithms"))
    }

    private object FixedClock : ClockProvider {
        override fun now(): Instant = Instant.ofEpochMilli(1_700_000_000_000)
    }
}
