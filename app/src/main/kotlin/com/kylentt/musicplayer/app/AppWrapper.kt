package com.kylentt.musicplayer.app

import android.app.Application
import android.app.WallpaperManager
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.core.content.ContextCompat
import com.kylentt.musicplayer.app.AppWrapper.Companion.providesWrapper

// Application Level resource accessor
private class AppWrapper constructor(
    private val base: Application
) {

    private fun checkSelfPermission(perm: String) = with(base) { ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED }
    private fun getVectorImage(id: Int) = with(base) { ImageVector.vectorResource(theme, resources, id) }
    private fun getWallpaperManager() = with(base) { WallpaperManager.getInstance(this) }

    companion object {
        private lateinit var app: AppWrapper

        fun checkPermission(perm: String) = app.checkSelfPermission(perm)
        fun getVectorImage(id: Int) = app.getVectorImage(id)

        fun Application.providesWrapper() {
            app = AppWrapper(this)
        }
    }
}

// caps
public object AppProxy {
    fun Application.provideWrapper() = run { providesWrapper() }
    fun checkPermission(perm: String) = AppWrapper.checkPermission(perm)
    fun getVectorImage(id: Int) = AppWrapper.getVectorImage(id)


    var holdThis: Any? = { null }
}



