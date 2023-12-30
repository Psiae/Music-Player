package dev.dexsr.klio.base.compose

import androidx.compose.ui.Modifier
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.text.*

/**
* with Modifier interface as object receiver,
* does call to Modifier.modifierFactoryLambda() actually received by the Modifier.Companion ?
* */
val EmptyModifier: Modifier
    @Suppress("ModifierFactoryExtensionFunction")
    get() = Modifier.Companion

@OptIn(ExperimentalContracts::class)
inline fun Modifier.thenIf(condition: Boolean, other: Modifier.() -> Modifier) = combineIf(condition, other)

@OptIn(ExperimentalContracts::class)
inline fun Modifier.combineIf(
    condition: Boolean,
    other: Modifier.() -> Modifier
): Modifier {
    contract {
        callsInPlace(other, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) this.then(EmptyModifier.other()) else this
}

inline fun Modifier.combineIf(
    condition: Boolean,
    other: Modifier
): Modifier = if (condition) this.then(other) else this

@OptIn(ExperimentalContracts::class)
inline fun <T: Any> Modifier.combineIfNotNull(
    value: T?,
    factory: Modifier.(T) -> Modifier
): Modifier {
    contract {
        callsInPlace(factory, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }
    return if (value != null) this.then(EmptyModifier.factory(value)) else this
}

@OptIn(ExperimentalContracts::class)
inline fun Modifier.combineIfNotNull(
    other: Modifier?
): Modifier {
    return if (other != null) this.then(other) else this
}
