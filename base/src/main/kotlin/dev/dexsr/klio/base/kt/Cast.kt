package dev.dexsr.klio.base.kt

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <reified R> Any?.cast(): R {
    contract {
        returns() implies (this@cast is R)
    }
    return this as R
}

@OptIn(ExperimentalContracts::class)
inline fun <reified R> Any?.castOrNull(): R? {
    contract {
        returns() implies (this@castOrNull is R?)
    }
    return this as? R
}
