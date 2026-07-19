package com.zeuroux.launchly.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.zeuroux.launchly.data.DownloadRecordDao
import com.zeuroux.launchly.data.DownloadRecordEntity
import com.zeuroux.launchly.data.ManagedVersionRepository
import com.zeuroux.launchly.model.DownloadRecord
import com.zeuroux.launchly.model.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

interface DownloadCoordinator {
    val downloads: Flow<List<DownloadRecord>>
    suspend fun enqueue(versionId: String)
    suspend fun pause(versionId: String)
    suspend fun resume(versionId: String)
    suspend fun cancel(versionId: String)
    suspend fun delete(versionId: String)
    suspend fun reconcile()

    companion object {
        const val LAUNCHLY_WORK_TAG = "launchly_download"
    }
}

class WorkManagerDownloadCoordinator(
    private val context: Context,
    private val workManager: WorkManager,
    private val managedVersions: ManagedVersionRepository,
    private val records: DownloadRecordDao
) : DownloadCoordinator {
    override val downloads: Flow<List<DownloadRecord>> = records.observeAll().map { values ->
        values.map(DownloadRecordEntity::toModel)
    }

    override suspend fun enqueue(versionId: String) = withContext(Dispatchers.IO) {
        val version = managedVersions.get(versionId) ?: return@withContext
        val request = OneTimeWorkRequestBuilder<VersionDownloadWorker>()
            .setInputData(Data.Builder().putString(VersionDownloadWorker.KEY_VERSION_ID, version.id).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(DownloadCoordinator.LAUNCHLY_WORK_TAG)
            .addTag(tagFor(versionId))
            .build()
        records.upsert(
            DownloadRecordEntity(
                versionId = versionId,
                workId = request.id.toString(),
                status = DownloadStatus.QUEUED,
                bytesDownloaded = existingPartBytes(versionId),
                totalBytes = records.get(versionId)?.totalBytes,
                speedBytesPerSecond = null,
                failureType = null,
                failureMessage = null,
                updatedAt = System.currentTimeMillis()
            )
        )
        workManager.enqueueUniqueWork(uniqueName(versionId), ExistingWorkPolicy.REPLACE, request)
        Unit
    }

    override suspend fun pause(versionId: String) {
        val current = records.get(versionId) ?: return
        records.upsert(current.copy(status = DownloadStatus.PAUSED, speedBytesPerSecond = null, updatedAt = now()))
        awaitOperation { workManager.cancelUniqueWork(uniqueName(versionId)) }
        VersionFileLocks.withLock(versionId) {
            records.get(versionId)?.let {
                records.upsert(it.copy(status = DownloadStatus.PAUSED, speedBytesPerSecond = null, updatedAt = now()))
            }
        }
    }

    override suspend fun resume(versionId: String) = enqueue(versionId)

    override suspend fun cancel(versionId: String) {
        records.get(versionId)?.let {
            records.upsert(it.copy(status = DownloadStatus.CANCELLED, speedBytesPerSecond = null, updatedAt = now()))
        }
        awaitOperation { workManager.cancelUniqueWork(uniqueName(versionId)) }
        VersionFileLocks.withLock(versionId) {
            records.get(versionId)?.let {
                records.upsert(it.copy(status = DownloadStatus.CANCELLED, speedBytesPerSecond = null, updatedAt = now()))
            }
            withContext(Dispatchers.IO) {
                versionDirectory(versionId).listFiles { file ->
                    file.name.endsWith(".part") || file.name.endsWith(".part.meta")
                }?.forEach(File::delete)
            }
        }
    }

    override suspend fun delete(versionId: String) {
        awaitOperation { workManager.cancelUniqueWork(uniqueName(versionId)) }
        VersionFileLocks.withLock(versionId) {
            withContext(Dispatchers.IO) { deleteVersionDirectory(versionId) }
            managedVersions.delete(versionId)
        }
    }

    override suspend fun reconcile() = withContext(Dispatchers.IO) {
        val workInfos = runCatching {
            workManager.getWorkInfosByTag(DownloadCoordinator.LAUNCHLY_WORK_TAG).get(10, TimeUnit.SECONDS)
        }.getOrDefault(emptyList())
        val liveIds = workInfos
            .filter { !it.state.isFinished }
            .mapTo(mutableSetOf()) { it.id.toString() }
        downloadsSnapshot().forEach { record ->
            if (record.status in setOf(DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING) && record.workId !in liveIds) {
                records.upsert(
                    record.copy(
                        status = DownloadStatus.PAUSED,
                        speedBytesPerSecond = null,
                        failureType = "INTERRUPTED",
                        failureMessage = "The previous download was interrupted and can be resumed.",
                        updatedAt = now()
                    )
                )
            }
        }
    }

    private suspend fun downloadsSnapshot(): List<DownloadRecordEntity> =
        records.observeAll().map { it }.kotlinxFirst()

    private fun existingPartBytes(versionId: String): Long =
        versionDirectory(versionId).listFiles { file ->
            file.name.endsWith(".part") || file.extension.equals("apk", true)
        }
            ?.sumOf(File::length) ?: 0L

    private fun versionDirectory(versionId: String) = File(context.filesDir, "versions/$versionId")

    private fun deleteVersionDirectory(versionId: String) {
        val root = File(context.filesDir, "versions").canonicalFile
        val target = versionDirectory(versionId).canonicalFile
        if (target.parentFile == root && target.name == versionId && UUID.fromString(versionId).toString() == versionId) {
            target.deleteRecursively()
        }
    }

    private suspend fun awaitOperation(operation: () -> androidx.work.Operation) = withContext(Dispatchers.IO) {
        runCatching { operation().result.get(10, TimeUnit.SECONDS) }
    }

    companion object {
        fun uniqueName(versionId: String) = "launchly-download-$versionId"
        fun tagFor(versionId: String) = "launchly-version-$versionId"
        private fun now() = System.currentTimeMillis()
    }
}

private suspend fun <T> Flow<T>.kotlinxFirst(): T = first()
