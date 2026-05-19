package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CloudAuthGateway
import fr.olegueyan.algomix.domain.cloud.CloudSession
import fr.olegueyan.algomix.domain.cloud.CloudUser
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class SupabaseAuthGateway(
    private val config: SupabaseConfig,
    private val httpClient: HttpClient = defaultSupabaseHttpClient(),
    private val clockProvider: ClockProvider = SystemClockProvider,
) : CloudAuthGateway {
    private var session: CloudSession? = SupabaseRuntimeSession.cloudSession
    private var accessToken: String? = SupabaseRuntimeSession.accessToken

    internal fun currentAccessToken(): String? = accessToken

    internal fun currentUserId(): String? = session?.user?.id

    override suspend fun currentSession(): AppResult<CloudSession?> =
        networkResult {
            val token = accessToken ?: return@networkResult AppResult.success(session)
            val response = httpClient.get("${config.normalizedUrl}/auth/v1/user") {
                supabaseHeaders(config, token)
            }
            if (response.status == HttpStatusCode.Unauthorized) {
                session = null
                accessToken = null
                SupabaseRuntimeSession.clear()
                return@networkResult AppResult.success(null)
            }
            response.requireSuccess().errorOrNull()?.let { return@networkResult AppResult.failure(it) }
            session = parseUser(response.bodyAsText(), session?.expiresAt)
            SupabaseRuntimeSession.cloudSession = session
            AppResult.success(session)
        }

    override suspend fun signIn(email: String, password: String): AppResult<CloudSession> =
        networkResult {
            val response = httpClient.post("${config.normalizedUrl}/auth/v1/token?grant_type=password") {
                supabaseHeaders(config)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("email", email)
                        put("password", password)
                    }.toString(),
                )
            }
            response.requireSuccess().errorOrNull()?.let { return@networkResult AppResult.failure(it) }
            AppResult.success(importSession(response.bodyAsText()))
        }

    override suspend fun createAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): AppResult<CloudSession> =
        networkResult {
            val response = httpClient.post("${config.normalizedUrl}/auth/v1/signup") {
                supabaseHeaders(config)
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("email", email)
                        put("password", password)
                        put(
                            "data",
                            buildJsonObject {
                                put("first_name", firstName)
                                put("last_name", lastName)
                            },
                        )
                    }.toString(),
                )
            }
            response.requireSuccess().errorOrNull()?.let { return@networkResult AppResult.failure(it) }
            val body = SupabaseJson.parseToJsonElement(response.bodyAsText()).jsonObject
            val token = body["access_token"]?.jsonPrimitive?.content
            if (token.isNullOrBlank()) {
                return@networkResult AppResult.failure(
                    AppError.Unauthorized("Compte cree, confirmation email requise avant connexion"),
                )
            }
            AppResult.success(importSession(body))
        }

    override suspend fun signOut(): AppResult<Unit> =
        networkResult {
            val token = accessToken
            if (token != null) {
                val response = httpClient.post("${config.normalizedUrl}/auth/v1/logout") {
                    supabaseHeaders(config, token)
                }
                response.requireSuccess().errorOrNull()?.let { return@networkResult AppResult.failure(it) }
            }
            session = null
            accessToken = null
            SupabaseRuntimeSession.clear()
            AppResult.success(Unit)
        }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> {
        @Suppress("UNUSED_VARIABLE")
        val ignoredCurrentPassword = currentPassword
        return networkResult {
            val token = accessToken ?: return@networkResult AppResult.failure(AppError.Unauthorized())
            val response = httpClient.put("${config.normalizedUrl}/auth/v1/user") {
                supabaseHeaders(config, token)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject { put("password", newPassword) }.toString())
            }
            response.requireSuccess()
        }
    }

    private fun importSession(responseBody: String): CloudSession =
        importSession(SupabaseJson.parseToJsonElement(responseBody).jsonObject)

    private fun importSession(body: JsonObject): CloudSession {
        accessToken = body["access_token"]?.jsonPrimitive?.content
        val expiresIn = body["expires_in"]?.jsonPrimitive?.longOrNull
        val authenticatedAt = clockProvider.now()
        session = parseUserObject(
            body.getValue("user").jsonObject,
            expiresIn?.let { authenticatedAt.plusSeconds(it) },
        )
        SupabaseRuntimeSession.accessToken = accessToken
        SupabaseRuntimeSession.cloudSession = session
        return session ?: error("Session should have been parsed")
    }

    private fun parseUser(responseBody: String, expiresAt: java.time.Instant?): CloudSession =
        parseUserObject(SupabaseJson.parseToJsonElement(responseBody).jsonObject, expiresAt)

    private fun parseUserObject(user: JsonObject, expiresAt: java.time.Instant?): CloudSession {
        val metadata = user["user_metadata"]?.jsonObject
        return CloudSession(
            user = CloudUser(
                id = user.getValue("id").jsonPrimitive.content,
                email = user["email"]?.jsonPrimitive?.content.orEmpty(),
                firstName = metadata?.get("first_name")?.jsonPrimitive?.content,
                lastName = metadata?.get("last_name")?.jsonPrimitive?.content,
            ),
            authenticatedAt = clockProvider.now(),
            expiresAt = expiresAt,
        )
    }
}
