package com.kylentt.mediaplayer.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.kylentt.mediaplayer.ui.screen.BottomNavigationRoute
import com.kylentt.mediaplayer.ui.screen.Screen
import com.kylentt.mediaplayer.ui.screen.home.HomeScreen
import com.kylentt.mediaplayer.ui.screen.search.SearchScreen
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor
import timber.log.Timber

@Composable
fun Root() {
    Timber.d("ComposeDebug Root")
    RootScreen()
}

@Composable
fun RootScreen() {
    Timber.d("RootScreen")
    val navController = rememberNavController()
    val state = navController.currentBackStackEntryAsState()
    RootScaffold(
        bottomBar = {
            if (shouldShowBottomBar(state.value)) {
                RootBottomBar(state = state) {
                    navController.navigate(it) {
                        restoreState = true
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            }
        }
    ) {
        RootNavHost(rootController = navController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(
    bottomBar: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Timber.d("ComposeDebug RootScaffold")
    Scaffold(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar
    ) {
        content()
    }
}

fun shouldShowBottomBar(entry: NavBackStackEntry?): Boolean {
    return entry?.let { nav ->
        nav.destination.route as String in BottomNavigationRoute.routeList.map { it.route }
    } ?: false
}

@Composable
fun RootBottomBar(
    state: State<NavBackStackEntry?>,
    navigateTo: (String) -> Unit
) {
    Timber.d("ComposeDebug RootBottomBar")
    val screens = BottomNavigationRoute.routeList
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        CompositionLocalProvider(
            LocalRippleTheme provides object : RippleTheme {
                @Composable
                override fun defaultColor(): Color = Color.Transparent
                @Composable
                override fun rippleAlpha(): RippleAlpha = RippleAlpha(
                    0f, 0f, 0f, 0f
                )
            }
        ) {
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val navBar = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
                BottomNavigation(
                    modifier = Modifier
                        .height(56.dp + navBar),
                    backgroundColor = Color.Transparent,
                    elevation = 0.dp
                ) {
                    Timber.d("ComposeDebug RootBottomNavigation")
                    val selected = state.value?.destination?.route
                    val textColor = DefaultColor.getDNTextColor()
                    screens.forEach { screen ->
                        BottomNavigationItem(
                            modifier = Modifier.navigationBarsPadding(),
                            selected = screen.route == selected,
                            selectedContentColor = MaterialTheme.colorScheme.onSurface,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            onClick = {
                                if (screen.route != selected) navigateTo(screen.route)
                            },
                            label = {
                                Text(text = screen.title, color = textColor)
                            },
                            icon = {
                                Icon(imageVector = screen.icon, contentDescription = screen.title)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RootNavHost(
    rootController: NavHostController
) {
    Timber.d("ComposeDebug RootNavigation")
    NavHost(
        navController = rootController,
        startDestination = BottomNavigationRoute.routeName,
    ) {
        bottomBarNavGraph()
    }
}

fun NavGraphBuilder.bottomBarNavGraph() {
    Timber.d("ComposeDebug BottomBarNavigationGraph")
    navigation(
        startDestination = BottomNavigationRoute.HomeScreen.screen.route,
        BottomNavigationRoute.routeName
    ) {
        composable(Screen.HomeScreen.route) {
            HomeScreen()
        }
        composable(Screen.SearchScreen.route) {
            SearchScreen()
        }
    }
}














