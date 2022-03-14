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
import com.kylentt.mediaplayer.disposed.domain.service.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class NotificationUpdates {

    data class MediaItemTransition(val mediaItem: MediaItem) : NotificationUpdates() {
        override fun toString(): String {
            val m = "MediaItemTransition"
            return "$m, title: ${mediaItem.getDisplayTitle} id: ${mediaItem.mediaId}. ${mediaItem.hashCode()}"
        }
    }

    data class PlaybackStateChanged(@Player.State val state: Int) : NotificationUpdates() {
        override fun toString(): String {
            val m = "PlaybackStateChanged"
            return when (state) {
                Player.STATE_IDLE -> "$m, PLAYER_STATE_IDLE"
                Player.STATE_BUFFERING -> "$m, PLAYER_STATE_BUFFERING"
                Player.STATE_READY -> "$m, PLAYER_STATE_READY"
                Player.STATE_ENDED -> "$m, PLAYER_STATE_ENDED"
                else -> "$m, NOTHING"
            }
        }
    }
    data class PlayWhenReadyChanged(val play: Boolean) : NotificationUpdates() {
        override fun toString(): String {
            val m = "PlayWhenReadyChanged"
            return "$m, $play"
        }
    }
    data class RepeatModeChanged(@Player.RepeatMode val repeat: Int) : NotificationUpdates() {
        override fun toString(): String {
            val m = "RepeatModeChanged"
            return when (repeat) {
                Player.REPEAT_MODE_OFF-> "$m, PLAYER_REPEAT_MODE_OFF"
                Player.REPEAT_MODE_ONE -> "$m, PLAYER_REPEAT_MODE_ONE"
                Player.REPEAT_MODE_ALL -> "$m, PLAYER_REPEAT_MODE_ALL"
                else -> "$m, NOTHING"
            }
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
            return when (repeat) {
                Player.REPEAT_MODE_OFF-> "$m, PLAYER_REPEAT_MODE_OFF"
                Player.REPEAT_MODE_ONE -> "$m, PLAYER_REPEAT_MODE_ONE"
                Player.REPEAT_MODE_ALL -> "$m, PLAYER_REPEAT_MODE_ALL"
                else -> "$m, NOTHING"
            }
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
    object StopCancel : ControllerCommand() {
        override fun toString(): String {
            return "StopCancel"
        }
    }
}

@MainThread
class ExoControllers(
    private var service: MusicService,
    session: MediaSession
) {

    private var playerController: PlayerController? = PlayerController(session)
    private var notificationManager: ExoNotificationManagers? = ExoNotificationManagers(service, session)

    private var session: MediaSession? = session
        set(value) {
            if (value?.player !== field?.player) {
                playerController?.playerListener = null
                field?.player?.release()
            }
            value?.let {
                playerController = PlayerController(it)
                notificationManager = ExoNotificationManagers(service, it)
            }
            field = value
        }

    fun updateNotification(
        type: NotificationUpdates
    ) {

        Timber.d("ExoController UpdateNotification $type")

        when (type) {
            is NotificationUpdates.MediaItemTransition -> {
                handleMediaItemTransition(type.mediaItem)
            }
            is NotificationUpdates.PlayWhenReadyChanged -> {
                handlePlayWhenReadyChanged(type.play)
            }
            is NotificationUpdates.PlaybackStateChanged -> {
                handlePlaybackStateChanged(type.state)
            }
            is NotificationUpdates.RepeatModeChanged -> {
                handleRepeatModeChanged(type.repeat)
            }
        }
    }

    fun commandController(
        type: ControllerCommand
    ) {

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
            ControllerCommand.SkipToNext -> {
                handleSeekToNext()
            }
            ControllerCommand.SkipToPrev -> {
                handleSeekToPrev()
            }
            ControllerCommand.StopCancel -> {
                session?.player?.stop()
                service.stopForeground(true)
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

    private fun handleMediaItemTransition(item: MediaItem) {
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
    }

    private fun handleChangePlayWhenReady(play: Boolean) {
        val p = session?.player
        p?.let {
            if (it.playbackState == Player.STATE_IDLE) it.prepare()
            it.playWhenReady = play
            Timber.d("ExoController handled ChangePlayWhenReady")
        }
    }

    private fun handleChangeRepeat(@Player.RepeatMode mode: Int) {
        session?.player?.let {
            it.repeatMode = mode
            Timber.d("ExoController handled ChangeRepeat $mode")
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

    private fun handlePlayWhenReadyChanged(play: Boolean) {
        Timber.d("ExoController handled pwr Changed $play")
        notificationManager?.makeNotification(
            bm = lastBitmap?.first,
            isPlaying = play,
        )
    }

    private fun handlePlaybackStateChanged(@Player.State state: Int) {
        if (state != Player.STATE_IDLE
            && state != Player.STATE_BUFFERING
        ) {
            session?.player?.playWhenReady?.let {
                Timber.d("ExoController handled pbs Changed $state")
                notificationManager?.makeNotification(
                    bm = lastBitmap?.first,
                    isPlaying = it
                )
            }
        }
    }

    private fun handleRepeatModeChanged(@Player.RepeatMode mode: Int) {
        Timber.d("ExoController handled repeatMode Changed $mode")
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
                    updateNotification(NotificationUpdates.MediaItemTransition(mediaItem))
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

                updateNotification(NotificationUpdates.PlaybackStateChanged(playbackState))
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                updateNotification(NotificationUpdates.PlayWhenReadyChanged(playWhenReady))
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                updateNotification(NotificationUpdates.RepeatModeChanged(repeatMode))
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
                if (!service.isForegroundService) {
                    service.startForeground(NOTIFICATION_ID, it)
                }
                service.serviceScope.launch { validatePP(it, isPlaying) } 
            }
        }

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
                        Constants.NOTIFICATION_ID,
                        notif
                    )
                }
            }
        }

        private fun makeActionCancel() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_close, "ACTION_CANCEL", PendingIntent.getBroadcast(
                service, Constants.ACTION_CANCEL_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_CANCEL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOffToOne() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_off, "REPEAT_OFF", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_OFF_TO_ONE_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_REPEAT_OFF_TO_ONE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOneToAll() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_one, "REPEAT_ONE", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_ONE_TO_ALL_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_REPEAT_ONE_TO_ALL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatAllToOff() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_all, "REPEAT_ALL", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_ALL_TO_OFF_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_REPEAT_ALL_TO_OFF)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPlay() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_play, "PLAY", PendingIntent.getBroadcast(
                service, Constants.ACTION_PLAY_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PLAY)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPause() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_pause, "PAUSE", PendingIntent.getBroadcast(
                service, Constants.ACTION_PAUSE_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PAUSE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionNext() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_next, "NEXT", PendingIntent.getBroadcast(
                service, Constants.ACTION_NEXT_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_NEXT)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionNextDisabled() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_next_disabled, "NEXT_DISABLED", PendingIntent.getBroadcast(
                service, 900, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_NEXT_DISABLED)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPrev() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev, "PREV", PendingIntent.getBroadcast(
                service, Constants.ACTION_PREV_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PREV)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionPrevDisabled() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev_disabled, "PREV_DISABLED", PendingIntent.getBroadcast(
                service, 901, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PREV_DISABLED)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

    }

    companion object {
        var instance: ExoControllers? = null

        fun getInstance(service: MusicService, session: MediaSession): ExoControllers {
            if (instance == null) {
                Timber.d("ExoController getInstance $instance == null")
                instance = ExoControllers(service, session)
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
    }
}



@MainThread
class ExoController(
    exo: ExoPlayer,
    notificationManager: ExoNotificationManager,
    listener: Player.Listener?
) {

    // maybe extension function

    private var activePlayer: ExoPlayer? = null
        set(value) {

            if (field != null) {
                if (activeListener != null) activeListener = null
                field?.release()
                field = value
            } else if (field == null) {
                when (value) {
                    is ExoPlayer -> {
                        // assign
                        if (activeListener != null) {
                            throw RuntimeException(IllegalArgumentException("Invalid Argument Listener hasn't been cleared $activeListener $activePlayer"))
                        }
                        field = value
                    }
                    null ->  field = value
                }
            }
        }

    private var activeListener: Player.Listener? = null
        set(value) {
            Timber.d("ExoController activeListener set to $value")
            if (activePlayer != null) {

                when {
                    value != null && field != null -> {
                        activePlayer!!.removeListener(field!!)
                        activePlayer!!.addListener(value)
                        field = value
                    }

                    value != null && field == null -> {
                        activePlayer!!.addListener(value)
                        field = value
                    }

                    value == null && field != null -> {
                        activePlayer!!.removeListener(field!!)
                        field = value
                    }
                }

            } else if (activePlayer == null) {
                if (value == null) field = null
                else throw RuntimeException(
                    "Stub!",
                    IllegalStateException("Invalid Argument player hasn't been set, listener: $activeListener, player: $activePlayer")
                )
            }
        }

    private var activeManager: ExoNotificationManager? = notificationManager
        set(value) {
            Timber.d("activeManager set to $value")
            value?.let { notif ->
                activePlayer?.currentMediaItem?.let {
                    notif.updateNotification(NotificationUpdate.MediaItemTransition(it))
                }
            }
            field = value
        }

    private val exoIdleListener = mutableListOf<( (ExoPlayer) -> Unit)>()
    private val exoBufferListener = mutableListOf<( (ExoPlayer) -> Unit )>()
    private val exoReadyListener = mutableListOf<( (ExoPlayer) -> Unit )>()
    private val exoEndedListener = mutableListOf<( (ExoPlayer) -> Unit )>()

    private fun exoReady() = synchronized(exoReadyListener) {
        exoReadyListener.forEach { it(activePlayer!!) }
        exoReadyListener.clear()
    }
    private fun exoBuffer() = synchronized(exoBufferListener) {
        exoBufferListener.forEach { it(activePlayer!!) }
        exoBufferListener.clear()
    }
    private fun exoEnded() = synchronized(exoEndedListener) {
        exoEndedListener.forEach { it(activePlayer!!) }
        exoEndedListener.clear()
    }
    private fun exoIdle() = synchronized(exoIdleListener) {
        exoIdleListener.forEach { it(activePlayer!!) }
        exoIdleListener.clear()
    }

    private fun whenExoReady(command: ( (ExoPlayer) -> Unit ) ) {
        if (activePlayer!!.playbackState == Player.STATE_READY)
            command(activePlayer!!) else  exoReadyListener.add(command)
    }

    private fun whenBuffer( command: ( (ExoPlayer) -> Unit ) ) {
        if (activePlayer!!.playbackState == Player.STATE_BUFFERING)
            command(activePlayer!!) else exoBufferListener.add(command)
    }

    private fun whenEnded(command: ( (ExoPlayer) -> Unit ) ) {
        if (activePlayer!!.playbackState == Player.STATE_ENDED)
            command(activePlayer!!) else exoEndedListener.add(command)
    }

    private fun whenIdle( command: ( (ExoPlayer) -> Unit ) ) {
        if (activePlayer!!.playbackState == Player.STATE_IDLE)
            command(activePlayer!!) else exoReadyListener.add(command)
    }

    private var defaultListener = object  : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            when(playbackState) {
                Player.STATE_IDLE -> exoIdle()
                Player.STATE_BUFFERING -> exoBuffer()
                Player.STATE_READY -> exoReady()
                Player.STATE_ENDED -> {
                    activePlayer!!.pause()
                    exoEnded()
                }
            }

            if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_BUFFERING)
                notificationManager.updateNotification(NotificationUpdate.PlaybackStateChanged)
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaItem?.let {
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        notificationManager.updateNotification(NotificationUpdate.MediaItemTransition(it))
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED -> {
                        notificationManager.updateNotification(NotificationUpdate.MediaItemTransition(it))
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                        notificationManager.updateNotification(NotificationUpdate.MediaItemTransition(it))
                    }
                    Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> {
                        notificationManager.updateNotification(NotificationUpdate.MediaItemTransition(it))
                    }
                }
            }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            super.onPlayWhenReadyChanged(playWhenReady, reason)

            when (reason) {

                Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST -> {
                    notificationManager.updateNotification(NotificationUpdate.PlayWhenReadyChanged(playWhenReady))
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM -> {
                    // TODO()
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY -> {
                    notificationManager.updateNotification(NotificationUpdate.PlayWhenReadyChanged(playWhenReady))
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS -> {
                    notificationManager.updateNotification(NotificationUpdate.PlayWhenReadyChanged(playWhenReady))
                }
                Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE -> {
                    // TODO()
                }
            }
        }
        override fun onRepeatModeChanged(repeatMode: Int) {
            super.onRepeatModeChanged(repeatMode)
            val repeat = when (repeatMode) {
                Player.REPEAT_MODE_ALL -> {
                    "Repeat_Mode_All"
                }
                Player.REPEAT_MODE_OFF -> {
                    "Repeat_Mode_Off"
                }
                Player.REPEAT_MODE_ONE -> {
                    "Repeat_Mode_One"
                }
                else -> "Repeat_Mode_Unknown"
            }
            notificationManager.updateNotification(NotificationUpdate.RepeatModeChanged)
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Timber.e(error)
            when(error.errorCodeName) {
                "ERROR_CODE_DECODING_FAILED" -> {
                    controller {
                        it.stop()
                        it.prepare()
                    }
                }
                "ERROR_CODE_IO_FILE_NOT_FOUND" -> {
                    controller {
                        it.removeMediaItem(it.currentMediaItemIndex)
                        it.pause()
                        it.prepare()
                    }
                }
            }
            Timber.e("onPlayerError ${error.errorCodeName}")
        }
    }


    fun controller(
        whenReady: ( (ExoPlayer) -> Unit)? = null,
        command: ( (ExoPlayer) -> Unit) = {}
    ) {
        command(activePlayer!!)
        whenReady?.let { whenExoReady(it) }
    }

    private var fading = false
    private var fadeListener = mutableListOf< (ExoPlayer) -> Unit >()
    suspend fun exoFade(
        @FloatRange(from = 0.0,to = 1.0) to: Float,
        clear: Boolean,
        command: (ExoPlayer) -> Unit
    ) {

        if (clear) fadeListener.clear()
        fadeListener.add(command)

        if (fading) {
            fading = false
            controller(
                whenReady = {
                    it.volume = 1f
                }
            ) { exo ->
                synchronized(fadeListener) {
                    fadeListener.forEach { it(exo) }
                    fadeListener.clear()
                }
            }
        }

        fading = true
        while (activePlayer!!.volume > to && activePlayer!!.playWhenReady && fading) {
            activePlayer!!.volume -= 0.1f
            delay(100)
        }

        if (fading) {
            controller(
                whenReady = {
                    activePlayer!!.volume = 1f
                    fading = false
                }
            ) { exo ->
                synchronized(fadeListener) {
                    fadeListener.forEach { it(exo) }
                    fadeListener.clear()
                }
            }
        }
    }

    init {
        Timber.d("ExoController Init")
        synchronized(this) {
            activeManager = notificationManager
            activePlayer = exo
            activeListener = listener ?: defaultListener
        }
    }

    fun stopExo() {
        this.activePlayer?.stop()
    }

    fun reassign(exo: ExoPlayer, manager: ExoNotificationManager, listener: Player.Listener?) {

        Timber.d("ExoController reassign exo, $exo ${this.activePlayer} ${exo === this.activePlayer}")
        Timber.d("ExoController reassign manager, $manager ${this.activeManager} ${manager === this.activeManager}")

        if (exo !== this.activePlayer) {
            this.activeListener = null
            this.activePlayer = null
            this.activePlayer = exo
            this.activeListener = listener ?: defaultListener
        }
        if (manager !== this.activeManager) {
            this.activeManager = manager
        }
    }

    companion object {

        private var instance: ExoController? = null

        fun getInstance(exo: ExoPlayer, notificationManager: ExoNotificationManager, listener: Player.Listener?): ExoController {
            if (instance == null) {
                instance = ExoController(exo, notificationManager, listener)
            } else {
                instance!!.reassign(exo, notificationManager, listener)
            }
            return instance!!
        }
    }
}



