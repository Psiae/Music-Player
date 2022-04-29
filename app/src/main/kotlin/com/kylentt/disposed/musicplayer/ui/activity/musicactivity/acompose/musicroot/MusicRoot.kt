package com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.musicroot

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
import com.kylentt.mediaplayer.app.delegates.device.DeviceWallpaperDelegate
import com.kylentt.mediaplayer.app.settings.WallpaperSettings.Source.*
import com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.screen.home.HomeScreen
import com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.screen.library.LibraryScreen
import com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.screen.search.SearchScreen
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.LifeCycleExtension.RecomposeOnEvent
import com.kylentt.disposed.musicplayer.core.helper.PermissionHelper
import com.kylentt.disposed.musicplayer.domain.MediaViewModel
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.environtment.NoRipple
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.theme.md3.AppTypography
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.theme.md3.ColorHelper
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun MusicRoot(
    navWallpaper: Boolean
) {
    MusicRootContent(navWallpaper)
}

@Composable
fun MusicRootContent(
    navWallpaper: Boolean
) {
    val navController = rememberNavController()
    MusicRootNavigator(
        hostController = navController,
        shouldShowBottomNav = shouldShowBottomNav(entry = navController.currentBackStackEntry),
        navWallpaper = navWallpaper
    )
}

@Composable
internal fun MusicRootNavigator(
    vm: MediaViewModel = viewModel(),
    hostController: NavHostController,
    shouldShowBottomNav: Boolean,
    navWallpaper: Boolean
) {
    val context = LocalContext.current

    val stateHolder = hostController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()

    MusicRootScaffold(
        entry = stateHolder.value,
        onNavigate = {
            hostController.navigate(it) {
                restoreState = true
                launchSingleTop = true
                popUpTo(hostController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        },
        navWallpaper = navWallpaper
    ) {
        MusicRootNavHost(modifier = Modifier.padding(it), rootController = hostController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicRootScaffold(
    vm: MediaViewModel = viewModel(),
    entry: NavBackStackEntry?,
    navWallpaper: Boolean,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {

    Timber.d("ComposeDebug MusicRootScaffold")

    val settings = vm.appSettings.collectAsState()

    Scaffold(
        bottomBar = {
            if (shouldShowBottomNav(entry = entry)) {
                MusicRootBottomNav(
                    itemList = MusicRootNavigation.routeList,
                    selectedItem = MusicRootNavigation.routeList
                        .find { it.routeName == entry?.destination?.route }!!
                ) {
                    onNavigate(it)
                }
            }
        }
    ) {
        if (navWallpaper && PermissionHelper.checkStoragePermission()) {

            LocalLifecycleOwner.current.lifecycle.RecomposeOnEvent(
                onEvent = Lifecycle.Event.ON_START
            ) {
                val wallpaper by DeviceWallpaperDelegate
                val wallpaperPadding =
                    if (ColorHelper.getNavBarColor().alpha < 1) PaddingValues(0.dp) else it


                val localContext = LocalContext.current
                val density = LocalDensity.current
                val hpx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
                val wpx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                val background = MaterialTheme.colorScheme.surfaceVariant

                val deviceWallpaper = remember(wallpaper) {
                    wallpaper?.let { drawable -> drawable as BitmapDrawable }
                }
                val mediaItemBitmapDrawable = remember(vm.itemBitmap.value) {
                    vm.itemBitmap.value.bitmapDrawable
                }
                val systemModeBitmap = remember(isSystemInDarkTheme()) {
                    // No need for key, just for clarity
                    Paint()
                        .apply {
                            color = background
                            style = PaintingStyle.Fill
                        }
                        .let { paint ->
                            ImageBitmap(wpx.roundToInt(), hpx.roundToInt()).let { bmp ->
                                Canvas(bmp).drawRect(0f,0f,wpx,hpx, paint)
                                BitmapDrawable(localContext.resources, bmp.asAndroidBitmap())
                            }
                        }
                }
                val alt = remember(settings.value, vm.itemBitmap, wallpaper) {
                    when (settings.value.wallpaperSettings.sourceALT) {
                        DEVICE_WALLPAPER -> deviceWallpaper
                        MEDIA_ITEM -> mediaItemBitmapDrawable
                        SYSTEM_MODE -> systemModeBitmap
                    }
                }
                val wp = remember(settings.value, vm.itemBitmap, wallpaper) {
                    when (settings.value.wallpaperSettings.source) {
                        DEVICE_WALLPAPER -> deviceWallpaper
                        MEDIA_ITEM -> mediaItemBitmapDrawable
                        SYSTEM_MODE -> systemModeBitmap
                    }
                }
                val nWallpaper = wp ?: alt
                /*com.kylentt.mediaplayer.ui.activity.mainactivity.compose.NavWallpaper(
                    modifier = Modifier.padding(wallpaperPadding),
                    wallpaper = nWallpaper,
                    size = MusicRootNavigation.routeList.size,
                    fadeDuration = 500,
                    currentIndex = MusicRootNavigation.routeList
                        .map { it.routeName }
                        .indexOf(entry?.destination?.route),
                )*/
            }
        }
        content(it)
    }

}

@Composable
fun MusicRootBottomNav(
    itemList: List<MusicRootBottomItem>,
    selectedItem: MusicRootBottomItem,
    navigateTo: (String) -> Unit
) {
    NoRipple {
        Surface(
            color = ColorHelper.getNavBarColor()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 5.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemList.forEach { item ->
                        MusicRootBottomNavItem(
                            item = item,
                            isSelected = item.routeName == selectedItem.routeName,
                        ) {
                            navigateTo(it)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicRootBottomNavItem(
    item: MusicRootBottomItem,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {

    val background =
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    val icon = if (isSelected) item.selectedIcon else item.notSelectedIcon

    val iconTint = if (isSelected) {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(
            alpha = 0.75f
        ).compositeOver(Color.White)
    } else {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.25f
        ).compositeOver(Color.White)
    }

    val textColor =
        if (isSelected) ColorHelper.getDNTextColor() else MaterialTheme.colorScheme.onSurfaceVariant

    val iconSize = 30.dp
    val textHeight = with(LocalDensity.current) {
        AppTypography.labelMedium.lineHeight.toDp()
    }

    Row(
        modifier = Modifier
            .clickable(true, onClick = { onClick(item.routeName) })
            .size(iconSize + textHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                modifier = Modifier.size(iconSize),
                imageVector = icon,
                contentDescription = item.title,
                tint = iconTint
            )

            AnimatedVisibility(visible = isSelected) {
                Text(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = AppTypography.labelMedium.fontWeight,
                    fontSize = AppTypography.labelMedium.fontSize,
                    fontStyle = AppTypography.labelMedium.fontStyle,
                    lineHeight = AppTypography.labelMedium.lineHeight,
                    text = item.title
                )
            }
        }
    }
}

@Composable
internal fun MusicRootNavHost(
    vm: MediaViewModel = viewModel(),
    modifier: Modifier,
    rootController: NavHostController,
    start: Int = 0
) {
    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = MusicRootNavigation.routeName,
    ) {
        bottomNavGraph(MusicRootNavigation.routeList[start].routeName)
    }
}

fun NavGraphBuilder.bottomNavGraph(
    start: String = MusicRootNavigation.routeList.first().routeName
) {
    navigation(
        startDestination = start,
        MusicRootNavigation.routeName
    ) {
        composable(MusicRootBottomItem.HomeItem.routeName) {
            HomeScreen()
        }
        composable(MusicRootBottomItem.SearchItem.routeName) {
            SearchScreen()
        }
        composable(MusicRootBottomItem.LibraryScreen.routeName) {
            LibraryScreen()
        }
        composable(MusicRootBottomItem.ExtraScreen.routeName) {
            LibraryScreen()
        }
    }
}

@Composable
fun shouldShowBottomNav(
    entry: NavBackStackEntry?
): Boolean {
    return MusicRootNavigation.routeListString.find { it == entry?.destination?.route }
        ?.let { true }
        ?: false
}
