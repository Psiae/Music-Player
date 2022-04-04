package com.kylentt.musicplayer.domain.mediasession

import android.app.Application
import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.session.MediaLibraryService
import com.kylentt.musicplayer.app.util.AppScope
import com.kylentt.musicplayer.domain.mediasession.service.ControllerCommand
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceConnector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


// Current Media Playback Single Info Source
internal class MediaSessionManager private constructor(
    private val base: Context,
    private val appScope: AppScope
) {

    private var mediaServiceConnector: MediaServiceConnector
    private val controller get() = mediaServiceConnector.mediaServiceController


    private fun MediaLibraryService.getServiceController() = controller
    fun serviceController(service: MediaLibraryService) = service.getServiceController()

    val bitmapState
        get() = mediaServiceConnector.playerBitmap
    val itemState
        get() = mediaServiceConnector.playerMediaItem
    val playbackState
        get() = mediaServiceConnector.playerState
    val serviceState
        get() = mediaServiceConnector.serviceState

    init {
        check(base is Application) { "MediaSessionManager Invalid Context" }
        mediaServiceConnector = MediaServiceConnector(this, base, appScope)
    }

    @MainThread
    fun connectService() { controller.commandController(ControllerCommand.Unit) }

    @MainThread
    fun sendCommand(command: ControllerCommand) { controller.commandController(command) }

    companion object {
        fun build(context: Context, scope: AppScope) = MediaSessionManager(context, scope)
    }
}