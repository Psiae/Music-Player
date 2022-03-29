package com.kylentt.mediaplayer.core.exoplayer

import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.MediaNotification
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicService
import com.kylentt.mediaplayer.domain.mediaSession.service.MusicServiceConstants.NOTIFICATION_ID
import timber.log.Timber

class NotifProvider(
    private val notif: (MediaController) -> MediaNotification
) : MediaNotification.Provider {

    // Handle Notification myself
    override fun createNotification(
        mediaController: MediaController,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        Timber.d("NotificationProvider createNotification")
        return notif(mediaController)
    }

    // This method wasn't called for some reason
    override fun handleCustomAction(
        mediaController: MediaController,
        action: String,
        extras: Bundle,
    ) = Unit
}