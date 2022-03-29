package com.kylentt.musicplayer.ui.musicactivity.compose.musicroot

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
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
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.NavWallpaper
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.home.HomeScreen
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.library.LibraryScreen
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.screen.search.SearchScreen
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.ui.musicactivity.compose.environtment.NoRipple
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.AppTypography
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.ColorHelper

@Composable
@RequiresPermission(anyOf = [Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE])
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
        navWallpaper
    )
}

@Composable
fun MusicRootNavigator(
    hostController: NavHostController,
    shouldShowBottomNav: Boolean,
    navWallpaper: Boolean
) {
    val stateHolder = hostController.currentBackStackEntryAsState()

    MusicRootScaffold(
        entry = stateHolder.value,
        onNavigate = { hostController.navigate(it) {
            restoreState = true
            launchSingleTop = true
            popUpTo(hostController.graph.findStartDestination().id) {
                saveState = true
            } }
        },
        navWallpaper = navWallpaper
    ) {
        MusicRootNavHost(modifier = Modifier.padding(it), rootController = hostController)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicRootScaffold(
    entry: NavBackStackEntry?,
    navWallpaper: Boolean,
    onNavigate: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            if (shouldShowBottomNav(entry = entry)) {
                MusicRootBottomNav(
                    ripple = true,
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
            NavWallpaper(current = MusicRootNavigation.routeList.map { it.routeName }.indexOf(entry?.destination?.route), MusicRootNavigation.routeList.size)
        }
        content(it)
    }

}

@Composable
fun MusicRootBottomNav(
    ripple: Boolean,
    itemList: List<MusicRootBottomItem>,
    selectedItem: MusicRootBottomItem,
    navigateTo: (String) -> Unit
) {
    Surface(
        color = ColorHelper.getTonedSurface()
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
                    .padding(top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemList.forEach { item ->
                    if (ripple) {
                        MusicRootBottomNavItem(
                            item = item,
                            isSelected = item.routeName == selectedItem.routeName,
                        ) {
                            navigateTo(it)
                        }
                    } else NoRipple {
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

    val background = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    val icon = if (isSelected) item.selectedIcon else item.notSelectedIcon

    val iconTint = if (isSelected) {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f).compositeOver(Color.White)
    } else {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f).compositeOver(Color.White)
    }

    val textColor = if (isSelected) ColorHelper.getDNTextColor() else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier,
        containerColor = background,
        shape = CircleShape
    ) {

        Row(
            modifier = Modifier
                .clipToBounds()
                .clickable(true, onClick = { onClick(item.routeName) })
                .padding(start = 10.dp, top = 5.dp, bottom = 5.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                modifier = Modifier.size(30.dp),
                imageVector = icon,
                contentDescription = item.title,
                tint = iconTint
            )

            Spacer(modifier = Modifier.width(2.dp))

            AnimatedVisibility(visible = isSelected) {
                Text(
                    color = textColor,
                    fontWeight = AppTypography.labelMedium.fontWeight,
                    fontSize = AppTypography.bodyMedium.fontSize,
                    fontStyle = AppTypography.labelMedium.fontStyle,
                    lineHeight = AppTypography.labelMedium.lineHeight,
                    text = item.title
                )
            }
        }
    }
}

@Composable
fun MusicRootNavHost(
    modifier: Modifier,
    rootController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = MusicRootNavigation.routeName,
    ) {
        bottomNavGraph()
    }
}

fun NavGraphBuilder.bottomNavGraph() {
    navigation(
        startDestination = MusicRootBottomItem.HomeItem.routeName,
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
    }
}

@Composable
fun shouldShowBottomNav(
    entry: NavBackStackEntry?
): Boolean {
    return MusicRootNavigation.routeList.map { it.routeName }.find { it == entry?.destination?.route }?.let { true } ?: false
}