package fr.olegueyan.algomix.infrastructure.cloud.supabase

data class SupabaseConfig(
    val url: String,
    val publishableKey: String,
) {
    val isConfigured: Boolean
        get() = url.isNotBlank() && publishableKey.isNotBlank()

    val normalizedUrl: String
        get() = url.trim().trimEnd('/')
}
