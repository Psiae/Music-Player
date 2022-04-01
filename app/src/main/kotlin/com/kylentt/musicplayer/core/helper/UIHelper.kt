package com.kylentt.musicplayer.core.helper

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.core.view.WindowCompat
import com.kylentt.musicplayer.app.AppProxy

object UIHelper {

    @Composable
    fun Context.getComposeImageVector(id: Int) = ImageVector.vectorResource(id = id)

    fun ComponentActivity.disableFitWindow() = WindowCompat.setDecorFitsSystemWindows(window, false)
    fun Context.getImageVector(id: Int) = ImageVector.vectorResource(theme, resources, id)

    fun getVectorImage(id: Int, context: Context? = null): ImageVector = context?.getImageVector(id) ?: AppProxy.getVectorImage(id)
}