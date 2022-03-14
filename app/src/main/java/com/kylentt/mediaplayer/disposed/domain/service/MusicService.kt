package com.kylentt.mediaplayer.disposed.domain.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.ControllerCommand
import com.kylentt.mediaplayer.core.exoplayer.ExoController
import com.kylentt.mediaplayer.core.exoplayer.ExoControllers
import com.kylentt.mediaplayer.core.exoplayer.MediaItemHandler
import com.kylentt.mediaplayer.core.util.CoilHandler
import com.kylentt.mediaplayer.core.util.Constants.ACTION
import com.kylentt.mediaplayer.core.util.Constants.ACTION_CANCEL
import com.kylentt.mediaplayer.core.util.Constants.ACTION_NEXT
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PAUSE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PLAY
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PREV
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.core.util.Constants.MEDIA_SESSION_ID
import com.kylentt.mediaplayer.core.util.Constants.PLAYBACK_INTENT
import com.kylentt.mediaplayer.disposed.domain.presenter.ServiceConnectorImpl
import com.kylentt.mediaplayer.disposed.domain.presenter.util.State
import com.kylentt.mediaplayer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    companion object {
        val TAG: String = MusicService::class.simpleName ?: "Music Service"
        var isActive: Boolean? = null
            get() = field ?: false
            private set
    }

    @Inject
    lateinit var exo: ExoPlayer
    @Inject
    lateinit var coilHandler: CoilHandler
    @Inject
    lateinit var mediaItemHandler: MediaItemHandler
    @Inject
    lateinit var serviceConnectorImpl: ServiceConnectorImpl

    private lateinit var exoControllers: ExoControllers

    private lateinit var exoController: ExoController

    private var session: MediaLibrarySession? = null

    private val playbackReceiver = PlaybackReceiver()

    var isForegroundService = false

    val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Timber.d("$TAG onCreate")
        initializeSession()
        registerReceiver()
    }

    private fun initializeSession() {
        session = makeLibrarySession(makeActivityIntent()!!)
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotification = PlayerNotificationImpl(this)
        isActive = true
    }

    private fun makeActivityIntent() = packageManager?.getLaunchIntentForPackage(packageName)?.let {
        PendingIntent.getActivity(this, 444, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun registerReceiver() =
        registerReceiver(playbackReceiver, IntentFilter(PLAYBACK_INTENT))

    private fun makeLibrarySession(
        intent: PendingIntent
    ) = MediaLibrarySession.Builder(this, exo, SessionCallback())
        .setId(MEDIA_SESSION_ID)
        .setSessionActivity(intent)
        .setMediaItemFiller(RepoItemFiller())
        .build()

    // Just forward any possible command here for now
    @MainThread
    fun controller(
        whenReady: ( (ExoPlayer) -> Unit)? = null,
        command: ( (ExoPlayer) -> Unit) = {}
    ) {
        exoController.controller(
            whenReady = whenReady
        ) {
            command(it)
        }
    }

    var lastServiceItem: String? = null
    var lastServiceBitmap: Bitmap? = null
    private lateinit var manager: NotificationManager
    private lateinit var mNotification: PlayerNotificationImpl

    /** MediaSession */
    override fun onUpdateNotification(session: MediaSession): MediaNotification? {
        return null
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("$TAG onGetSession")
        return session!!
    }

    inner class SessionCallback : MediaLibrarySession.MediaLibrarySessionCallback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            Timber.d("MusicService onConnect")
            // any exception here just Log.w()'d
            exoControllers = ExoControllers.getInstance(this@MusicService, session)
            return super.onConnect(session, controller)
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            Timber.d("MusicService onPostConnect")
            ServiceConnectorImpl.setServiceState(serviceConnectorImpl, State.ServiceState.Connected)
            super.onPostConnect(session, controller)
        }

        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            Timber.d("MusicService onDisconnected")
            ServiceConnectorImpl.setServiceState(serviceConnectorImpl, State.ServiceState.Disconnected)
            super.onDisconnected(session, controller)
        }
    }

    inner class RepoItemFiller : MediaSession.MediaItemFiller {
        override fun fillInLocalConfiguration(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItem: MediaItem,
        ) = mediaItemHandler.rebuildMediaItem(mediaItem)
    }

    inner class PlaybackReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ACTION)) {
                ACTION_NEXT -> exoControllers.commandController(
                    ControllerCommand.CommandWithFade(ControllerCommand.SkipToNext, false)
                )
                ACTION_PREV -> exoControllers.commandController(
                    ControllerCommand.CommandWithFade(ControllerCommand.SkipToPrev, false)
                )

                ACTION_PLAY -> exoControllers.commandController(ControllerCommand.SetPlayWhenReady(true))
                ACTION_PAUSE -> exoControllers.commandController(ControllerCommand.SetPlayWhenReady(false))

                ACTION_REPEAT_OFF_TO_ONE -> exoControllers.commandController(ControllerCommand.SetRepeatMode(Player.REPEAT_MODE_ONE))
                ACTION_REPEAT_ONE_TO_ALL -> exoControllers.commandController(ControllerCommand.SetRepeatMode(Player.REPEAT_MODE_ALL))
                ACTION_REPEAT_ALL_TO_OFF -> exoControllers.commandController(ControllerCommand.SetRepeatMode(Player.REPEAT_MODE_OFF))

                ACTION_CANCEL -> {
                    exoControllers.commandController(
                        ControllerCommand.CommandWithFade(ControllerCommand.StopCancel, true)
                    )
                    session?.let {
                        stopService(it)
                    }
                }
            }
        }
    }

    private fun stopService(session: MediaLibrarySession) {
        stopSelf()
        if (!MainActivity.isActive) releaseSession(session)
    }

    private fun releaseSession(session: MediaLibrarySession) {
        exo.release()
        unregisterReceiver(playbackReceiver)
        serviceScope.cancel()
        serviceConnectorImpl.releaseSession()
        session.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("$TAG onDestroy")

        isActive = false
        if (!MainActivity.isActive) exitProcess(0)
    }
}