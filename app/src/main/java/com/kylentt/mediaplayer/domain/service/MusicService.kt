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
import android.os.Looper
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
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
import com.kylentt.mediaplayer.core.util.MediaItemHandler
import com.kylentt.mediaplayer.core.util.getArtUri
import com.kylentt.mediaplayer.data.repository.SongRepositoryImpl
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
        val TAG: String = MusicService::class.simpleName ?: "Music Service"
        var isActive: Boolean? = null
            private set
            get() = field ?: false
    }

    @Inject
    lateinit var exo: ExoPlayer
    @Inject
    lateinit var coilHandler: CoilHandler
    @Inject
    lateinit var mediaItemHandler: MediaItemHandler
    @Inject
    lateinit var repositoryImpl: SongRepositoryImpl
    @Inject
    lateinit var serviceConnectorImpl: ServiceConnectorImpl

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // onCreate() called before onGetSession
    override fun onCreate() {
        Timber.d("$TAG onCreate")
        super.onCreate()
        initializeSession()
        registerReceiver()
    }

    // Player
    private var exoIdleListener = mutableListOf<( (ExoPlayer) -> Unit)>()
    private var exoBufferListener = mutableListOf<( (ExoPlayer) -> Unit )>()
    private var exoReadyListener = mutableListOf<( (ExoPlayer) -> Unit )>()
    private var exoEndedListener = mutableListOf<( (ExoPlayer) -> Unit )>()


    private val lock = Any()
    fun exoReady() = synchronized(lock) { exoReadyListener.forEach { it(exo) }
        exoReadyListener.clear()
    }
    private val lock1 = Any()
    fun exoBuffer() = synchronized(lock1) { exoBufferListener.forEach { it(exo) }
        exoBufferListener.clear()
    }
    private val lock2 = Any()
    fun exoEnded() = synchronized(lock2) { exoEndedListener.forEach { it(exo) }
        exoEndedListener.clear()
    }
    private val lock3 = Any()
    fun exoIdle() = synchronized(lock3) { exoIdleListener.forEach { it(exo) }
        exoIdleListener.clear()
    }

    @MainThread
    private fun whenExoReady(command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_READY)
            command(exo) else { exoReadyListener.add(command) }
    }

    @MainThread
    private fun whenBuffer( command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_BUFFERING)
            command(exo) else exoBufferListener.add(command)
    }

    @MainThread
    private fun whenEnded(command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_ENDED)
            command(exo) else exoEndedListener.add(command)
    }

    @MainThread
    private fun whenIdle( command: ( (ExoPlayer) -> Unit ) ) {
        if (exo.playbackState == Player.STATE_IDLE)
            command(exo) else exoReadyListener.add(command)
    }

    // Just forward any possible command here for now
    @MainThread
    fun controller(
        whenReady: ( (ExoPlayer) -> Unit)? = null,
        command: ( (ExoPlayer) -> Unit) = {}
    ) {
        command(exo)
        whenReady?.let { whenExoReady(it) }
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
        mSession = session
        val item = session.player.currentMediaItem

        if (lastItem == item?.mediaId) {
            mNotif = mNotification.makeNotif(NOTIFICATION_ID, session, lastBitmap)
            manager.notify(NOTIFICATION_ID, mNotif)
            return null
            // Since getting the embedded picture and adjusting aspect ratio is too expensive
            // (10mb of memory every interaction (yes because it called 3 times each)) and not GC'd after minutes so just do a simple caching
        }

        lastItem = item?.mediaId

        serviceScope.launch {
            val pict =
                item?.mediaMetadata?.artworkData
                    ?: item?.mediaMetadata?.mediaUri?.let { mediaItemHandler.getEmbeds(it) }

            val uri =
                session.player.currentMediaItem?.getArtUri

            val bm = withContext(Dispatchers.IO) {
                with(coilHandler) {
                    pict?.let { squareWithCoil(BitmapFactory.decodeByteArray(it, 0, it.size)) } ?: uri?.let { makeSquaredBitmap(it) }
                }
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

    private fun initializeSession() {
        makeActivityIntent()
        isActive = true
        manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotification = PlayerNotificationImpl(this)
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        Timber.d("Event PlaybackState STATE_IDLE")
                        exoIdle()
                        // TODO Something to do while Idling
                    }
                    Player.STATE_BUFFERING -> {
                        Timber.d("Event PlaybackState STATE_BUFFERING")
                        exoBuffer()
                        // TODO Something to do while Buffering
                    }
                    Player.STATE_READY -> {
                        Timber.d("Event PlaybackState STATE_READY")
                        exoReady()
                        // TODO Another thing to do when Ready
                    }
                    Player.STATE_ENDED -> {
                        Timber.d("MusicService Event PlaybackState STATE_ENDED")
                        exoEnded()
                        if (!exo.hasNextMediaItem()) exo.pause()
                        // TODO Another thing to do when ENDED
                    }
                }
                /*mSession?.let { onUpdateNotification(it) }*/
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                mSession?.let { onUpdateNotification(it) }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                /*mSession?.let { onUpdateNotification(it) }*/
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                mSession?.let { onUpdateNotification(it) }
            }

            var retry = true
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                if (error.errorCodeName == "ERROR_CODE_DECODING_FAILED") {
                    controller(
                        whenReady = { retry = true }
                    ) {
                        it.stop()
                        it.prepare()
                    }
                }
                if (error.errorCodeName == "ERROR_CODE_IO_FILE_NOT_FOUND") {
                    controller {
                        it.removeMediaItem(it.currentMediaItemIndex)
                        it.pause()
                        it.prepare()
                    }
                }
                Timber.d("MusicService PlaybackException onPlayerError ${error.errorCodeName}")
                serviceToast("Unable to Play This Song ${error.errorCodeName}")
            }
        })
    }

    private suspend fun sServiceToast(msg: String, long: Boolean = true) = withContext(Dispatchers.Main) {
        serviceConnectorImpl.connectorToast(msg)
    }

    private fun serviceToast(msg: String, long: Boolean = true) = serviceScope.launch { sServiceToast(msg, long) }

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

    private fun toggleExo() = controller {
        if (it.playbackState == Player.STATE_ENDED && !it.hasNextMediaItem()) it.seekTo(0)
        if (it.playbackState == Player.STATE_ENDED && it.hasNextMediaItem()) it.seekToNextMediaItem()
        if (it.playbackState == Player.STATE_IDLE) it.prepare()
        it.playWhenReady = !it.playWhenReady
    }

    var fading = false
    private fun exoFade(listener: ( (ExoPlayer) -> Unit )) {
        if (fading) {
            controller { listener(it) }
            return
        }
        fading = true
        serviceScope.launch {
            while (exo.volume > 0.21f && exo.playWhenReady) {
                Timber.d("MusicService AudioEvent FadingAudio ${exo.volume}")
                exo.volume = exo.volume - 0.20f
                delay(100)
            }

            Timber.d("MusicService AudioEvent FadingAudio Done ${exo.volume}")
            controller(
                whenReady = {
                    fading = false
                    it.volume = 1f
                }
            ) { listener(it) }

            // Line After Loop
            Timber.d("MusicService Event AudioFaded")
        }
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
                    exoFade { stopService(session) }
                    return
                } }
            }
            mSession?.let { validateNotification(it) }
        }
    }

    private fun validateNotification(session: MediaSession) = serviceScope.launch {
        delay(500)
        onUpdateNotification(session)
    }

    // LibrarySession
    private var session: MediaLibrarySession? = null
    private var activityIntent: PendingIntent? = null

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