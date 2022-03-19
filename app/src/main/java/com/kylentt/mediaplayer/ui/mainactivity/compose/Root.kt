package com.kylentt.mediaplayer.ui.mainactivity.compose

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.RootBottomNav
import com.kylentt.mediaplayer.ui.mainactivity.compose.root.BottomNavigationItem
import com.kylentt.mediaplayer.ui.mainactivity.compose.root.BottomNavigationRoute
import com.kylentt.mediaplayer.ui.mainactivity.compose.screen.home.HomeScreen
import com.kylentt.mediaplayer.ui.mainactivity.compose.screen.library.LibraryScreen
import com.kylentt.mediaplayer.ui.mainactivity.compose.screen.search.SearchScreen
import timber.log.Timber

@Composable
fun Root() {
    Timber.d("ComposeDebug Root")

    val context = LocalContext.current
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
                RootBottomNav(
                    ripple = true,
                    selectedRoute = state.value!!.destination.route!!
                ) {
                    navController.navigate(it) {
                        restoreState = true
                        launchSingleTop = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (ContextCompat.checkSelfPermission(LocalContext.current,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val ir = ImageRequest.Builder(LocalContext.current)
                .data(WallpaperManager.getInstance(LocalContext.current).drawable)
                .build()
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                painter = rememberImagePainter(ir),
                contentScale = ContentScale.Crop,
                alignment = Alignment.CenterStart,
                contentDescription = "Wallpaper"
            )
        }
        RootNavHost(rootController = navController, modifier = Modifier.padding(it))
    }
}

fun shouldShowBottomBar(entry: NavBackStackEntry?): Boolean {
    return entry?.let { nav ->
        nav.destination.route as String in BottomNavigationRoute.routeList.map { it.route }
    } ?: false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(
    bottomBar: @Composable () -> Unit,
    containerColor: Color,
    content: @Composable (PaddingValues) -> Unit
) {
    Timber.d("ComposeDebug RootScaffold")
    Scaffold(

        modifier = Modifier,
        bottomBar = bottomBar,
        containerColor = containerColor,
    ) {
        content(it)
    }
}

@Composable
fun RootNavHost(
    modifier: Modifier,
    rootController: NavHostController
) {
    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = BottomNavigationRoute.routeName,
    ) {
        bottomNavGraph()
    }
}

fun NavGraphBuilder.extraNavGraph() {

}

fun NavGraphBuilder.bottomNavGraph() {
    navigation(
        startDestination = BottomNavigationRoute.HomeScreen.screen.route,
        BottomNavigationRoute.routeName
    ) {
        composable(BottomNavigationItem.Home.route) {
            HomeScreen()
        }
        composable(BottomNavigationItem.Search.route) {
            SearchScreen()
        }
        composable(BottomNavigationItem.Library.route) {
            LibraryScreen()
        }
    }
}














