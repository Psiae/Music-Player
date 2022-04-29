package com.kylentt.mediaplayer.ui.activity.mainactivity.compose

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import timber.log.Timber
import java.lang.IllegalStateException

object LifeCycleExtension {

    @Composable
    internal inline fun Lifecycle.RecomposeOnEvent(
        onEvent: Lifecycle.Event,
        content: @Composable () -> Unit
    ) {
        val state = remember { mutableStateOf(onEvent, policy = neverEqualPolicy()) }
        check(state.value == onEvent)

        DisposableEffect(key1 = this) {
            val observer = LifecycleEventObserver { _, event ->
                if (onEvent == event) {
                    Timber.d("RecomposeOnEvent, recomposing $onEvent")
                    state.value = event
                }
            }
            addObserver(observer)
            onDispose { removeObserver(observer) }
        }
        content()
    }
}
