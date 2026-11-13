package com.gigi.cca.shared

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gigi.cca.shared.data.AuthState
import com.gigi.cca.shared.data.Repository
import com.gigi.cca.shared.ui.BehaviourScreen
import com.gigi.cca.shared.ui.HomeworkDetailScreen
import com.gigi.cca.shared.ui.HomeworkListScreen
import com.gigi.cca.shared.ui.LoginScreen
import com.gigi.cca.shared.ui.TimetableScreen
import com.gigi.cca.shared.ui.icons.AppIcons
import com.gigi.cca.shared.ui.icons.calendar_month
import com.gigi.cca.shared.ui.icons.list
import com.gigi.cca.shared.ui.theme.ClassChartsAndroidTheme
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Entry point used by Android, desktop and web. [context] is the Android Context, or null.
 * [onNavHostReady] lets the web build bind the navigation to the browser address bar.
 */
class App(private val context: Any? = null) {
    @Composable
    fun Ui(onNavHostReady: suspend (NavController) -> Unit = {}) {
        val repository = remember { sharedRepository(context) }
        ClassChartsAndroidTheme { AppRoot(repository, onNavHostReady) }
    }
}

// One repository per process, so rotating the phone doesn't reload everything.
private var repositoryInstance: Repository? = null

private fun sharedRepository(context: Any?): Repository =
    repositoryInstance ?: Repository(createStore(context)).also { repositoryInstance = it }

// The serial names are what show up in the browser address bar on web, e.g. #timetable.
@Serializable @SerialName("homework") data object HomeworkRoute
@Serializable @SerialName("timetable") data object TimetableRoute
@Serializable @SerialName("behaviour") data object BehaviourRoute
@Serializable @SerialName("homework-details") data class HomeworkDetailRoute(val id: Int)

private class Tab(val label: String, val icon: ImageVector, val route: Any)

@Composable
fun AppRoot(repository: Repository, onNavHostReady: suspend (NavController) -> Unit = {}) {
    val auth by repository.auth.collectAsState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        // Fade between screens only when the kind of state changes, not on every update.
        val screen = when (auth) {
            AuthState.Loading -> 0
            AuthState.LoggedOut -> 1
            is AuthState.LoggedIn -> 2
        }
        Crossfade(targetState = screen) { current ->
            when (current) {
                1 -> LoginScreen(repository)
                2 -> MainScreen(repository, onNavHostReady)
                else -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MainScreen(repository: Repository, onNavHostReady: suspend (NavController) -> Unit) {
    val navController = rememberNavController()
    LaunchedEffect(navController) { onNavHostReady(navController) }
    val tabs = remember {
        listOf(
            Tab("Homework", list, HomeworkRoute),
            Tab("Timetable", calendar_month, TimetableRoute),
            Tab("Behaviour", AppIcons.Star, BehaviourRoute),
        )
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selected = tabs.indexOfFirst { tab -> destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true }
    val showNavigation = destination == null || selected >= 0

    fun open(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Phones get a bottom bar; tablets, desktop and web get a side rail.
        val wide = maxWidth >= 720.dp
        Row(Modifier.fillMaxSize()) {
            if (wide && showNavigation) {
                NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Spacer(Modifier.height(12.dp))
                    tabs.forEachIndexed { i, tab ->
                        NavigationRailItem(
                            selected = i == selected,
                            onClick = { open(tab) },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    if (!wide && showNavigation) {
                        NavigationBar {
                            tabs.forEachIndexed { i, tab ->
                                NavigationBarItem(
                                    selected = i == selected,
                                    onClick = { open(tab) },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                NavHost(
                    navController,
                    startDestination = HomeworkRoute,
                    modifier = Modifier.padding(padding).consumeWindowInsets(padding),
                    enterTransition = { fadeIn(tween(160)) },
                    exitTransition = { fadeOut(tween(160)) },
                    popEnterTransition = { fadeIn(tween(160)) },
                    popExitTransition = { fadeOut(tween(160)) },
                ) {
                    composable<HomeworkRoute> {
                        HomeworkListScreen(repository, onOpen = { id -> navController.navigate(HomeworkDetailRoute(id)) })
                    }
                    composable<HomeworkDetailRoute> { entry ->
                        HomeworkDetailScreen(repository, entry.toRoute<HomeworkDetailRoute>().id, onBack = { navController.popBackStack() })
                    }
                    composable<TimetableRoute> { TimetableScreen(repository) }
                    composable<BehaviourRoute> { BehaviourScreen(repository) }
                }
            }
        }
    }
}
