package dev.dexsr.klio.library.compose

/**
 * override object stability to stable (mutable)
 *
 * [docs](https://developer.android.com/jetpack/compose/performance/stability#types)
 */
typealias ComposeStable = androidx.compose.runtime.Stable

/**
 * override object stability to stable (immutable)
 *
 * [docs](https://developer.android.com/jetpack/compose/performance/stability#types)
 */
typealias ComposeImmutable = androidx.compose.runtime.Immutable
