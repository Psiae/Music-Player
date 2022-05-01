package com.kylentt.mediaplayer.app.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

/**
 * Better than hardcoding the dispatcher Ig
 * @constructor [computation] Dispatcher for Computation Task
 * @constructor [io] Dispatcher for IO_Operation Task
 * @constructor [main] Dispatcher for Main_Thread Task
 * @constructor [mainImmediate] Dispatcher for Main_Thread Immediate Task
 * @constructor [unconfined] Dispatcher for other than above Task
 * @property [AppDispatchers.DEFAULT] the Default Implementation
 * @see [AppScope]
 * @author Kylentt, copied from Tivi Google Sample
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
