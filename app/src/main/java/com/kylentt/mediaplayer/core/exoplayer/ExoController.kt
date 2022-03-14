package com.kylentt.mediaplayer.core.exoplayer

import android.app.*
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
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.util.Constants
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_CHANNEL_ID
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_ID
import com.kylentt.mediaplayer.core.util.VersionHelper
import com.kylentt.mediaplayer.core.util.toStrRepeat
import com.kylentt.mediaplayer.core.util.toStrState
import com.kylentt.mediaplayer.domain.service.MusicService
import com.kylentt.mediaplayer.domain.service.ServiceConstants.PLAYBACK_INTENT
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.IllegalStateException

@MainThread
class ExoController(
    private var service: MusicService,
    session: MediaSession
) {

    private var playerController: PlayerController? = PlayerController(session)
    private var notificationManager: ExoNotificationManagers? = ExoNotificationManagers(service, session)

    private var session: MediaSession? = session
        set(value) {
            synchronized(this) {
                if (value?.player !== field?.player) {
                    playerController?.playerListener = null
                    field?.player?.release()

                    Timber.d("ExoController oldPlayerReleased, old $field new $value")
                }
                value?.let {
                    playerController = PlayerController(it)
                    notificationManager = ExoNotificationManagers(service, it)
                }
                field = value
            }
        }

    fun release() {
        notificationManager = null
        session = null
        playerController = null
        lastBitmap = null
    }

    fun getNotification(session: MediaSession): Notification? {
        Timber.d("ExoController getNotification ${session === this.session}")
        return notificationManager?.makeNotification(
            lastBitmap?.first,
            NOTIFICATION_CHANNEL_ID,
            session.player.currentMediaItemIndex,
            session.player.currentMediaItem,
            session.player.repeatMode,
            session.player.hasPreviousMediaItem(),
            session.player.hasNextMediaItem(),
            session.player.playWhenReady,
            session.player.currentMediaItem?.getSubtitle.toString(),
            session.player.currentMediaItem?.getDisplayTitle.toString()
        )
    }

    private val commandLock = Any()
    fun commandController(
        type: ControllerCommand
    ) {

        synchronized(commandLock) {

            Timber.d("ExoController CommandController $type")

            when (type) {
                is ControllerCommand.CommandWithFade -> {
                    service.serviceScope.launch {
                        handleCommandWithFade(0f, type.flush, type)
                    }
                }
                is ControllerCommand.SetPlayWhenReady -> {
                    handleChangePlayWhenReady(type.play)
                }
                is ControllerCommand.SetRepeatMode -> {
                    handleChangeRepeat(type.repeat)
                }
                is ControllerCommand.StopCancel -> {
                    session?.player?.stop()
                    service.stopForeground(true)
                    type.callback.invoke()
                }
                ControllerCommand.SkipToNext -> {
                    handleSeekToNext()
                }
                ControllerCommand.SkipToPrev -> {
                    handleSeekToPrev()
                }
                ControllerCommand.TogglePlayWhenReady -> {
                    // TODO()
                }
                ControllerCommand.SkipToPrevMedia -> {
                    handleSeekToPrevMedia()
                }
            }
        }
    }

    private val updateLock = Any()
    fun updateNotification(
        type: NotificationUpdate
    ) {
        synchronized(updateLock) {

            Timber.d("ExoController UpdateNotification $type")

            when (type) {
                is NotificationUpdate.MediaItemTransition -> {
                    handleNotifMediaItemTransition(type.mediaItem)
                }
                is NotificationUpdate.PlayWhenReadyChanged -> {
                    handleNotifPlayWhenReadyChanged(type.play)
                }
                is NotificationUpdate.PlaybackStateChanged -> {
                    handleNotifPlaybackStateChanged(type.state)
                }
                is NotificationUpdate.RepeatModeChanged -> {
                    handleNotifRepeatModeChanged(type.repeat)
                }
                NotificationUpdate.InvalidateMediaItem -> {
                    handleNotifMediaItemTransition(null)
                }
            }
        }
    }

    private suspend fun handleCommandWithFade(
        @FloatRange(from = 0.0, to = 1.0) to: Float,
        flush: Boolean,
        command: ControllerCommand.CommandWithFade
    ) {
        playerController?.exoFade(to, flush, command)
        Timber.d("ExoController handled $command")
    }

    private var lastBitmap: Pair<Bitmap?, String>? = null

    private fun handleNotifMediaItemTransition(item: MediaItem?) {
        item?.let { _ ->
            if (item.mediaId == lastBitmap?.second) {
                notificationManager?.makeNotification(
                    bm = lastBitmap?.first,
                    mi = item,
                    isPlaying = session?.player?.playWhenReady == true
                )
            } else {
                service.serviceScope.launch {
                    val bm = item.getUri?.let { getDisplayEmbed(it) }
                    lastBitmap = Pair(bm, item.mediaId)
                    notificationManager?.makeNotification(
                        bm = bm,
                        mi = item,
                        isPlaying = session?.player?.playWhenReady == true
                    )
                }
            }
        } ?: service.serviceScope.launch {
            val n = session?.player?.currentMediaItem
            n?.let { item ->
                val bm = if (lastBitmap?.second != n.mediaId) {
                    Timber.d("ExoController UpdateNotification MediaItemTransition Invalidating with new Bitmap")
                    item.getUri?.let { getDisplayEmbed(it) }
                } else {
                    Timber.d("ExoController UpdateNotification MediaItemTransition Invalidating with old Bitmap ${lastBitmap?.second}")
                    lastBitmap?.first
                }
                lastBitmap = Pair(bm, item.mediaId)
                notificationManager?.makeNotification(
                    bm = bm,
                    mi = item,
                    isPlaying = session?.player?.playWhenReady == true
                )
            }
            Timber.d("ExoController UpdateNotification MediaItemTransition Invalidated ${n?.getDisplayTitle} ${n?.mediaId}")
        }
    }

    private fun handleChangePlayWhenReady(play: Boolean) {
        val p = session?.player
        p?.let {
            if (it.playbackState == Player.STATE_ENDED) {
                it.seekTo(0)
            }
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            if (it.playWhenReady == play) {
                Timber.d("ExoController ChangePlayWhenReady playWhenReady == play, updating Notification... ")
                updateNotification(NotificationUpdate.PlayWhenReadyChanged(play))
            }
            it.playWhenReady = play
            Timber.d("ExoController handled ChangePlayWhenReady $play")
        }
    }

    private fun handleChangeRepeat(@Player.RepeatMode mode: Int) {
        session?.player?.let {
            it.repeatMode = mode
            Timber.d("ExoController handled ChangeRepeat ${mode.toStrState()}")
        }
    }

    private fun handleSeekToNext() {
        session?.player?.let {
            Timber.d("ExoController handled SeekToNext()")
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.seekToNextMediaItem()
        }
    }

    private fun handleSeekToPrev() {
        session?.player?.let {
            Timber.d("ExoController handled SeekToPrev()")
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.seekToPrevious()
        }
    }

    private fun handleSeekToPrevMedia() {
        session?.player?.let {
            Timber.d("ExoController handled SeekToPrev()")
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.seekToPreviousMediaItem()
        }
    }

    private fun handleTogglePWR() {
        session?.player?.let {
            it.playWhenReady = !it.playWhenReady
        }
    }

    private fun handleNotifPlayWhenReadyChanged(play: Boolean) {
        Timber.d("ExoController handled pwr Changed $play")
        notificationManager?.makeNotification(
            bm = lastBitmap?.first,
            isPlaying = play,
        )
    }

    private fun handleNotifPlaybackStateChanged(@Player.State state: Int) {

        when (state) {
            Player.STATE_READY -> {
                session?.player?.let {
                    val item = it.currentMediaItem
                    if (lastBitmap?.second != item?.mediaId) {
                        item?.let {
                            Timber.d("ExoController pbs forwarded to TransitionHandler ${state.toStrState()}")
                            updateNotification(NotificationUpdate.InvalidateMediaItem)
                        }
                    } else {
                        Timber.d("ExoController handled pbs Changed ${state.toStrState()}")
                        updateNotification(NotificationUpdate.InvalidateMediaItem)
                    }
                }
            }
        }
    }

    private fun handleNotifRepeatModeChanged(@Player.RepeatMode mode: Int) {
        Timber.d("ExoController handled repeatMode Changed ${mode.toStrRepeat()}")
        notificationManager?.makeNotification(
            bm = lastBitmap?.first,
            repeatMode = mode
        )
    }

    private suspend fun getDisplayEmbed(uri: Uri): Bitmap? {
        return service.mediaItemHandler.getEmbeds(uri)?.let {
            service.coilHandler.squareWithCoil(BitmapFactory.decodeByteArray(it, 0, it.size))
        }
    }

    inner class PlayerController(
        session: MediaSession
    ) {

        private val player = session.player as ExoPlayer

        var playerListener: Player.Listener? = null
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
                    updateNotification(NotificationUpdate.MediaItemTransition(mediaItem))
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    Player.STATE_ENDED -> player.pause()
                    Player.STATE_BUFFERING -> { /* TODO() */ }
                    Player.STATE_IDLE -> { /* TODO() */ }
                    Player.STATE_READY -> { exoReady() }
                }

                updateNotification(NotificationUpdate.PlaybackStateChanged(playbackState))
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                when (error.errorCodeName) {
                    "ERROR_CODE_DECODING_FAILED" -> {
                        controller {
                            it.stop()
                            it.prepare()
                        }
                    }
                    "ERROR_CODE_IO_FILE_NOT_FOUND" -> {
                        controller {
                            it.removeMediaItem(it.currentMediaItemIndex)
                            it.prepare()
                        }
                    }
                }
                Timber.e("onPlayerError ${error.errorCodeName}")
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                updateNotification(NotificationUpdate.PlayWhenReadyChanged(playWhenReady))
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                updateNotification(NotificationUpdate.RepeatModeChanged(repeatMode))
            }
        }

        private var fading = false
        private var fadeListener = mutableListOf<ControllerCommand.CommandWithFade>()
        suspend fun exoFade(
            @FloatRange(from = 0.0,to = 1.0) to: Float,
            clear: Boolean,
            command: ControllerCommand.CommandWithFade
        ) {

            if (clear) fadeListener.clear()
            fadeListener.add(command)

            if (fading) {
                fading = false
                controller(
                    whenReady = { exo ->
                        exo.volume = 1f
                    }
                ) { _ ->
                    synchronized(fadeListener) {
                        fadeListener.forEach { commandController(it.command) }
                        fadeListener.clear()
                    }
                }
            }

            fading = true
            while (player.volume > to && player.playWhenReady && fading) {
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
                    synchronized(fadeListener) {
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
                NotificationChannel(NOTIFICATION_CHANNEL_ID, Constants.NOTIFICATION_NAME, NotificationManager.IMPORTANCE_LOW)
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
        ): Notification {
            
            return NotificationCompat.Builder(context, Constants.NOTIFICATION_NAME).apply {

                setContentIntent(context.packageManager?.getLaunchIntentForPackage(
                    context.packageName
                )?.let {
                    PendingIntent.getActivity(context, 445,
                        it.apply {
                            this.putExtra("NOTIFICATION_CONTENT", mi?.mediaMetadata?.mediaUri)
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

                if (isPlaying) { addAction(actionPause) } else addAction(actionPlay)

                if (showNext) addAction(actionNext) else addAction(actionNextDisabled)
                
                addAction(actionCancel)

                val media = MediaStyleNotificationHelper.MediaStyle(session)
                media.setShowActionsInCompactView(1,2,3)

                setLargeIcon( bm )
                setStyle( media )

            }.build().also {
                manager.notify(NOTIFICATION_ID,it)
                with(service) {
                    serviceScope.launch {
                        if (!service.isForegroundService) {
                            service.startForeground(NOTIFICATION_ID, it)
                        }
                        validatePP(it, isPlaying)
                    }
                }
            }
        }

        // sometimes the NotificationManager kind of debounce the updates
        private suspend fun validatePP(notif: Notification, pp: Boolean) {
            pp.let { play ->
                if (manager.activeNotifications.isEmpty()) return
                val p = manager.activeNotifications.last().notification.actions.find {
                    it.title == if (play) "PAUSE" else "PLAY"
                }
                if (p == null) {
                    val iden = service.exo.currentMediaItem?.mediaId
                    delay(250)
                    if (service.exo.isPlaying == play && service.exo.currentMediaItem?.mediaId == iden) manager.notify(
                        NOTIFICATION_ID,
                        notif
                    )
                }
            }
        }

        private fun makeActionCancel() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_close, "ACTION_CANCEL", PendingIntent.getBroadcast(
                service, Constants.ACTION_CANCEL_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_CANCEL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOffToOne() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_off, "REPEAT_OFF", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_OFF_TO_ONE_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_REPEAT_OFF_TO_ONE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOneToAll() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_one, "REPEAT_ONE", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_ONE_TO_ALL_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_REPEAT_ONE_TO_ALL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatAllToOff() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_all, "REPEAT_ALL", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_ALL_TO_OFF_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_REPEAT_ALL_TO_OFF)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPlay() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_play, "PLAY", PendingIntent.getBroadcast(
                service, Constants.ACTION_PLAY_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PLAY)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPause() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_pause, "PAUSE", PendingIntent.getBroadcast(
                service, Constants.ACTION_PAUSE_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PAUSE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionNext() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_next, "NEXT", PendingIntent.getBroadcast(
                service, Constants.ACTION_NEXT_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_NEXT)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionNextDisabled() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_next_disabled, "NEXT_DISABLED", PendingIntent.getBroadcast(
                service, 900, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_NEXT_DISABLED)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPrev() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev, "PREV", PendingIntent.getBroadcast(
                service, Constants.ACTION_PREV_CODE, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PREV)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPrevDisabled() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev_disabled, "PREV_DISABLED", PendingIntent.getBroadcast(
                service, 901, Intent(PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PREV_DISABLED)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

    }

    companion object {

        var instance: ExoController? = null

        fun getInstance(service: MusicService, session: MediaSession): ExoController {
            if (instance == null) {
                Timber.d("ExoController getInstance $instance == 'null'")
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

        fun updateSession(session: MediaSession) {
            Timber.d("ExoController updateSession")
            if (instance == null) throw IllegalStateException("ExoController hasn't been initialized")
            instance!!.session = session
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