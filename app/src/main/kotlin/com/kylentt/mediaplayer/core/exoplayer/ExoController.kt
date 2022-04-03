package com.kylentt.mediaplayer.core.exoplayer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.exoplayer.util.toStrRepeat
import com.kylentt.mediaplayer.core.exoplayer.util.toStrState
import com.kylentt.mediaplayer.core.util.handler.*
import com.kylentt.mediaplayer.core.util.helper.VersionHelper
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicService
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_CANCEL
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_CANCEL_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_NEXT
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_NEXT_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_NEXT_DISABLED
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PAUSE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PAUSE_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PLAY
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PLAY_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PREV
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PREV_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_PREV_DISABLED
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_REPEAT_ALL_TO_OFF_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_REPEAT_OFF_TO_ONE_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.ACTION_REPEAT_ONE_TO_ALL_CODE
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.NOTIFICATION_CHANNEL_ID
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.NOTIFICATION_ID
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.NOTIFICATION_NAME
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.PLAYBACK_INTENT
import kotlinx.coroutines.*
import timber.log.Timber

@MainThread
@Deprecated("Remake new Module")
internal class ExoController(
    private var service: MusicService,
    session: MediaSession
) {

    // TODO: Move those Inner class

    private var playerController: PlayerController? = PlayerController(session)
        set(value) {
            synchronized(this) {
                if (field != null) {
                    field!!.release(value?.player !== field?.player)
                    Timber.d("ExoController oldPlayerReleased, old ${field?.player} new ${value?.player}")
                }
                field = value
            }
        }

    private var notificationManager: ExoNotificationManagers? = ExoNotificationManagers(service, session)

    private var session: MediaSession? = session
        set(value) {
            synchronized(this) {
                if (value != null) {
                    playerController = PlayerController(value)
                    notificationManager = ExoNotificationManagers(service, value)
                } else {
                    playerController = null
                    notificationManager = null
                }
                field = value
            }
        }

    private fun release() {
        session?.release()
        session = null
        lastBitmap = null
    }

    private var lastNotification: Notification? = null
    private var getNotifCallback = MediaNotification.Provider.Callback {  }

    fun getNotification(controller: MediaController, callback: MediaNotification.Provider.Callback): Notification {
        val current = controller.currentMediaItem
        getNotifCallback = callback
        if (lastBitmap?.second != current?.mediaId && controller.mediaItemCount > 0) {
            Timber.d("getNotification returned with invalidating $lastBitmap")

            val currentIndex = controller.currentMediaItemIndex
            val next = if (controller.mediaItemCount >= currentIndex) controller.nextMediaItemIndex else currentIndex
            val prev = if (currentIndex > 0) controller.previousMediaItemIndex else currentIndex

            val nextItem = if (next != -1) controller.getMediaItemAt(next) else controller.getMediaItemAt(currentIndex)
            val prevItem = if (prev != -1) controller.getMediaItemAt(prev) else controller.getMediaItemAt(currentIndex)

            val bm = when(lastBitmap?.second) {
                prevItem.mediaId, nextItem.mediaId -> lastBitmap?.first
                else -> null
            }
            controller.currentMediaItem?.let { updateNotification(NotificationUpdate.MediaItemTransition(it)) }
                ?: updateNotification(NotificationUpdate.InvalidateMediaItem)
            return notificationManager!!.makeNotification(bm, notify = false, isPlaying = controller.playWhenReady)
        }
        if (controller.playbackState == Player.STATE_IDLE) {
            return notificationManager!!.makeNotification(lastBitmap?.first, notify = false, isPlaying = false)
        }
        if (controller.playbackState == Player.STATE_BUFFERING) {
            return notificationManager!!.makeNotification(lastBitmap?.first, notify = false, isPlaying = controller.playWhenReady)
        }
        return run {
            Timber.d("getNotification returned makeNotification with old $lastBitmap")
            notificationManager!!.makeNotification(
                lastBitmap?.first, isPlaying = controller.playWhenReady, notify = true, validatePlayPause = true
            )
        }
    }

    private val commandLock = Any()
    fun commandController(
        type: ControllerCommand
    ) {
        synchronized(commandLock) {
            try {
                Timber.d("ExoController CommandController with type: $type")
                session?.let {
                    playerController?.let { controller ->
                        when (type) {
                            is ControllerCommand.CommandWithFade -> {
                                service.serviceScope.launch { controller.handleCommandWithFade(0f, type) }
                            }
                            is ControllerCommand.SetPlayWhenReady -> {
                                controller.handleChangePlayWhenReady(type.play)
                            }
                            is ControllerCommand.SetRepeatMode -> {
                                controller.handleChangeRepeat(type.repeat)
                            }
                            is ControllerCommand.StopCancel -> {
                                controller.handleStopCancel()
                                service.stopForeground(true)
                                service.isForegroundService = false
                                service.stopSelf()
                                type.callback()
                            }
                            ControllerCommand.SkipToNext -> {
                                controller.handleSeekToNext()
                            }
                            ControllerCommand.SkipToPrev -> {
                                controller.handleSeekToPrev()
                            }
                            ControllerCommand.TogglePlayWhenReady -> {
                                // TODO()
                            }
                            ControllerCommand.SkipToPrevMedia -> {
                                controller.handleSeekToPrevMedia()
                            }
                        }
                    } ?: Timber.e("ExoController CommandController called when PlayerController is null")
                } ?: Timber.e("ExoController CommandController called when session is null")
            } catch (e : Exception) {
                if (e !is NullPointerException) throw e else Timber.e(e)
            }
        }
    }

    private val updateLock = Any()
    fun updateNotification(
        type: NotificationUpdate
    ) {
        synchronized(updateLock) {
            try {
                Timber.d("ExoController UpdateNotification with type: $type")
                session?.let { _ ->
                    playerController?.let { controller ->
                        notificationManager?.let { manager ->
                            when (type) {
                                is NotificationUpdate.MediaItemTransition -> {
                                    handleNotifMediaItemTransition(controller, manager, type.mediaItem)
                                }
                                is NotificationUpdate.PlayWhenReadyChanged -> {
                                    handleNotifPlayWhenReadyChanged(manager, type.play)
                                }
                                is NotificationUpdate.PlaybackStateChanged -> {
                                    handleNotifPlaybackStateChanged(controller, type.state)
                                }
                                is NotificationUpdate.RepeatModeChanged -> {
                                    handleNotifRepeatModeChanged(manager, type.repeat)
                                }
                                NotificationUpdate.InvalidateMediaItem -> {
                                    handleNotifMediaItemTransition(controller, manager, null)
                                }
                            }
                        } ?: Timber.e("ExoController UpdateNotification is Called when manager is null")
                    } ?: Timber.e("ExoController UpdateNotification is Called when controller is null")
                } ?: Timber.e("ExoController UpdateNotification is Called when session is null")
            } catch (e : Exception) {
                if (e !is NullPointerException) throw e else Timber.e(e)
            }
        }
    }

    private var lastBitmap: Pair<Bitmap?, String>? = null
        private set(value) {
            Timber.d("lastBitmap set to $value")
            field = value
        }


    // Ignore this mess, remake this on new Module

    private val newBitmapRequestException = CancellationException("new updateBitmapRequest")
    private var mediaItemTransitionJob = Job().job
    private fun handleNotifMediaItemTransition(pc: PlayerController, manager: ExoNotificationManagers, item: MediaItem?) {
        val p = pc.player
        item?.let {
            if (item.mediaId == lastBitmap?.second) {
                manager.makeNotification(
                    bm = lastBitmap?.first,
                    mi = it,
                    isPlaying = p.playWhenReady
                )
            } else {
                mediaItemTransitionJob.cancel(newBitmapRequestException)
                mediaItemTransitionJob = service.serviceScope.launch {
                    val bmId = Pair(it.getUri?.let { uri -> getDisplayEmbed(uri) }, it.mediaId)
                    lastBitmap = bmId
                    ensureActive()
                    val notif = manager.makeNotification(bm = lastBitmap?.first, notify = false, mi = item, isPlaying = p.playWhenReady)
                    getNotifCallback.onNotificationChanged(MediaNotification(NOTIFICATION_ID, notif))
                }
            }
        } ?: run {
            mediaItemTransitionJob.cancel(newBitmapRequestException)
            mediaItemTransitionJob = service.serviceScope.launch {
                p.currentMediaItem?.let { item ->
                    val bm = if (lastBitmap?.second != item.mediaId) {
                        if (lastBitmap != null) {
                            Timber.e("ExoController UpdateNotification MediaItemTransition Invalidating with new Bitmap ${item.mediaId}, old ${lastBitmap?.second}")
                        }
                        item.getUri?.let { uri -> getDisplayEmbed(uri) }
                    } else {
                        Timber.d("ExoController UpdateNotification MediaItemTransition Invalidating with old Bitmap ${lastBitmap?.second}")
                        lastBitmap?.first
                    }
                    val bmId = Pair(bm, item.mediaId)
                    lastBitmap = bmId
                    ensureActive()
                    val notif = manager.makeNotification(
                        bm = lastBitmap?.first,
                        notify = false,
                        mi = item,
                        isPlaying = p.playWhenReady
                    )
                    getNotifCallback.onNotificationChanged(MediaNotification(NOTIFICATION_ID, notif))
                }
            }
        }
    }

    private fun handleNotifPlayWhenReadyChanged(m: ExoNotificationManagers, play: Boolean) {
        m.makeNotification( bm = lastBitmap?.first, isPlaying = play)
        Timber.d("ExoController UpdateNotification handled PWR changed to $play")
    }

    @SuppressLint("SwitchIntDef")
    private fun handleNotifPlaybackStateChanged(p: PlayerController, @Player.State state: Int) {
        when (state) {
            Player.STATE_READY -> {
                if (lastBitmap?.second != p.player.currentMediaItem?.mediaId) {
                    Timber.e("ExoController UpdateNotification pbs INVALID, forwarded to TransitionHandler ${state.toStrState()}")
                    updateNotification(NotificationUpdate.InvalidateMediaItem)
                } else {
                    Timber.d("ExoController UpdateNotification pbs forwarded to TransitionHandler ${state.toStrState()}")
                    updateNotification(NotificationUpdate.InvalidateMediaItem)
                }
            }
        }
    }

    private fun handleNotifRepeatModeChanged(m: ExoNotificationManagers, @Player.RepeatMode mode: Int) {
        m.makeNotification(bm = lastBitmap?.first, repeatMode = mode)
        Timber.d("ExoController UpdateNotification handled repeatMode Changed ${mode.toStrRepeat()}")
    }

    private suspend fun getDisplayEmbed(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        service.mediaItemHandler.getEmbeds(uri)?.let {
            service.coilHandler.squareWithCoil(BitmapFactory.decodeByteArray(it, 0, it.size))
        }
    }

    inner class PlayerController(
        session: MediaSession
    ) {

        val player = session.player as ExoPlayer

        private var playerListener: Player.Listener? = null
            set(value) {
                synchronized(this) {
                    if (field == null) {
                        if (value != null) {
                            player.addListener(value)
                        }
                        field = value
                        return
                    }
                    if (field != null) {
                        if (value != null) {
                            player.removeListener(field!!)
                            player.addListener(value)
                        } else {
                            player.removeListener(field!!)
                        }
                        field = value
                        return
                    }
                }
            }

        fun preparePlayer() {
            player.prepare()
            Timber.d("ExoController PlayerController, prepare()")
        }

        fun release(p: Boolean) {
            if (p) player.release()
            playerListener = null
        }

        suspend fun handleCommandWithFade(
            @FloatRange(from = 0.0, to = 1.0) to: Float,
            command: ControllerCommand.CommandWithFade
        ) {
            exoFade(to, command)
        }

        fun handleChangePlayWhenReady(play: Boolean) {
            val p = this.player
            if (p.playbackState == Player.STATE_IDLE) {
                preparePlayer()
            }
            if (p.playbackState == Player.STATE_ENDED) {
                p.seekTo(0)
            }
            if (p.playWhenReady == play) {
                updateNotification(NotificationUpdate.PlayWhenReadyChanged(play))
                Timber.e("ExoController command ChangePlayWhenReady playWhenReady == play, updating Notification... ")
            }
            p.playWhenReady = play
            Timber.d("ExoController handled command ChangePlayWhenReady $play")
        }

        fun handleChangeRepeat(@Player.RepeatMode mode: Int) {
            player.repeatMode = mode
            Timber.d("ExoController handled command ChangeRepeat ${mode.toStrRepeat()}")
        }

        fun handleStopCancel() {
            player.stop()
            Timber.d("ExoController handled command StopCancel")
        }

        fun handleSeekToNext() {
            val p = this.player
            p.seekToNextMediaItem()
            if (p.playbackState == Player.STATE_IDLE) preparePlayer()
            Timber.d("ExoController handled command SeekToNext()")
        }

        fun handleSeekToPrev() {
            val p = this.player
            p.seekToPrevious()
            if (p.playbackState == Player.STATE_IDLE) preparePlayer()
            Timber.d("ExoController handled command SeekToPrev()")
        }

        fun handleSeekToPrevMedia() {
            val p = this.player
            p.seekToPreviousMediaItem()
            if (p.playbackState == Player.STATE_IDLE) preparePlayer()
            Timber.d("ExoController handled command SeekToPrevMedia()")
        }

        private val exoReadyListener = mutableListOf<( (ExoPlayer) -> Unit )>()

        private fun exoReady() = synchronized(exoReadyListener) {
            exoReadyListener.forEach { it(player) }
            exoReadyListener.clear()
        }

        private fun whenExoReady(command: ( (ExoPlayer) -> Unit ) ) {
            if (player.playbackState == Player.STATE_READY) command(player)
            else exoReadyListener.add(command)
        }

        private fun controller(
            whenReady: ( (ExoPlayer) -> Unit)? = null,
            command: ( (ExoPlayer) -> Unit) = {}
        ) {
            command(player)
            whenReady?.let { whenExoReady(it) }
        }

        private var defaultListener = object : Player.Listener {

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                mediaItem?.let {
                    /*updateNotification(NotificationUpdate.MediaItemTransition(mediaItem))*/
                }
                Timber.d("onMediaItemTransition player listener $mediaItem ${mediaItem?.getDisplayTitle} ${mediaItem?.localConfiguration}")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_ENDED -> player.pause()
                    Player.STATE_BUFFERING -> { /* TODO() */ }
                    Player.STATE_IDLE -> { /* TODO() */ }
                    Player.STATE_READY -> { exoReady() }
                }

                /*updateNotification(NotificationUpdate.PlaybackStateChanged(playbackState))*/
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                when (error.errorCodeName) {
                    "ERROR_CODE_DECODING_FAILED" -> {
                        controller {
                            it.stop()
                            preparePlayer()
                        }
                    }
                    "ERROR_CODE_IO_FILE_NOT_FOUND" -> {
                        controller {
                            it.removeMediaItem(it.currentMediaItemIndex)
                            preparePlayer()
                        }
                    }
                    "ERROR_CODE_IO_UNSPECIFIED" -> {
                        controller {
                            commandController(ControllerCommand.StopCancel() {
                                session.player.removeMediaItem(it.currentMediaItemIndex)
                            })
                        }
                    }
                }
                Timber.e("onPlayerError ${error.errorCodeName}")
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                /*updateNotification(NotificationUpdate.PlayWhenReadyChanged(playWhenReady))*/
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                updateNotification(NotificationUpdate.RepeatModeChanged(repeatMode))
            }
        }

        private var fadingEnabled = false
        private var fading = false
        private val fadeLock = Any()
        private var fadeListener = mutableListOf<ControllerCommand.CommandWithFade>()
        private suspend fun exoFade(
            @FloatRange(from = 0.0,to = 1.0) to: Float,
            command: ControllerCommand.CommandWithFade
        ) {

            if (command.flush) fadeListener.clear()
            fadeListener.add(command)

            if (fading || command.command is ControllerCommand.SkipToNext || command.command is ControllerCommand.SkipToPrevMedia) {
                controller(
                    whenReady = { exo ->
                        exo.volume = 1f
                        fading = false
                    }
                ) { _ ->
                    synchronized(fadeLock) {
                        fadeListener.forEach { commandController(it.command) }
                        fadeListener.clear()
                    }
                }
                return
            }

            fading = true
            while (player.volume > to
                && player.playWhenReady
                && player.playbackState == Player.STATE_READY
                && fading
            ) {
                player.volume -= 0.1f
                delay(100)
            }

            if (fading) {
                controller(
                    whenReady = { exo ->
                        exo.volume = 1f
                        fading = false
                    }
                ) { _ ->
                    synchronized(fadeLock) {
                        fadeListener.forEach { commandController(it.command) }
                        fadeListener.clear()
                    }
                }
            }
        }

        init {
            player.addListener(defaultListener)
            Timber.d("ExoController Player Init, listener Added")
        }
    }

    inner class ExoNotificationManagers(
        private var service: MusicService,
        private var session: MediaSession,
    ) {
        
        private val manager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        init {
            if (VersionHelper.isOreo()) createNotificationChannel()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createNotificationChannel() {
            manager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_LOW)
            )
        }

        val context = service

        private val actionCancel = makeActionCancel()
        private val actionPlay = makeActionPlay()
        private val actionPause = makeActionPause()
        private val actionPrev = makeActionPrev()
        private val actionPrevDisabled = makeActionPrevDisabled()
        private val actionNext = makeActionNext()
        private val actionNextDisabled = makeActionNextDisabled()
        private val actionRepeatOff = makeActionRepeatOffToOne()
        private val actionRepeatOne = makeActionRepeatOneToAll()
        private val actionRepeatAll = makeActionRepeatAllToOff()

        fun makeNotification(
            bm: Bitmap? = null,
            id: String = NOTIFICATION_CHANNEL_ID,
            i: Int = session.player.currentMediaItemIndex,
            mi: MediaItem? = session.player.currentMediaItem,
            @Player.RepeatMode repeatMode: Int = session.player.repeatMode,
            showPrev: Boolean = session.player.hasPreviousMediaItem(),
            showNext: Boolean = session.player.hasNextMediaItem(),
            isPlaying: Boolean = session.player.isPlaying,
            subtitle: String = mi?.getSubtitle.toString(),
            title: String = mi?.getDisplayTitle.toString(),
            notify: Boolean = true,
            validatePlayPause: Boolean = false
        ): Notification {
            
            return NotificationCompat.Builder(context, NOTIFICATION_NAME).apply {

                setContentIntent(context.packageManager?.getLaunchIntentForPackage(
                    context.packageName
                )?.let {
                    PendingIntent.getActivity(context, 445,
                        it.apply {
                            this.putExtra("NOTIFICATION_CONTENT_ID", mi?.mediaId)
                            this.putExtra("NOTIFICATION_CONTENT_URI", mi?.mediaMetadata?.mediaUri)
                            this.putExtra("NOTIFICATION_CONTENT_INDEX", i)
                        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                })

                setContentTitle(title)
                setContentText(subtitle)
                setSmallIcon(R.drawable.play_icon_theme3)
                setChannelId(id)
                setColorized(true)
                setOngoing(true)

                when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> addAction(actionRepeatOff)
                    Player.REPEAT_MODE_ONE -> addAction(actionRepeatOne)
                    Player.REPEAT_MODE_ALL -> addAction(actionRepeatAll)
                }

                if (showPrev) addAction(actionPrev) else addAction(actionPrevDisabled)

                if (isPlaying) addAction(actionPause) else addAction(actionPlay)

                if (showNext) addAction(actionNext) else addAction(actionNextDisabled)
                
                addAction(actionCancel)

                val media = MediaStyleNotificationHelper.DecoratedMediaCustomViewStyle(session)
                media.setShowActionsInCompactView(1,2,3)

                setLargeIcon( bm )
                setStyle( media )

            }.build().also {
                lastNotification = it
                if (notify) {
                    manager.notify(NOTIFICATION_ID,it)
                    Timber.d("ExoController NotificationUpdated")
                    with(service) {
                        serviceScope.launch {
                            if (!service.isForegroundService) {
                                service.startForeground(NOTIFICATION_ID, it)
                                isForegroundService = true
                            }
                            validatePP(it, isPlaying)
                        }
                    }
                }
            }
        }

        // sometimes the NotificationManager kind of debounce the updates
        private suspend fun validatePP(notif: Notification, pp: Boolean) {
            pp.let { play ->
                if (manager.activeNotifications.isEmpty()) return
                val p = manager.activeNotifications.last()?.notification?.actions?.find {
                    it.title == if (play) "PAUSE" else "PLAY"
                }
                if (p == null) {
                    val iden = service.exo.currentMediaItem?.mediaId
                    delay(300)
                    if (service.exo.isPlaying == play && service.exo.currentMediaItem?.mediaId == iden) {
                        manager.notify(
                            NOTIFICATION_ID,
                            notif
                        )
                        Timber.d("ExoController NotificationUpdate playPause Debounce handled")
                    }
                }
            }
        }

        private fun makeActionCancel() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_close, "ACTION_CANCEL", PendingIntent.getBroadcast(
                service, ACTION_CANCEL_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_CANCEL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOffToOne() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_off, "REPEAT_OFF", PendingIntent.getBroadcast(
                service, ACTION_REPEAT_OFF_TO_ONE_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_REPEAT_OFF_TO_ONE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOneToAll() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_one, "REPEAT_ONE", PendingIntent.getBroadcast(
                service, ACTION_REPEAT_ONE_TO_ALL_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_REPEAT_ONE_TO_ALL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatAllToOff() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_all, "REPEAT_ALL", PendingIntent.getBroadcast(
                service, ACTION_REPEAT_ALL_TO_OFF_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_REPEAT_ALL_TO_OFF)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPlay() = NotificationCompat.Action.Builder(
            R.drawable.ic_notification_play_50, "PLAY", PendingIntent.getBroadcast(
                service, ACTION_PLAY_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_PLAY)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPause() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_pause, "PAUSE", PendingIntent.getBroadcast(
                service, ACTION_PAUSE_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_PAUSE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionNext() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_next, "NEXT", PendingIntent.getBroadcast(
                service, ACTION_NEXT_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_NEXT)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionNextDisabled() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_next_disabled, "NEXT_DISABLED", PendingIntent.getBroadcast(
                service, 900, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_NEXT_DISABLED)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPrev() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev, "PREV", PendingIntent.getBroadcast(
                service, ACTION_PREV_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_PREV)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPrevDisabled() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev_disabled, "PREV_DISABLED", PendingIntent.getBroadcast(
                service, 901, Intent(PLAYBACK_INTENT).apply {
                    putExtra(ACTION, ACTION_PREV_DISABLED)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

    }

    companion object {

        private var instance: ExoController? = null

        fun getInstance(service: MusicService, session: MediaSession): ExoController {
            if (instance == null) {
                Timber.d("ExoController getInstance $instance == 'null', initializing...")
                instance = ExoController(service, session)
            }
            if (instance?.service != service) {
                Timber.d("ExoController getInstance ${instance?.service} != $service")
                instance?.service = service
            }
            if (instance?.session != session)  {
                Timber.d("ExoController getInstance ${instance?.session} != $session")
                instance?.session = session
            }
            return instance!!
        }

        fun releaseInstance(n: ExoController) {
            if (instance === n) {
                instance!!.release()
                instance = null
                Timber.d("ExoController instance Released")
            } else {
                Timber.e("ExoController release Instance Ignored, $instance === $n ${instance === n}, == ${instance == n}")
            }
        }

        // re-set the session to add the listener , prepare() isn't called
        fun updateSession(session: MediaSession) {
            Timber.d("ExoController updateSession")
            if (instance == null) {
                throw IllegalStateException("ExoController hasn't been initialized")
            } else {
                instance!!.session = session
            }
        }
    }
}

sealed class NotificationUpdate {

    data class MediaItemTransition(val mediaItem: MediaItem) : NotificationUpdate() {
        override fun toString(): String {
            val m = "MediaItemTransition"
            return "$m, title: ${mediaItem.getDisplayTitle} id: ${mediaItem.mediaId}. ${mediaItem.hashCode()}"
        }
    }

    data class PlaybackStateChanged(@Player.State val state: Int) : NotificationUpdate() {
        override fun toString(): String {
            val m = "PlaybackStateChanged"
            return "$m ${state.toStrState()}"
        }
    }
    data class PlayWhenReadyChanged(val play: Boolean) : NotificationUpdate() {
        override fun toString(): String {
            val m = "PlayWhenReadyChanged"
            return "$m, $play"
        }
    }
    data class RepeatModeChanged(@Player.RepeatMode val repeat: Int) : NotificationUpdate() {
        override fun toString(): String {
            val m = "RepeatModeChanged"
            return "$m, ${repeat.toStrRepeat()}"
        }
    }
    object InvalidateMediaItem : NotificationUpdate() {
        override fun toString(): String {
            return "InvalidateMediaItem"
        }
    }
}

sealed class ControllerCommand {
    data class CommandWithFade(val command: ControllerCommand, val flush: Boolean) : ControllerCommand() {
        override fun toString(): String {
            val m = "CommandWithFade"
            return "$m, $command"
        }
    }
    data class SetPlayWhenReady(val play: Boolean) : ControllerCommand() {
        override fun toString(): String {
            val m = "SetPlayWhenReady"
            return "$m, $play"
        }
    }
    data class SetRepeatMode(@Player.RepeatMode val repeat: Int) : ControllerCommand() {
        override fun toString(): String {
            val m = "SetRepeatMode"
            return "$m, ${repeat.toStrRepeat()}"
        }
    }
    object SkipToNext : ControllerCommand() {
        override fun toString(): String {
            return "SkipToNext"
        }
    }
    object SkipToPrev : ControllerCommand() {
        override fun toString(): String {
            return "SkipToPrev"
        }
    }
    object SkipToPrevMedia : ControllerCommand() {
        override fun toString(): String {
            return "SkipToPrevMedia"
        }
    }
    object TogglePlayWhenReady: ControllerCommand() {
        override fun toString(): String {
            return "TogglePWR"
        }
    }
    data class StopCancel(
        val callback: () -> Unit
    ) : ControllerCommand() {
        override fun toString(): String {
            return "StopCancel"
        }
    }
}