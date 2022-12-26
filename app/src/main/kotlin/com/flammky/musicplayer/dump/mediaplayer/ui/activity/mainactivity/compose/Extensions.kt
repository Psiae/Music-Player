package com.flammky.musicplayer.dump.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_ANY
import androidx.lifecycle.LifecycleEventObserver
import timber.log.Timber

object ComposableExtension {
    val noPadding = PaddingValues(0.dp)
}

object LifeCycleExtension {

    @Composable
    internal fun Lifecycle.eventAsState(onEvent: Lifecycle.Event = ON_ANY): State<Lifecycle.Event> {
        val event = remember { mutableStateOf(onEvent, policy = neverEqualPolicy()) }

        DisposableEffect(key1 = this) {
            val observer = LifecycleEventObserver { _, newEvent ->
                if (onEvent == newEvent) {
                    Timber.d("RecomposeOnEvent, recomposing $onEvent")
                    event.value = newEvent
                }
            }
            addObserver(observer)
            onDispose { removeObserver(observer) }
        }
        return event
    }

    @Composable
    internal fun Lifecycle.RepeatOnEvent(
        onEvent: Lifecycle.Event,
        block: @Composable (State<Lifecycle.Event>) -> Unit
    ) = block(eventAsState(onEvent))

    @Composable
    internal fun Lifecycle.RecomposeOnEvent(
        onEvent: Lifecycle.Event,
        block: @Composable (Lifecycle.Event) -> Unit
    ) = RepeatOnEvent(onEvent) { block(it.value) }

}
