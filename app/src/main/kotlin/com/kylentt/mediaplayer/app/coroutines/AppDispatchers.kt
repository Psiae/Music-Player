package com.kylentt.mediaplayer.app.coroutines

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
data class AppDispatchers(
  @JvmField val computation: CoroutineDispatcher,
  @JvmField val io: CoroutineDispatcher,
  @JvmField val main: CoroutineDispatcher,
  @JvmField val mainImmediate: CoroutineDispatcher,
  @JvmField val unconfined: CoroutineDispatcher
) {

  companion object {

    /** Default Dispatchers from [kotlinx.coroutines.Dispatchers] */

    @JvmStatic
    val DEFAULT = with(kotlinx.coroutines.Dispatchers) {
      AppDispatchers(
        computation = Default, io = IO,
        main = Main, mainImmediate = Main.immediate,
        unconfined = Unconfined
      )
    }

  }

}
