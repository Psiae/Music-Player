package com.kylentt.mediaplayer.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Application Coroutine Dispatchers
 *
 * ~ copied from Tivi Google Sample
 *
 * @param [computation] Computation Task
 * @param [io] IO Task
 * @param [main] Main Thread Task
 * @param [mainImmediate] Main Thread Immediate Task
 * @param [unconfined] Other Task
 * @see [AppScope]
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
data class CoroutineDispatchers(
  val computation: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val main: CoroutineDispatcher,
  val mainImmediate: CoroutineDispatcher,
  val unconfined: CoroutineDispatcher
) {

  companion object {

    /** Default Dispatchers from [kotlinx.coroutines.Dispatchers] */

    @JvmStatic
    val DEFAULT = with(kotlinx.coroutines.Dispatchers) {
      CoroutineDispatchers(
        computation = Default, io = IO,
        main = Main, mainImmediate = Main.immediate,
        unconfined = Unconfined
      )
    }
  }

}
