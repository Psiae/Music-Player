package com.kylentt.mediaplayer.domain.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.util.Constants.ACTION
import com.kylentt.mediaplayer.core.util.Constants.ACTION_CANCEL
import com.kylentt.mediaplayer.core.util.Constants.ACTION_CANCEL_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_NEXT
import com.kylentt.mediaplayer.core.util.Constants.ACTION_NEXT_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PAUSE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PAUSE_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PLAY
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PLAY_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PREV
import com.kylentt.mediaplayer.core.util.Constants.ACTION_PREV_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ALL_TO_OFF
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ALL_TO_OFF_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_OFF_TO_ONE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_OFF_TO_ONE_CODE
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ONE_TO_ALL
import com.kylentt.mediaplayer.core.util.Constants.ACTION_REPEAT_ONE_TO_ALL_CODE
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_CHANNEL_ID
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_ID
import com.kylentt.mediaplayer.core.util.Constants.NOTIFICATION_NAME
import com.kylentt.mediaplayer.core.util.Constants.PLAYBACK_INTENT
import com.kylentt.mediaplayer.core.util.VersionHelper

class PlayerNotification(
    private val service: MusicService,
    private val context: Context,
) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (VersionHelper.isOreo()) createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        id: String = NOTIFICATION_CHANNEL_ID, name: String = NOTIFICATION_NAME,
    ) {
        val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)
    }

    var havePrevButton = false
    var haveNextButton = false

    fun makeNotif(id: Int, session: MediaSession) = NotificationCompat.Builder(context, NOTIFICATION_NAME).apply {
        service.activityIntent?.let { setContentIntent(it) }
        setSmallIcon(R.drawable.ic_baseline_music_note_24)
        setUsesChronometer(true)
        setChannelId(NOTIFICATION_CHANNEL_ID)
        setColorized(true)
        setOngoing(true)

        when (service.exo.repeatMode) {
            Player.REPEAT_MODE_OFF -> addAction(makeActionRepeatOffToOne())
            Player.REPEAT_MODE_ONE -> addAction(makeActionRepeatOneToAll())
            Player.REPEAT_MODE_ALL -> addAction(makeActionRepeatAllToOff())
        }
        havePrevButton = if (service.exo.hasPreviousMediaItem()) {
            addAction(makeActionPrev())
            true
        } else false

        if (service.exo.playWhenReady) { addAction(makeActionPause()) }
        else addAction( makeActionPlay() )

        haveNextButton = if (service.exo.hasNextMediaItem()) {
            addAction(makeActionNext())
            true
        } else false

        addAction(makeActionCancel())

        val media = MediaStyleNotificationHelper.MediaStyle(session)
            .setCancelButtonIntent(setCancelButtonIntent())
            .setShowCancelButton(true)

        when {
            haveNextButton && havePrevButton -> media.setShowActionsInCompactView(1,2,3)
            havePrevButton -> media.setShowActionsInCompactView(1,2)
            haveNextButton -> media.setShowActionsInCompactView(1,2)
            else -> media.setShowActionsInCompactView(1)
        }
        setStyle( media )

    }.build()

    private fun setCancelButtonIntent() = PendingIntent.getBroadcast(
        service, ACTION_CANCEL_CODE, Intent(PLAYBACK_INTENT).apply {
            putExtra(ACTION, ACTION_CANCEL)
            setPackage(context.packageName)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun makeActionCancel() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_close, "ACTION_CANCEL", PendingIntent.getBroadcast(
            service, ACTION_CANCEL_CODE, Intent(PLAYBACK_INTENT).apply {
                putExtra(ACTION, ACTION_CANCEL)
                setPackage(context.packageName)
            },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
        R.drawable.ic_notif_play, "PLAY", PendingIntent.getBroadcast(
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

    private fun makeActionPrev() = NotificationCompat.Action.Builder(
        R.drawable.ic_notif_prev, "PREV", PendingIntent.getBroadcast(
            service, ACTION_PREV_CODE, Intent(PLAYBACK_INTENT).apply {
                putExtra(ACTION, ACTION_PREV)
                setPackage(context.packageName)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    ).build()

    fun make( id: Int = NOTIFICATION_ID, session: MediaSession ) = makeNotif( id, session )
    fun cancel(id: Int = NOTIFICATION_ID) = manager.cancel(id)

    fun notify(id: Int = NOTIFICATION_ID, session: MediaSession) {
        manager.notify(id, makeNotif(id, session))
    }
    fun get(id: Int, session: MediaSession): MediaSessionService.MediaNotification {
       return MediaSessionService.MediaNotification(id, makeNotif(id,session))
    }

}