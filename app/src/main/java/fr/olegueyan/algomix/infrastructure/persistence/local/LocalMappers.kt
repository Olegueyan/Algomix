package fr.olegueyan.algomix.infrastructure.persistence.local

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
import fr.olegueyan.algomix.domain.timer.TimerEntry
import fr.olegueyan.algomix.domain.timer.TimerEntryId
import java.time.Instant

fun CollectionEntity.toDomain(): LibraryCollection =
    LibraryCollection(
        id = CollectionId(id),
        name = name,
    )

fun LibraryCollection.toEntity(updatedAt: Long): CollectionEntity =
    CollectionEntity(
        id = id.value,
        name = name.trim(),
        updatedAt = updatedAt,
    )

fun AlgorithmSheetEntity.toDomain(): AlgorithmSheet =
    AlgorithmSheet(
        id = SheetId(id),
        collectionId = CollectionId(collectionId),
        name = name,
    )

fun AlgorithmSheet.toEntity(updatedAt: Long): AlgorithmSheetEntity =
    AlgorithmSheetEntity(
        id = id.value,
        collectionId = collectionId.value,
        name = name.trim(),
        updatedAt = updatedAt,
    )

fun AlgorithmEntity.toDomain(): AlgorithmEntry =
    AlgorithmEntry(
        id = AlgorithmId(id),
        sheetId = SheetId(sheetId),
        name = name,
        sequence = sequence,
        position = position,
    )

fun AlgorithmEntry.toEntity(sequence: String, updatedAt: Long): AlgorithmEntity =
    AlgorithmEntity(
        id = id.value,
        sheetId = sheetId.value,
        name = name.trim(),
        sequence = sequence,
        position = position,
        updatedAt = updatedAt,
    )

fun ScrambleEntity.toDomain(): Scramble =
    Scramble(
        id = ScrambleId(id),
        collectionId = CollectionId(collectionId),
        name = name,
        sequence = sequence,
    )

fun Scramble.toEntity(sequence: String, updatedAt: Long): ScrambleEntity =
    ScrambleEntity(
        id = id.value,
        collectionId = collectionId.value,
        name = name.trim(),
        sequence = sequence,
        updatedAt = updatedAt,
    )

fun TagEntity.toDomain(): Tag =
    Tag(
        id = TagId(id),
        name = name,
    )

fun Tag.toEntity(updatedAt: Long): TagEntity =
    TagEntity(
        id = id.value,
        name = name.trim(),
        updatedAt = updatedAt,
    )

fun TimerEntryEntity.toDomain(): TimerEntry =
    TimerEntry(
        id = TimerEntryId(id),
        durationMillis = durationMillis,
        solvedAt = Instant.ofEpochMilli(solvedAt),
    )

fun TimerEntry.toEntity(updatedAt: Long): TimerEntryEntity =
    TimerEntryEntity(
        id = id.value,
        durationMillis = durationMillis,
        solvedAt = solvedAt.toEpochMilli(),
        updatedAt = updatedAt,
    )
