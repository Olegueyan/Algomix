@file:Suppress("TooManyFunctions")

package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.domain.settings.UserPreferences
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.AlgorithmSheetEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.CollectionEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.ScrambleEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TagEntity
import fr.olegueyan.algomix.infrastructure.persistence.local.TimerEntryEntity
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

class SupabaseRestRemoteDataSource(
    private val config: SupabaseConfig,
    private val accessTokenProvider: () -> String?,
    private val ownerIdProvider: () -> String?,
    private val httpClient: HttpClient = defaultSupabaseHttpClient(),
) : CloudRemoteDataSource {
    override suspend fun fetchDataset(): AppResult<CloudDataset> =
        networkResult {
            AppResult.success(
                CloudDataset(
                    collections = getRows(TABLE_COLLECTIONS).map(::collectionFromJson),
                    sheets = getRows(TABLE_SHEETS).map(::sheetFromJson),
                    algorithms = getRows(TABLE_ALGORITHMS).map(::algorithmFromJson),
                    scrambles = getRows(TABLE_SCRAMBLES).map(::scrambleFromJson),
                    tags = getRows(TABLE_TAGS).map(::tagFromJson),
                    sheetTags = getRows(TABLE_SHEET_TAGS).map(::sheetTagFromJson),
                    scrambleTags = getRows(TABLE_SCRAMBLE_TAGS).map(::scrambleTagFromJson),
                    timerEntries = getRows(TABLE_TIMER_ENTRIES).map(::timerEntryFromJson),
                    userPreferences = getRows(TABLE_USER_PREFERENCES).firstOrNull()?.let(::preferencesFromJson),
                ),
            )
        }

    override suspend fun upsertCollection(entity: CollectionEntity): AppResult<Unit> =
        upsert(TABLE_COLLECTIONS, "id", collectionToJson(entity))

    override suspend fun upsertSheet(entity: AlgorithmSheetEntity): AppResult<Unit> =
        upsert(TABLE_SHEETS, "id", sheetToJson(entity))

    override suspend fun upsertAlgorithm(entity: AlgorithmEntity): AppResult<Unit> =
        upsert(TABLE_ALGORITHMS, "id", algorithmToJson(entity))

    override suspend fun upsertScramble(entity: ScrambleEntity): AppResult<Unit> =
        upsert(TABLE_SCRAMBLES, "id", scrambleToJson(entity))

    override suspend fun upsertTag(entity: TagEntity): AppResult<Unit> =
        upsert(TABLE_TAGS, "id", tagToJson(entity))

    override suspend fun replaceSheetTags(
        sheetId: String,
        tagIds: Set<String>,
        updatedAt: Long,
    ): AppResult<Unit> =
        replaceTags(
            table = TABLE_SHEET_TAGS,
            ownerColumn = "sheet_id",
            ownerId = sheetId,
            tagIds = tagIds,
            updatedAt = updatedAt,
        )

    override suspend fun replaceScrambleTags(
        scrambleId: String,
        tagIds: Set<String>,
        updatedAt: Long,
    ): AppResult<Unit> =
        replaceTags(
            table = TABLE_SCRAMBLE_TAGS,
            ownerColumn = "scramble_id",
            ownerId = scrambleId,
            tagIds = tagIds,
            updatedAt = updatedAt,
        )

    override suspend fun upsertTimerEntry(entity: TimerEntryEntity): AppResult<Unit> =
        upsert(TABLE_TIMER_ENTRIES, "id", timerEntryToJson(entity))

    override suspend fun upsertUserPreferences(
        preferences: UserPreferences,
        updatedAt: Long,
    ): AppResult<Unit> =
        upsert(TABLE_USER_PREFERENCES, "owner_id", preferencesToJson(preferences, updatedAt))

    override suspend fun tombstone(entityType: String, entityId: String, deletedAt: Long): AppResult<Unit> =
        networkResult {
            val table = entityType.toRemoteTable()
            val response = httpClient.patch("${restUrl(table)}?id=eq.$entityId") {
                authorizedHeaders()
                contentType(ContentType.Application.Json)
                setBody(tombstoneJson(deletedAt).toString())
            }
            response.requireSuccess()
        }

    override suspend fun purgeRemoteOnly(deletedAt: Long): AppResult<Int> =
        networkResult {
            val datasetResult = fetchDataset()
            val dataset = datasetResult.getOrNull()
                ?: return@networkResult AppResult.failure(datasetResult.errorOrNull() ?: AppError.Network())
            val count = dataset.countActiveRows()
            val owner = ownerId()
            PURGE_TABLES.forEach { table ->
                val response = httpClient.patch("${restUrl(table)}?owner_id=eq.$owner") {
                    authorizedHeaders()
                    contentType(ContentType.Application.Json)
                    setBody(tombstoneJson(deletedAt).toString())
                }
                response.requireSuccess().errorOrNull()?.let { return@networkResult AppResult.failure(it) }
            }
            AppResult.success(count)
        }

    private suspend fun replaceTags(
        table: String,
        ownerColumn: String,
        ownerId: String,
        tagIds: Set<String>,
        updatedAt: Long,
    ): AppResult<Unit> =
        networkResult {
            val deleteResponse = httpClient.patch("${restUrl(table)}?$ownerColumn=eq.$ownerId") {
                authorizedHeaders()
                contentType(ContentType.Application.Json)
                setBody(tombstoneJson(updatedAt).toString())
            }
            deleteResponse.requireSuccess().errorOrNull()?.let { return@networkResult AppResult.failure(it) }
            tagIds.forEach { tagId ->
                val body = relationToJson(ownerColumn, ownerId, tagId, updatedAt)
                upsert(table, "$ownerColumn,tag_id", body).errorOrNull()
                    ?.let { return@networkResult AppResult.failure(it) }
            }
            AppResult.success(Unit)
        }

    private suspend fun getRows(table: String): List<JsonObject> {
        val response = httpClient.get("${restUrl(table)}?select=*") { authorizedHeaders() }
        response.requireSuccess().errorOrNull()?.let { throw IllegalStateException(it.message) }
        return SupabaseJson.parseToJsonElement(response.bodyAsText()).jsonArray.map { it.jsonObject }
    }

    private suspend fun upsert(table: String, conflictTarget: String, body: JsonObject): AppResult<Unit> =
        networkResult {
            val response = httpClient.post("${restUrl(table)}?on_conflict=$conflictTarget") {
                authorizedHeaders()
                header(HttpHeaders.Prefer, "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            response.requireSuccess()
        }

    private fun io.ktor.client.request.HttpRequestBuilder.authorizedHeaders() {
        val token = accessTokenProvider() ?: error("No Supabase session")
        supabaseHeaders(config, token)
    }

    private fun restUrl(table: String): String =
        "${config.normalizedUrl}/rest/v1/$table"

    private fun ownerId(): String =
        ownerIdProvider() ?: error("No Supabase user")

    private fun collectionToJson(entity: CollectionEntity): JsonObject =
        baseJson(entity.id, entity.updatedAt, entity.deletedAt) { put("name", entity.name) }

    private fun sheetToJson(entity: AlgorithmSheetEntity): JsonObject =
        baseJson(entity.id, entity.updatedAt, entity.deletedAt) {
            put("collection_id", entity.collectionId)
            put("name", entity.name)
        }

    private fun algorithmToJson(entity: AlgorithmEntity): JsonObject =
        baseJson(entity.id, entity.updatedAt, entity.deletedAt) {
            put("sheet_id", entity.sheetId)
            put("name", entity.name)
            put("sequence", entity.sequence)
            put("position", entity.position)
        }

    private fun scrambleToJson(entity: ScrambleEntity): JsonObject =
        baseJson(entity.id, entity.updatedAt, entity.deletedAt) {
            put("collection_id", entity.collectionId)
            put("name", entity.name)
            put("sequence", entity.sequence)
        }

    private fun tagToJson(entity: TagEntity): JsonObject =
        baseJson(entity.id, entity.updatedAt, entity.deletedAt) { put("name", entity.name) }

    private fun timerEntryToJson(entity: TimerEntryEntity): JsonObject =
        baseJson(entity.id, entity.updatedAt, entity.deletedAt) {
            put("duration_millis", entity.durationMillis)
            put("solved_at", entity.solvedAt)
        }

    private fun preferencesToJson(preferences: UserPreferences, updatedAt: Long): JsonObject =
        buildJsonObject {
            put("owner_id", ownerId())
            put("app_appearance", preferences.appAppearance.name)
            put("cube_theme", preferences.cubeTheme.name)
            put("local_cube_cache_enabled", preferences.localCubeCacheEnabled)
            put("session_persistence_enabled", preferences.sessionPersistenceEnabled)
            put("updated_at", updatedAt)
            put("deleted_at", JsonNull)
        }

    private fun relationToJson(
        ownerColumn: String,
        rowOwnerId: String,
        tagId: String,
        updatedAt: Long,
    ): JsonObject =
        buildJsonObject {
            put("owner_id", ownerId())
            put(ownerColumn, rowOwnerId)
            put("tag_id", tagId)
            put("updated_at", updatedAt)
            put("deleted_at", JsonNull)
        }

    private fun baseJson(
        id: String,
        updatedAt: Long,
        deletedAt: Long?,
        content: JsonObjectBuilder.() -> Unit,
    ): JsonObject =
        buildJsonObject {
            put("id", id)
            put("owner_id", ownerId())
            content()
            put("updated_at", updatedAt)
            putNullable("deleted_at", deletedAt)
        }

    private fun tombstoneJson(deletedAt: Long): JsonObject =
        buildJsonObject {
            put("updated_at", deletedAt)
            put("deleted_at", deletedAt)
        }

    private fun collectionFromJson(json: JsonObject): CollectionEntity =
        CollectionEntity(
            json.string("id"),
            json.string("name"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun sheetFromJson(json: JsonObject): AlgorithmSheetEntity =
        AlgorithmSheetEntity(
            json.string("id"),
            json.string("collection_id"),
            json.string("name"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun algorithmFromJson(json: JsonObject): AlgorithmEntity =
        AlgorithmEntity(
            json.string("id"),
            json.string("sheet_id"),
            json.string("name"),
            json.string("sequence"),
            json.int("position"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun scrambleFromJson(json: JsonObject): ScrambleEntity =
        ScrambleEntity(
            json.string("id"),
            json.string("collection_id"),
            json.string("name"),
            json.string("sequence"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun tagFromJson(json: JsonObject): TagEntity =
        TagEntity(json.string("id"), json.string("name"), json.long("updated_at"), json.nullableLong("deleted_at"))

    private fun sheetTagFromJson(json: JsonObject): CloudSheetTag =
        CloudSheetTag(
            json.string("sheet_id"),
            json.string("tag_id"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun scrambleTagFromJson(json: JsonObject): CloudScrambleTag =
        CloudScrambleTag(
            json.string("scramble_id"),
            json.string("tag_id"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun timerEntryFromJson(json: JsonObject): TimerEntryEntity =
        TimerEntryEntity(
            json.string("id"),
            json.long("duration_millis"),
            json.long("solved_at"),
            json.long("updated_at"),
            json.nullableLong("deleted_at"),
        )

    private fun preferencesFromJson(json: JsonObject): CloudUserPreferences =
        CloudUserPreferences(
            preferences = UserPreferences(
                appAppearance = enumValueOf<AppAppearance>(json.string("app_appearance")),
                cubeTheme = enumValueOf<CubeTheme>(json.string("cube_theme")),
                localCubeCacheEnabled = json.boolean("local_cube_cache_enabled"),
                sessionPersistenceEnabled = json.boolean("session_persistence_enabled"),
            ),
            updatedAt = json.long("updated_at"),
            deletedAt = json.nullableLong("deleted_at"),
        )

    private fun CloudDataset.countActiveRows(): Int =
        collections.count { it.deletedAt == null } +
            sheets.count { it.deletedAt == null } +
            algorithms.count { it.deletedAt == null } +
            scrambles.count { it.deletedAt == null } +
            tags.count { it.deletedAt == null } +
            sheetTags.count { it.deletedAt == null } +
            scrambleTags.count { it.deletedAt == null } +
            timerEntries.count { it.deletedAt == null } +
            if (userPreferences?.deletedAt == null && userPreferences != null) 1 else 0

    private fun JsonObject.string(key: String): String =
        getValue(key).jsonPrimitive.content

    private fun JsonObject.long(key: String): Long =
        getValue(key).jsonPrimitive.long

    private fun JsonObject.int(key: String): Int =
        getValue(key).jsonPrimitive.int

    private fun JsonObject.boolean(key: String): Boolean =
        getValue(key).jsonPrimitive.boolean

    private fun JsonObject.nullableLong(key: String): Long? {
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.long
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: Long?) {
        if (value == null) {
            put(key, JsonNull)
        } else {
            put(key, value)
        }
    }

    private fun String.toRemoteTable(): String =
        when (this) {
            "collection" -> TABLE_COLLECTIONS
            "algorithm_sheet" -> TABLE_SHEETS
            "algorithm" -> TABLE_ALGORITHMS
            "scramble" -> TABLE_SCRAMBLES
            "tag" -> TABLE_TAGS
            "timer_entry" -> TABLE_TIMER_ENTRIES
            "user_preferences" -> TABLE_USER_PREFERENCES
            else -> error("Unknown cloud entity type: $this")
        }

    private companion object {
        const val TABLE_COLLECTIONS = "collections"
        const val TABLE_SHEETS = "algorithm_sheets"
        const val TABLE_ALGORITHMS = "algorithms"
        const val TABLE_SCRAMBLES = "scrambles"
        const val TABLE_TAGS = "tags"
        const val TABLE_SHEET_TAGS = "sheet_tags"
        const val TABLE_SCRAMBLE_TAGS = "scramble_tags"
        const val TABLE_TIMER_ENTRIES = "timer_entries"
        const val TABLE_USER_PREFERENCES = "user_preferences"

        val PURGE_TABLES = listOf(
            TABLE_COLLECTIONS,
            TABLE_SHEETS,
            TABLE_ALGORITHMS,
            TABLE_SCRAMBLES,
            TABLE_TAGS,
            TABLE_SHEET_TAGS,
            TABLE_SCRAMBLE_TAGS,
            TABLE_TIMER_ENTRIES,
            TABLE_USER_PREFERENCES,
        )
    }
}

private typealias JsonObjectBuilder = kotlinx.serialization.json.JsonObjectBuilder
