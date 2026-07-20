package com.zeuroux.launchly.catalog

import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.ReleaseTrack
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogParserTest {
    @Test
    fun malformedAndOldRowsDoNotDiscardValidRows() {
        val json = """[
            {"version_name":"1.21.0","codes":{"arm64-v8a":871000500,"x86_64":881000500}},
            {"version_name":"malformed"},
            {"version_name":"too old","codes":{"arm64-v8a":871000499}},
            {"version_name":"1.21.1","codes":{"arm64-v8a":871000501},"beta":true},
            {"version_name":"missing requested ABI","codes":{"x86":871000502}}
        ]""".trimIndent()

        val values = CatalogParser.parse(json, listOf(Architecture.ARM64, Architecture.X86_64))

        assertEquals(listOf(871000500L, 881000500L, 871000501L), values.map { it.code })
        assertEquals(listOf(ReleaseTrack.RELEASE, ReleaseTrack.RELEASE, ReleaseTrack.BETA), values.map { it.track })
        assertEquals(listOf(Architecture.ARM64, Architecture.X86_64), values.map { it.architecture }.distinct())
    }

    @Test
    fun duplicateNamesRetainStableCodeAndAbiIdentity() {
        val values = CatalogParser.parse(
            """[{"version_name":"Same","codes":{"x86_64":871000500}},{"version_name":"Same","codes":{"x86_64":871000501}}]""",
            listOf(Architecture.X86_64)
        )

        assertEquals(2, values.map { it.stableId }.distinct().size)
    }
}
