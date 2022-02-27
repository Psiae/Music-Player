package com.kylentt.mediaplayer.domain.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
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
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_ID
import com.kylentt.mediaplayer.core.util.Constants.PLAYBACK_INTENT
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
import com.kylentt.mediaplayer.domain.model.artUri
import com.kylentt.mediaplayer.domain.model.rebuild
import com.kylentt.mediaplayer.domain.presenter.ServiceConnectorImpl
import com.kylentt.mediaplayer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.transformers.coil.CropSquareTransformation
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
    lateinit var coil: ImageLoader
    @Inject
    lateinit var exo: ExoPlayer
    @Inject
    lateinit var repositoryImpl: SongRepositoryImpl
    @Inject
    lateinit var serviceConnectorImpl: ServiceConnectorImpl

    val serviceScope = (CoroutineScope(Dispatchers.Main + SupervisorJob()))

    /** onCreate() called before onGetSession*/
    override fun onCreate() {
        Timber.d("$TAG onCreate")
        super.onCreate()
        isActive = true
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        registerReceiver()
        initializeSession()
    }

    /** Player */
    // Idk if its good approach
    private var exoListener = mutableListOf<( (ExoPlayer) -> Unit )>()

    // executed when player goes STATE_READY
    private var lock = Any()
    fun exoReady() = synchronized(lock) { exoListener.forEach { it(exo) } ; exoListener.clear() }

    // Just forward any possible command here for now
    @MainThread
    fun controller(f: Boolean = true, command: ( (ExoPlayer) -> Unit) ) {
        if (exoListener.size > 10) {
            exoListener.removeAt(0)
            exo.prepare()
        }
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

    /** MediaSession & Notification */
    private lateinit var mSession: MediaSession
    private lateinit var manager: NotificationManager
    private lateinit var notification: PlayerNotificationImpl
    private lateinit var mNotif: Notification

    private var isForeground = false

    // Notification update should call here
    // Return MediaNotification to have it handled by Media3 else null
    override fun onUpdateNotification(session: MediaSession): MediaNotification? {
        Timber.d("$TAG OnUpdateNotification")
        mSession = session

        if (!::notification.isInitialized) notification = PlayerNotificationImpl(
            this, this, mSession
        )

        serviceScope.launch {
            val pict = session.player.currentMediaItem?.mediaMetadata?.artworkData
            val uri = session.player.currentMediaItem?.artUri
            val bm = pict?.let { mapBM( BitmapFactory.decodeByteArray(
                it, 0, it.size
            )) } ?: makeBm(uri)
            mNotif = notification.makeNotif(NOTIFICATION_ID, session, bm)
            if (!isForeground) {
                startForeground(NOTIFICATION_ID, mNotif)
                isForeground = true
            }
            manager.notify(NOTIFICATION_ID, mNotif)
        }
        return null
    }

    private suspend fun mapBM(bm: Bitmap?) = withContext(Dispatchers.IO) {
        val req = ImageRequest.Builder(this@MusicService)
            .size(256)
            .scale(Scale.FILL)
            .data(bm)
            .build()
        ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    private suspend fun makeBm(uri: Uri?) = withContext(Dispatchers.IO) {
        val req = ImageRequest.Builder(this@MusicService)
            .size(256)
            .scale(Scale.FILL)
            .data(uri)
            .build()
        ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    private fun initializeSession() {
        makeActivityIntent()
        exo.repeatMode = Player.REPEAT_MODE_ALL
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                if (playbackState == Player.STATE_READY) exoReady()
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                Toast.makeText(this@MusicService.applicationContext,
                    "Unable to play this Song, code: $error", Toast.LENGTH_LONG
                ).show()
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
                ACTION_CANCEL -> { session?.let {
                    exo.stop()
                    stopSelf()
                    stopForeground(true)
                    isForeground = false
                    if (!MainActivity.isActive) serviceConnectorImpl.releaseSession()
                    return
                } }
            }

            serviceScope.launch {
                if (::mSession.isInitialized) {
                    onUpdateNotification(mSession)
                    delay(1000)
                    onUpdateNotification(mSession)
                }
            }
        }
    }

    // LibrarySession
    private var session: MediaLibrarySession? = null
    var activityIntent: PendingIntent? = null

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

    inner class SessionCallback : MediaLibrarySession.MediaLibrarySessionCallback

    // TODO: Looking for better solution
    inner class RepoItemFiller : MediaSession.MediaItemFiller {
        override fun fillInLocalConfiguration(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItem: MediaItem,
        ) = mediaItem.rebuild()
    }

    override fun onDestroy() {
        Timber.d("$TAG onDestroy")
        exo.release()
        isActive = false
        serviceScope.cancel()
        unregisterReceiver(playbackReceiver)
        if (!MainActivity.isActive) exitProcess(0)
        super.onDestroy()
    }
}