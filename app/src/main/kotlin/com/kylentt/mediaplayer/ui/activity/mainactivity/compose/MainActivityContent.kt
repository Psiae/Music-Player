@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)

package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import android.content.Intent
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.core.app.settings.AppSettings
import com.kylentt.mediaplayer.core.app.settings.WallpaperSettings
import com.kylentt.mediaplayer.core.app.settings.WallpaperSettings.Source.*
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClearSync
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.ComposableExtension.noPadding
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.theme.AppTypography
import com.kylentt.mediaplayer.ui.compose.rememberWallpaperBitmapAsState
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.core.app.delegates.AppDelegate
import com.kylentt.musicplayer.core.app.delegates.device.StoragePermissionHelper
import com.kylentt.musicplayer.ui.compose.util.PermissionHelper
import com.kylentt.musicplayer.ui.main.compose.theme.Material3Theme
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun MainActivityContent(
    mainViewModel: MainViewModel = viewModel(),
    mediaViewModel: MediaViewModel = viewModel()
) {
    val appSettings = mainViewModel.appSettings.collectAsState()

    Material3Theme {

        PermissionHelper.RequirePermissions(
			permissions = listOf(
				PermissionHelper.Permission.READ_EXTERNAL_STORAGE,
				PermissionHelper.Permission.WRITE_EXTERNAL_STORAGE,
			),
            whenAllDenied = {
				StorageDenied(state = it)
			},
            showRationale = {
				/* Temporary */ StorageDenied(state = it)
			},
        ) {
			val permission =
				StoragePermissionHelper.checkReadWriteStoragePermission(LocalContext.current)

			require(permission) { "App Storage Permission is Not Granted" }

            with(mainViewModel) {
                pendingStorageGranted.forEachClearSync()
                pendingStorageIntent.forEachClearSync { mediaViewModel.handleMediaIntent(it) }
            }

            MainActivityRoot(appSettings.value)
        }
    }
}

@RequiresPermission(anyOf = [
    StoragePermissionHelper.Read_External_Storage,
    StoragePermissionHelper.Write_External_Storage
])
@Composable
private fun MainActivityRoot(
    appSettings: AppSettings
) {
    val navController = rememberAnimatedNavController()

    RootScaffold(
        appSettings = appSettings,
        navController = navController
    ) { padding ->
        AnimatedMainAppNavigator(
            modifier = Modifier.padding(padding),
            controller = navController
        )
    }
}

