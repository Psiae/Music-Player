package com.kylentt.mediaplayer.disposed.domain.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.ExoController
import com.kylentt.mediaplayer.core.exoplayer.MediaItemHandler
import com.kylentt.mediaplayer.core.exoplayer.getArtUri
import com.kylentt.mediaplayer.core.util.CoilHandler
import com.kylentt.mediaplayer.core.util.Constants.ACTION
import com.kylentt.mediaplayer.core.util.Constants.ACTION_CANCEL
import com.kylentt.mediaplayer.core.util.Constants.ACTION_FADE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_FADE_PAUSE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_NEXT
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PAUSE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PLAY
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PREV
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.core.util.Constants.ACTION_UNIT
import com.kylentt.mediaplayer.core.util.Constants.MEDIA_SESSION_ID
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_ID
import com.kylentt.mediaplayer.core.util.Constants.PLAYBACK_INTENT
import com.kylentt.mediaplayer.disposed.domain.presenter.ServiceConnectorImpl
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

    private lateinit var exoController: ExoController

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Timber.d("$TAG onCreate")

        initializeSession()
        registerReceiver()
    }

    private fun initializeSession() {
        makeActivityIntent()
        session = makeLibrarySession(activityIntent!!)
        isActive = true
    }

    private var session: MediaLibrarySession? = null
    private var activityIntent: PendingIntent? = null
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        Timber.d("$TAG onGetSession")
        return session!!
    }

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

    /** MediaSession & Notification */
    private var mSession: MediaSession? = null
    private var mNotif: Notification? = null
    private lateinit var manager: NotificationManager
    private lateinit var mNotification: PlayerNotificationImpl

    private var isForeground = false

    // Notification update should call here
    // Return MediaNotification to have it handled by Media3 else null
    var lastItem: String? = null
    var lastBitmap: Bitmap? = null
    override fun onUpdateNotification(session: MediaSession): MediaNotification? {
        Timber.d("$TAG OnUpdateNotification")
        val p = session.player
        val item = p.currentMediaItem

        if (lastItem == item?.mediaId) {
            if (p.playbackState == Player.STATE_IDLE) return null
            mNotif = mNotification.makeNotif(NOTIFICATION_ID, session, lastBitmap)
            manager.notify(NOTIFICATION_ID, mNotif)
            return null
            // Since getting the embedded picture and adjusting ar is too expensive
            // 10mb of memory every interaction (yes because it called 3 times each) and not GC'd after minutes so just do a simple caching
        }

        lastItem = item?.mediaId

        serviceScope.launch {
            val pict =
                item?.mediaMetadata?.artworkData
                    ?: item?.mediaMetadata?.mediaUri?.let { mediaItemHandler.getEmbeds(it) }

            val uri =
                session.player.currentMediaItem?.getArtUri

            val bm = with(coilHandler) {
                pict?.let { squareWithCoil(BitmapFactory.decodeByteArray(it, 0, it.size)) } ?: uri?.let { makeSquaredBitmap(it) }
            }

            lastBitmap = bm
            mNotif = mNotification.makeNotif(NOTIFICATION_ID, session, bm)
            if (!isForeground) {
                startForeground(NOTIFICATION_ID, mNotif)
                isForeground = true
            }
            manager.notify(NOTIFICATION_ID, mNotif)
        }
        return null
    }



    private fun makeLibrarySession(
        intent: PendingIntent
    ) = MediaLibrarySession.Builder(this, exo, SessionCallback())
            .setId(MEDIA_SESSION_ID)
            .setSessionActivity(intent)
            .setMediaItemFiller(RepoItemFiller())
            .build()

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
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotification = PlayerNotificationImpl(this)
        registerReceiver(playbackReceiver, IntentFilter(PLAYBACK_INTENT))
    }

    private fun toggleExo() = controller {
        it.playWhenReady = !it.playWhenReady
        if (it.playbackState == Player.STATE_ENDED && !it.hasNextMediaItem()) it.seekTo(0)
        if (it.playbackState == Player.STATE_ENDED && it.hasNextMediaItem()) it.seekToNextMediaItem()
        if (it.playbackState == Player.STATE_IDLE) it.prepare()
    }

    private fun exoFade(clear: Boolean = false, listener: ( (ExoPlayer) -> Unit )) {
        serviceScope.launch { exoController.exoFade(0f, clear, listener) }
    }

    inner class PlaybackReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra(ACTION)) {
                ACTION_NEXT -> { exoFade() { it.seekToNext() } }
                ACTION_PREV -> { exoFade() { it.seekToPreviousMediaItem() } }

                ACTION_PLAY -> toggleExo()
                ACTION_PAUSE -> toggleExo()

                ACTION_UNIT -> Unit
                ACTION_FADE -> exoFade { /* TODO: Something todo while its fading? */ }
                ACTION_FADE_PAUSE -> exoFade { it.pause() }

                ACTION_REPEAT_OFF_TO_ONE -> exo.repeatMode = Player.REPEAT_MODE_ONE
                ACTION_REPEAT_ONE_TO_ALL -> exo.repeatMode = Player.REPEAT_MODE_ALL
                ACTION_REPEAT_ALL_TO_OFF -> exo.repeatMode = Player.REPEAT_MODE_OFF

                ACTION_CANCEL -> { session?.let { session ->
                    exoFade(true) { stopService(session) }
                    return
                } }
            }
            validateNotification(sessions.first())
        }
    }

    private fun validateNotification(session: MediaSession) = serviceScope.launch {
        delay(500)
        onUpdateNotification(session)
    }

    inner class SessionCallback : MediaLibrarySession.MediaLibrarySessionCallback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            mSession = session
            exoController = ExoController(mSession!!, this@MusicService)
            return super.onConnect(session, controller)
        }
    }

    inner class RepoItemFiller : MediaSession.MediaItemFiller {
        override fun fillInLocalConfiguration(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItem: MediaItem,
        ) = mediaItemHandler.rebuildMediaItem(mediaItem)
    }

    private fun stopService(session: MediaLibrarySession) {
        exo.stop()
        stopForeground(true).also { isForeground = false }
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
        Timber.d("$TAG onDestroy")
        super.onDestroy()
        isActive = false
        if (!MainActivity.isActive) exitProcess(0)
    }
}