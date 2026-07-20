package com.zeuroux.launchly.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.auth.AuthState
import com.zeuroux.launchly.catalog.CatalogSource
import com.zeuroux.launchly.catalog.CatalogState
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.DownloadRecord
import com.zeuroux.launchly.model.DownloadStatus
import com.zeuroux.launchly.model.InstalledPackage
import com.zeuroux.launchly.model.ManagedVersion
import com.zeuroux.launchly.model.ReleaseTrack
import com.zeuroux.launchly.model.VersionData
import com.zeuroux.launchly.packageops.LaunchResult
import com.zeuroux.launchly.packageops.PackageOperationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val loading: Boolean = true,
    val complete: Boolean = false,
    val auth: AuthState = AuthState.Loading,
    val showResetNotice: Boolean = false
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LaunchlyApp
    private val preferences = app.container.preferences
    private val authRepository = app.container.authRepository
    private val resetNotice = MutableStateFlow(app.resetResult.didResetLegacyData)

    val state: StateFlow<OnboardingUiState> = combine(
        preferences.onboardingComplete,
        authRepository.state,
        resetNotice
    ) { complete, auth, showReset ->
        OnboardingUiState(
            loading = auth == AuthState.Loading,
            complete = complete,
            auth = auth,
            showResetNotice = showReset
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingUiState())

    fun finishOnboarding() = viewModelScope.launch { preferences.setOnboardingComplete(true) }

    fun dismissResetNotice() {
        app.resetCoordinator.consumeResetNotice()
        resetNotice.value = false
    }
}

data class ManagedVersionItem(
    val version: ManagedVersion,
    val download: DownloadRecord?
)

data class LibraryUiState(
    val versions: List<ManagedVersionItem> = emptyList(),
    val installed: InstalledPackage? = null,
    val operation: PackageOperationState = PackageOperationState.Idle,
    val imageError: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaunchlyApp).container
    private val imageError = MutableStateFlow<String?>(null)
    val state: StateFlow<LibraryUiState> = combine(
        container.managedVersions.versions,
        container.downloads.downloads,
        container.packages.installedPackage,
        container.packages.operation,
        imageError
    ) { versions, downloads, installed, operation, iconError ->
        val byId = downloads.associateBy(DownloadRecord::versionId)
        LibraryUiState(versions.map { ManagedVersionItem(it, byId[it.id]) }, installed, operation, iconError)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun download(id: String) = viewModelScope.launch { container.downloads.enqueue(id) }
    fun install(id: String) = viewModelScope.launch { container.packages.install(id) }
    fun confirmDowngrade(id: String) = viewModelScope.launch { container.packages.confirmDowngrade(id) }
    fun delete(id: String) = viewModelScope.launch { container.downloads.delete(id) }
    fun rename(version: ManagedVersion, name: String) = viewModelScope.launch {
        container.managedVersions.update(version.copy(displayName = name.trim().ifBlank { version.versionName }))
    }
    fun setCustomIcon(version: ManagedVersion, uri: Uri) = viewModelScope.launch {
        runCatching { container.userImageStore.storeIcon(version.id, uri) }
            .onSuccess { path -> container.managedVersions.update(version.copy(customIconPath = path)) }
            .onFailure { imageError.value = it.message ?: "The selected image could not be used." }
    }
    fun dismissImageError() { imageError.value = null }
    fun dismissOperation() = viewModelScope.launch { container.packages.dismissOperation() }
    fun launchMinecraft(): LaunchResult = container.packages.launchMinecraft()
}

data class DownloadsUiState(
    val records: List<Pair<DownloadRecord, ManagedVersion?>> = emptyList()
)

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaunchlyApp).container
    val state: StateFlow<DownloadsUiState> = combine(
        container.downloads.downloads,
        container.managedVersions.versions
    ) { downloads, versions ->
        val byId = versions.associateBy(ManagedVersion::id)
        DownloadsUiState(downloads.map { it to byId[it.versionId] })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadsUiState())

    fun pause(id: String) = viewModelScope.launch { container.downloads.pause(id) }
    fun resume(id: String) = viewModelScope.launch { container.downloads.resume(id) }
    fun retry(id: String) = viewModelScope.launch { container.downloads.enqueue(id) }
    fun cancel(id: String) = viewModelScope.launch { container.downloads.cancel(id) }
}

