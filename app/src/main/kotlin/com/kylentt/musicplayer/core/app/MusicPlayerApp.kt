package com.kylentt.musicplayer.core.app

import android.app.Application
import android.util.Log
import com.kylentt.musicplayer.medialib.api.MediaLibraryAPI
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MusicPlayerApp : Application() {
	override fun onCreate() { MediaLibraryAPI.current!!.serviceComponent.startService() }
}
