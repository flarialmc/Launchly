@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.zeuroux.launchly.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.R
import com.zeuroux.launchly.auth.AuthState
import com.zeuroux.launchly.diagnostics.DiagnosticExporter
import com.zeuroux.launchly.model.Architecture
import com.zeuroux.launchly.model.DownloadRecord
import com.zeuroux.launchly.model.DownloadStatus
import com.zeuroux.launchly.model.ManagedVersion
import com.zeuroux.launchly.model.ReleaseTrack
import com.zeuroux.launchly.model.VersionData
import com.zeuroux.launchly.packageops.LaunchResult
import com.zeuroux.launchly.packageops.PackageOperationState
import com.zeuroux.launchly.viewmodel.AddVersionUiState
import com.zeuroux.launchly.viewmodel.AddVersionViewModel
import com.zeuroux.launchly.viewmodel.DownloadsUiState
import com.zeuroux.launchly.viewmodel.DownloadsViewModel
import com.zeuroux.launchly.viewmodel.LibraryUiState
import com.zeuroux.launchly.viewmodel.LibraryViewModel
import com.zeuroux.launchly.viewmodel.ManagedVersionItem
import com.zeuroux.launchly.viewmodel.OnboardingUiState
import com.zeuroux.launchly.viewmodel.SettingsUiState
import com.zeuroux.launchly.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onSignIn: () -> Unit,
    onContinue: () -> Unit,
    onDismissReset: () -> Unit
) {
    val context = LocalContext.current
    LaunchlyBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().widthIn(max = 620.dp).fillMaxHeight(0.92f),
                color = LaunchlyDesign.SurfacePrimary,
                contentColor = LaunchlyDesign.TextPrimary,
                shape = LaunchlyDesign.PanelShape
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("onboarding"),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item { LaunchlyBrandRow(Modifier.fillMaxWidth()) }
                    item {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                stringResource(R.string.onboarding_title),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.onboarding_body),
                                style = MaterialTheme.typography.bodyLarge,
                                color = LaunchlyDesign.TextMuted
                            )
                        }
                    }
                    item {
                        LaunchlyRaisedCard(Modifier.fillMaxWidth()) {
                            Text(
                                stringResource(R.string.onboarding_unofficial),
                                modifier = Modifier.padding(18.dp),
                                color = LaunchlyDesign.TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            when (val auth = state.auth) {
                                is AuthState.Authenticated -> {
                                    Text(
                                        stringResource(
                                            R.string.signed_in_as,
                                            auth.session.displayName ?: auth.session.email
                                        ),
                                        color = LaunchlyDesign.TextSecondary
                                    )
                                    LaunchlyPrimaryButton(
                                        onClick = onContinue,
                                        modifier = Modifier.fillMaxWidth().testTag("finish_setup")
                                    ) {
                                        Text(stringResource(R.string.finish_setup))
                                    }
                                }
                                else -> LaunchlyPrimaryButton(
                                    onClick = onSignIn,
                                    modifier = Modifier.fillMaxWidth().testTag("sign_in")
                                ) {
                                    Text(stringResource(R.string.sign_in_google))
                                }
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton({ openUrl(context, PRIVACY_URL) }) {
                                Text(stringResource(R.string.privacy_policy), color = LaunchlyDesign.TextSecondary)
                            }
                            TextButton({ openUrl(context, DISCLAIMER_URL) }) {
                                Text(stringResource(R.string.disclaimer), color = LaunchlyDesign.TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
    if (state.showResetNotice) {
        LaunchlyDialog(
            onDismissRequest = onDismissReset,
            title = stringResource(R.string.reset_notice_title),
            message = stringResource(R.string.reset_notice_body),
            confirmText = stringResource(R.string.close),
            onConfirm = onDismissReset
        )
    }
}

@Composable
internal fun LibraryScreen(
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    navController: NavHostController,
    snackbar: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionDeniedMessage = stringResource(R.string.notification_permission_denied)
    val linkUnavailableMessage = stringResource(R.string.link_unavailable)
    var pendingDownload by rememberSaveable { mutableStateOf<String?>(null) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingDownload?.let { id ->
            if (granted) viewModel.download(id)
            else scope.launch { snackbar.showSnackbar(notificationPermissionDeniedMessage) }
        }
        pendingDownload = null
    }
    val requestDownload: (String) -> Unit = { id ->
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingDownload = id
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else viewModel.download(id)
    }

    val installSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val operation = state.operation
        if (operation is PackageOperationState.PermissionRequired && context.packageManager.canRequestPackageInstalls()) {
            viewModel.install(operation.versionId)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("library"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LaunchlySectionTitle(stringResource(R.string.installed_minecraft))
            Spacer(Modifier.height(10.dp))
            LaunchlyRaisedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val installed = state.installed
                    Text(
                        installed?.let { stringResource(R.string.minecraft_version, it.versionName, it.versionCode) }
                            ?: stringResource(R.string.not_installed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (installed == null) LaunchlyDesign.TextMuted else LaunchlyDesign.TextPrimary
                    )
                    if (installed != null) {
                        LaunchlyPrimaryButton(
                            onClick = {
                                when (val result = viewModel.launchMinecraft()) {
                                    is LaunchResult.Failure -> scope.launch { snackbar.showSnackbar(result.message) }
                                    LaunchResult.Launched -> Unit
                                }
                            }
                        ) { Text(stringResource(R.string.launch)) }
                    }
                }
            }
        }
        item { LaunchlySectionTitle(stringResource(R.string.managed_versions)) }
        if (state.versions.isEmpty()) {
            item { EmptyCard(R.string.empty_library_title, R.string.empty_library_body) }
        } else {
            items(state.versions, key = { it.version.id }) { item ->
                ManagedVersionCard(
                    item = item,
                    onDownload = { requestDownload(item.version.id) },
                    onViewDownload = { navController.navigate(Route.DOWNLOADS.path) },
                    onInstall = { viewModel.install(item.version.id) },
                    onRename = { viewModel.rename(item.version, it) },
                    onPickIcon = { viewModel.setCustomIcon(item.version, it) },
                    onDelete = { viewModel.delete(item.version.id) }
                )
            }
        }
    }

    when (val operation = state.operation) {
        is PackageOperationState.Preparing -> InstallationStatusPopup(
            title = stringResource(R.string.preparing_installation),
            message = stringResource(R.string.preparing_installation_message)
        )
        is PackageOperationState.Installing -> InstallationStatusPopup(
            title = stringResource(R.string.installing_minecraft),
            message = operation.progress?.let { stringResource(R.string.installation_progress, it) }
                ?: stringResource(R.string.installation_waiting)
        )
        is PackageOperationState.Uninstalling -> InstallationStatusPopup(
            title = stringResource(R.string.uninstalling_minecraft),
            message = stringResource(R.string.uninstalling_minecraft_message)
        )
        is PackageOperationState.PermissionRequired -> LaunchlyDialog(
            onDismissRequest = viewModel::dismissOperation,
            title = stringResource(R.string.install_permission_title),
            message = stringResource(R.string.install_permission_message),
            confirmText = stringResource(R.string.open_settings),
            onConfirm = {
                    runCatching {
                        installSettingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                        )
                    }.onFailure {
                        scope.launch { snackbar.showSnackbar(linkUnavailableMessage) }
                    }
            },
            dismissText = stringResource(R.string.cancel),
            onDismiss = viewModel::dismissOperation
        )
        is PackageOperationState.DowngradeConfirmationRequired -> LaunchlyDialog(
            onDismissRequest = viewModel::dismissOperation,
            title = stringResource(R.string.downgrade_title),
            message = stringResource(R.string.downgrade_message, operation.currentVersion, operation.targetVersion),
            confirmText = stringResource(R.string.uninstall_and_continue),
            onConfirm = { viewModel.confirmDowngrade(operation.versionId) },
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onDismiss = viewModel::dismissOperation
        )
        is PackageOperationState.Failure -> MessageDialog(R.string.operation_failed, operation.message, viewModel::dismissOperation)
        is PackageOperationState.Completed -> MessageDialog(R.string.operation_complete, operation.message, viewModel::dismissOperation)
        else -> Unit
    }
    state.imageError?.let {
        MessageDialog(R.string.invalid_image_title, it, viewModel::dismissImageError)
    }
}

@Composable
private fun ManagedVersionCard(
    item: ManagedVersionItem,
    onDownload: () -> Unit,
    onViewDownload: () -> Unit,
    onInstall: () -> Unit,
    onRename: (String) -> Unit,
    onPickIcon: (Uri) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var name by remember(item.version.id) { mutableStateOf(item.version.displayName) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPickIcon)
    }
    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, item.version.customIconPath) {
        value = withContext(Dispatchers.IO) {
            item.version.customIconPath?.let(BitmapFactory::decodeFile)?.asImageBitmap()
        }
    }
    LaunchlyRaisedCard(Modifier.fillMaxWidth().testTag("version_${item.version.id}")) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Image(
                        bitmap = it,
                        contentDescription = item.version.displayName,
                        modifier = Modifier.padding(end = 12.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = 48.dp, maxHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(item.version.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.version.versionName, style = MaterialTheme.typography.bodyMedium, color = LaunchlyDesign.TextMuted)
                }
                Box {
                    TextButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = stringResource(R.string.more_actions)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = LaunchlyDesign.PromptSurface,
                        shape = LaunchlyDesign.CardShape
                    ) {
                        DropdownMenuItem({ Text(stringResource(R.string.edit)) }, onClick = { menuExpanded = false; editing = true })
                        DropdownMenuItem(
                            { Text(stringResource(R.string.choose_icon)) },
                            onClick = {
                                menuExpanded = false
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                        DropdownMenuItem({ Text(stringResource(R.string.delete)) }, onClick = { menuExpanded = false; deleting = true })
                    }
                }
            }
            Text(
                stringResource(R.string.architecture, item.version.architecture.abi),
                color = LaunchlyDesign.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.download_state, item.download?.status?.displayName() ?: stringResource(R.string.download)),
                color = LaunchlyDesign.TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            item.download?.progress?.let { LaunchlyProgress(it) }
            when (item.download?.status) {
                DownloadStatus.READY -> LaunchlyPrimaryButton(onInstall) { Text(stringResource(R.string.install)) }
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> LaunchlySecondaryButton(onViewDownload) {
                    Text(stringResource(R.string.view_download))
                }
                DownloadStatus.PAUSED, DownloadStatus.FAILED -> LaunchlyPrimaryButton(onDownload) {
                    Text(stringResource(R.string.resume))
                }
                else -> LaunchlyPrimaryButton(onDownload) { Text(stringResource(R.string.download)) }
            }
        }
    }
    if (editing) {
        LaunchlyPrompt(
            onDismissRequest = { editing = false },
            title = stringResource(R.string.rename_version)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.version_display_name)) },
                singleLine = true,
                shape = LaunchlyDesign.ControlShape,
                colors = launchlyTextFieldColors()
            )
            LaunchlyPrimaryButton(
                onClick = { onRename(name); editing = false },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }
            TextButton(
                onClick = { editing = false },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
            ) { Text(stringResource(R.string.cancel)) }
        }
    }
    if (deleting) {
        LaunchlyDialog(
            onDismissRequest = { deleting = false },
            title = stringResource(R.string.delete_version_title),
            message = stringResource(R.string.delete_version_message, item.version.displayName),
            confirmText = stringResource(R.string.delete),
            onConfirm = { onDelete(); deleting = false },
            dismissText = stringResource(R.string.cancel),
            destructive = true,
            onDismiss = { deleting = false }
        )
    }
}

