package com.zeuroux.launchly.model

import android.os.Build

const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
const val MIN_SUPPORTED_MINECRAFT_VERSION_CODE = 871_000_500L

enum class ReleaseTrack {
    RELEASE,
    BETA,
    UNKNOWN;

    companion object {
        fun fromCatalogValue(value: Int): ReleaseTrack = when (value) {
            0 -> RELEASE
            1 -> BETA
            else -> UNKNOWN
        }
    }
}

enum class Architecture(val abi: String) {
    ARM64("arm64-v8a"),
    ARM("armeabi-v7a"),
    X86_64("x86_64"),
    X86("x86"),
    UNKNOWN("unknown");

    companion object {
        fun fromAbi(value: String): Architecture = entries.firstOrNull { it.abi == value } ?: UNKNOWN

        fun compatibleDeviceArchitectures(): List<Architecture> = Build.SUPPORTED_ABIS
            .map(::fromAbi)
            .filterNot { it == UNKNOWN }
            .distinct()
    }
}

data class VersionData(
    val code: Long,
    val name: String,
    val track: ReleaseTrack,
    val architecture: Architecture
) {
    val stableId: String = "$code:${architecture.abi}"
}

data class ManagedVersion(
    val id: String,
    val displayName: String,
    val versionCode: Long,
    val versionName: String,
    val track: ReleaseTrack,
    val architecture: Architecture,
    val customIconPath: String?,
    val createdAt: Long,
    val updatedAt: Long
)

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    FAILED,
    READY,
    CANCELLED
}

data class DownloadRecord(
    val versionId: String,
    val workId: String?,
    val status: DownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long?,
    val speedBytesPerSecond: Long?,
    val failureType: String?,
    val failureMessage: String?,
    val updatedAt: Long
) {
    val progress: Float?
        get() = totalBytes?.takeIf { it > 0L }?.let {
            (bytesDownloaded.toDouble() / it.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }
}

data class InstalledPackage(
    val versionCode: Long,
    val versionName: String,
    val signerSha256: String,
    val architecture: Architecture
)
