package fr.olegueyan.algomix.infrastructure.cloud.supabase

import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalPersistenceDao
import fr.olegueyan.algomix.infrastructure.persistence.local.LocalSettingsRepository
import io.ktor.client.HttpClient

data class SupabaseGateways(
    val authGateway: SupabaseAuthGateway,
    val syncGateway: SupabaseCloudSyncGateway,
)

object SupabaseClientFactory {
    fun createGateways(
        config: SupabaseConfig,
        dao: LocalPersistenceDao,
        settingsRepository: LocalSettingsRepository,
        clockProvider: ClockProvider = SystemClockProvider,
        httpClient: HttpClient = defaultSupabaseHttpClient(),
        tokenStore: SupabaseTokenStore? = null,
    ): SupabaseGateways? {
        if (!config.isConfigured) {
            return null
        }
        val authGateway = SupabaseAuthGateway(config, httpClient, clockProvider, tokenStore)
        val remoteDataSource = SupabaseRestRemoteDataSource(
            config = config,
            accessTokenProvider = authGateway::currentAccessToken,
            ownerIdProvider = authGateway::currentUserId,
            httpClient = httpClient,
        )
        return SupabaseGateways(
            authGateway = authGateway,
            syncGateway = SupabaseCloudSyncGateway(
                dao = dao,
                settingsRepository = settingsRepository,
                remoteDataSource = remoteDataSource,
                clockProvider = clockProvider,
            ),
        )
    }
}
