package com.flammky.musicplayer.player.presentation.queue

import androidx.annotation.MainThread
import androidx.lifecycle.SavedStateHandle
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

internal interface QueuePresenter {

    @MainThread
    fun init(viewModel: ViewModel)

    @MainThread
    fun dispose()

    val disposed: Boolean
        @MainThread
        get

    val auth: Auth

    val playback: Playback

    val repo: Repo

    interface Auth {
        @MainThread
        fun observeUser(): Flow<User?>
    }

    interface Playback {
        @MainThread
        fun createController(user: User): PlaybackController
    }

    interface Repo {
        @MainThread
        fun observeArtwork(id: String): Flow<Any?>

        @MainThread
        fun observeMetadata(id: String): Flow<MediaMetadata?>
    }

    interface ViewModel {
        val stateHandle: SavedStateHandle
        val coroutineSupervisor: Job
    }
}