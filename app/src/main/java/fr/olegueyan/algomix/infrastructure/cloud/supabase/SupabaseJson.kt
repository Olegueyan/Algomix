package fr.olegueyan.algomix.infrastructure.cloud.supabase

import kotlinx.serialization.json.Json

internal val SupabaseJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
