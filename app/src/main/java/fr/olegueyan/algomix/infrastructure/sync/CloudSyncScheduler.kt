package fr.olegueyan.algomix.infrastructure.sync

interface CloudSyncScheduler {
    fun schedulePush()
}

object NoOpCloudSyncScheduler : CloudSyncScheduler {
    override fun schedulePush() = Unit
}
