@file:Suppress("NOTHING_TO_INLINE")

package dev.flammky.compose_components.core

inline fun notImplementedError(): Nothing = throw NotImplementedError()

inline fun exhaustedStateException(): Nothing = error(
    """
        Condition should've been exhausted, please fill a bug report with the stacktrace
    """
)