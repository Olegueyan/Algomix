package fr.olegueyan.algomix.application.di

import fr.olegueyan.algomix.application.core.AppError
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.core.ClockProvider
import fr.olegueyan.algomix.application.core.SystemClockProvider
import fr.olegueyan.algomix.application.port.CloudAuthGateway
import fr.olegueyan.algomix.application.port.CloudSyncGateway
import fr.olegueyan.algomix.application.port.CubeScanner
import fr.olegueyan.algomix.application.port.CubeSessionRepository
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.PdfExporter
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.application.port.TimerRepository

@Suppress("LongParameterList")
class AppContainer(
    val clockProvider: ClockProvider = SystemClockProvider,
    private val configuredCubeSessionRepository: CubeSessionRepository? = null,
    private val configuredLibraryRepository: LibraryRepository? = null,
    private val configuredTimerRepository: TimerRepository? = null,
    private val configuredSettingsRepository: SettingsRepository? = null,
    private val configuredCloudAuthGateway: CloudAuthGateway? = null,
    private val configuredCloudSyncGateway: CloudSyncGateway? = null,
    private val configuredPdfExporter: PdfExporter? = null,
    private val configuredCubeScanner: CubeScanner? = null,
) {
    fun cubeSessionRepository(): AppResult<CubeSessionRepository> =
        configuredCubeSessionRepository.configured("CubeSessionRepository")

    fun libraryRepository(): AppResult<LibraryRepository> =
        configuredLibraryRepository.configured("LibraryRepository")

    fun timerRepository(): AppResult<TimerRepository> =
        configuredTimerRepository.configured("TimerRepository")

    fun settingsRepository(): AppResult<SettingsRepository> =
        configuredSettingsRepository.configured("SettingsRepository")

    fun cloudAuthGateway(): AppResult<CloudAuthGateway> =
        configuredCloudAuthGateway.configured("CloudAuthGateway")

    fun cloudSyncGateway(): AppResult<CloudSyncGateway> =
        configuredCloudSyncGateway.configured("CloudSyncGateway")

    fun pdfExporter(): AppResult<PdfExporter> =
        configuredPdfExporter.configured("PdfExporter")

    fun cubeScanner(): AppResult<CubeScanner> =
        configuredCubeScanner.configured("CubeScanner")

    private fun <T> T?.configured(name: String): AppResult<T> =
        if (this != null) {
            AppResult.success(this)
        } else {
            AppResult.failure(AppError.Unknown("$name is not configured yet"))
        }
}
