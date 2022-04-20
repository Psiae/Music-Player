package com.kylentt.mediaplayer.disposed.core.exoplayer.disposed

/*
* @MainThread
class ExoController(
    exo: ExoPlayer,
    notificationManager: ExoNotificationManager_,
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

    private var activeManager: ExoNotificationManager_? = notificationManager
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

    fun reassign(exo: ExoPlayer, manager: ExoNotificationManager_, listener: Player.Listener?) {

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

        fun getInstance(exo: ExoPlayer, notificationManager: ExoNotificationManager_, listener: Player.Listener?): ExoController {
            if (instance == null) {
                instance = ExoController(exo, notificationManager, listener)
            } else {
                instance!!.reassign(exo, notificationManager, listener)
            }
            return instance!!
        }
    }
}
 */
