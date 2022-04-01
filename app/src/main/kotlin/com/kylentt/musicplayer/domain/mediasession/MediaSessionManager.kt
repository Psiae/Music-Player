package com.kylentt.musicplayer.domain.mediasession

import android.content.Context
import com.kylentt.musicplayer.domain.mediasession.service.ControllerCommand
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceConnector

internal class MediaSessionManager (
    private val base: Context
) {

    private val mediaServiceConnector = MediaServiceConnector(this, base)
    private val controller = mediaServiceConnector.mediaServiceController
    val serviceState = mediaServiceConnector.serviceState
    val playbackState = mediaServiceConnector.playerState
    val itemState = mediaServiceConnector.playerMediaItem

    fun connectService() { controller.commandController(ControllerCommand.Unit) }
    fun sendCommand(command: ControllerCommand) { controller.commandController(command) }

}