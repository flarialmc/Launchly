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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

private enum class Route(val path: String, val label: Int, val icon: Int) {
    LIBRARY("library", R.string.library, R.drawable.ic_list),
    DOWNLOADS("downloads", R.string.downloads, R.drawable.ic_download),
    SETTINGS("settings", R.string.settings, R.drawable.ic_settings),
    ADD_VERSION("add-version", R.string.add_version, R.drawable.ic_add)
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onSignIn: () -> Unit,
    onContinue: () -> Unit,
    onDismissReset: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("onboarding"),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        item { Text(stringResource(R.string.onboarding_body), style = MaterialTheme.typography.bodyLarge) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(stringResource(R.string.onboarding_unofficial), Modifier.padding(20.dp))
            }
        }
        item {
            when (val auth = state.auth) {
                is AuthState.Authenticated -> {
                    Text(stringResource(R.string.signed_in_as, auth.session.displayName ?: auth.session.email))
                    Spacer(Modifier.height(12.dp))
                    Button(onContinue, Modifier.fillMaxWidth().testTag("finish_setup")) {
                        Text(stringResource(R.string.finish_setup))
                    }
                }
                else -> Button(onSignIn, Modifier.fillMaxWidth().testTag("sign_in")) {
                    Text(stringResource(R.string.sign_in_google))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ openUrl(context, PRIVACY_URL) }) { Text(stringResource(R.string.privacy_policy)) }
                TextButton({ openUrl(context, DISCLAIMER_URL) }) { Text(stringResource(R.string.disclaimer)) }
            }
        }
    }
    if (state.showResetNotice) {
        AlertDialog(
            onDismissRequest = onDismissReset,
            title = { Text(stringResource(R.string.reset_notice_title)) },
            text = { Text(stringResource(R.string.reset_notice_body)) },
            confirmButton = { TextButton(onDismissReset) { Text(stringResource(R.string.close)) } }
        )
    }
}