data class SettingsUiState(
    val auth: AuthState = AuthState.Loading,
    val canInstallPackages: Boolean = false,
    val appVersion: String = "",
    val catalogSource: String = CatalogSource.DEFAULT_TEMPLATE,
    val catalogSourceError: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaunchlyApp).container
    private val permissionRefresh = MutableStateFlow(0)
    private val catalogSourceError = MutableStateFlow<String?>(null)
    val state: StateFlow<SettingsUiState> = combine(
        container.authRepository.state,
        container.preferences.catalogSource,
        permissionRefresh,
        catalogSourceError
    ) { auth, catalogSource, _, sourceError ->
        SettingsUiState(
            auth = auth,
            canInstallPackages = application.packageManager.canRequestPackageInstalls(),
            appVersion = com.zeuroux.launchly.BuildConfig.VERSION_NAME,
            catalogSource = catalogSource,
            catalogSourceError = sourceError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun signOut() = viewModelScope.launch {
        container.authRepository.signOut()
        container.preferences.setOnboardingComplete(false)
    }

    fun refreshProfile() = viewModelScope.launch { container.authRepository.refreshProfile() }

    fun refreshPermission() { permissionRefresh.value++ }

    fun setCatalogSource(value: String): Boolean {
        val source = runCatching { CatalogSource.normalize(value) }
            .getOrElse {
                catalogSourceError.value = it.message ?: "The catalog URL is invalid."
                return false
            }
        catalogSourceError.value = null
        viewModelScope.launch {
            runCatching { container.catalog.setSource(source) }
                .onFailure { catalogSourceError.value = it.message ?: "The catalog source could not be changed." }
        }
        return true
    }

    fun resetCatalogSource() {
        setCatalogSource(CatalogSource.DEFAULT_TEMPLATE)
    }
}

data class AddVersionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val cached: Boolean = false,
    val query: String = "",
    val track: ReleaseTrack? = null,
    val architecture: Architecture? = null,
    val availableArchitectures: List<Architecture> = emptyList(),
    val versions: List<VersionData> = emptyList(),
    val addedVersionId: String? = null
)

class AddVersionViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as LaunchlyApp).container
    private val query = MutableStateFlow("")
    private val track = MutableStateFlow<ReleaseTrack?>(null)
    private val architecture = MutableStateFlow<Architecture?>(null)
    private val addedVersionId = MutableStateFlow<String?>(null)

    val state: StateFlow<AddVersionUiState> = combine(
        container.catalog.state,
        query,
        track,
        architecture,
        addedVersionId
    ) { catalog, search, selectedTrack, selectedArchitecture, addedId ->
        val source = when (catalog) {
            is CatalogState.Ready -> catalog.versions
            is CatalogState.Error -> catalog.cachedVersions
            CatalogState.Loading -> emptyList()
        }
        val architectures = source.map(VersionData::architecture).distinct()
        val effectiveArchitecture = selectedArchitecture?.takeIf { it in architectures }
        AddVersionUiState(
            loading = catalog == CatalogState.Loading,
            error = (catalog as? CatalogState.Error)?.message,
            cached = (catalog as? CatalogState.Ready)?.cached == true ||
                (catalog is CatalogState.Error && catalog.cachedVersions.isNotEmpty()),
            query = search,
            track = selectedTrack,
            architecture = effectiveArchitecture,
            availableArchitectures = architectures,
            versions = source.filter { version ->
                (search.isBlank() || version.name.contains(search, true) || version.code.toString().contains(search)) &&
                    (selectedTrack == null || version.track == selectedTrack) &&
                    (effectiveArchitecture == null || version.architecture == effectiveArchitecture)
            },
            addedVersionId = addedId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddVersionUiState())

    fun setQuery(value: String) { query.value = value }
    fun setTrack(value: ReleaseTrack?) { track.value = value }
    fun setArchitecture(value: Architecture?) { architecture.value = value }
    fun retry() = container.catalog.refresh()
    fun add(value: VersionData) = viewModelScope.launch {
        addedVersionId.value = container.managedVersions.add(value).id
    }
    fun consumeAdded() { addedVersionId.value = null }
}

class LaunchlyViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(application)
        modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(application)
        modelClass.isAssignableFrom(DownloadsViewModel::class.java) -> DownloadsViewModel(application)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(application)
        modelClass.isAssignableFrom(AddVersionViewModel::class.java) -> AddVersionViewModel(application)
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    } as T
}
