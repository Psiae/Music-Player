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
import androidx.compose.ui.graphics.compositeOver
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
import com.kylentt.mediaplayer.ui.screen.Library.LibraryScreen
import com.kylentt.mediaplayer.ui.screen.Root.BottomBarScreen
import com.kylentt.mediaplayer.ui.screen.Screen
import com.kylentt.mediaplayer.ui.screen.home.HomeScreen
import com.kylentt.mediaplayer.ui.screen.search.SearchScreen
import com.kylentt.mediaplayer.ui.theme.md3.AppTypography
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor
import timber.log.Timber
import kotlin.math.ln

@Composable
fun Root() {
    Timber.d("ComposeDebug Root")
    RootScreen()
}

@Composable
fun RootScreen() {
    Timber.d("ComposeDebug RootScreen")
    val navController = rememberNavController()
    val state = navController.currentBackStackEntryAsState()

    RootScaffold(
        bottomBar = {
            if (shouldShowBottomBar(entry = state.value)) {
                RootBottomBar(ripple = false, state = state) {
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
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
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
    ripple: Boolean,
    state: State<NavBackStackEntry?>,
    navigateTo: (String) -> Unit,
) {
    Timber.d("ComposeDebug RootBottomBar")
    val screens = BottomNavigationRoute.routeList

    val alpha = ((4.5f * ln(2.dp.value /* Tonal Elevation */ + 1)) + 2f) / 100f
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        color = primary.copy(alpha = alpha).compositeOver(surface)
    ) {
        if (!ripple) {
            NoRipple {
                RootBottomBarContent(state = state, navigateTo = navigateTo, screens = screens)
            }
        } else RootBottomBarContent(state = state, navigateTo = navigateTo, screens = screens)
    }
}

@Composable
fun NoRipple(
    content: @Composable () -> Unit
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
        content()
    }
}

@Composable
fun RootBottomBarContent(
    state: State<NavBackStackEntry?>,
    navigateTo: (String) -> Unit,
    screens: List<BottomBarScreen>
) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val navBar =
            with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
        BottomNavigation(
            modifier = Modifier.height(56.dp + navBar),
            backgroundColor = Color.Transparent,
            elevation = 0.dp,
        ) {
            Timber.d("ComposeDebug RootBottomNavigation")
            val selected = state.value?.destination?.route
            val textColor = DefaultColor.getDNTextColor()
            screens.forEach { screen ->
                val isSelected = (screen.route == selected)
                BottomNavigationItem(
                    modifier = Modifier.navigationBarsPadding(),
                    selected = isSelected,
                    selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    unselectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(
                        alpha = 0.25f),
                    onClick = {
                        if (!isSelected) navigateTo(screen.route)
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) screen.icon else screen.outlinedIcon,
                            contentDescription = screen.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = 0.5f)
                        )
                    },
                    label = {
                        Text(
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = AppTypography.labelMedium.fontWeight,
                            fontSize = AppTypography.labelMedium.fontSize,
                            fontStyle = AppTypography.labelMedium.fontStyle,
                            lineHeight = AppTypography.labelMedium.lineHeight,
                            text = screen.title
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun RootNavHost(
    rootController: NavHostController
) {
    Timber.d("ComposeDebug RootNavHost")
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
        composable(Screen.LibraryScreen.route) {
            LibraryScreen()
        }
    }
}














