package com.kylentt.mediaplayer.app.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Better than hardcoding the dispatcher Ig
 * */

data class AppScope(
  val computationScope: CoroutineScope,
  val ioScope: CoroutineScope,
  val mainScope: CoroutineScope,
  val immediateScope: CoroutineScope
)

data class AppDispatchers(
  val computation: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val main: CoroutineDispatcher,
  val mainImmediate: CoroutineDispatcher
) {
  companion object {
    val Default = with(kotlinx.coroutines.Dispatchers) {
      AppDispatchers(computation = Default, io = IO, main = Main, mainImmediate = Main.immediate)
    }
  }
}
