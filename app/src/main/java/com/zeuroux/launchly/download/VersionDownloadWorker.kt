package com.zeuroux.launchly.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.storage.StorageManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.R
import com.zeuroux.launchly.activities.MainActivity
import com.zeuroux.launchly.data.DownloadRecordEntity
import com.zeuroux.launchly.gplay.GPlayArtifact
import com.zeuroux.launchly.gplay.GPlayDeliveryException
import com.zeuroux.launchly.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.coroutines.coroutineContext

class VersionDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val container = (appContext as LaunchlyApp).container
    private val records = container.database.downloadRecordDao()
    private val versionId = inputData.getString(KEY_VERSION_ID).orEmpty()
    private var lastProgressAt = 0L
    private var previousBytes = 0L
    private var previousSampleAt = System.currentTimeMillis()

    override suspend fun doWork(): Result = VersionFileLocks.withLock(versionId) {
        performDownload()
    }

    private suspend fun performDownload(): Result {
        val version = container.managedVersions.get(versionId)
            ?: return Result.failure(failureData("MISSING_VERSION", "The managed version no longer exists."))
        return try {
            setForeground(createForegroundInfo(0L, null))
            records.upsert(currentRecord().copy(status = DownloadStatus.DOWNLOADING, updatedAt = now()))
            val artifacts = container.gPlayService.getArtifacts(version.versionCode)
            if (artifacts.isEmpty()) throw DownloadFailure("EMPTY_DELIVERY", "Google returned no APK files.")
            val safeArtifacts = try {
                ArtifactNamePolicy.sanitize(artifacts)
            } catch (failure: IllegalArgumentException) {
                throw DownloadFailure("UNSAFE_OR_DUPLICATE_NAME", failure.message ?: "Google returned an invalid APK filename.")
            }
            val total = safeArtifacts.map { it.expectedSize }.takeIf { values -> values.all { it > 0L } }?.sum()
            var completedBytes = 0L
            for (artifact in safeArtifacts) {
                completedBytes += downloadArtifact(artifact, completedBytes, total)
            }
            val apkFiles = versionDirectory().listFiles { file -> file.extension.equals("apk", true) }
                ?.sortedBy { it.name }
                .orEmpty()
            container.apkSetValidator.validate(apkFiles, version.versionCode, version.architecture)
            records.upsert(
                currentRecord().copy(
                    status = DownloadStatus.READY,
                    bytesDownloaded = completedBytes,
                    totalBytes = total ?: completedBytes,
                    speedBytesPerSecond = null,
                    failureType = null,
                    failureMessage = null,
                    updatedAt = now()
                )
            )
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: GPlayDeliveryException) {
            handleFailure(DownloadFailure(failure.type, failure.message, failure.retryable))
        } catch (failure: DownloadFailure) {
            handleFailure(failure)
        } catch (failure: IOException) {
            val storageFailure = failure.message?.contains("space", true) == true || isStorageExhausted()
            handleFailure(
                DownloadFailure(
                    if (storageFailure) "STORAGE_FULL" else "NETWORK",
                    if (storageFailure) "There is not enough storage to finish this download."
                    else failure.message ?: "The network download was interrupted.",
                    retryable = !storageFailure
                )
            )
        } catch (failure: Exception) {
            handleFailure(DownloadFailure("UNKNOWN", failure.message ?: "The download failed."))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo(0L, null)

    private suspend fun downloadArtifact(
        artifact: GPlayArtifact,
        alreadyCompleted: Long,
        totalBytes: Long?
    ): Long = withContext(Dispatchers.IO) {
        val directory = versionDirectory().apply { mkdirs() }
        val finalFile = safeFile(directory, artifact.name)
        val partFile = safeFile(directory, "${artifact.name}.part")
        val validatorFile = safeFile(directory, "${artifact.name}.part.meta")
        if (finalFile.isFile && (artifact.expectedSize <= 0L || finalFile.length() == artifact.expectedSize)) {
            return@withContext finalFile.length()
        }
        if (finalFile.exists()) finalFile.delete()

        var existing = partFile.length().coerceAtLeast(0L)
        if (artifact.expectedSize > 0 && existing > artifact.expectedSize) {
            partFile.delete()
            existing = 0L
        }
        val validator = validatorFile.takeIf { existing > 0L && it.isFile }
            ?.readText()?.trim()?.takeIf(String::isNotBlank)
        if (existing > 0L && validator == null) {
            partFile.delete()
            existing = 0L
        }
        val request = Request.Builder().url(artifact.url).apply {
            if (existing > 0L) {
                header("Range", "bytes=$existing-")
                header("If-Range", validator.orEmpty())
            }
        }.build()

        container.okHttpClient.newCall(request).execute().use { response ->
            val append = ResumePolicy.canAppend(existing, response.code, response.header("Content-Range"))
            if (!response.isSuccessful) {
                throw DownloadFailure(
                    "HTTP_${response.code}",
                    "Downloading ${artifact.name} failed with HTTP ${response.code}.",
                    response.code >= 500 || response.code == 408 || response.code == 429
                )
            }
            if (!append) existing = 0L
            val body = response.body ?: throw DownloadFailure("EMPTY_FILE", "${artifact.name} had no response body.")
            val responseValidator = response.header("ETag") ?: response.header("Last-Modified")
            if (!append && responseValidator.isNullOrBlank()) validatorFile.delete()
            else if (!responseValidator.isNullOrBlank()) validatorFile.writeText(responseValidator)
            RandomAccessFile(partFile, "rw").use { output ->
                if (append) output.seek(existing) else output.setLength(0L)
                body.byteStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = existing
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        output.write(buffer, 0, count)
                        downloaded += count
                        publishProgress(alreadyCompleted + downloaded, totalBytes)
                    }
                    output.fd.sync()
                }
            }
        }
        val actualSize = partFile.length()
        if (actualSize <= 0L) throw DownloadFailure("EMPTY_FILE", "${artifact.name} was empty.")
        if (artifact.expectedSize > 0L && actualSize != artifact.expectedSize) {
            throw DownloadFailure(
                "SIZE_MISMATCH",
                "${artifact.name} was $actualSize bytes; ${artifact.expectedSize} bytes were expected.",
                true
            )
        }
        Files.move(
            partFile.toPath(), finalFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
        )
        validatorFile.delete()
        actualSize
    }

    private suspend fun publishProgress(bytes: Long, total: Long?) {
        val timestamp = now()
        if (timestamp - lastProgressAt < PROGRESS_INTERVAL_MS) return
        val elapsed = (timestamp - previousSampleAt).coerceAtLeast(1L)
        val speed = ((bytes - previousBytes).coerceAtLeast(0L) * 1000L / elapsed).takeIf { it > 0L }
        records.upsert(
            currentRecord().copy(
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = bytes.coerceAtLeast(0L),
                totalBytes = total?.coerceAtLeast(bytes),
                speedBytesPerSecond = speed,
                updatedAt = timestamp
            )
        )
        setForeground(createForegroundInfo(bytes, total))
        previousBytes = bytes
        previousSampleAt = timestamp
        lastProgressAt = timestamp
    }

    private suspend fun handleFailure(failure: DownloadFailure): Result {
        val willRetry = failure.retryable && runAttemptCount < MAX_RETRIES
        records.upsert(
            currentRecord().copy(
                status = if (willRetry) DownloadStatus.QUEUED else DownloadStatus.FAILED,
                speedBytesPerSecond = null,
                failureType = failure.type,
                failureMessage = failure.message,
                updatedAt = now()
            )
        )
        return if (willRetry) Result.retry() else Result.failure(failureData(failure.type, failure.message))
    }

    private suspend fun currentRecord(): DownloadRecordEntity = records.get(versionId) ?: DownloadRecordEntity(
        versionId, id.toString(), DownloadStatus.QUEUED, 0L, null, null, null, null, now()
    )

    private fun safeFile(directory: File, name: String): File {
        val file = File(directory, name).canonicalFile
        if (file.parentFile != directory.canonicalFile) {
            throw DownloadFailure("UNSAFE_PATH", "An APK path escaped the managed version directory.")
        }
        return file
    }

    private fun versionDirectory() = File(applicationContext.filesDir, "versions/$versionId")

    private fun isStorageExhausted(): Boolean = runCatching {
        val storageManager = applicationContext.getSystemService(StorageManager::class.java)
        val storageUuid = storageManager.getUuidForPath(versionDirectory())
        storageManager.getAllocatableBytes(storageUuid) < MIN_FREE_BYTES
    }.getOrDefault(false)

    private fun createForegroundInfo(bytes: Long, total: Long?): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, applicationContext.getString(R.string.download_channel_name), NotificationManager.IMPORTANCE_LOW)
        )
        val progress = total?.takeIf { it > 0L }?.let { (bytes * 100L / it).toInt().coerceIn(0, 100) }
        val launchIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, notificationId(), launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download_notification)
            .setContentTitle(applicationContext.getString(R.string.downloading_minecraft))
            .setContentText(progress?.let { "$it%" } ?: applicationContext.getString(R.string.download_size_unknown))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress ?: 0, progress == null)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId(), notification)
        }
    }

    private fun notificationId(): Int = NOTIFICATION_ID_BASE + (versionId.hashCode() and 0x7fff)

    private fun failureData(type: String, message: String) = Data.Builder()
        .putString("failure_type", type)
        .putString("failure_message", message)
        .build()

    private class DownloadFailure(
        val type: String,
        override val message: String,
        val retryable: Boolean = false
    ) : Exception(message)

    companion object {
        const val KEY_VERSION_ID = "managed_version_id"
        private const val CHANNEL_ID = "persistent_downloads"
        private const val NOTIFICATION_ID_BASE = 3102
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_INTERVAL_MS = 500L
        private const val MAX_RETRIES = 3
        private const val MIN_FREE_BYTES = 1024L * 1024L
        private fun now() = System.currentTimeMillis()
    }
}
