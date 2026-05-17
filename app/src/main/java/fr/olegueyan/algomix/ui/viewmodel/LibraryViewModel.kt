package fr.olegueyan.algomix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.cube.ScrambleGenerator
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
import fr.olegueyan.algomix.ui.state.LibraryFeedback
import fr.olegueyan.algomix.ui.state.LibraryFilterState
import fr.olegueyan.algomix.ui.state.LibraryItemType
import fr.olegueyan.algomix.ui.state.LibraryScreen
import fr.olegueyan.algomix.ui.state.LibraryUiState
import fr.olegueyan.algomix.ui.state.LoadLibraryItemResult
import fr.olegueyan.algomix.ui.state.SaveEditTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@Suppress("TooManyFunctions")
class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val taskLauncher: (((suspend () -> Unit)) -> Unit)? = null,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = mutableUiState.asStateFlow()

    init {
        launchTask { refresh() }
    }

    suspend fun refresh() {
        mutableUiState.value = mutableUiState.value.copy(isLoading = true)
        val collections = libraryRepository.listCollections().getOrReport() ?: return
        val sheets = libraryRepository.listSheets().getOrReport() ?: return
        val scrambles = libraryRepository.listScrambles().getOrReport() ?: return
        val tags = libraryRepository.listTags().getOrReport() ?: return
        val algorithmsBySheet = sheets.associate { sheet ->
            sheet.id to libraryRepository.listAlgorithms(sheet.id).getOrReport().orEmpty()
        }
        val sheetTags = sheets.associate { sheet ->
            sheet.id to libraryRepository.listSheetTagIds(sheet.id).getOrReport().orEmpty()
        }
        val scrambleTags = scrambles.associate { scramble ->
            scramble.id.value to libraryRepository.listScrambleTagIds(scramble.id).getOrReport().orEmpty()
        }
        mutableUiState.value = mutableUiState.value.copy(
            collections = collections,
            sheets = sheets,
            algorithmsBySheet = algorithmsBySheet,
            scrambles = scrambles,
            tags = tags,
            sheetTags = sheetTags,
            scrambleTags = scrambleTags,
            isLoading = false,
        )
    }

    fun setQuery(query: String) {
        updateFilter { it.copy(query = query) }
    }

    fun setItemType(itemType: LibraryItemType) {
        updateFilter { it.copy(itemType = itemType) }
    }

    fun toggleTagFilter(tagId: TagId) {
        updateFilter { current ->
            val nextTags = if (tagId in current.selectedTagIds) {
                current.selectedTagIds - tagId
            } else {
                current.selectedTagIds + tagId
            }
            current.copy(selectedTagIds = nextTags)
        }
    }

    fun showOverview() {
        mutableUiState.value = mutableUiState.value.copy(screen = LibraryScreen.OVERVIEW, selectedSheetId = null)
    }

    fun showSheet(sheetId: SheetId) {
        mutableUiState.value = mutableUiState.value.copy(
            screen = LibraryScreen.SHEET_PREVIEW,
            selectedSheetId = sheetId,
        )
    }

    fun showScrambleCreate() {
        mutableUiState.value = mutableUiState.value.copy(screen = LibraryScreen.SCRAMBLE_CREATE)
    }

    fun createCollection(name: String) {
        runMutation(success = "Collection creee") {
            libraryRepository.saveCollection(LibraryCollection(CollectionId(newId()), name))
        }
    }

    fun createSheet(collectionId: CollectionId, name: String) {
        runMutation(success = "Fiche creee") {
            libraryRepository.saveSheet(AlgorithmSheet(SheetId(newId()), collectionId, name))
        }
    }

    fun createScramble(collectionId: CollectionId, name: String, sequence: String) {
        runMutation(success = "Melange sauvegarde") {
            val normalizedSequence = normalizeSequence(sequence) ?: return@runMutation validation("Sequence invalide")
            libraryRepository.saveScramble(Scramble(ScrambleId(newId()), collectionId, name, normalizedSequence))
        }
    }

    fun generateDraftScramble(length: Int = DEFAULT_SCRAMBLE_LENGTH) {
        mutableUiState.value = mutableUiState.value.copy(
            draftScrambleSequence = ScrambleGenerator.generate(length).normalizedNotation,
            feedback = LibraryFeedback("Melange genere"),
        )
    }

    fun setDraftScrambleSequence(sequence: String) {
        mutableUiState.value = mutableUiState.value.copy(draftScrambleSequence = sequence)
    }

    fun saveDraftScramble(collectionId: CollectionId, name: String) {
        createScramble(collectionId, name, mutableUiState.value.draftScrambleSequence)
    }

    fun importAlgorithm(sheetId: SheetId, name: String, sequence: String) {
        runMutation(success = "Algorithme importe") {
            val algorithms = libraryRepository.listAlgorithms(sheetId).getOrNull().orEmpty()
            val normalizedSequence = normalizeSequence(sequence) ?: return@runMutation validation("Code algo invalide")
            libraryRepository.saveAlgorithm(
                AlgorithmEntry(
                    id = AlgorithmId(newId()),
                    sheetId = sheetId,
                    name = name,
                    sequence = normalizedSequence,
                    position = algorithms.size,
                ),
            )
        }
    }

    fun saveEditingSequence(
        target: SaveEditTarget,
        collectionId: CollectionId,
        sheetId: SheetId?,
        name: String,
        sequence: String,
    ) {
        if (sequence.isBlank()) {
            setError("Sequence vide")
            return
        }
        when (target) {
            SaveEditTarget.EXISTING_SHEET -> {
                val targetSheetId = sheetId ?: return setError("Selection de fiche requise")
                importAlgorithm(targetSheetId, name, sequence)
            }
            SaveEditTarget.NEW_SCRAMBLE -> createScramble(collectionId, name, sequence)
        }
    }

    fun renameCollection(collection: LibraryCollection, name: String) {
        runMutation(success = "Collection renommee") {
            libraryRepository.saveCollection(collection.copy(name = name))
        }
    }

    fun renameSheet(sheet: AlgorithmSheet, name: String) {
        runMutation(success = "Fiche renommee") {
            libraryRepository.saveSheet(sheet.copy(name = name))
        }
    }

    fun renameScramble(scramble: Scramble, name: String) {
        runMutation(success = "Melange renomme") {
            libraryRepository.saveScramble(scramble.copy(name = name))
        }
    }

    fun deleteCollection(collectionId: CollectionId) {
        runMutation(success = "Collection supprimee") { libraryRepository.deleteCollection(collectionId) }
    }

    fun deleteSheet(sheetId: SheetId) {
        runMutation(success = "Fiche supprimee") { libraryRepository.deleteSheet(sheetId) }
    }

    fun deleteScramble(scrambleId: ScrambleId) {
        runMutation(success = "Melange supprime") { libraryRepository.deleteScramble(scrambleId) }
    }

    fun setSheetTags(sheetId: SheetId, tagNames: List<String>) {
        runMutation(success = "Tags mis a jour") {
            val tagIds = saveTags(tagNames)
            libraryRepository.setSheetTags(sheetId, tagIds)
        }
    }

    fun setScrambleTags(scrambleId: ScrambleId, tagNames: List<String>) {
        runMutation(success = "Tags mis a jour") {
            val tagIds = saveTags(tagNames)
            libraryRepository.setScrambleTags(scrambleId, tagIds)
        }
    }

    fun selectLoadItem(item: LoadLibraryItemResult) {
        mutableUiState.value = mutableUiState.value.copy(feedback = LibraryFeedback("Selection: ${item.name}"))
    }

    fun consumeFeedback() {
        mutableUiState.value = mutableUiState.value.copy(feedback = null)
    }

    private fun updateFilter(update: (LibraryFilterState) -> LibraryFilterState) {
        mutableUiState.value = mutableUiState.value.copy(filterState = update(mutableUiState.value.filterState))
    }

    private fun runMutation(
        success: String,
        operation: suspend () -> AppResult<*>,
    ) {
        launchTask {
            when (val result = operation()) {
                is AppResult.Success -> {
                    mutableUiState.value = mutableUiState.value.copy(feedback = LibraryFeedback(success))
                    refresh()
                }
                is AppResult.Failure -> setError(result.error)
            }
        }
    }

    private suspend fun saveTags(tagNames: List<String>): Set<TagId> {
        val existingByName = libraryRepository.listTags().getOrNull().orEmpty()
            .associateBy { it.name.lowercase() }
        val result = mutableSetOf<TagId>()
        tagNames.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .forEach { name ->
                val existing = existingByName[name.lowercase()]
                val tagId = existing?.id ?: libraryRepository.saveTag(Tag(TagId(newId()), name)).getOrNull()?.id
                if (tagId != null) {
                    result += tagId
                }
            }
        return result
    }

    private fun normalizeSequence(sequence: String): String? =
        try {
            MoveParser.parse(sequence).normalizedNotation.takeIf(String::isNotBlank)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun validation(message: String): AppResult<Unit> =
        AppResult.failure(AppError.Validation(message))

    private fun <T> AppResult<T>.getOrReport(): T? =
        fold(
            onSuccess = { it },
            onFailure = {
                setError(it)
                mutableUiState.value = mutableUiState.value.copy(isLoading = false)
                null
            },
        )

    private fun setError(error: AppError) {
        setError(error.message)
    }

    private fun setError(message: String) {
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = false,
            feedback = LibraryFeedback(message, isError = true),
        )
    }

    private fun launchTask(block: suspend () -> Unit) {
        val launcher = taskLauncher
        if (launcher != null) {
            launcher(block)
        } else {
            viewModelScope.launch { block() }
        }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        private const val DEFAULT_SCRAMBLE_LENGTH = 20
    }
}
