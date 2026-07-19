package com.zeuroux.launchly.catalog

import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.ReleaseTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogParserTest {
    @Test
    fun malformedAndOldRowsDoNotDiscardValidRows() {
        val json = """[
            [871000500,"1.21.0",0],
            ["malformed"],
            [871000499,"too old",0],
            [871000501,"1.21.1 beta",1],
            [871000502,"unrecognized track",2]
        ]""".trimIndent()

        val values = CatalogParser.parse(json, Architecture.ARM64)

        assertEquals(listOf(871000500L, 871000501L, 871000502L), values.map { it.code })
        assertEquals(listOf(ReleaseTrack.RELEASE, ReleaseTrack.BETA, ReleaseTrack.UNKNOWN), values.map { it.track })
        assertEquals(listOf(Architecture.ARM64), values.map { it.architecture }.distinct())
    }

    @Test
    fun duplicateNamesRetainStableCodeAndAbiIdentity() {
        val values = CatalogParser.parse(
            """[[871000500,"Same",0],[871000501,"Same",0]]""",
            Architecture.X86_64
        )

        assertEquals(2, values.map { it.stableId }.distinct().size)
    }
}
