package net.grafixmafia.spacestate

import android.os.Bundle
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.grafixmafia.spacestate.directory.DirectoryViewModel
import net.grafixmafia.spacestate.data.TemporarySettings
import net.grafixmafia.spacestate.details.DetailLinks
import net.grafixmafia.spacestate.ui.SpaceDetailsScreen
import net.grafixmafia.spacestate.status.StatusViewModel
import net.grafixmafia.spacestate.ui.SettingsScreen
import net.grafixmafia.spacestate.ui.SpacePickerScreen
import net.grafixmafia.spacestate.ui.StatusScreen
import net.grafixmafia.spacestate.ui.theme.SpaceStateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpaceStateTheme {
                SpaceStateApp()
            }
        }
    }
}

private object Routes {
    const val STATUS = "status"
    const val SETTINGS = "settings"
    const val DETAILS = "details"
    const val LINKED = "linked/{endpoint}"
    const val SPACE_PICKER = "settings/space"
}

@Composable
private fun SpaceStateApp() {
    // Scoped to the activity, so every screen shares one status and one directory.
    val status: StatusViewModel = viewModel(factory = StatusViewModel.Factory)
    val directory: DirectoryViewModel = viewModel(factory = DirectoryViewModel.Factory)
    val statusState by status.state.collectAsStateWithLifecycle()
    val directoryState by directory.state.collectAsStateWithLifecycle()

    // Polls while the app is visible.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            directory.recheck()
            status.poll()
        }
    }

    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.STATUS) {
        composable(Routes.STATUS) {
            StatusScreen(
                state = statusState,
                onRefresh = status::refresh,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDetails = { navController.navigate(Routes.DETAILS) },
            )
        }
        composable(Routes.DETAILS) {
            SpaceDetailsScreen(statusState, onBack = { navController.popBackStack() },
                onLinkedSpace = { endpoint ->
                    DetailLinks.endpoint(endpoint)?.let { navController.navigate("linked/${Uri.encode(it)}") }
                })
        }
        composable(Routes.LINKED) { entry ->
            val endpoint = DetailLinks.endpoint(entry.arguments?.getString("endpoint"))
            if (endpoint == null) {
                LaunchedEffect(entry) { navController.popBackStack() }
            } else {
                val application = LocalContext.current.applicationContext as SpaceStateApplication
                val linked: StatusViewModel = viewModel(
                    viewModelStoreOwner = entry,
                    factory = viewModelFactory {
                        initializer { StatusViewModel(application.statusSource, TemporarySettings(endpoint, statusState.refreshSeconds)) }
                    },
                )
                val linkedState by linked.state.collectAsStateWithLifecycle()
                var showsDetails by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(entry, linked) {
                    entry.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { linked.poll() }
                }
                BackHandler(enabled = showsDetails) { showsDetails = false }
                if (showsDetails) {
                    SpaceDetailsScreen(linkedState, onBack = { showsDetails = false },
                        onLinkedSpace = { next ->
                            DetailLinks.endpoint(next)?.let { navController.navigate("linked/${Uri.encode(it)}") }
                        })
                } else {
                    StatusScreen(linkedState, onRefresh = linked::refresh, onOpenSettings = {},
                        onOpenDetails = { showsDetails = true }, onBack = { navController.popBackStack() })
                }
            }
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                state = statusState,
                onChooseSpace = { navController.navigate(Routes.SPACE_PICKER) },
                onRefreshSecondsChange = status::setRefreshSeconds,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SPACE_PICKER) {
            SpacePickerScreen(
                directory = directoryState,
                selectedEndpoint = statusState.endpoint,
                onAppear = directory::loadIfNeeded,
                onRetry = directory::load,
                onSelect = { endpoint ->
                    status.selectSpace(endpoint)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
