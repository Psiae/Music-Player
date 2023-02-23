package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.*

internal class SliderLayoutComposition() {

}

internal class RootPlaybackControlSliderState(
    private val composition: RootPlaybackControlMainScope
) {

    var layoutComposition by mutableStateOf<SliderLayoutComposition?>(null)

    fun incrementQueueReader() {
        composition.currentQueueReaderCount++
    }

    fun decrementQueueReader() {
        composition.currentQueueReaderCount--
    }
}

internal class RootPlaybackControlSliderApplier(
    private val state: RootPlaybackControlSliderState
) {

    fun prepareState() {
        state.layoutComposition = null
    }

    @Composable
    fun PrepareCompose() {
        DisposableEffect(
            key1 = this,
            effect = {
                state.incrementQueueReader()
                onDispose { state.decrementQueueReader() }
            }
        )
    }
}

internal class RootPlaybackControlSliderComposition()

@Composable
internal fun RootPlaybackControlSlider(
    state: RootPlaybackControlSliderState
) {
    val applier = remember(state) {
        RootPlaybackControlSliderApplier(state)
            .apply {
                prepareState()
            }
    }.apply {
        PrepareCompose()
    }


}