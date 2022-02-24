package com.kylentt.mediaplayer.domain.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.util.Constants.ACTION
import com.kylentt.mediaplayer.core.util.Constants.ACTION_CANCEL
import com.kylentt.mediaplayer.core.util.Constants.MEDIA_SESSION_ID
import com.kylentt.mediaplayer.core.util.Constants.ACTION_NEXT
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PAUSE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PLAY
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PREV
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_ID
import com.kylentt.mediaplayer.core.util.Constants.PLAYBACK_INTENT
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.rebuild
import com.kylentt.mediaplayer.domain.presenter.ServiceConnectorImpl
import com.kylentt.mediaplayer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.exitProcess

// Service for UI to interact With ViewModel Controller through Music Service Connector

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    companion object {
        val TAG: String = MusicService::class.java.simpleName
        var isActive = false
    }

    @Inject
    lateinit var exo: ExoPlayer
    @Inject
    lateinit var repositoryImpl: SongRepositoryImpl
    @Inject
    lateinit var serviceConnectorImpl: ServiceConnectorImpl

    val serviceScope = (CoroutineScope(Dispatchers.Main + SupervisorJob()))

    // Idk if its good approach
    private var exoListener = mutableListOf<( (ExoPlayer) -> Unit )>()

    // executed when player goes STATE_READY
    var lock = Any()
    var exoReady = false
        set(value) {
            field = if (value && !field) {
                val idk = synchronized(lock) {
                    exoListener.forEach {
                        it(exo)
                    }
                    exoListener.clear()
                    value
                }
                idk
            } else value
        }

    @MainThread
    fun controller(f: Boolean = true, command: ( (ExoPlayer) -> Unit) ) {
        if (exoListener.size > 10) exoListener.removeAt(0)
        when {
            ::exo.isInitialized -> {
                if (exo.playbackState != Player.STATE_IDLE) {
                    Timber.d("Exoplayer Controller Command")
                    command(exo)
                } else {
                    Timber.d("Exoplayer Controller add Command & Prepare")
                    exoListener.add(command)
                    exo.prepare()
                }
            }
            f -> {
                Timber.d("Exoplayer Controller not Initialized, add Command")
                exoListener.add(command)
            }
            else -> Timber.e("Exoplayer Controller Exception")
        }
    }

    private lateinit var mSession: MediaSession
    private lateinit var playerNotification: PlayerNotification
    private var isForeground = false

    inner class SessionCallback : MediaLibrarySession.MediaLibrarySessionCallback {}

    // TODO: Simplify This
    override fun onUpdateNotification(session: MediaSession): MediaNotification? {
        Timber.d("$TAG OnUpdateNotification")
        mSession = session
        playerNotification.notify(NOTIFICATION_ID, mSession)

        if (!isForeground) {
            startForeground(NOTIFICATION_ID, playerNotification.make(NOTIFICATION_ID, mSession))
            isForeground = true
        }
        return null
    }

    // TODO: Looking for better solution
    inner class RepoItemFiller : MediaSession.MediaItemFiller {
        override fun fillInLocalConfiguration(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItem: MediaItem,
        ) = mediaItem.rebuild()
    }

    // Session
    private var session: MediaLibrarySession? = null
    var activityIntent: PendingIntent? = null

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

    // onCreate() called before onGetSession
    override fun onCreate() {
        Timber.d("$TAG onCreate")
        super.onCreate()
        isActive = true
        playerNotification = PlayerNotification(this, this)
        registerReceiver()
        initializeSession()
    }

    private fun initializeSession() {
        makeActivityIntent()
        exo.repeatMode = Player.REPEAT_MODE_ALL
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                exoReady = playbackState == Player.STATE_READY
            }
        })
    }

    private fun makeActivityIntent() {
        activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 444, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    // handle notification buttons
    private val playbackReceiver = PlaybackReceiver()
    private fun registerReceiver() {
        registerReceiver(playbackReceiver, IntentFilter(PLAYBACK_INTENT))
    }

    fun toggleExo() = controller {
        it.playWhenReady = !it.playWhenReady
    }

    // TODO: make request processor function like connector
    inner class PlaybackReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ACTION)) {
                ACTION_NEXT -> controller { it.seekToNext() }
                ACTION_PREV -> controller { it.seekToPrevious() }
                ACTION_PLAY -> toggleExo()
                ACTION_PAUSE -> toggleExo()
                ACTION_REPEAT_OFF_TO_ONE -> exo.repeatMode = Player.REPEAT_MODE_ONE
                ACTION_REPEAT_ONE_TO_ALL -> exo.repeatMode = Player.REPEAT_MODE_ALL
                ACTION_REPEAT_ALL_TO_OFF -> exo.repeatMode = Player.REPEAT_MODE_OFF
                ACTION_CANCEL -> {
                    session?.let {
                        exo.stop()
                        stopSelf()
                        stopForeground(true)
                        isForeground = false
                        return
                    }
                }
            }

            serviceScope.launch {
                delay(500)
                if (::mSession.isInitialized) playerNotification.notify(NOTIFICATION_ID, mSession)
            }
        }
    }

    override fun onDestroy() {
        Timber.d("$TAG onDestroy")
        isActive = false

        exo.release()
        serviceScope.cancel()
        unregisterReceiver(playbackReceiver)

        if (!MainActivity.isActive) exitProcess(0)
        super.onDestroy()
    }
}