@Composable
private fun RootScaffold(
    appSettings: AppSettings,
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val backStackEntry = navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            if (showBottomNav(backStackEntry.value)) {
                val backgroundColor = ColorHelper.getTonedSurface(elevation = 2.dp)
                    .copy(alpha = appSettings.navigationSettings.bnvSettings.visibility / 100)
                RootBottomNavigation(
                    appSettings = appSettings,
                    backgroundColor = backgroundColor,
                    modifier = Modifier.fillMaxWidth(),
                    selectedItem = MainBottomNavItems
                        .map { it.screen }
                        .find { it.route == backStackEntry.value!!.destination.route }!!,
                    onItemClicked = { item ->
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        val isBnvTransparent = appSettings.navigationSettings.bnvSettings.visibility < 100
        val padding = if (isBnvTransparent) noPadding else it
        if (appSettings.wallpaperSettings.mode == WallpaperSettings.Mode.NAVIGATION) {
            MainActivityNavWallpaper(
                modifier = Modifier.padding(padding),
                backstackEntry = backStackEntry.value,
                appSettings = appSettings
            )
        }
        content(padding)
    }
}

@Immutable
private object NoRippleTheme : RippleTheme {
	@Composable override fun defaultColor(): Color = Color.Transparent

	@Composable override fun rippleAlpha(): RippleAlpha {
		return RippleAlpha(0F,0F,0F,0F)
	}
}

@Composable
private fun NoRipple(content: @Composable () -> Unit) {
    CompositionLocalProvider (LocalRippleTheme provides NoRippleTheme) { content() }
}

@Composable
private fun RootBottomNavigation(
    appSettings: AppSettings,
    backgroundColor: Color,
    modifier: Modifier,
    selectedItem: Screen,
    onItemClicked: (Screen) -> Unit
) {
    NoRipple {

        Surface(
            color = backgroundColor
        ) {
            Box(
                modifier = Modifier
					.fillMaxWidth()
					.navigationBarsPadding(),
            ) {

                val contentColor = if (isSystemInDarkTheme()) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
					MaterialTheme.colorScheme.onSecondaryContainer
                }

                BottomNavigation(
                    modifier = modifier,
                    elevation = 0.dp,
                    backgroundColor = Color.Transparent,
                    contentColor = contentColor,
                ) {
                    MainBottomNavItems.forEach { item ->
                        BottomNavigationItem(
                            alwaysShowLabel = false,
                            selected = item.screen == selectedItem,
                            icon = {
                                MainBottomNavItemIcon(
                                    item = item, selected = item.screen == selectedItem
                                )
                            },
                            label = {
                                Text(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = AppTypography.labelMedium.fontWeight,
                                    fontSize = AppTypography.labelMedium.fontSize,
                                    fontStyle = AppTypography.labelMedium.fontStyle,
                                    lineHeight = AppTypography.labelMedium.lineHeight,
                                    text = item.screen.label
                                )
                            },
                            onClick = { onItemClicked(item.screen) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationBarsSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier
        .then(Modifier
            .height(
                with(LocalDensity.current) {
                    WindowInsets.navigationBars.getBottom(this).toDp()
                }
            )
        )
    )
}

@Composable
private fun AnimatedVisibilityText(visible: Boolean, text: String) {
    AnimatedVisibility(visible = visible) {
        Text(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = AppTypography.labelMedium.fontWeight,
            fontSize = AppTypography.bodyMedium.fontSize,
            fontStyle = AppTypography.labelMedium.fontStyle,
            lineHeight = AppTypography.labelMedium.lineHeight,
            text = text
        )
    }
}

private fun showBottomNav(stack: NavBackStackEntry?): Boolean =
    MainBottomNavItems.map { it.screen }.find { it.route == stack?.destination?.route } != null


@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun StorageDenied(state: MultiplePermissionsState) {
	val localContext = LocalContext.current

	val permission = StoragePermissionHelper.checkReadWriteStoragePermission(localContext)

	check(!permission)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )
    val intent = Intent()
        .apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = "package:${localContext.packageName}".toUri()
        }
    PermissionScreen(grantButtonText = "Grant Storage Permission") {
		if (state.shouldShowRationale) {
			state.launchMultiplePermissionRequest()
		} else {
			launcher.launch(intent)
		}
    }
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun ShowStorageRationale(state: PermissionState) {
    require(state.status.shouldShowRationale)

	Timber.d("RequirePermission ShowStorageRationale")

	val permission = StoragePermissionHelper.checkReadWriteStoragePermission(LocalContext.current)
    check(!permission)

	SideEffect {
		state.launchPermissionRequest()
    }
}

@Composable
private fun MainActivityNavWallpaper(
    mediaViewModel: MediaViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    modifier: Modifier,
    backstackEntry: NavBackStackEntry?,
    appSettings: AppSettings,
) {

	if (!appSettings.isValid) return

	Timber.d("MainActivity NavWallpaper Recomposed")

    val context = LocalContext.current
    val wpx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val hpx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val backgroundColor = MaterialTheme.colorScheme.background

    val systemModeBitmap: () -> Bitmap = {
        Paint()
            .apply {
                color = backgroundColor
                style = PaintingStyle.Fill
            }
            .let { paint ->
                ImageBitmap(wpx.roundToInt(), hpx.roundToInt()).let { bmp ->
                    Canvas(bmp).drawRect(0f,0f,wpx,hpx, paint)
                    bmp.asAndroidBitmap()
                }
            }
    }

    val wp = when(appSettings.wallpaperSettings.source) {
        DEVICE_WALLPAPER -> rememberWallpaperBitmapAsState().value
        MEDIA_ITEM -> mediaViewModel.mediaItemBitmap.collectAsState().value.bitmap
        SYSTEM_MODE -> systemModeBitmap()
    }

    val alt: @Composable () -> Bitmap? = {
        when(appSettings.wallpaperSettings.sourceALT) {
            DEVICE_WALLPAPER -> rememberWallpaperBitmapAsState().value
            MEDIA_ITEM -> mediaViewModel.mediaItemBitmap.collectAsState().value.bitmap
            SYSTEM_MODE -> systemModeBitmap()
        }
    }

    val itemIndex = MainBottomNavItems
        .map { it.screen.route }
        .indexOf(backstackEntry?.destination?.route)

    val currentIndex = if (itemIndex > -1) {
        mainViewModel.savedBottomNavIndex = itemIndex
        itemIndex
    } else {
        mainViewModel.savedBottomNavIndex
    }

    NavWallpaper(
        modifier = modifier,
        wallpaper = wp ?: alt(),
        fadeDuration = 500,
        size = MainBottomNavItems.size,
        currentIndex = currentIndex,
    )
}

@Composable
fun NavWallpaper(
    modifier: Modifier,
    wallpaper: Bitmap?,
    fadeDuration: Int,
    currentIndex: Int,
    size: Int,
) {

    val context = LocalContext.current
    val scale = ContentScale.Crop
    val req = remember(wallpaper.hashCode()) {
        ImageRequest.Builder(context)
            .crossfade(fadeDuration)
            .data(wallpaper)
            .build()
    }
    val painter = rememberImagePainter(req)
    val scrollState = rememberScrollState()

    Image(
        modifier = modifier
			.fillMaxSize()
			.horizontalScroll(scrollState),
        alignment = Alignment.CenterStart,
        contentDescription = null,
        contentScale = scale,
        painter = painter,
    )

    LaunchedEffect(scrollState.maxValue) {
        (currentIndex.toFloat() / (size - 1) * scrollState.maxValue).toInt().let { px ->
            scrollState.scrollTo(px)
        }
    }

    LaunchedEffect(currentIndex) {
        (currentIndex.toFloat() / (size - 1) * scrollState.maxValue).toInt().let { px ->
            scrollState.animateScrollTo(
                px,
                animationSpec = SpringSpec(stiffness = Spring.StiffnessLow)
            )
        }
    }
}

private fun Bitmap?.copyDefaultArgb() = this?.copy(Bitmap.Config.ARGB_8888, false)

sealed class MainBottomNavItem(
    val screen: Screen,
) {
    class ImageVectorIcon(
        screen: Screen,
        val imageVector: ImageVector,
        val selectedImageVector: ImageVector
    ): MainBottomNavItem(screen)

}

private val MainBottomNavItems = listOf(
    MainBottomNavItem.ImageVectorIcon(
        screen = Screen.Home,
        imageVector = Icons.Filled.Home,
        selectedImageVector = Icons.Filled.Home
    ),
    MainBottomNavItem.ImageVectorIcon(
        screen = Screen.Search,
        imageVector = Icons.Outlined.Search,
        selectedImageVector = Icons.Filled.Search
    ),
    MainBottomNavItem.ImageVectorIcon(
        screen = Screen.Library,
        imageVector = AppDelegate.imageVectorFromDrawableId(R.drawable.ic_bookshelf),
        selectedImageVector = AppDelegate.imageVectorFromDrawableId(R.drawable.ic_bookshelf)
    )
)

@Composable
private fun MainBottomNavItemIcon(
    item: MainBottomNavItem,
    selected: Boolean
) {
    val painter = when(item) {
        is MainBottomNavItem.ImageVectorIcon -> {
            rememberVectorPainter(image = item.imageVector)
        }
    }
    val selectedPainter = when(item) {
        is MainBottomNavItem.ImageVectorIcon -> {
            rememberVectorPainter(image = item.selectedImageVector)
        }
    }
    Icon(
        modifier = Modifier
            .size(27.5.dp),
        painter = if (selected) selectedPainter else painter,
        contentDescription = null
    )
}

@Composable
fun PermissionScreen(
    grantButtonText: String,
    onGrantButton: () -> Unit
) {
    Column(
        modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onGrantButton() },
            colors = ButtonDefaults
                .buttonColors(containerColor = ColorHelper.getTonedSurface(elevation = 8.dp))
        ) {
            Text(text = grantButtonText, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
