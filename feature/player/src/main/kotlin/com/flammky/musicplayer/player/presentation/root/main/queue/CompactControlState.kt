package com.flammky.musicplayer.player.presentation.root.main.queue

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import kotlinx.coroutines.flow.Flow

data class CompactControlDataSource(
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observeProperties: () -> Flow<PlaybackProperties>,
    val observeArtwork: (String) -> Flow<Any?>,
    val observeMetadata: (String) -> Flow<MediaMetadata?>
)

data class CompactControlIntents(
    val seekNext: () -> Unit,
    val seekPrevious: () -> Unit,
    val play: () -> Unit,
    val pause: () -> Unit
)

@Composable
internal fun rememberCompactControlState(
    intents: CompactControlIntents,
    dataSource: CompactControlDataSource,
): CompactControlState {
    return rememberSaveable(
        intents, dataSource,
        saver = CompactControlState.Saver(intents, dataSource)
    ) {
        CompactControlState(dataSource, intents)
    }
}

internal class CompactControlState(
    val dataSource: CompactControlDataSource,
    val intents: CompactControlIntents
) {

    var stagedLayoutHeight by mutableStateOf(0.dp)

    companion object {
        fun Saver(
            intents: CompactControlIntents,
            dataSource: CompactControlDataSource,
        ): Saver<CompactControlState, Bundle> {

            return Saver(
                save = {
                    Bundle()
                        .apply {

                        }
                },
                restore = {
                    CompactControlState(dataSource, intents)
                }
            )
        }
    }
}