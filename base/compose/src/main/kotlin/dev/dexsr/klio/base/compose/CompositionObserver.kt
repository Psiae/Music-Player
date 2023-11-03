package dev.dexsr.klio.base.compose

import androidx.compose.runtime.*

@Composable
fun CompositionObserver(
    key: Any? = Unit,
    onRemembered: () -> Unit,
    onForgotten: () -> Unit,
    onAbandoned: () -> Unit,
) {
    val upOnRemembered = rememberUpdatedState(newValue = onRemembered)
    val upOnForgotten = rememberUpdatedState(newValue = onForgotten)
    val upOnAbandoned = rememberUpdatedState(newValue = onAbandoned)
    remember(key) {
        object : RememberObserver {

            override fun onAbandoned() {
                upOnAbandoned.value.invoke()
            }

            override fun onForgotten() {
                upOnForgotten.value.invoke()
            }

            override fun onRemembered() {
                upOnRemembered.value.invoke()
            }
        }
    }
}

@Composable
inline fun <T> rememberWithCompositionObserver(
    key: Any?,
    noinline onRemembered: (T) -> Unit,
    noinline onForgotten: (T) -> Unit,
    noinline onAbandoned: (T) -> Unit,
    crossinline block: @DisallowComposableCalls () -> T
): T {
    val upOnRemembered = rememberUpdatedState(newValue = onRemembered)
    val upOnForgotten = rememberUpdatedState(newValue = onForgotten)
    val upOnAbandoned = rememberUpdatedState(newValue = onAbandoned)
    return remember(key) {
        object : RememberObserver {
            val value = block()

            override fun onAbandoned() {
                upOnAbandoned.value.invoke(value)
            }

            override fun onForgotten() {
                upOnForgotten.value.invoke(value)
            }

            override fun onRemembered() {
                upOnRemembered.value.invoke(value)
            }
        }
    }.value
}
