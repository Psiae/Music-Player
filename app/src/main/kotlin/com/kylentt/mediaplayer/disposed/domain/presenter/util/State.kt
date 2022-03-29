package com.kylentt.mediaplayer.disposed.domain.presenter.util

import androidx.media3.common.MediaItem

// TODO: Separate this
sealed class State {

    sealed class ServiceState : State() {
        object Unit : ServiceState()
        object Connecting : ServiceState()
        object Connected : ServiceState()
        object Disconnected : ServiceState()
        data class Error(val Error: Exception) : ServiceState()
    }

    // TODO : Wrap the Player.State provided by Exoplayer to more convenience one
    sealed class PlayerState : State() {

        sealed class PlayerPlaybackState : PlayerState() {
            object Unit : PlayerPlaybackState()
            object Idle : PlayerPlaybackState()
            object Buffering : PlayerPlaybackState()
            object Ready : PlayerPlaybackState()
            object Ended : PlayerPlaybackState()

            // When Error occurred
            data class Error(
                val Error: Exception,
                val Desc: String,
                val PlaybackErrorDesc : String? = null
            ) : PlayerPlaybackState()
        }

        sealed class PlayerPlayState : PlayerState() {
            object PlayWhenReadyEnabled : PlayerPlayState()
            object PlayWhenReadyDisabled : PlayerPlayState()
        }

        sealed class PlayerItemState : PlayerState() {
            data class CurrentlyPlaying(
                val CurrentIndex: Int,
                val CurrentMediaItem: MediaItem,
                val CurrentPlaylist: List<MediaItem>
            ) : PlayerItemState()
        }
    }
}




