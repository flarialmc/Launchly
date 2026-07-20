package com.zeuroux.launchly.catalog

import android.content.Context
import com.zeuroux.launchly.data.AppPreferences
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.MIN_SUPPORTED_MINECRAFT_VERSION_CODE
import com.zeuroux.launchly.model.ReleaseTrack
import com.zeuroux.launchly.model.VersionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.net.URI
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
    suspend fun setSource(sourceTemplate: String)
}

object CatalogSource {
    const val DEFAULT_TEMPLATE = AppPreferences.DEFAULT_CATALOG_SOURCE

    fun normalize(sourceTemplate: String): String {
        val normalized = sourceTemplate.trim()
        require(normalized.none(Char::isWhitespace)) { "The catalog URL cannot contain spaces." }
        val uri = runCatching { URI(normalized) }
            .getOrElse { throw IllegalArgumentException("Enter a valid HTTPS catalog URL.") }
        require(uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()) {
            "The catalog URL must use HTTPS."
        }
        return normalized
    }
}

class CachedVersionCatalogRepository(
    context: Context,
    private val client: OkHttpClient,
    private val preferences: AppPreferences,
    private val applicationScope: CoroutineScope
) : VersionCatalogRepository {
    private val cacheDirectory = File(context.noBackupFilesDir, "catalog")
    private val refreshMutex = Mutex()
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Loading)
    override val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        applicationScope.launch {
            refreshMutex.withLock { refreshLocked() }
        }
    }

    override suspend fun setSource(sourceTemplate: String) {
        val source = CatalogSource.normalize(sourceTemplate)
        refreshMutex.withLock {
            if (source != preferences.currentCatalogSource()) {
                withContext(Dispatchers.IO) { cacheDirectory.deleteRecursively() }
                preferences.clearCatalogEtags()
                preferences.setCatalogSource(source)
            }
            refreshLocked()
        }
    }

    private suspend fun refreshLocked() {
            val previous = when (val current = _state.value) {
                is CatalogState.Ready -> current.versions
                is CatalogState.Error -> current.cachedVersions
                CatalogState.Loading -> emptyList()
            }
            _state.value = CatalogState.Loading
            val source = preferences.currentCatalogSource()
            val architectures = Architecture.compatibleDeviceArchitectures()
            if (architectures.isEmpty()) {
                _state.value = CatalogState.Error("This device does not expose a supported ABI.")
                return
            }

            val result = fetchCatalog(architectures, source)
            val versions = result.versions
                .distinctBy(VersionData::stableId)
                .sortedByDescending(VersionData::code)

            _state.value = when {
                versions.isNotEmpty() -> CatalogState.Ready(versions, result.cached)
                previous.isNotEmpty() -> CatalogState.Error(
                    result.error ?: "The version catalog could not be refreshed.",
                    previous
                )
                else -> CatalogState.Error(result.error ?: "No compatible versions were found.")
            }
    }

    private suspend fun fetchCatalog(
        architectures: List<Architecture>,
        source: String
    ): FetchResult = withContext(Dispatchers.IO) {
        cacheDirectory.mkdirs()
        val cacheFile = File(cacheDirectory, "versions.json")
        val requestBuilder = Request.Builder()
            .url(source)
            .header("Accept", "application/json")
        preferences.catalogEtag()?.let { requestBuilder.header("If-None-Match", it) }

        runCatching {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 304 && cacheFile.isFile) {
                    return@use FetchResult(CatalogParser.parse(cacheFile.readText(), architectures), cached = true)
                }
                if (!response.isSuccessful) error("Catalog request failed with HTTP ${response.code}.")
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("The catalog response was empty.")
                val parsed = CatalogParser.parse(body, architectures)
                if (parsed.isEmpty()) error("The catalog contained no supported versions.")
                val temporary = File(cacheDirectory, "${cacheFile.name}.tmp")
                temporary.writeText(body)
                Files.move(
                    temporary.toPath(), cacheFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
                )
                preferences.setCatalogEtag(response.header("ETag"))
                FetchResult(parsed, cached = false)
            }
        }.getOrElse { failure ->
            val cachedVersions = runCatching {
                if (cacheFile.isFile) CatalogParser.parse(cacheFile.readText(), architectures) else emptyList()
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
    fun parse(json: String, architectures: List<Architecture>): List<VersionData> {
        val root = JSONArray(json)
        return buildList {
            for (index in 0 until root.length()) {
                runCatching {
                    val row = root.getJSONObject(index)
                    val name = row.getString("version_name").trim()
                    if (name.isBlank()) return@runCatching
                    val codes = row.getJSONObject("codes")
                    val track = if (row.optBoolean("beta", false)) ReleaseTrack.BETA else ReleaseTrack.RELEASE
                    architectures.forEach { architecture ->
                        if (!codes.has(architecture.abi)) return@forEach
                        val code = codes.getLong(architecture.abi)
                        if (code < MIN_SUPPORTED_MINECRAFT_VERSION_CODE) return@forEach
                        add(
                            VersionData(
                                code = code,
                                name = name,
                                track = track,
                                architecture = architecture
                            )
                        )
                    }
                }
            }
        }
    }

}
