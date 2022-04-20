@file:OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class
)

package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.Scale
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import com.kylentt.mediaplayer.app.settings.AppSettings
import com.kylentt.mediaplayer.app.settings.WallpaperSettings
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.theme.MaterialDesign3Theme
import timber.log.Timber

private val noPadding = PaddingValues(0.dp)


@Composable
fun MainActivityContent(
    mainViewModel: MainViewModel
) {
    val appSettings = mainViewModel.appSettings.collectAsState()
    val pendingStorageGranted = remember { mainViewModel.pendingStorageGranted }

    MaterialDesign3Theme {

        RequireStorage(
            whenDenied = { StorageDenied(it) },
            whenShowRationale = { ShowStorageRationale(it) },
        ) {
            if (AppDelegate.checkStoragePermission()) {
                pendingStorageGranted.syncEachClear()
                MainActivityRoot(appSettings.value)
            }
        }
    }
}

@RequiresPermission(anyOf = [
    StoragePermissionDelegate.Read_External_Storage,
    StoragePermissionDelegate.Write_External_Storage
])
@Composable
private fun MainActivityRoot(
    appSettings: AppSettings
) {
    val navController = rememberAnimatedNavController()
    val navState = navController.currentBackStackEntryAsState()
    val isBnvTransparent = appSettings.navigationSettings.bnvSettings.visibility < 100

    RootScaffold { padding ->
        Box(
            modifier = Modifier.padding( if (isBnvTransparent) noPadding else padding )
        ) {
            AnimatedMainAppNavigator(controller = navController)
            if (appSettings.wallpaperSettings.mode
                == WallpaperSettings.Mode.NAVIGATION
            ) {
                MainActivityNavWallpaper(
                    backstackEntry = navState.value,
                    wallpaperSettings = appSettings.wallpaperSettings
                )
            }
        }
    }
}



@RequiresPermission(anyOf = [
    StoragePermissionDelegate.Read_External_Storage,
    StoragePermissionDelegate.Write_External_Storage
])
@Composable
private fun MainActivityNavWallpaper(
    mediaViewModel: MediaViewModel = viewModel(),
    backstackEntry: NavBackStackEntry?,
    wallpaperSettings: WallpaperSettings,
) {
    if (backstackEntry == null) return
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
    val darkTheme = isSystemInDarkTheme()
    val scale = remember(wallpaper) { ContentScale.Crop }
    val req = remember(wallpaper) {
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

@Composable
private fun RootScaffold(
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold() {
        content(it)
    }
}

private fun showBottomNav(stack: NavBackStackEntry?): Boolean {
    return MainBottomNavigation.routeList.find { it == stack?.destination?.route }?.let { true }
        ?: false
}


@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun StorageDenied(
    state: PermissionState
) {
    require(!state.status.isGranted)
    require(!state.status.shouldShowRationale)
    check(!AppDelegate.hasStoragePermission)
    val localContext = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )
    val intent = Intent()
        .apply {
            action =  Settings.ACTION_APPLICATION_SETTINGS
            data = "package:${localContext.packageName}".toUri()
        }
    launcher.launch(intent)
    Toast.makeText(localContext, "Permission Required", Toast.LENGTH_SHORT).show()
}

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun ShowStorageRationale(
    state: PermissionState
) {
    require(state.status.shouldShowRationale)
    check(!AppDelegate.hasStoragePermission)
    SideEffect {
        state.launchPermissionRequest()
    }
}

private fun <T> MutableList< () -> T >.syncEachClear(lock: Any = this) {
    synchronized(lock) {
        forEach { it() }
        clear()
    }
}
