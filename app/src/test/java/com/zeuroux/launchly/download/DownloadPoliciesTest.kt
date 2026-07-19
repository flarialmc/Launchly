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
        } finally {
            server.shutdown()
        }
    }
}
