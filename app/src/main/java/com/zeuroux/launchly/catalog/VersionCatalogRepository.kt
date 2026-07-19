package com.zeuroux.launchly.catalog

import android.content.Context
import com.zeuroux.launchly.data.AppPreferences
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.MIN_SUPPORTED_MINECRAFT_VERSION_CODE
import com.zeuroux.launchly.model.ReleaseTrack
import com.zeuroux.launchly.model.VersionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val versions: List<VersionData>, val cached: Boolean) : CatalogState
    data class Error(val message: String, val cachedVersions: List<VersionData> = emptyList()) : CatalogState
}

interface VersionCatalogRepository {
    val state: StateFlow<CatalogState>
    fun refresh()
}

class CachedVersionCatalogRepository(
    context: Context,
    private val client: OkHttpClient,
    private val preferences: AppPreferences,
    private val applicationScope: CoroutineScope
) : VersionCatalogRepository {
    private val cacheDirectory = File(context.noBackupFilesDir, "catalog")
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Loading)
    override val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        applicationScope.launch {
            val previous = when (val current = _state.value) {
                is CatalogState.Ready -> current.versions
                is CatalogState.Error -> current.cachedVersions
                CatalogState.Loading -> emptyList()
            }
            _state.value = CatalogState.Loading
            val architectures = Architecture.compatibleDeviceArchitectures()
            if (architectures.isEmpty()) {
                _state.value = CatalogState.Error("This device does not expose a supported ABI.")
                return@launch
            }

            val results = architectures.map { architecture ->
                async(Dispatchers.IO) { fetchArchitecture(architecture) }
            }.awaitAll()
            val versions = results.flatMap { it.versions }
                .distinctBy(VersionData::stableId)
                .sortedByDescending(VersionData::code)
            val failures = results.mapNotNull { it.error }
            val usedCache = results.any { it.cached }

            _state.value = when {
                versions.isNotEmpty() -> CatalogState.Ready(versions, usedCache || failures.isNotEmpty())
                previous.isNotEmpty() -> CatalogState.Error(
                    failures.firstOrNull() ?: "The version catalog could not be refreshed.",
                    previous
                )
                else -> CatalogState.Error(failures.firstOrNull() ?: "No compatible versions were found.")
            }
        }
    }

    private suspend fun fetchArchitecture(architecture: Architecture): FetchResult = withContext(Dispatchers.IO) {
        cacheDirectory.mkdirs()
        val cacheFile = File(cacheDirectory, "versions.${architecture.abi}.json")
        val requestBuilder = Request.Builder()
            .url("https://raw.githubusercontent.com/minecraft-linux/mcpelauncher-versiondb/refs/heads/master/versions.${architecture.abi}.json.min")
            .header("Accept", "application/json")
        preferences.catalogEtag(architecture.abi)?.let { requestBuilder.header("If-None-Match", it) }

        runCatching {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 304 && cacheFile.isFile) {
                    return@use FetchResult(CatalogParser.parse(cacheFile.readText(), architecture), cached = true)
                }
                if (!response.isSuccessful) error("Catalog request failed with HTTP ${response.code}.")
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("The catalog response was empty.")
                val parsed = CatalogParser.parse(body, architecture)
                if (parsed.isEmpty()) error("The catalog contained no supported versions.")
                val temporary = File(cacheDirectory, "${cacheFile.name}.tmp")
                temporary.writeText(body)
                Files.move(
                    temporary.toPath(), cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
                preferences.setCatalogEtag(architecture.abi, response.header("ETag"))
                FetchResult(parsed, cached = false)
            }
        }.getOrElse { failure ->
            val cachedVersions = runCatching {
                if (cacheFile.isFile) CatalogParser.parse(cacheFile.readText(), architecture) else emptyList()
            }.getOrDefault(emptyList())
            FetchResult(
                versions = cachedVersions,
                cached = cachedVersions.isNotEmpty(),
                error = failure.message ?: "Catalog refresh failed."
            )
        }
    }

    private data class FetchResult(
        val versions: List<VersionData>,
        val cached: Boolean,
        val error: String? = null
    )
}

internal object CatalogParser {
    fun parse(json: String, architecture: Architecture): List<VersionData> {
        val root = JSONArray(json)
        return buildList {
            for (index in 0 until root.length()) {
                runCatching {
                    val row = root.getJSONArray(index)
                    val code = row.getLong(0)
                    if (code < MIN_SUPPORTED_MINECRAFT_VERSION_CODE) return@runCatching
                    val name = row.getString(1).trim()
                    if (name.isBlank()) return@runCatching
                    add(
                        VersionData(
                            code = code,
                            name = name,
                            track = ReleaseTrack.fromCatalogValue(row.optInt(2, -1)),
                            architecture = architecture
                        )
                    )
                }
            }
        }
    }

}
