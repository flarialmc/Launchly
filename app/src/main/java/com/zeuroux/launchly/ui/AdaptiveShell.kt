package com.zeuroux.launchly.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zeuroux.launchly.LaunchlyApp
import com.zeuroux.launchly.R
import com.zeuroux.launchly.viewmodel.AddVersionViewModel
import com.zeuroux.launchly.viewmodel.DownloadsViewModel
import com.zeuroux.launchly.viewmodel.LibraryViewModel
import com.zeuroux.launchly.viewmodel.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal enum class Route(
    val path: String,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int
) {
    LIBRARY("library", R.string.library, R.drawable.ic_list),
    DOWNLOADS("downloads", R.string.downloads, R.drawable.ic_download),
    SETTINGS("settings", R.string.settings, R.drawable.ic_settings),
    ADD_VERSION("add-version", R.string.add_version, R.drawable.ic_add)
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
    val routePath = backStackEntry?.destination?.route ?: Route.LIBRARY.path
    val route = Route.entries.firstOrNull { it.path == routePath } ?: Route.LIBRARY
    val snackbar = remember { SnackbarHostState() }

    LaunchlyBackdrop {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(22.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    WideNavigation(
                        current = routePath,
                        modifier = Modifier.width(190.dp).fillMaxHeight(),
                        onNavigate = { navigateMain(navController, it) }
                    )
                    ContentPanel(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        route = route,
                        navController = navController,
                        app = app,
                        libraryViewModel = libraryViewModel,
                        downloadsViewModel = downloadsViewModel,
                        settingsViewModel = settingsViewModel,
                        addVersionViewModel = addVersionViewModel,
                        onSignIn = onSignIn,
                        snackbar = snackbar,
                        wide = true
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ContentPanel(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        route = route,
                        navController = navController,
                        app = app,
                        libraryViewModel = libraryViewModel,
                        downloadsViewModel = downloadsViewModel,
                        settingsViewModel = settingsViewModel,
                        addVersionViewModel = addVersionViewModel,
                        onSignIn = onSignIn,
                        snackbar = snackbar,
                        wide = false
                    )
                    if (route != Route.ADD_VERSION) {
                        CompactNavigation(routePath) { navigateMain(navController, it) }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = LaunchlyDesign.SurfaceRaised,
                    contentColor = LaunchlyDesign.TextPrimary,
                    actionColor = LaunchlyDesign.Accent,
                    shape = LaunchlyDesign.CardShape
                )
            }
        }
    }
}

@Composable
private fun ContentPanel(
    modifier: Modifier,
    route: Route,
    navController: NavHostController,
    app: LaunchlyApp,
    libraryViewModel: LibraryViewModel,
    downloadsViewModel: DownloadsViewModel,
    settingsViewModel: SettingsViewModel,
    addVersionViewModel: AddVersionViewModel,
    onSignIn: () -> Unit,
    snackbar: SnackbarHostState,
    wide: Boolean
) {
    Surface(
        modifier = modifier,
        color = LaunchlyDesign.SurfacePrimary,
        contentColor = LaunchlyDesign.TextPrimary,
        shape = LaunchlyDesign.PanelShape
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = if (wide) 24.dp else 18.dp,
                        end = if (wide) 24.dp else 18.dp,
                        top = if (wide) 22.dp else 18.dp,
                        bottom = 12.dp
                    ),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (route == Route.ADD_VERSION) {
                        TextButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = LaunchlyDesign.TextSecondary)
                        ) {
                            Text(stringResource(R.string.back))
                        }
                    }
                    LaunchlyPageTitle(route.label)
                    Spacer(Modifier.weight(1f))
                }
                NavHost(
                    navController = navController,
                    startDestination = Route.LIBRARY.path,
                    modifier = Modifier.weight(1f).fillMaxWidth()
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
            if (route == Route.LIBRARY) {
                LaunchlyAddVersionButton(
                    onClick = { navController.navigate(Route.ADD_VERSION.path) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WideNavigation(
    current: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = modifier,
        color = LaunchlyDesign.SurfacePrimary,
        contentColor = LaunchlyDesign.TextPrimary,
        shape = LaunchlyDesign.PanelShape
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            LaunchlyBrandRow()
            Spacer(Modifier.height(30.dp))
            Route.entries.take(3).forEach { route ->
                NavigationButton(
                    route = route,
                    selected = current == route.path,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(route.path) }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
internal fun CompactNavigation(current: String, onNavigate: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        color = LaunchlyDesign.SurfacePrimary,
        contentColor = LaunchlyDesign.TextPrimary,
        shape = LaunchlyDesign.PanelShape
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Route.entries.take(3).forEach { route ->
                NavigationButton(
                    route = route,
                    selected = current == route.path,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(route.path) },
                    compact = true
                )
            }
        }
    }
}

@Composable
internal fun LaunchlyAddVersionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(58.dp).testTag("add_version"),
        shape = RoundedCornerShape(17.dp),
        containerColor = LaunchlyDesign.Accent,
        contentColor = Color.White
    ) {
        Icon(
            painter = painterResource(Route.ADD_VERSION.icon),
            contentDescription = stringResource(Route.ADD_VERSION.label),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun NavigationButton(
    route: Route,
    selected: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(if (compact) 56.dp else 50.dp).testTag("nav_${route.path}"),
        shape = LaunchlyDesign.ControlShape,
        color = if (selected) LaunchlyDesign.Accent else LaunchlyDesign.NavigationSurface,
        contentColor = if (selected) Color.White else LaunchlyDesign.TextSecondary
    ) {
        if (compact) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(painterResource(route.icon), contentDescription = null, Modifier.size(21.dp))
                Text(
                    text = stringResource(route.label),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(painterResource(route.icon), contentDescription = null, Modifier.size(22.dp))
                Text(
                    text = stringResource(route.label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
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
