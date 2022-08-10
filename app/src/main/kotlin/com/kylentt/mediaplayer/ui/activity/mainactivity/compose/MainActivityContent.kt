@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)

package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kylentt.mediaplayer.core.app.settings.AppSettings
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.ComposableExtension.noPadding
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.theme.AppTypography
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.ui.main.compose.local.MainProvider
import com.kylentt.musicplayer.ui.main.compose.navigation.MainNavigator
import com.kylentt.musicplayer.ui.main.compose.navigation.MainNavigator.ProvideNavHostController
import com.kylentt.musicplayer.ui.main.compose.screens.root.PlaybackControl
import com.kylentt.musicplayer.ui.main.compose.theme.color.ColorHelper
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun MainActivityRoot(
    appSettings: AppSettings
) {
	val mediaVM = MainProvider.mediaViewModel

	ProvideNavHostController(rememberNavController()) {
		RootScaffold(
			appSettings = appSettings,
			navController = MainNavigator.controller
		) { padding ->
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.BottomCenter
			) {
				Column {
					StatusBarSpacer()
					AnimatedMainAppNavigator(
						modifier = Modifier
							.padding(padding)
						,
						controller = MainNavigator.controller
					)
				}
				PlaybackControl(mediaVM.playbackControlModel, padding.calculateBottomPadding())
			}
		}
	}
}

@Composable
private fun StatusBarSpacer() {
	val height = LocalDensity.current.run {
		WindowInsets.statusBars.getTop(this).toDp()
	}
	Spacer(Modifier.height(height))
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
				val elevation = if (isSystemInDarkTheme()) 2.dp else 8.dp

                val backgroundColor = ColorHelper.tonePrimarySurface(elevation = elevation)
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
                            alwaysShowLabel = true,
                            selected = item.screen == selectedItem,
                            icon = {
								MainBottomNavItemIcon(
									item = item,
									selected = item.screen == selectedItem
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
    val backgroundColor = MaterialTheme.colorScheme.surface

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

    /*val wp = when(appSettings.wallpaperSettings.source) {
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
    )*/
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
    val painter = rememberAsyncImagePainter(req)
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
		Timber.d("NavWallpaper Launched effect for scrollState.maxValue: ${scrollState.maxValue}")

		val value =
			if (scrollState.maxValue > 0 && size > 0 && currentIndex > 0) {
				(currentIndex.toFloat() / (size - 1) * scrollState.maxValue).toInt()
			} else {
				0
			}
		scrollState.scrollTo(value = value)
    }

    LaunchedEffect(currentIndex) {
		Timber.d("NavWallpaper Launched effect for currentIndex: $currentIndex")

		val value =
			if (scrollState.maxValue > 0 && size > 0 && currentIndex > 0) {
				(currentIndex.toFloat() / (size - 1) * scrollState.maxValue).toInt()
			} else {
				0
			}
		scrollState.animateScrollTo(value = value,
			animationSpec = SpringSpec(stiffness = Spring.StiffnessLow)
		)
    }
}

sealed class MainBottomNavItem(
    val screen: Screen,
) {

	class ResourceIcon(
		screen: Screen,
		@DrawableRes val unselectedId: Int,
		@DrawableRes val selectedId: Int
	): MainBottomNavItem(screen)

    class ImageVectorIcon(
        screen: Screen,
        val imageVector: ImageVector,
        val selectedImageVector: ImageVector
    ): MainBottomNavItem(screen)

}

private val MainBottomNavItems = listOf(
    MainBottomNavItem.ResourceIcon(
        screen = Screen.Home,
        unselectedId = R.drawable.home_outlined_base_512_24,
        selectedId = R.drawable.home_filled_base_512_24
    ),
    MainBottomNavItem.ResourceIcon(
        screen = Screen.Search,
		unselectedId = R.drawable.search_outlined_base_128_24,
		selectedId = R.drawable.search_filled_base_128_24
    ),
    MainBottomNavItem.ResourceIcon(
        screen = Screen.Library,
		unselectedId = R.drawable.library_outlined_base_128_24,
		selectedId = R.drawable.library_filled_base_128_24
    ),
	MainBottomNavItem.ResourceIcon(
		screen = Screen.User,
		unselectedId = R.drawable.user_circle_outlined_base_512_24,
		selectedId = R.drawable.user_circle_filled_base_512_24
	)
)

@Composable
private fun MainBottomNavItemIcon(
    item: MainBottomNavItem,
    selected: Boolean
) {
    val painter = when(item) {
		is MainBottomNavItem.ResourceIcon -> painterResource(id = item.unselectedId)
        is MainBottomNavItem.ImageVectorIcon -> rememberVectorPainter(image = item.imageVector)
    }
    val selectedPainter = when(item) {
		is MainBottomNavItem.ResourceIcon -> painterResource(id = item.selectedId)
        is MainBottomNavItem.ImageVectorIcon -> rememberVectorPainter(image = item.selectedImageVector)
    }
    Icon(
        modifier = Modifier
            .size(24.dp),
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
				.buttonColors(containerColor = ColorHelper.tonePrimarySurface(elevation = 8.dp))
        ) {
            Text(text = grantButtonText, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