@Composable
fun AppShell(
    app: LaunchlyApp,
    libraryViewModel: LibraryViewModel,
    downloadsViewModel: DownloadsViewModel,
    settingsViewModel: SettingsViewModel,
    addVersionViewModel: AddVersionViewModel,
    onSignIn: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: Route.LIBRARY.path
    val snackbar = remember { SnackbarHostState() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (wide && route != Route.ADD_VERSION.path) {
                MainNavigationRail(navController, route)
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(Route.entries.firstOrNull { it.path == route }?.label ?: R.string.app_name)) },
                        navigationIcon = {
                            if (route == Route.ADD_VERSION.path) {
                                TextButton({ navController.popBackStack() }) { Text(stringResource(R.string.back)) }
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!wide && route != Route.ADD_VERSION.path) MainNavigationBar(navController, route)
                },
                floatingActionButton = {
                    if (route == Route.LIBRARY.path) {
                        FloatingActionButton(
                            onClick = { navController.navigate(Route.ADD_VERSION.path) },
                            modifier = Modifier.testTag("add_version")
                        ) {
                            Icon(
                                painter = painterResource(Route.ADD_VERSION.icon),
                                contentDescription = stringResource(Route.ADD_VERSION.label)
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbar) }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Route.LIBRARY.path,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Route.LIBRARY.path) {
                        val state by libraryViewModel.state.collectAsStateWithLifecycle()
                        LibraryScreen(state, libraryViewModel, navController, snackbar)
                    }
                    composable(Route.DOWNLOADS.path) {
                        val state by downloadsViewModel.state.collectAsStateWithLifecycle()
                        DownloadsScreen(state, downloadsViewModel)
                    }
                    composable(Route.SETTINGS.path) {
                        val state by settingsViewModel.state.collectAsStateWithLifecycle()
                        SettingsScreen(app, state, settingsViewModel, onSignIn, snackbar)
                    }
                    composable(Route.ADD_VERSION.path) {
                        val state by addVersionViewModel.state.collectAsStateWithLifecycle()
                        AddVersionScreen(state, addVersionViewModel)
                        LaunchedEffect(state.addedVersionId) {
                            if (state.addedVersionId != null) {
                                addVersionViewModel.consumeAdded()
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainNavigationBar(navController: NavHostController, current: String) {
    NavigationBar {
        Route.entries.take(3).forEach { route ->
            NavigationBarItem(
                selected = current == route.path,
                onClick = { navigateMain(navController, route.path) },
                icon = { Icon(painterResource(route.icon), contentDescription = null) },
                label = { Text(stringResource(route.label)) }
            )
        }
    }
}

@Composable
private fun MainNavigationRail(navController: NavHostController, current: String) {
    NavigationRail(Modifier.fillMaxHeight()) {
        Spacer(Modifier.height(12.dp))
        Route.entries.take(3).forEach { route ->
            NavigationRailItem(
                selected = current == route.path,
                onClick = { navigateMain(navController, route.path) },
                icon = { Icon(painterResource(route.icon), contentDescription = null) },
                label = { Text(stringResource(route.label)) }
            )
        }
    }
}

private fun navigateMain(controller: NavHostController, path: String) {
    controller.navigate(path) {
        popUpTo(controller.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun LibraryScreen(
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.installed_minecraft), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val installed = state.installed
                    Text(
                        installed?.let { stringResource(R.string.minecraft_version, it.versionName, it.versionCode) }
                            ?: stringResource(R.string.not_installed),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (installed != null) {
                        Button({
                            when (val result = viewModel.launchMinecraft()) {
                                is LaunchResult.Failure -> scope.launch { snackbar.showSnackbar(result.message) }
                                LaunchResult.Launched -> Unit
                            }
                        }) { Text(stringResource(R.string.launch)) }
                    }
                }
            }
        }
        item { Text(stringResource(R.string.managed_versions), style = MaterialTheme.typography.titleMedium) }
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
        is PackageOperationState.PermissionRequired -> AlertDialog(
            onDismissRequest = viewModel::dismissOperation,
            title = { Text(stringResource(R.string.install_permission_title)) },
            text = { Text(stringResource(R.string.install_permission_message)) },
            confirmButton = {
                TextButton({
                    runCatching {
                        installSettingsLauncher.launch(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                        )
                    }.onFailure {
                        scope.launch { snackbar.showSnackbar(linkUnavailableMessage) }
                    }
                }) { Text(stringResource(R.string.open_settings)) }
            },
            dismissButton = { TextButton(viewModel::dismissOperation) { Text(stringResource(R.string.cancel)) } }
        )
        is PackageOperationState.DowngradeConfirmationRequired -> AlertDialog(
            onDismissRequest = viewModel::dismissOperation,
            title = { Text(stringResource(R.string.downgrade_title)) },
            text = { Text(stringResource(R.string.downgrade_message, operation.currentVersion, operation.targetVersion)) },
            confirmButton = {
                Button({ viewModel.confirmDowngrade(operation.versionId) }) { Text(stringResource(R.string.uninstall_and_continue)) }
            },
            dismissButton = { TextButton(viewModel::dismissOperation) { Text(stringResource(R.string.cancel)) } }
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
    Card(Modifier.fillMaxWidth().testTag("version_${item.version.id}")) {
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
                    Text(item.version.versionName, style = MaterialTheme.typography.bodyMedium)
                }
                Box {
                    TextButton({ menuExpanded = true }, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                        Text(stringResource(R.string.more_actions))
                    }
                    DropdownMenu(menuExpanded, { menuExpanded = false }) {
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
            Text(stringResource(R.string.architecture, item.version.architecture.abi))
            Text(stringResource(R.string.download_state, item.download?.status?.displayName() ?: stringResource(R.string.download)))
            item.download?.progress?.let { LinearProgressIndicator(progress = { it }, Modifier.fillMaxWidth()) }
            when (item.download?.status) {
                DownloadStatus.READY -> Button(onInstall) { Text(stringResource(R.string.install)) }
                DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> OutlinedButton(onViewDownload) { Text(stringResource(R.string.view_download)) }
                DownloadStatus.PAUSED, DownloadStatus.FAILED -> Button(onDownload) { Text(stringResource(R.string.resume)) }
                else -> Button(onDownload) { Text(stringResource(R.string.download)) }
            }
        }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.rename_version)) },
            text = { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.version_display_name)) }, singleLine = true) },
            confirmButton = { TextButton({ onRename(name); editing = false }) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton({ editing = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.delete_version_title)) },
            text = { Text(stringResource(R.string.delete_version_message, item.version.displayName)) },
            confirmButton = { Button({ onDelete(); deleting = false }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton({ deleting = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun DownloadsScreen(state: DownloadsUiState, viewModel: DownloadsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("downloads"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.records.isEmpty()) item { EmptyCard(R.string.no_downloads_title, R.string.no_downloads_body) }
        items(state.records, key = { it.first.versionId }) { (download, version) ->
            DownloadCard(download, version, viewModel)
        }
    }
}

@Composable
private fun DownloadCard(download: DownloadRecord, version: ManagedVersion?, viewModel: DownloadsViewModel) {
    Card(Modifier.fillMaxWidth().testTag("download_${download.versionId}")) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(version?.displayName ?: download.versionId, style = MaterialTheme.typography.titleMedium)
            Text(download.status.displayName())
            download.progress?.let { LinearProgressIndicator(progress = { it }, Modifier.fillMaxWidth()) }
            Text(
                download.totalBytes?.let { stringResource(R.string.download_progress, formatBytes(download.bytesDownloaded), formatBytes(it)) }
                    ?: stringResource(R.string.download_size_unknown)
            )
            download.speedBytesPerSecond?.let { Text(stringResource(R.string.download_speed, formatBytes(it))) }
            download.failureMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> OutlinedButton({ viewModel.pause(download.versionId) }) { Text(stringResource(R.string.pause)) }
                    DownloadStatus.PAUSED -> Button({ viewModel.resume(download.versionId) }) { Text(stringResource(R.string.resume)) }
                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> Button({ viewModel.retry(download.versionId) }) { Text(stringResource(R.string.retry)) }
                    DownloadStatus.READY -> Unit
                }
                if (download.status != DownloadStatus.READY) {
                    TextButton({ viewModel.cancel(download.versionId) }) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
}

@Composable
private fun AddVersionScreen(state: AddVersionUiState, viewModel: AddVersionViewModel) {
    Column(Modifier.fillMaxSize().testTag("add_version_screen")) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.search_versions)) },
            singleLine = true
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(state.track == null, { viewModel.setTrack(null) }, { Text(stringResource(R.string.all)) }) }
            item { FilterChip(state.track == ReleaseTrack.RELEASE, { viewModel.setTrack(ReleaseTrack.RELEASE) }, { Text(stringResource(R.string.release)) }) }
            item { FilterChip(state.track == ReleaseTrack.BETA, { viewModel.setTrack(ReleaseTrack.BETA) }, { Text(stringResource(R.string.beta)) }) }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(state.architecture == null, { viewModel.setArchitecture(null) }, { Text(stringResource(R.string.all)) }) }
            items(state.availableArchitectures, key = Architecture::abi) { architecture ->
                FilterChip(state.architecture == architecture, { viewModel.setArchitecture(architecture) }, { Text(architecture.abi) })
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null && state.versions.isEmpty() -> ErrorState(state.error, viewModel::retry)
            else -> {
                if (state.cached) Text(stringResource(R.string.catalog_cached), Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.tertiary)
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.versions.isEmpty()) item { EmptyCard(R.string.no_versions, R.string.no_versions_body) }
                    items(state.versions, key = VersionData::stableId) { version ->
                        Card(Modifier.fillMaxWidth().testTag("catalog_${version.stableId}")) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(version.name, style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(R.string.catalog_version_details, version.code, version.architecture.abi, version.track.displayName()))
                                }
                                Button({ viewModel.add(version) }) { Text(stringResource(R.string.add_version)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text(stringResource(R.string.account), style = MaterialTheme.typography.titleMedium) }
        item {
            SettingsCard {
                when (val auth = state.auth) {
                    is AuthState.Authenticated -> {
                        Text(auth.session.displayName ?: auth.session.email, style = MaterialTheme.typography.titleMedium)
                        Text(auth.session.email)
                        auth.profileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        OutlinedButton(viewModel::refreshProfile, Modifier.fillMaxWidth()) { Text(stringResource(R.string.refresh)) }
                        OutlinedButton(onSignIn, Modifier.fillMaxWidth()) { Text(stringResource(R.string.reauthenticate)) }
                        TextButton(viewModel::signOut, Modifier.fillMaxWidth()) { Text(stringResource(R.string.sign_out)) }
                    }
                    AuthState.SignedOut -> {
                        Text(stringResource(R.string.signed_out))
                        Button(onSignIn) { Text(stringResource(R.string.sign_in_google)) }
                    }
                    AuthState.Loading -> CircularProgressIndicator()
                }
            }
        }
        item { Text(stringResource(R.string.install_permission), style = MaterialTheme.typography.titleMedium) }
        item {
            SettingsCard {
                Text(stringResource(if (state.canInstallPackages) R.string.permission_allowed else R.string.permission_not_allowed))
                if (!state.canInstallPackages) {
                    OutlinedButton({
                        runCatching {
                            installSettingsLauncher.launch(
                                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                            )
                        }.onFailure {
                            scope.launch { snackbar.showSnackbar(linkUnavailableMessage) }
                        }
                    }) { Text(stringResource(R.string.open_settings)) }
                }
            }
        }
        item { Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium) }
        item {
            SettingsCard {
                Text(stringResource(R.string.version_label, state.appVersion))
                HorizontalDivider()
                TextButton({ openUrl(context, SOURCE_URL) }) { Text(stringResource(R.string.source_code)) }
                TextButton({ openUrl(context, PRIVACY_URL) }) { Text(stringResource(R.string.privacy_policy)) }
                TextButton({ openUrl(context, DISCLAIMER_URL) }) { Text(stringResource(R.string.disclaimer)) }
            }
        }
        item {
            SettingsCard {
                Text(stringResource(R.string.diagnostics_description))
                Button({
                    runCatching { diagnosticLauncher.launch("launchly-diagnostics.json") }
                        .onFailure { scope.launch { snackbar.showSnackbar(diagnosticsFailedMessage) } }
                }) { Text(stringResource(R.string.export_diagnostics)) }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun EmptyCard(title: Int, body: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body))
        }
    }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Button(retry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun MessageDialog(title: Int, message: String, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(title)) },
        text = { Text(message) },
        confirmButton = { TextButton(dismiss) { Text(stringResource(R.string.close)) } }
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
