package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

@Composable
internal fun RootPlaybackControlQueueScope.Compose() {
    Layout(
        queueControl = { QueueControl(composition = it) },
        scrollableQueue = { LazyReorderableQueue(composition = it) }
    )
}

@Composable
internal fun RootPlaybackControlQueueScope.Layout(
    queueControl: @Composable BoxScope.(composition: RootPlaybackControlQueueScope) -> Unit,
    scrollableQueue: @Composable BoxScope.(composition: RootPlaybackControlQueueScope) -> Unit
) {

}

@Composable
internal fun BoxScope.QueueControl(composition: RootPlaybackControlQueueScope) {

}

@Composable
internal fun BoxScope.LazyReorderableQueue(composition: RootPlaybackControlQueueScope) {

}