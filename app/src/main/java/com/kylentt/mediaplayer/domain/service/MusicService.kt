package com.kylentt.mediaplayer.domain.service

import android.app.PendingIntent
import android.widget.Toast
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

// Service for UI to interact With ViewModel Controller

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    companion object {
        val TAG: String = MusicService::class.java.simpleName
        val ID: String = "KYLENTT"
    }

    @Inject
    lateinit var exo: ExoPlayer

    val callback = object : MediaLibrarySession.MediaLibrarySessionCallback {

    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d(TAG + "onGetSession")
        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 444, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        return MediaLibrarySession.Builder(this, exo, callback)
            .setId(ID)
            .setSessionActivity(activityIntent!!)
            .build()
    }



    override fun onCreate() {
        Timber.d(TAG + "onCreate")
        super.onCreate()
    }

    override fun onDestroy() {
        Toast.makeText(this, TAG + "Destroyed", Toast.LENGTH_LONG).show()
        Timber.d(TAG + "onDestroy")
        super.onDestroy()
    }
}