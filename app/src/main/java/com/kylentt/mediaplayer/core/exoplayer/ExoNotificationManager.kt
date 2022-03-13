package com.kylentt.mediaplayer.core.exoplayer

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.util.Constants
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_CHANNEL_ID
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_ID
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_NAME
import com.kylentt.mediaplayer.core.util.VersionHelper
import com.kylentt.mediaplayer.disposed.domain.service.MusicService
import com.kylentt.mediaplayer.disposed.domain.service.PlayerNotificationImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/** Just Playing Around, seems need to wait until next updates */

// TODO: Use Provided Notification Manager soon as Available

sealed class NotificationUpdate() {
    object RepeatModeChanged : NotificationUpdate()
    object PlaybackStateChanged : NotificationUpdate()
    data class MediaItemTransition(val mediaItem: MediaItem) : NotificationUpdate()
    data class PlayWhenReadyChanged(val play: Boolean) : NotificationUpdate()
}

class ExoNotificationManager(
    private val service: MusicService,
    private val session: MediaSession,
    private val exo: ExoPlayer
) {

    private val notificationManager = service
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val exoNotification = ExoNotification(NOTIFICATION_CHANNEL_ID)
    private val playerNotification = PlayerNotificationImpl(service)

    init {
        if (VersionHelper.isOreo()) createNotificationChannel()
    }

    var lastBitmap: Bitmap? = null
        private set

    fun updateNotification(
        type: NotificationUpdate
    ) {
        when (type) {

            is NotificationUpdate.RepeatModeChanged -> {
                val notif = exoNotification.makeNotification(NOTIFICATION_CHANNEL_ID, transition = false, bm = lastBitmap)
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
            is NotificationUpdate.PlaybackStateChanged -> {
                val notif = exoNotification.makeNotification(NOTIFICATION_CHANNEL_ID, transition = false, bm = lastBitmap)
                notificationManager.notify(NOTIFICATION_ID, notif)
            }
            is NotificationUpdate.MediaItemTransition -> {
                service.serviceScope.launch {
                    val embed = service.mediaItemHandler.getEmbeds(type.mediaItem) ?: service.mediaItemHandler.getEmbeds(type.mediaItem.getArtUri)
                    val bm = embed?.let {
                        service.coilHandler.squareWithCoil(android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size))
                    }
                    lastBitmap = bm

                    val notif = exoNotification.makeNotification(
                        NOTIFICATION_CHANNEL_ID, transition = true, bm = bm
                    )

                    notificationManager.notify(NOTIFICATION_ID,
                        notif
                    )

                    if (!service.isForegroundService) {
                        service.startForeground(NOTIFICATION_ID, notif)
                    }
                }
            }
            is NotificationUpdate.PlayWhenReadyChanged -> {
                val notif = exoNotification.makeNotification(NOTIFICATION_CHANNEL_ID, transition = false, bm = lastBitmap, pp = type.play)
                notificationManager.notify(NOTIFICATION_ID, notif)

                service.serviceScope.launch {
                    validatePP(notif, type.play)
                }
            }
        }
    }

    private suspend fun validatePP(notif: Notification, pp: Boolean) {
        pp.let { play ->
            if (notificationManager.activeNotifications.isEmpty()) return
            val p = notificationManager.activeNotifications.last().notification.actions.find {
                it.title == if (play) "PAUSE" else "PLAY"
            }
            if (p == null) {
                val iden = service.exo.currentMediaItem?.mediaId
                delay(250)
                if (service.exo.isPlaying == play && service.exo.currentMediaItem?.mediaId == iden) notificationManager.notify(
                    NOTIFICATION_ID,
                    notif)
            }
        }
    }

    /*pp?.let { play ->
        if (notificationManager.activeNotifications.isEmpty()) return@launch
        val p = notificationManager.activeNotifications.last().notification.actions.find {
            it.title == if (play) "PAUSE" else "PLAY"
        }
        if (p == null) {
            val iden = service.exo.currentMediaItem?.mediaId
            delay(250)
            if (service.exo.isPlaying == play && service.exo.currentMediaItem?.mediaId == iden) notificationManager.notify(NOTIFICATION_ID, notif)
        }
    }*/

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_LOW)
        )
    }


    inner class ExoNotification(
        private val channelId: String
    ) {

        private val context = service

        private val actionCancel = makeActionCancel()
        private val actionPlay = makeActionPlay()
        private val actionPause = makeActionPause()
        private val actionPrev = makeActionPrev()
        private val actionNext = makeActionNext()
        private val actionRepeatOff = makeActionRepeatOffToOne()
        private val actionRepeatOne = makeActionRepeatOneToAll()
        private val actionRepeatAll = makeActionRepeatAllToOff()

        @SuppressLint("RestrictedApi")
        fun makeNotification(
            CHANNEL_ID: String = channelId,
            pp: Boolean? = null,
            bm: Bitmap?,
            transition: Boolean
        ): Notification {

            val p = session.player
            val isp = pp ?: p.isPlaying
            val pwr = p.playWhenReady
            val mi = p.currentMediaItem
            val ct = mi?.getDisplayTitle
            val st = mi?.getSubtitle


            return NotificationCompat.Builder(context, NOTIFICATION_NAME).apply {

                setContentIntent(context.packageManager?.getLaunchIntentForPackage(
                    context.packageName
                )?.let {
                    PendingIntent.getActivity(context, 445,
                        it.apply {
                            this.putExtra("NOTIFICATION_CONTENT", mi?.mediaMetadata?.mediaUri)
                            this.putExtra("NOTIFICATION_CONTENT_ID", mi?.mediaId)
                        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                })

                setContentTitle(ct)
                setContentText(st)
                setSmallIcon(R.drawable.play_icon_theme3)
                setChannelId(CHANNEL_ID)
                setColorized(true)
                setOngoing(true)

                when (p.repeatMode) {
                    Player.REPEAT_MODE_OFF -> addAction(actionRepeatOff)
                    Player.REPEAT_MODE_ONE -> addAction(actionRepeatOne)
                    Player.REPEAT_MODE_ALL -> addAction(actionRepeatAll)
                }

                val havePrevButton = if (p.hasPreviousMediaItem()) {
                    addAction(actionPrev)
                    true
                } else false

                if (isp || (transition && pwr)) { addAction(actionPause) } else addAction(actionPlay)

                val haveNextButton = if (p.hasNextMediaItem()) {
                    addAction(actionNext)
                    true
                } else false

                val media = MediaStyleNotificationHelper.MediaStyle(session)

                addAction(actionCancel)

                when {
                    haveNextButton && havePrevButton  -> media.setShowActionsInCompactView(1,2,3)
                    havePrevButton && !haveNextButton -> media.setShowActionsInCompactView(1,2)
                    haveNextButton && !havePrevButton -> media.setShowActionsInCompactView(1,2)
                    else -> media.setShowActionsInCompactView(1)
                }

                setLargeIcon( bm )
                setStyle( media )

            }.build()
        }

        private fun makeActionCancel() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_close, "ACTION_CANCEL", PendingIntent.getBroadcast(
                service, Constants.ACTION_CANCEL_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_CANCEL)
                    setPackage(context.packageName)
                },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOffToOne() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_off, "REPEAT_OFF", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_OFF_TO_ONE_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, ACTION_REPEAT_OFF_TO_ONE)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatOneToAll() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_one, "REPEAT_ONE", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_ONE_TO_ALL_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, ACTION_REPEAT_ONE_TO_ALL)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()

        private fun makeActionRepeatAllToOff() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_repeat_all, "REPEAT_ALL", PendingIntent.getBroadcast(
                service, Constants.ACTION_REPEAT_ALL_TO_OFF_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, ACTION_REPEAT_ALL_TO_OFF)
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

        private fun makeActionPrev() = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_prev, "PREV", PendingIntent.getBroadcast(
                service, Constants.ACTION_PREV_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                    putExtra(Constants.ACTION, Constants.ACTION_PREV)
                    setPackage(context.packageName)
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }


}

