package com.kylentt.musicplayer.app

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.core.content.ContextCompat
import dagger.hilt.android.HiltAndroidApp

/*@HiltAndroidApp
internal class MediaPlayerApp : Application()*/

// Application Level Resource Accessor
// Should be very rarely used
internal object AppProxy {

  private lateinit var base: Application

  fun provideBase(app: Application) {
    base = app
  }

  fun checkSelfPermission(perm: String): Boolean =
    ContextCompat.checkSelfPermission(base, perm) == PackageManager.PERMISSION_GRANTED

  fun getVectorImage(id: Int): ImageVector =
    ImageVector.vectorResource(base.theme, base.resources, id)
}


