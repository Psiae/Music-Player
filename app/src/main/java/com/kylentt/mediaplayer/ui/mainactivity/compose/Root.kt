package com.kylentt.mediaplayer.ui.mainactivity.compose

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
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
    RootScreen()
}

@OptIn(ExperimentalCoilApi::class)
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
            Timber.d("ComposeDebug RootScaffold Content")
            val context = LocalContext.current
            val density = LocalDensity.current
            val config = LocalConfiguration.current
            val wm = remember { WallpaperManager.getInstance(context) }
            val drw = wm.drawable.toBitmap()
            val pageIndex = remember { mutableStateOf(0) }
            val req = remember { ImageRequest.Builder(context)
                .data(drw)
                .crossfade(true)
                .build()
            }
            val ir = rememberImagePainter(request = req)
            val current = BottomNavigationRoute.routeList.map { it.route }.indexOf(state.value?.destination?.route)
            val size = BottomNavigationRoute.routeList.size
            pageIndex.value = if (current != -1) current else pageIndex.value
            val scrollState = rememberScrollState()
            LaunchedEffect(key1 = pageIndex.value) {
                val pagex = pageIndex.value
                val value = (if (pagex == 0) 0 else drw.width / (size.toFloat() / pagex.toFloat())).toInt()
                Timber.d("LaunchedEffect $value")
                scrollState.animateScrollTo(
                    value,
                    animationSpec = SpringSpec(stiffness = Spring.StiffnessLow)
                )
            }
            CoilShimmerImage(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                painter = ir,
                contentAlignment = Alignment.CenterStart,
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
        RootNavHost(rootController = navController, modifier = Modifier.padding(it))
    }
}
@OptIn(ExperimentalCoilApi::class)
sealed class CoilShimmerState {
    object EMPTY : CoilShimmerState()
    object LOADING : CoilShimmerState()
    object SUCCESS : CoilShimmerState()
    object ERROR : CoilShimmerState()
}

@ExperimentalCoilApi
@Composable
fun CoilShimmerImage(
    modifier: Modifier,
    painter: ImagePainter,
    placeHolderColor: Color = MaterialTheme.colorScheme.surface,
    placeHolderShimmerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    shimmerState: CoilShimmerState = CoilShimmerState.LOADING,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    contentAlignment: Alignment = Alignment.Center,
) {
    val holder = when (shimmerState) {
        CoilShimmerState.EMPTY -> painter.state is ImagePainter.State.Empty
        CoilShimmerState.ERROR -> painter.state is ImagePainter.State.Error
        CoilShimmerState.LOADING -> painter.state is ImagePainter.State.Loading
        CoilShimmerState.SUCCESS -> painter.state is ImagePainter.State.Success
    }
    Image(modifier = modifier
        .then(Modifier
            .placeholder(holder,
            color = placeHolderColor,
            highlight = PlaceholderHighlight.shimmer(placeHolderShimmerColor))
        ),
        painter = painter,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = contentAlignment
    )
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














