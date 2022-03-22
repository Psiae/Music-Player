package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.WallpaperManager
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
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
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components.RootBottomNav
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.root.BottomNavigationItem
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.root.BottomNavigationRoute
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.home.HomeScreen
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.library.LibraryScreen
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.search.SearchScreen
import timber.log.Timber

@Composable
fun Root() {
    Timber.d("ComposeDebug Root")
    RootScreen()
}

@Composable
fun RootScreen() {
    Timber.d("ComposeDebug RootScreen")
    RootNavigation(
        navController = rememberNavController(),
        navWallpaper = true
    )
}

@Composable
fun RootNavigation(
    navController: NavHostController,
    navWallpaper: Boolean
) {
    Timber.d("ComposeDebug RootNavigation")
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
    ) { padding ->
        if (ContextCompat.checkSelfPermission(LocalContext.current, READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED && navWallpaper
        ) {
            Timber.d("ComposeDebug RootScaffold Content")
            val currentIndex = BottomNavigationRoute.routeList.map { it.route }.indexOf(state.value?.destination?.route)
            val wps = remember {
                mutableStateOf(0)
            }
            if (currentIndex != -1) wps.value = currentIndex
            NavWallpaper(current = wps.value, size = BottomNavigationRoute.routeList.size)
        }
        RootNavHost(rootController = navController, modifier = Modifier.padding(padding))
    }
}

@Composable
@RequiresPermission(anyOf = [READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE])
fun NavWallpaperState(
    index: Int,
    size: Int = BottomNavigationRoute.routeList.size
) {
    Timber.d("ComposeDebug NavWallpaperState $index")
}

@Composable
@RequiresPermission(anyOf = [READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE])
fun NavWallpaper(
    current: Int,
    size: Int
) {
    Timber.d("ComposeDebug Root NavWallpaper $current")

    val context = LocalContext.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidth = with(density) { config.screenWidthDp.dp.toPx() }
    val wm = remember { WallpaperManager.getInstance(context).drawable.toBitmap() }
    val req = remember { ImageRequest.Builder(context)
        .data(wm)
        .crossfade(true)
        .build()
    }
    val scrollState = rememberScrollState()
    CoilShimmerImage(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        painter = rememberImagePainter(request = req),
        contentAlignment = Alignment.CenterStart,
        contentScale = ContentScale.Crop,
        contentDescription = null,
    )
    LaunchedEffect(key1 = current) {
        Timber.d("ComposeDebug NavWallpaper LaunchedEffect $current")
        scrollState.animateScrollTo(
            with((current.toFloat() / size) * wm.width) {
                when {
                    this < 400 * current -> this / 1.5
                    else -> this
                }
            }.toInt().also { Timber.d("ComposeDebug NavWallpaper Animate to $it from width of ${wm.width}") },
            animationSpec = SpringSpec(stiffness = Spring.StiffnessLow)
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
sealed class CoilShimmerState {
    object EMPTY : CoilShimmerState()
    object LOADING : CoilShimmerState()
    object SUCCESS : CoilShimmerState()
    object ERROR : CoilShimmerState()
}

@Composable
@OptIn(ExperimentalCoilApi::class)
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
                highlight = PlaceholderHighlight.shimmer(placeHolderShimmerColor)
            )
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
    content: @Composable (PaddingValues) -> Unit,
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
    rootController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = BottomNavigationRoute.routeName,
    ) {
        bottomNavGraph()
    }
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














