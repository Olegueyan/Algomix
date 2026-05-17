package fr.olegueyan.algomix.ui.state

import fr.olegueyan.algomix.domain.library.AlgorithmEntry
import fr.olegueyan.algomix.domain.library.AlgorithmSheet
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.library.Scramble
import fr.olegueyan.algomix.domain.library.SheetId
import fr.olegueyan.algomix.domain.library.Tag
import fr.olegueyan.algomix.domain.library.TagId

enum class LibraryScreen {
    OVERVIEW,
    SHEET_PREVIEW,
    SCRAMBLE_CREATE,
}

enum class LibraryItemType {
    ALL,
    SHEETS,
    SCRAMBLES,
}

enum class SaveEditTarget {
    EXISTING_SHEET,
    NEW_SCRAMBLE,
}

data class LibraryFilterState(
    val query: String = "",
    val itemType: LibraryItemType = LibraryItemType.ALL,
    val selectedTagIds: Set<TagId> = emptySet(),
)

data class LibraryFeedback(
    val message: String,
    val isError: Boolean = false,
)

sealed interface LoadLibraryItemResult {
    val name: String
    val sequence: String

    data class Algorithm(
        val algorithm: AlgorithmEntry,
    ) : LoadLibraryItemResult {
        override val name: String = algorithm.name
        override val sequence: String = algorithm.sequence
    }

    data class ScrambleItem(
        val scramble: Scramble,
    ) : LoadLibraryItemResult {
        override val name: String = scramble.name
        override val sequence: String = scramble.sequence
    }
}

data class LibraryCollectionSection(
    val collection: LibraryCollection,
    val sheets: List<AlgorithmSheet>,
    val scrambles: List<Scramble>,
)

data class LibraryUiState(
    val screen: LibraryScreen = LibraryScreen.OVERVIEW,
    val collections: List<LibraryCollection> = emptyList(),
    val sheets: List<AlgorithmSheet> = emptyList(),
    val algorithmsBySheet: Map<SheetId, List<AlgorithmEntry>> = emptyMap(),
    val scrambles: List<Scramble> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val sheetTags: Map<SheetId, Set<TagId>> = emptyMap(),
    val scrambleTags: Map<String, Set<TagId>> = emptyMap(),
    val filterState: LibraryFilterState = LibraryFilterState(),
    val selectedSheetId: SheetId? = null,
    val draftScrambleSequence: String = "",
    val feedback: LibraryFeedback? = null,
    val isLoading: Boolean = false,
) {
    val selectedSheet: AlgorithmSheet?
        get() = sheets.firstOrNull { it.id == selectedSheetId }

    val selectedSheetAlgorithms: List<AlgorithmEntry>
        get() = selectedSheetId?.let { algorithmsBySheet[it].orEmpty() }.orEmpty()

    val visibleSections: List<LibraryCollectionSection>
        get() = collections.mapNotNull { collection ->
            val sectionSheets = if (filterState.itemType == LibraryItemType.SCRAMBLES) {
                emptyList()
            } else {
                sheets.filter { sheet ->
                    sheet.collectionId == collection.id && sheet.matchesFilters(filterState, sheetTags[sheet.id])
                }
            }
            val sectionScrambles = if (filterState.itemType == LibraryItemType.SHEETS) {
                emptyList()
            } else {
                scrambles.filter { scramble ->
                    scramble.collectionId == collection.id &&
                        scramble.matchesFilters(filterState, scrambleTags[scramble.id.value])
                }
            }
            if (sectionSheets.isEmpty() && sectionScrambles.isEmpty() && !collection.matchesQuery(filterState.query)) {
                null
            } else {
                LibraryCollectionSection(collection, sectionSheets, sectionScrambles)
            }
        }

    val loadableItems: List<LoadLibraryItemResult>
        get() = sheets.flatMap { sheet ->
            algorithmsBySheet[sheet.id].orEmpty().map(LoadLibraryItemResult::Algorithm)
        } + scrambles.map(LoadLibraryItemResult::ScrambleItem)

    private fun AlgorithmSheet.matchesFilters(filter: LibraryFilterState, tagIds: Set<TagId>?): Boolean =
        matchesQuery(filter.query) && matchesTags(filter.selectedTagIds, tagIds.orEmpty())

    private fun Scramble.matchesFilters(filter: LibraryFilterState, tagIds: Set<TagId>?): Boolean =
        matchesQuery(filter.query) && matchesTags(filter.selectedTagIds, tagIds.orEmpty())

    private fun LibraryCollection.matchesQuery(query: String): Boolean =
        query.isBlank() || name.contains(query, ignoreCase = true)

    private fun AlgorithmSheet.matchesQuery(query: String): Boolean =
        query.isBlank() || name.contains(query, ignoreCase = true)

    private fun Scramble.matchesQuery(query: String): Boolean =
        query.isBlank() || name.contains(query, ignoreCase = true)

    private fun matchesTags(selectedTagIds: Set<TagId>, tagIds: Set<TagId>): Boolean =
        selectedTagIds.isEmpty() || tagIds.containsAll(selectedTagIds)
}
