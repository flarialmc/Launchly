package com.zeuroux.launchly.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSourceTest {
    @Test
    fun validHttpsUrlIsTrimmed() {
        val source = CatalogSource.normalize(" https://example.test/catalog/versions.json ")

        assertEquals("https://example.test/catalog/versions.json", source)
    }

    @Test
    fun sourceMustUseHttps() {
        val insecure = runCatching { CatalogSource.normalize("http://example.test/versions.json") }.exceptionOrNull()

        assertTrue(insecure is IllegalArgumentException)
    }
}
