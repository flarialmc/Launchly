package com.zeuroux.launchly.download

import com.zeuroux.launchly.gplay.GPlayArtifact
import kotlinx.coroutines.sync.Mutex
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

internal fun OkHttpClient.forPersistentDownloads(): OkHttpClient = newBuilder()
    // Large Minecraft artifacts may transfer for many minutes. Keep the base
    // client's connect and idle-read timeouts, but never cap the whole call.
    .callTimeout(0, TimeUnit.MILLISECONDS)
    .build()

internal object VersionFileLocks {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun <T> withLock(versionId: String, block: suspend () -> T): T {
        val mutex = locks.getOrPut(versionId) { Mutex() }
        mutex.lock()
        return try {
            block()
        } finally {
            mutex.unlock()
        }
    }
}

internal object ArtifactNamePolicy {
    fun sanitize(values: List<GPlayArtifact>): List<GPlayArtifact> {
        val seen = mutableSetOf<String>()
        return values.map { artifact ->
            val sanitized = artifact.name.substringAfterLast('/').substringAfterLast('\\')
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            require(sanitized.isNotBlank() && sanitized !in setOf(".", "..") && sanitized.endsWith(".apk", true)) {
                "Google returned an invalid APK filename."
            }
            require(seen.add(sanitized.lowercase())) { "Google returned duplicate APK filenames." }
            artifact.copy(name = sanitized)
        }
    }
}

internal object ArtifactUrlPolicy {
    fun requireHttps(value: String): HttpUrl {
        val url = value.toHttpUrlOrNull()
        require(url?.isHttps == true) { "Google returned an unsafe APK URL." }
        return url
    }
}

internal object ResumePolicy {
    private val contentRangePattern = Regex("""bytes (\d+)-(\d+)/(\d+|\*)""", RegexOption.IGNORE_CASE)

    fun canAppend(existingBytes: Long, responseCode: Int, contentRange: String?): Boolean {
        if (existingBytes <= 0L || responseCode != 206) return false
        val match = contentRangePattern.matchEntire(contentRange?.trim().orEmpty()) ?: return false
        val start = match.groupValues[1].toLongOrNull() ?: return false
        val end = match.groupValues[2].toLongOrNull() ?: return false
        val total = match.groupValues[3].takeUnless { it == "*" }?.toLongOrNull()
        return start == existingBytes && end >= start && (total == null || total > end)
    }
}

internal object ArtifactCachePolicy {
    fun isReusableFinal(file: File, expectedSize: Long): Boolean =
        expectedSize > 0L && file.isFile && file.length() == expectedSize

    fun invalidateFinalApks(directory: File): Int {
        val files = directory.listFiles { file -> file.isFile && file.extension.equals("apk", true) }.orEmpty()
        return files.count(File::delete)
    }

    fun invalidatePartial(partFile: File, validatorFile: File) {
        partFile.delete()
        validatorFile.delete()
    }

    fun promoteCompletePartial(partFile: File, finalFile: File, validatorFile: File) {
        require(partFile.isFile) { "The completed partial APK is missing." }
        try {
            java.nio.file.Files.move(
                partFile.toPath(),
                finalFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            java.nio.file.Files.move(
                partFile.toPath(),
                finalFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        validatorFile.delete()
    }
}

internal object StoragePolicy {
    const val RESERVE_BYTES = 64L * 1024L * 1024L
    const val MAX_ARTIFACT_BYTES = 4L * 1024L * 1024L * 1024L
    const val MAX_TOTAL_BYTES = 8L * 1024L * 1024L * 1024L
    const val MAX_ARTIFACT_COUNT = 128

    fun validateDelivery(artifacts: List<GPlayArtifact>) {
        require(artifacts.size <= MAX_ARTIFACT_COUNT) { "Google returned too many APK files." }
        require(artifacts.none { it.expectedSize > MAX_ARTIFACT_BYTES }) {
            "Google returned an APK larger than Launchly's safety limit."
        }
        val knownTotal = artifacts.asSequence()
            .map(GPlayArtifact::expectedSize)
            .filter { it > 0L }
            .fold(0L, ::saturatedAdd)
        require(knownTotal <= MAX_TOTAL_BYTES) { "Google returned an APK set larger than Launchly's safety limit." }
    }

    fun streamLimit(artifact: GPlayArtifact, alreadyCompleted: Long): Long {
        val artifactLimit = artifact.expectedSize.takeIf { it > 0L } ?: MAX_ARTIFACT_BYTES
        val consumed = alreadyCompleted.coerceIn(0L, MAX_TOTAL_BYTES)
        return minOf(artifactLimit, MAX_TOTAL_BYTES - consumed)
    }

    fun remainingBytes(
        artifacts: List<GPlayArtifact>,
        cachedBytes: (GPlayArtifact) -> Long
    ): Long? {
        if (artifacts.any { it.expectedSize <= 0L }) return null
        return artifacts.fold(0L) { total, artifact ->
            val cached = cachedBytes(artifact).coerceIn(0L, artifact.expectedSize)
            saturatedAdd(total, artifact.expectedSize - cached)
        }
    }

    fun hasEnoughSpace(allocatableBytes: Long, remainingBytes: Long): Boolean =
        remainingBytes <= 0L || allocatableBytes >= saturatedAdd(remainingBytes, RESERVE_BYTES)

    private fun saturatedAdd(left: Long, right: Long): Long =
        if (right > Long.MAX_VALUE - left) Long.MAX_VALUE else left + right
}
