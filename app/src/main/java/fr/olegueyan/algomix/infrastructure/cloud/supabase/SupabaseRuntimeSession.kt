package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.domain.cloud.CloudSession

internal object SupabaseRuntimeSession {
    var cloudSession: CloudSession? = null
    var accessToken: String? = null

    fun clear() {
        cloudSession = null
        accessToken = null
    }
}
