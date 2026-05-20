@file:Suppress("TooGenericExceptionCaught")

package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

internal fun defaultSupabaseHttpClient(): HttpClient =
    HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
    }

internal fun io.ktor.client.request.HttpRequestBuilder.supabaseHeaders(
    config: SupabaseConfig,
    bearerToken: String? = null,
) {
    header("apikey", config.publishableKey)
    header(HttpHeaders.Accept, "application/json")
    bearerToken?.let { token -> header(HttpHeaders.Authorization, "Bearer $token") }
}

internal suspend fun <T> networkResult(block: suspend () -> AppResult<T>): AppResult<T> =
    try {
        block()
    } catch (error: Exception) {
        AppResult.failure(AppError.Network(detail = error.message, cause = error))
    }

internal suspend fun HttpResponse.requireSuccess(): AppResult<Unit> =
    if (status.value in HttpStatusCode.OK.value..299) {
        AppResult.success(Unit)
    } else {
        AppResult.failure(AppError.Network(bodyAsText().ifBlank { "Supabase request failed: $status" }))
    }