/*class ExoNotificationManager(
    private val service: MusicService,
    private val session: MediaLibraryService.MediaLibrarySession,
    private val controller: ExoController,
    private val context: Context = service
) {

    val mediaItemHandler = service.mediaItemHandler
    val coilHandler = service.coilHandler

    private val playerNotificationManager: PlayerNotificationManager

    private val actionPlay = makeActionPlay()
    private val actionPause = makeActionPause()
    private val actionPrev = makeActionPrev()
    private val actionNext = makeActionNext()
    private val actionCancel = makeActionCancel()
    private val actionRepeatOff = makeActionRepeatOffToOne()
    private val actionRepeatOne = makeActionRepeatOneToAll()
    private val actionRepeatAll = makeActionRepeatAllToOff()

    init {
        val mediaSession = service.sessions.first()
        if (VersionHelper.isOreo()) createNotificationChannel()
        playerNotificationManager = createPlayerNotification(mediaSession = mediaSession).apply {
            setColorized(true)
            setPriority(PRIORITY_LOW)
            setSmallIcon(R.drawable.play_icon_theme3)
            setUseChronometer(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_LOW)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun createPlayerNotification(mediaSession: MediaSession): PlayerNotificationManager {
        return object : PlayerNotificationManager(
            service,
            mediaSession,
            Bundle.EMPTY,
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_ID,
            DescriptionAdapter(mediaSession),
            PlayerNotificationListener(),
            R.drawable.play_icon_theme3,
            null
        ) {

            override fun getActionButtons(): MutableList<CommandButton> {
                val actions = super.getActionButtons()
                val toReturn = mutableListOf<CommandButton>()
                val rmb = when(mediaSession.player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> buttonRepeatAll
                    Player.REPEAT_MODE_ONE -> buttonRepeatOne
                    Player.REPEAT_MODE_ALL -> buttonRepeatOff
                    else -> null
                }
                val pp = actions.find { it.playerCommand == Player.COMMAND_PLAY_PAUSE }
                val prev = actions.find { it.playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS }
                val next = actions.find { it.playerCommand == Player.COMMAND_SEEK_TO_NEXT }
                val cb = buttonCancel
                rmb?.let { toReturn.add(it) }
                pp?.let { toReturn.add(it) }
                prev?.let { toReturn.add(it) }
                next?.let { toReturn.add(it) }
                cb.let { toReturn.add(it) }
                return toReturn
            }

            override fun getActionButtonIndicesForCompactView(actionButtons: MutableList<CommandButton>): IntArray {
                return super.getActionButtonIndicesForCompactView(actionButtons)
            }
        }
    }
    val buttonRepeatOff = makeButtonRepeatOff()
    val buttonRepeatOne = makeButtonRepeatOne()
    val buttonRepeatAll = makeButtonRepeatAll()
    val buttonCancel = makeButtonCancel()

    private fun makeButtonRepeatOff() =
        CommandButton.Builder()
            .setDisplayName("REPEAT_OFF")
            .setEnabled(true)
            .setIconResId(R.drawable.ic_notif_repeat_off)
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
            .build()

    private fun makeButtonRepeatOne() =
        CommandButton.Builder()
            .setDisplayName("REPEAT_ONE")
            .setEnabled(true)
            .setIconResId(R.drawable.ic_notif_repeat_one)
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
            .build()

    private fun makeButtonRepeatAll() =
        CommandButton.Builder()
            .setDisplayName("REPEAT_All")
            .setEnabled(true)
            .setIconResId(R.drawable.ic_notif_repeat_all)
            .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE)
            .build()

    private fun makeButtonCancel() =
        CommandButton.Builder()
            .setDisplayName("CANCEL")
            .setEnabled(true)
            .setIconResId(R.drawable.ic_notif_close)
            .setPlayerCommand(Player.COMMAND_STOP)
            .build()

    val activityIntent = context.packageManager?.getLaunchIntentForPackage(context.packageName)?.let {
        PendingIntent.getActivity(context, 445,
            it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private inner class PlayerNotificationListener(): PlayerNotificationManager.NotificationListener {
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            super.onNotificationCancelled(notificationId, dismissedByUser)
            with(service) {
                serviceScope.launch {
                    controller.exoFade(0f) {
                        stopService(session = session)
                    }
                }
            }
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean,
        ) {
            super.onNotificationPosted(notificationId, notification, ongoing)
            with(service) {
                if (ongoing && !isForeground) {
                    startForeground(notificationId, notification)
                    isForeground = true
                }
            }
        }
    }

    private inner class DescriptionAdapter(
        private val mediaSession: MediaSession
    ) : PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(session: MediaSession): CharSequence {
            return session.player.currentMediaItem?.getDisplayTitle.toString()
        }

        override fun getCurrentContentText(session: MediaSession): CharSequence? {
            return session.player.currentMediaItem?.getSubtitle
        }

        var lastBitmap: Bitmap? = null
        override fun getCurrentLargeIcon(
            session: MediaSession,
            callback: PlayerNotificationManager.BitmapCallback,
        ): Bitmap? {
            val p = session.player

            val item = p.currentMediaItem

            val pict =
                item?.mediaMetadata?.artworkData
                    ?: item?.mediaMetadata?.mediaUri?.let { mediaItemHandler.getEmbeds(it) }

            val uri = p.currentMediaItem?.getArtUri

            service.serviceScope.launch {
                val bm = with(coilHandler) {
                    pict?.let { squareWithCoil(android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size)) } ?: uri?.let { makeSquaredBitmap(it) }
                }
                bm?.let { callback.onBitmap(it) }
                lastBitmap = bm
            }
            return lastBitmap
        }
    }


    private fun makeActionCancel() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_close, "ACTION_CANCEL", PendingIntent.getBroadcast(
            service, Constants.ACTION_CANCEL_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                putExtra(Constants.ACTION, Constants.ACTION_CANCEL)
                setPackage(context.packageName)
            },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    ).build()

    private fun makeActionRepeatOffToOne() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_repeat_off, "REPEAT_OFF", PendingIntent.getBroadcast(
            service, Constants.ACTION_REPEAT_OFF_TO_ONE_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                putExtra(Constants.ACTION, ACTION_REPEAT_OFF_TO_ONE)
                setPackage(context.packageName)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    ).build()

    private fun makeActionRepeatOneToAll() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_repeat_one, "REPEAT_ONE", PendingIntent.getBroadcast(
            service, Constants.ACTION_REPEAT_ONE_TO_ALL_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                putExtra(Constants.ACTION, ACTION_REPEAT_ONE_TO_ALL)
                setPackage(context.packageName)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    ).build()

    private fun makeActionRepeatAllToOff() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_repeat_all, "REPEAT_ALL", PendingIntent.getBroadcast(
            service, Constants.ACTION_REPEAT_ALL_TO_OFF_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                putExtra(Constants.ACTION, ACTION_REPEAT_ALL_TO_OFF)
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

    private fun makeActionPrev() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_prev, "PREV", PendingIntent.getBroadcast(
            service, Constants.ACTION_PREV_CODE, Intent(Constants.PLAYBACK_INTENT).apply {
                putExtra(Constants.ACTION, Constants.ACTION_PREV)
                setPackage(context.packageName)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    ).build()
}*/