@Composable
internal fun DownloadsScreen(state: DownloadsUiState, viewModel: DownloadsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("downloads"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.records.isEmpty()) item { EmptyCard(R.string.no_downloads_title, R.string.no_downloads_body) }
        items(state.records, key = { it.first.versionId }) { (download, version) ->
            DownloadCard(download, version, viewModel)
        }
    }
}

@Composable
private fun DownloadCard(download: DownloadRecord, version: ManagedVersion?, viewModel: DownloadsViewModel) {
    LaunchlyRaisedCard(Modifier.fillMaxWidth().testTag("download_${download.versionId}")) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(version?.displayName ?: download.versionId, style = MaterialTheme.typography.titleMedium)
            Text(download.status.displayName(), color = LaunchlyDesign.TextSecondary)
            if (download.status == DownloadStatus.DOWNLOADING || download.status == DownloadStatus.QUEUED) {
                LaunchlyProgress(download.progress)
            } else {
                download.progress?.let { LaunchlyProgress(it) }
            }
            Text(
                text = download.totalBytes?.let {
                    stringResource(R.string.download_progress, formatBytes(download.bytesDownloaded), formatBytes(it))
                } ?: stringResource(R.string.download_size_unknown),
                color = LaunchlyDesign.TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            download.speedBytesPerSecond?.let {
                Text(
                    stringResource(R.string.download_speed, formatBytes(it)),
                    color = LaunchlyDesign.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            download.failureMessage?.let { Text(it, color = LaunchlyDesign.Error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> LaunchlySecondaryButton(
                        onClick = { viewModel.pause(download.versionId) }
                    ) { Text(stringResource(R.string.pause)) }
                    DownloadStatus.PAUSED -> LaunchlyPrimaryButton(
                        onClick = { viewModel.resume(download.versionId) }
                    ) { Text(stringResource(R.string.resume)) }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> LaunchlyPrimaryButton(
                        onClick = { viewModel.retry(download.versionId) }
                    ) { Text(stringResource(R.string.retry)) }
                    DownloadStatus.READY -> Unit
                }
                if (download.status != DownloadStatus.READY) {
                    TextButton(
                        onClick = { viewModel.cancel(download.versionId) },
                        modifier = Modifier.height(50.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
                    ) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

@Composable
internal fun AddVersionScreen(state: AddVersionUiState, viewModel: AddVersionViewModel) {
    Column(Modifier.fillMaxSize().testTag("add_version_screen")) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.search_versions)) },
            singleLine = true,
            shape = LaunchlyDesign.ControlShape,
            colors = launchlyTextFieldColors()
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { LaunchlyFilterChip(state.track == null, { viewModel.setTrack(null) }, stringResource(R.string.all)) }
            item {
                LaunchlyFilterChip(
                    state.track == ReleaseTrack.RELEASE,
                    { viewModel.setTrack(ReleaseTrack.RELEASE) },
                    stringResource(R.string.release)
                )
            }
            item {
                LaunchlyFilterChip(
                    state.track == ReleaseTrack.BETA,
                    { viewModel.setTrack(ReleaseTrack.BETA) },
                    stringResource(R.string.beta)
                )
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                LaunchlyFilterChip(
                    state.architecture == null,
                    { viewModel.setArchitecture(null) },
                    stringResource(R.string.all)
                )
            }
            items(state.availableArchitectures, key = Architecture::abi) { architecture ->
                LaunchlyFilterChip(
                    state.architecture == architecture,
                    { viewModel.setArchitecture(architecture) },
                    architecture.abi
                )
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LaunchlyDesign.Accent, trackColor = LaunchlyDesign.ProgressTrack)
            }
            state.error != null && state.versions.isEmpty() -> ErrorState(state.error, viewModel::retry)
            else -> {
                if (state.cached) {
                    LaunchlyRaisedCard(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                        Text(
                            stringResource(R.string.catalog_cached),
                            Modifier.padding(14.dp),
                            color = LaunchlyDesign.TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.versions.isEmpty()) item { EmptyCard(R.string.no_versions, R.string.no_versions_body) }
                    items(state.versions, key = VersionData::stableId) { version ->
                        LaunchlyRaisedCard(Modifier.fillMaxWidth().testTag("catalog_${version.stableId}")) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(version.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        stringResource(
                                            R.string.catalog_version_details,
                                            version.code,
                                            version.architecture.abi,
                                            version.track.displayName()
                                        ),
                                        color = LaunchlyDesign.TextMuted,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                LaunchlyPrimaryButton(onClick = { viewModel.add(version) }) {
                                    Text(stringResource(R.string.add_version))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LaunchlyFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = LaunchlyDesign.ControlShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = LaunchlyDesign.NavigationSurface,
            labelColor = LaunchlyDesign.TextSecondary,
            selectedContainerColor = LaunchlyDesign.Accent,
            selectedLabelColor = LaunchlyDesign.TextPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
internal fun SettingsScreen(
    app: LaunchlyApp,
    state: SettingsUiState,
    viewModel: SettingsViewModel,
    onSignIn: () -> Unit,
    snackbar: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val diagnosticsSavedMessage = stringResource(R.string.diagnostics_saved)
    val diagnosticsFailedMessage = stringResource(R.string.diagnostics_failed)
    val linkUnavailableMessage = stringResource(R.string.link_unavailable)
    var catalogSourceEditorOpen by rememberSaveable { mutableStateOf(false) }
    val installSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshPermission()
    }
    val diagnosticLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val success = runCatching {
                withContext(Dispatchers.IO) {
                    val json = DiagnosticExporter(app).createJson()
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json) }
                        ?: error("No output stream")
                }
            }.isSuccess
            snackbar.showSnackbar(if (success) diagnosticsSavedMessage else diagnosticsFailedMessage)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("settings"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { LaunchlySectionTitle(stringResource(R.string.account)) }
        item {
            SettingsCard {
                when (val auth = state.auth) {
                    is AuthState.Authenticated -> {
                        Text(auth.session.displayName ?: auth.session.email, style = MaterialTheme.typography.titleMedium)
                        Text(auth.session.email, color = LaunchlyDesign.TextMuted)
                        auth.profileError?.let { Text(it, color = LaunchlyDesign.Error) }
                        LaunchlySettingsButton(viewModel::refreshProfile, primary = false) {
                            Text(stringResource(R.string.refresh))
                        }
                        LaunchlySettingsButton(onSignIn, primary = false) {
                            Text(stringResource(R.string.reauthenticate))
                        }
                        TextButton(
                            onClick = viewModel::signOut,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
                        ) { Text(stringResource(R.string.sign_out)) }
                    }
                    AuthState.SignedOut -> {
                        Text(stringResource(R.string.signed_out), color = LaunchlyDesign.TextMuted)
                        LaunchlySettingsButton(onSignIn) { Text(stringResource(R.string.sign_in_google)) }
                    }
                    AuthState.Loading -> CircularProgressIndicator(
                        color = LaunchlyDesign.Accent,
                        trackColor = LaunchlyDesign.ProgressTrack
                    )
                }
            }
        }
        item { LaunchlySectionTitle(stringResource(R.string.install_permission)) }
        item {
            SettingsCard {
                Text(
                    stringResource(if (state.canInstallPackages) R.string.permission_allowed else R.string.permission_not_allowed),
                    color = if (state.canInstallPackages) LaunchlyDesign.TextPrimary else LaunchlyDesign.TextMuted
                )
                if (!state.canInstallPackages) {
                    LaunchlySettingsButton(
                        onClick = {
                            runCatching {
                                installSettingsLauncher.launch(
                                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                                )
                            }.onFailure {
                                scope.launch { snackbar.showSnackbar(linkUnavailableMessage) }
                            }
                        }
                    ) { Text(stringResource(R.string.open_settings)) }
                }
            }
        }
        item { LaunchlySectionTitle(stringResource(R.string.version_catalog)) }
        item {
            SettingsCard {
                Text(stringResource(R.string.catalog_source_description), color = LaunchlyDesign.TextMuted)
                Text(
                    state.catalogSource,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("catalog-source")
                )
                state.catalogSourceError?.let { Text(it, color = LaunchlyDesign.Error) }
                LaunchlySettingsButton(onClick = { catalogSourceEditorOpen = true }) {
                    Text(stringResource(R.string.change_catalog_source))
                }
                LaunchlySettingsButton(onClick = viewModel::resetCatalogSource, primary = false) {
                    Text(stringResource(R.string.reset_catalog_source))
                }
            }
        }
        item { LaunchlySectionTitle(stringResource(R.string.about)) }
        item {
            SettingsCard {
                Text(stringResource(R.string.version_label, state.appVersion), style = MaterialTheme.typography.titleMedium)
                HorizontalDivider(color = LaunchlyDesign.Divider)
                SettingsLink(stringResource(R.string.source_code)) { openUrl(context, SOURCE_URL) }
                SettingsLink(stringResource(R.string.privacy_policy)) { openUrl(context, PRIVACY_URL) }
                SettingsLink(stringResource(R.string.disclaimer)) { openUrl(context, DISCLAIMER_URL) }
            }
        }
        item {
            SettingsCard {
                Text(stringResource(R.string.diagnostics_description), color = LaunchlyDesign.TextMuted)
                LaunchlySettingsButton(
                    onClick = {
                        runCatching { diagnosticLauncher.launch("launchly-diagnostics.json") }
                            .onFailure { scope.launch { snackbar.showSnackbar(diagnosticsFailedMessage) } }
                    }
                ) { Text(stringResource(R.string.export_diagnostics)) }
            }
        }
    }
    if (catalogSourceEditorOpen) {
        CatalogSourceDialog(
            currentSource = state.catalogSource,
            error = state.catalogSourceError,
            onSave = { source ->
                if (viewModel.setCatalogSource(source)) catalogSourceEditorOpen = false
            },
            onDismiss = { catalogSourceEditorOpen = false }
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    LaunchlyRaisedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SettingsLink(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        color = Color.Transparent,
        contentColor = LaunchlyDesign.TextSecondary,
        shape = LaunchlyDesign.CardShape
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.CenterStart) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun InstallationStatusPopup(title: String, message: String) {
    LaunchlyPrompt(
        onDismissRequest = {},
        title = title
    ) {
        CircularProgressIndicator(
            color = LaunchlyDesign.Accent,
            trackColor = LaunchlyDesign.ProgressTrack
        )
        Text(
            message,
            color = LaunchlyDesign.TextMuted,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CatalogSourceDialog(
    currentSource: String,
    error: String?,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var source by remember(currentSource) { mutableStateOf(currentSource) }
    LaunchlyPrompt(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.change_catalog_source)
    ) {
        Text(
            stringResource(R.string.catalog_source_hint),
            color = LaunchlyDesign.TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedTextField(
            value = source,
            onValueChange = { source = it },
            label = { Text(stringResource(R.string.catalog_source_url)) },
            modifier = Modifier.fillMaxWidth().testTag("catalog-source-input"),
            minLines = 3,
            maxLines = 4,
            colors = launchlyTextFieldColors(),
            isError = error != null
        )
        error?.let { Text(it, color = LaunchlyDesign.Error) }
        LaunchlyPrimaryButton(
            onClick = { onSave(source) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.save)) }
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
        ) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun EmptyCard(title: Int, body: Int) {
    LaunchlyRaisedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), color = LaunchlyDesign.TextMuted)
        }
    }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = LaunchlyDesign.Error)
            LaunchlyPrimaryButton(retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun MessageDialog(title: Int, message: String, dismiss: () -> Unit) {
    LaunchlyDialog(
        onDismissRequest = dismiss,
        title = stringResource(title),
        message = message,
        confirmText = stringResource(R.string.close),
        onConfirm = dismiss
    )
}

@Composable
private fun DownloadStatus.displayName(): String = when (this) {
    DownloadStatus.QUEUED -> stringResource(R.string.status_queued)
    DownloadStatus.DOWNLOADING -> stringResource(R.string.status_downloading)
    DownloadStatus.PAUSED -> stringResource(R.string.status_paused)
    DownloadStatus.FAILED -> stringResource(R.string.status_failed)
    DownloadStatus.READY -> stringResource(R.string.status_ready)
    DownloadStatus.CANCELLED -> stringResource(R.string.status_cancelled)
}

@Composable
private fun ReleaseTrack.displayName(): String = when (this) {
    ReleaseTrack.RELEASE -> stringResource(R.string.release)
    ReleaseTrack.BETA -> stringResource(R.string.beta)
    ReleaseTrack.UNKNOWN -> stringResource(R.string.unknown)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, context.getString(R.string.link_unavailable), android.widget.Toast.LENGTH_LONG).show()
    }
}

private const val SOURCE_URL = "https://github.com/flarialmc/Launchly"
private const val PRIVACY_URL = "https://github.com/flarialmc/Launchly/blob/master/PRIVACY.md"
private const val DISCLAIMER_URL = "https://github.com/flarialmc/Launchly/blob/master/DISCLAIMER.md"
