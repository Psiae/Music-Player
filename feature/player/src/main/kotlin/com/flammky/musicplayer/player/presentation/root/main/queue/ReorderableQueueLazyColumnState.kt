package com.flammky.musicplayer.player.presentation.root.main.queue

import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

data class ReorderableQueueLazyColumnIntents(
    val requestMoveQueueItemAsync: (
        from: Int,
        fromId: String,
        to: Int,
        toId: String
    ) -> Deferred<Result<Boolean>>,
    val requestSeekIndexAsync: (
        from: Int,
        fromId: String,
        to: Int,
        toId: String
    ) -> Deferred<Result<Boolean>>
)

data class ReorderableQueueLazyColumnDataSource(
    val observeQueue: () -> Flow<OldPlaybackQueue>,
    val observeTrackMetadata: (String) -> Flow<MediaMetadata?>,
    val observeTrackArtwork: (String) -> Flow<Any?>
)

@Composable
fun rememberReorderableQueueLazyColumnState(
    intents: ReorderableQueueLazyColumnIntents,
    dataSource: ReorderableQueueLazyColumnDataSource
): ReorderableQueueLazyColumnState {
    return remember(
        intents,
        dataSource
    ) {
        ReorderableQueueLazyColumnState(intents, dataSource)
    }
}

class ReorderableQueueLazyColumnState(
    val intents: ReorderableQueueLazyColumnIntents,
    val dataSource: ReorderableQueueLazyColumnDataSource
) {

    var topContentPadding by mutableStateOf<Dp>(0.dp)
    var bottomContentPadding by mutableStateOf<Dp>(0.dp)
}