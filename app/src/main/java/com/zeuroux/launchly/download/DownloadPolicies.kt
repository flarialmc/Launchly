package com.zeuroux.launchly.download

import com.zeuroux.launchly.gplay.GPlayArtifact
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap

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

internal object ResumePolicy {
    fun canAppend(existingBytes: Long, responseCode: Int, contentRange: String?): Boolean =
        existingBytes > 0L && responseCode == 206 && contentRange?.startsWith("bytes $existingBytes-") == true
}
