package fr.olegueyan.algomix.infrastructure.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import fr.olegueyan.algomix.infrastructure.di.AndroidAppContainerFactory

class CloudSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val gateway = AndroidAppContainerFactory.create(applicationContext)
            .cloudSyncGateway()
            .getOrNull()
            ?: return Result.success()
        return if (gateway.pushPendingChanges().isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
