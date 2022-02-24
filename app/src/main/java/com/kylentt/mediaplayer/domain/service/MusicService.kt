package com.kylentt.mediaplayer.domain.service

import android.app.PendingIntent
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.kylentt.mediaplayer.core.util.Constants.MEDIA_SESSION_ID
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.rebuild
import com.kylentt.mediaplayer.domain.presenter.ServiceConnectorImpl
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

// Service for UI to interact With ViewModel Controller through Music Service Connector

@AndroidEntryPoint
class MusicService : MediaLibraryService() {
    companion object {
        val TAG: String = MusicService::class.java.simpleName
    }

    @Inject
    lateinit var serviceConnectorImpl: ServiceConnectorImpl

    @Inject
    lateinit var repositoryImpl: SongRepositoryImpl

    @Inject
    lateinit var exo: ExoPlayer

    inner class SessionCallback() : MediaLibrarySession.MediaLibrarySessionCallback {}

    inner class RepoItemFiller() : MediaSession.MediaItemFiller {
        override fun fillInLocalConfiguration(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItem: MediaItem,
        ): MediaItem {
            return mediaItem.rebuild()
        }
    }

    private var session: MediaLibrarySession? = null
    private var activityIntent: PendingIntent? = null

    // Binder
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("$TAG onGetSession")
        return session ?: activityIntent?.let { intent ->
            MediaLibrarySession.Builder(this, exo, SessionCallback())
                .setId(MEDIA_SESSION_ID)
                .setSessionActivity(intent)
                .setMediaItemFiller(RepoItemFiller())
                .build().also { session = it }
        }
    }

    override fun onCreate() {
        Timber.d("$TAG onCreate")
        activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 444, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        super.onCreate()
    }

    override fun onDestroy() {
        Toast.makeText(this, "$TAG Destroyed", Toast.LENGTH_LONG).show()
        Timber.d("$TAG onDestroy")
        super.onDestroy()
    }
}