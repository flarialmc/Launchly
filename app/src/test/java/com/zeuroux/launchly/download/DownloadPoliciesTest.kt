package com.zeuroux.launchly.download

import com.zeuroux.launchly.gplay.GPlayArtifact
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class DownloadPoliciesTest {
    @Test
    fun filenamesAreSanitizedAndDuplicatesAreRejected() {
        val sanitized = ArtifactNamePolicy.sanitize(listOf(GPlayArtifact("https://example.test", "folder/split config.apk", 3)))
        assertEquals("split_config.apk", sanitized.single().name)

        val result = runCatching {
            ArtifactNamePolicy.sanitize(
                listOf(
                    GPlayArtifact("https://example.test/a", "base.apk", 1),
                    GPlayArtifact("https://example.test/b", "BASE.APK", 1)
                )
            )
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun artifactUrlsMustUseHttps() {
        assertEquals("https", ArtifactUrlPolicy.requireHttps("https://example.test/base.apk").scheme)
        assertTrue(runCatching { ArtifactUrlPolicy.requireHttps("http://example.test/base.apk") }.isFailure)
        assertTrue(runCatching { ArtifactUrlPolicy.requireHttps("not a url") }.isFailure)
    }

    @Test
    fun rangeAppendRequiresMatchingPartialContentResponse() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(206).addHeader("Content-Range", "bytes 5-9/10").setBody("56789"))
        server.start()
        try {
            val response = OkHttpClient().newCall(
                Request.Builder().url(server.url("/base.apk")).header("Range", "bytes=5-").build()
            ).execute()
            response.use {
                assertTrue(ResumePolicy.canAppend(5, it.code, it.header("Content-Range")))
            }
            assertEquals("bytes=5-", server.takeRequest().getHeader("Range"))
            assertFalse(ResumePolicy.canAppend(5, 200, null))
            assertFalse(ResumePolicy.canAppend(5, 206, "bytes 5-garbage"))
            assertFalse(ResumePolicy.canAppend(5, 206, "bytes 5-4/10"))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun persistentDownloadClientHasNoWholeCallDeadline() {
        val client = OkHttpClient.Builder().callTimeout(2, java.util.concurrent.TimeUnit.MINUTES).build()

        val downloadClient = client.forPersistentDownloads()

        assertEquals(0, downloadClient.callTimeoutMillis)
        assertEquals(client.connectTimeoutMillis, downloadClient.connectTimeoutMillis)
        assertEquals(client.readTimeoutMillis, downloadClient.readTimeoutMillis)
    }

    @Test
    fun invalidFinalApksAreRemovedBeforeRetry() {
        val directory = Files.createTempDirectory("launchly-invalid-apks").toFile()
        try {
            val corrupt = directory.resolve("base.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val part = directory.resolve("split.apk.part").apply { writeBytes(byteArrayOf(4)) }
            assertTrue(ArtifactCachePolicy.isReusableFinal(corrupt, 3))

            assertEquals(1, ArtifactCachePolicy.invalidateFinalApks(directory))
            assertFalse(corrupt.exists())
            assertTrue(part.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectedOrInvalidPartialDownloadIsFullyReset() {
        val directory = Files.createTempDirectory("launchly-invalid-part").toFile()
        try {
            val part = directory.resolve("base.apk.part").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val validator = directory.resolve("base.apk.part.meta").apply { writeText("etag") }

            ArtifactCachePolicy.invalidatePartial(part, validator)

            assertFalse(part.exists())
            assertFalse(validator.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun storagePreflightAccountsForResumableBytesAndReserve() {
        val artifacts = listOf(
            GPlayArtifact("https://example.test/base", "base.apk", 100),
            GPlayArtifact("https://example.test/split", "split.apk", 50)
        )
        val cached = mapOf("base.apk" to 40L, "split.apk" to 50L)

        val remaining = StoragePolicy.remainingBytes(artifacts) { cached[it.name] ?: 0L }

        assertEquals(60L, remaining)
        assertFalse(StoragePolicy.hasEnoughSpace(StoragePolicy.RESERVE_BYTES + 59L, remaining!!))
        assertTrue(StoragePolicy.hasEnoughSpace(StoragePolicy.RESERVE_BYTES + 60L, remaining))
        assertTrue(StoragePolicy.hasEnoughSpace(0L, 0L))
    }

    @Test
    fun deliveryAndStreamingSizesAreBounded() {
        StoragePolicy.validateDelivery(
            listOf(GPlayArtifact("https://example.test/base", "base.apk", StoragePolicy.MAX_ARTIFACT_BYTES))
        )
        assertTrue(
            runCatching {
                StoragePolicy.validateDelivery(
                    listOf(
                        GPlayArtifact(
                            "https://example.test/base",
                            "base.apk",
                            StoragePolicy.MAX_ARTIFACT_BYTES + 1
                        )
                    )
                )
            }.isFailure
        )
        assertEquals(
            StoragePolicy.MAX_ARTIFACT_BYTES,
            StoragePolicy.streamLimit(GPlayArtifact("https://example.test/base", "base.apk", 0), 0)
        )
        assertEquals(
            1L,
            StoragePolicy.streamLimit(
                GPlayArtifact("https://example.test/base", "base.apk", 100),
                StoragePolicy.MAX_TOTAL_BYTES - 1
            )
        )
    }
}
