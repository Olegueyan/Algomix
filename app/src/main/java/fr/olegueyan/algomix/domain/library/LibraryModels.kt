package fr.olegueyan.algomix.domain.library

data class LibraryCollection(
    val id: CollectionId,
    val name: String,
)

data class AlgorithmSheet(
    val id: SheetId,
    val collectionId: CollectionId,
    val name: String,
)

data class AlgorithmEntry(
    val id: AlgorithmId,
    val sheetId: SheetId,
    val name: String,
    val sequence: String,
    val position: Int,
)

data class Scramble(
    val id: ScrambleId,
    val collectionId: CollectionId,
    val name: String,
    val sequence: String,
)

data class Tag(
    val id: TagId,
    val name: String,
)
