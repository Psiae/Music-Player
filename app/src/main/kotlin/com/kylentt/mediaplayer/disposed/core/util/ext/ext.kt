package com.kylentt.mediaplayer.disposed.core.util.ext

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
object Ext {

  inline fun <T> measureTimeMillisWithResult(block: () -> T): Pair<T, Long> {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    val r: T = block()
    val end = System.currentTimeMillis() - start
    return Pair(r, end)
  }

  inline fun <T> executeWithTimeMillis(log: (Long) -> Unit, block: () -> T): T {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    val r: T = block()
    log(System.currentTimeMillis() - start)
    return r
  }

  inline fun <T> measureTimeMillisWithResult(log: (Long) -> Unit, block: () -> T): T {
    contract {
      callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    val r: T = block()
    log(System.currentTimeMillis() - start)
    return r
  }
}
