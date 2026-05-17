package fr.olegueyan.algomix.ui.viewmodel

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
import fr.olegueyan.algomix.ui.state.LibraryItemType
import fr.olegueyan.algomix.ui.state.SaveEditTarget
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryViewModelTest {
    @Test
    fun loadsInitialLibraryData() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = createViewModel(repository)

        val state = viewModel.uiState.value

        assertEquals(1, state.collections.size)
        assertEquals(1, state.sheets.size)
        assertEquals(1, state.scrambles.size)
        assertEquals(1, state.algorithmsBySheet[SheetId("sheet-1")]?.size)
    }

    @Test
    fun filtersByQueryTypeAndTags() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = createViewModel(repository)

        viewModel.setQuery("OLL")
        viewModel.setItemType(LibraryItemType.SHEETS)
        viewModel.toggleTagFilter(TagId("tag-oll"))

        val section = viewModel.uiState.value.visibleSections.single()
        assertEquals(1, section.sheets.size)
        assertTrue(section.scrambles.isEmpty())
    }

    @Test
    fun createsRenamesAndDeletesItems() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = createViewModel(repository)

        viewModel.createCollection("New")
        val newCollection = repository.collections.first { it.name == "New" }
        viewModel.renameCollection(newCollection, "Renamed")
        viewModel.deleteCollection(newCollection.id)

        assertTrue(repository.collections.none { it.id == newCollection.id })
        assertEquals("Collection supprimee", viewModel.uiState.value.feedback?.message)
    }

    @Test
    fun tagAttachmentIsPersistedAndReloaded() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = createViewModel(repository)

        viewModel.setSheetTags(SheetId("sheet-1"), listOf("PLL"))

        val tag = repository.tags.first { it.name == "PLL" }
        assertEquals(setOf(tag.id), repository.sheetTags[SheetId("sheet-1")])
    }

    @Test
    fun validImportAddsAlgorithmAndInvalidImportLeavesSheetUnchanged() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = createViewModel(repository)
        val before = repository.algorithms.size

        viewModel.importAlgorithm(SheetId("sheet-1"), "Valid", "R U R' U'")
        viewModel.importAlgorithm(SheetId("sheet-1"), "Invalid", "NOPE")

        assertEquals(before + 1, repository.algorithms.size)
        assertTrue(repository.algorithms.none { it.name == "Invalid" })
        assertEquals("Code algo invalide", viewModel.uiState.value.feedback?.message)
    }

    @Test
    fun saveEditingSequenceCreatesAlgorithmOrScramble() {
        val repository = FakeLibraryRepository.seeded()
        val viewModel = createViewModel(repository)

        viewModel.saveEditingSequence(
            target = SaveEditTarget.EXISTING_SHEET,
            collectionId = CollectionId("collection-1"),
            sheetId = SheetId("sheet-1"),
            name = "Edited algo",
            sequence = "R U",
        )
        viewModel.saveEditingSequence(
            target = SaveEditTarget.NEW_SCRAMBLE,
            collectionId = CollectionId("collection-1"),
            sheetId = null,
            name = "Edited scramble",
            sequence = "F2 D",
        )

        assertTrue(repository.algorithms.any { it.name == "Edited algo" })
        assertTrue(repository.scrambles.any { it.name == "Edited scramble" })
    }

    private fun createViewModel(repository: FakeLibraryRepository): LibraryViewModel =
        LibraryViewModel(repository, taskLauncher = { block -> runBlocking { block() } })

    private class FakeLibraryRepository : LibraryRepository {
        val collections = mutableListOf<LibraryCollection>()
        val sheets = mutableListOf<AlgorithmSheet>()
        val algorithms = mutableListOf<AlgorithmEntry>()
        val scrambles = mutableListOf<Scramble>()
        val tags = mutableListOf<Tag>()
        val sheetTags = mutableMapOf<SheetId, Set<TagId>>()
        val scrambleTags = mutableMapOf<ScrambleId, Set<TagId>>()

        override suspend fun listCollections(): AppResult<List<LibraryCollection>> =
            AppResult.success(collections)

        override suspend fun saveCollection(collection: LibraryCollection): AppResult<LibraryCollection> {
            collections.removeAll { it.id == collection.id }
            collections += collection.copy(name = collection.name.trim())
            return AppResult.success(collection)
        }

        override suspend fun deleteCollection(id: CollectionId): AppResult<Unit> {
            collections.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listSheets(collectionId: CollectionId?): AppResult<List<AlgorithmSheet>> =
            AppResult.success(sheets.filter { collectionId == null || it.collectionId == collectionId })

        override suspend fun saveSheet(sheet: AlgorithmSheet): AppResult<AlgorithmSheet> {
            sheets.removeAll { it.id == sheet.id }
            sheets += sheet.copy(name = sheet.name.trim())
            return AppResult.success(sheet)
        }

        override suspend fun deleteSheet(id: SheetId): AppResult<Unit> {
            sheets.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listAlgorithms(sheetId: SheetId): AppResult<List<AlgorithmEntry>> =
            AppResult.success(algorithms.filter { it.sheetId == sheetId }.sortedBy { it.position })

        override suspend fun saveAlgorithm(algorithm: AlgorithmEntry): AppResult<AlgorithmEntry> {
            algorithms.removeAll { it.id == algorithm.id }
            algorithms += algorithm
            return AppResult.success(algorithm)
        }

        override suspend fun deleteAlgorithm(id: AlgorithmId): AppResult<Unit> {
            algorithms.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listScrambles(collectionId: CollectionId?): AppResult<List<Scramble>> =
            AppResult.success(scrambles.filter { collectionId == null || it.collectionId == collectionId })

        override suspend fun saveScramble(scramble: Scramble): AppResult<Scramble> {
            scrambles.removeAll { it.id == scramble.id }
            scrambles += scramble
            return AppResult.success(scramble)
        }

        override suspend fun deleteScramble(id: ScrambleId): AppResult<Unit> {
            scrambles.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun listTags(): AppResult<List<Tag>> =
            AppResult.success(tags)

        override suspend fun saveTag(tag: Tag): AppResult<Tag> {
            tags.removeAll { it.id == tag.id }
            tags += tag
            return AppResult.success(tag)
        }

        override suspend fun deleteTag(id: TagId): AppResult<Unit> {
            tags.removeAll { it.id == id }
            return AppResult.success(Unit)
        }

        override suspend fun setSheetTags(sheetId: SheetId, tagIds: Set<TagId>): AppResult<Unit> {
            sheetTags[sheetId] = tagIds
            return AppResult.success(Unit)
        }

        override suspend fun setScrambleTags(scrambleId: ScrambleId, tagIds: Set<TagId>): AppResult<Unit> {
            scrambleTags[scrambleId] = tagIds
            return AppResult.success(Unit)
        }

        override suspend fun listSheetTagIds(sheetId: SheetId): AppResult<Set<TagId>> =
            AppResult.success(sheetTags[sheetId].orEmpty())

        override suspend fun listScrambleTagIds(scrambleId: ScrambleId): AppResult<Set<TagId>> =
            AppResult.success(scrambleTags[scrambleId].orEmpty())

        companion object {
            fun seeded(): FakeLibraryRepository =
                FakeLibraryRepository().apply {
                    collections += LibraryCollection(CollectionId("collection-1"), "OLL")
                    sheets += AlgorithmSheet(SheetId("sheet-1"), CollectionId("collection-1"), "OLL Cas 01")
                    algorithms += AlgorithmEntry(
                        AlgorithmId("algorithm-1"),
                        SheetId("sheet-1"),
                        "Algo A",
                        "R U R' U'",
                        0,
                    )
                    scrambles += Scramble(
                        ScrambleId("scramble-1"),
                        CollectionId("collection-1"),
                        "Warmup",
                        "F R U",
                    )
                    tags += Tag(TagId("tag-oll"), "OLL")
                    sheetTags[SheetId("sheet-1")] = setOf(TagId("tag-oll"))
                }
        }
    }
}
