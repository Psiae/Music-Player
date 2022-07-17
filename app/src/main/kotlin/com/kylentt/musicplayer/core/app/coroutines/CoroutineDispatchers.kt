package com.kylentt.musicplayer.core.app.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Singleton
data class CoroutineDispatchers(
  val computation: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val main: CoroutineDispatcher,
  val mainImmediate: CoroutineDispatcher,
  val unconfined: CoroutineDispatcher
) {

  companion object {
    val DEFAULT = with(kotlinx.coroutines.Dispatchers) {
      CoroutineDispatchers(
        computation = Default, io = IO,
        main = Main, mainImmediate = Main.immediate,
        unconfined = Unconfined
      )
    }
  }

}
