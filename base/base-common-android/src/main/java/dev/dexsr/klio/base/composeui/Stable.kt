package dev.dexsr.klio.base.composeui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable


typealias ComposeUiStableAny<T> = StableAny<T>
typealias ComposeUiImmutableAny<T> = ImmutableAny<T>

// should T: Any ?

// remove this ?
@Stable
data class StableAny<T>(val actual: T)

@Immutable
data class ImmutableAny<T>(val actual: T)

fun <T> T.wrapComposeUiStable() = ComposeUiStableAny(this)

fun <T> T.wrapComposeUiImmutable() = ImmutableAny(this)
