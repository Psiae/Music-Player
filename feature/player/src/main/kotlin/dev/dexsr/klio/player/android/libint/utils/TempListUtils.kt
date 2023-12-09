package dev.dexsr.klio.player.android.libint.utils

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// TODO: remove these when we can add new APIs to ui-util outside of beta cycle

/**
 * Returns a list containing only elements matching the given [predicate].
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastFilter(predicate: (T) -> Boolean): List<T> {
    contract { callsInPlace(predicate) }
    val target = ArrayList<T>(size)
    fastForEach {
        if (predicate(it)) target += (it)
    }
    return target
}

/**
 * Accumulates value starting with [initial] value and applying [operation] from left to right
 * to current accumulator value and each element.
 *
 * Returns the specified [initial] value if the collection is empty.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 *
 * @param [operation] function that takes current accumulator value and an element, and calculates the next accumulator value.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R> List<T>.fastFold(initial: R, operation: (acc: R, T) -> R): R {
    contract { callsInPlace(operation) }
    var accumulator = initial
    fastForEach { e ->
        accumulator = operation(accumulator, e)
    }
    return accumulator
}

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each element in the original collection.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@OptIn(ExperimentalContracts::class)
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
internal inline fun <T, R> List<T>.fastMapIndexedNotNull(
    transform: (index: Int, T) -> R?
): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEachIndexed { index, e ->
        transform(index, e)?.let { target += it }
    }
    return target
}

/**
 * Returns the largest value among all values produced by selector function applied to each element
 * in the collection or null if there are no elements.
 *
 * **Do not use for collections that come from public APIs**, since they may not support random
 * access in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access.
 */
@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R : Comparable<R>> List<T>.fastMaxOfOrNull(selector: (T) -> R): R? {
    contract { callsInPlace(selector) }
    if (isEmpty()) return null
    var maxValue = selector(get(0))
    for (i in 1..lastIndex) {
        val v = selector(get(i))
        if (v > maxValue) maxValue = v
    }
    return maxValue
}
