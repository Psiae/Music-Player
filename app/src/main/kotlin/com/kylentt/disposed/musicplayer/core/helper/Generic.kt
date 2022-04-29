@file:OptIn(ExperimentalContracts::class)

package com.kylentt.disposed.musicplayer.core.helper

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T> elseNull(condition: Boolean, block: () -> T?): T? {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return if (condition) {
    block()
  } else {
    null
  }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> orNull(condition: Boolean?, block: () -> T?): T? {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return if (condition != false) {
    block()
  } else {
    null
  }
}

@OptIn(ExperimentalContracts::class)
inline fun elseFalse(condition: Boolean, block: () -> Boolean): Boolean {
  contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
  }
  return if (condition) {
    block()
  } else {
    false
  }
}
