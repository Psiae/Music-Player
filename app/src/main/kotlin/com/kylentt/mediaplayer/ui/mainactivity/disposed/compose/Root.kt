package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.WallpaperManager
import android.content.res.Configuration
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.kylentt.mediaplayer.core.util.handler.MediaItemHandler
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components.RootBottomNav
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.root.BottomNavigationItem
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.root.BottomNavigationRoute
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.home.HomeScreen
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.library.LibraryScreen
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.search.SearchScreen
import com.kylentt.musicplayer.core.helper.PermissionHelper
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
        val context = LocalContext.current
        if (PermissionHelper.checkStoragePermission(context) && navWallpaper) {
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
internal fun NavWallpaper(
    controller: ControllerViewModel = viewModel(),
    current: Int,
    size: Int
) {
    Timber.d("ComposeDebug Root NavWallpaper $current")

    val context = LocalContext.current

    val itemBitmap by remember { controller.playerCurrentBitmap }
    val wm = remember(context) { WallpaperManager.getInstance(context).drawable.toBitmap() }

    val data = itemBitmap ?: wm
    val req = remember(itemBitmap) {
        Timber.d("NavWallaper New Image Request $data")
        ImageRequest.Builder(context)
            .diskCachePolicy(CachePolicy.ENABLED)
            .data(data)
            .crossfade(300)
            .build()
    }

    val painter = rememberImagePainter(req)
    val scrollState = rememberScrollState()

    val scale = with(LocalConfiguration.current) { 
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            ContentScale.FillHeight
        } else  {
            ContentScale.FillWidth
        }
    }

    CoilShimmerImage(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxSize()
            .horizontalScroll(scrollState),
        painter = painter,
        contentAlignment = Alignment.CenterStart,
        contentScale = scale,
        contentDescription = null,
    )

    LaunchedEffect(current) {
        val value = (current.toFloat() / (size -1) * (scrollState.maxValue)).toInt()
        scrollState.animateScrollTo(value, animationSpec = SpringSpec(stiffness = Spring.StiffnessLow))
    }

    LaunchedEffect(scrollState.maxValue) {
        val value = (current.toFloat() / (size -1) * (scrollState.maxValue)).toInt()
        scrollState.scrollTo(value)
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
    Image(
        modifier = modifier
            .then(Modifier
                .placeholder(false,
                    color = placeHolderColor,
                    highlight = PlaceholderHighlight
                        .shimmer(placeHolderShimmerColor)
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














