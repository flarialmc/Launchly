package com.zeuroux.launchly.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {
    @Test
    fun progressIsUnknownForMissingTotalAndClampedForBadValues() {
        assertNull(record(bytes = 20, total = null).progress)
        assertEquals(1f, record(bytes = 200, total = 100).progress)
        assertEquals(0f, record(bytes = -5, total = 100).progress)
    }

    @Test
    fun architectureUsesAbiStringRatherThanOrdinal() {
        assertEquals(Architecture.ARM64, Architecture.fromAbi("arm64-v8a"))
        assertEquals(Architecture.X86_64, Architecture.fromAbi("x86_64"))
        assertEquals(Architecture.UNKNOWN, Architecture.fromAbi("mips"))
    }

    private fun record(bytes: Long, total: Long?) = DownloadRecord(
        versionId = "id",
        workId = null,
        status = DownloadStatus.DOWNLOADING,
        bytesDownloaded = bytes,
        totalBytes = total,
        speedBytesPerSecond = null,
        failureType = null,
        failureMessage = null,
        updatedAt = 0
    )
}
