package com.flammky.musicplayer.player.presentation.queue

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
internal class QueueViewModel @Inject constructor(
    private val presenter: QueuePresenter,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    init {
        presenter.init(
            viewModel = object : QueuePresenter.ViewModel {
                override val stateHandle: SavedStateHandle = savedStateHandle
                override val coroutineSupervisor: Job = SupervisorJob()
            }
        )
    }

    override fun onCleared() {
        presenter.dispose()
    }

    fun observeUser(): Flow<User?> {
        return presenter.auth.observeUser()
    }

    fun createPlaybackController(user: User): PlaybackController {
        return presenter.playback.createController(user)
    }

    fun observeArtwork(id: String): Flow<Any?> {
        return presenter.repo.observeArtwork(id)
    }

    fun observeMetadata(id: String): Flow<MediaMetadata?> {
        return presenter.repo.observeMetadata(id)
    }
}