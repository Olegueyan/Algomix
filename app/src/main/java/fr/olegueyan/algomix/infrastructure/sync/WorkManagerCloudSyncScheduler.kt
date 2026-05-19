package fr.olegueyan.algomix.infrastructure.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class WorkManagerCloudSyncScheduler(
    context: Context,
) : CloudSyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun schedulePush() {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val WORK_NAME = "algomix-cloud-sync"
    }
}
