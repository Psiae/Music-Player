package com.kylentt.mediaplayer.ui.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.LifeCycleExtension.RecomposeOnEvent
import com.kylentt.musicplayer.core.app.AppDelegate

@Composable
fun rememberWallpaperBitmapAsState(): State<Bitmap?> {
    val wallpaper = remember { mutableStateOf<Bitmap?>(null) }

    LocalLifecycleOwner.current.lifecycle.RecomposeOnEvent(
        onEvent = Lifecycle.Event.ON_START
    ) { _ ->

        val bitmap = AppDelegate.deviceManager.wallpaperDrawable?.toBitmap()

        LaunchedEffect(key1 = bitmap.hashCode()) {
            wallpaper.value = bitmap
        }
    }

    return wallpaper
}

@Composable
fun rememberWallpaperDrawableAsState(
    context: Context = LocalContext.current
): State<Drawable?> {
    val value = rememberWallpaperBitmapAsState().value
    val block: () -> State<Drawable?> = { mutableStateOf(BitmapDrawable(context.resources, value)) }
    return block()
}


