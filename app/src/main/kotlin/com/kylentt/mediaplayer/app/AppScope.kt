package com.kylentt.mediaplayer.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

data class AppScope(
  val computationScope: CoroutineScope,
  val ioScope: CoroutineScope,
  val mainScope: CoroutineScope,
  val immediateScope: CoroutineScope
) {

  companion object {

    fun fromAppDispatchers(dispatchers: AppDispatchers = AppDispatchers.Default) =
      with(dispatchers) {
        AppScope(
          computationScope = CoroutineScope(computation + SupervisorJob()),
          ioScope = CoroutineScope(io + SupervisorJob()),
          mainScope = CoroutineScope(main + SupervisorJob()),
          immediateScope = CoroutineScope(mainImmediate + SupervisorJob())
        )
      }
  }
}

data class AppDispatchers(
  val computation: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val main: CoroutineDispatcher,
  val mainImmediate: CoroutineDispatcher
) {

  companion object {
    val Default = with(kotlinx.coroutines.Dispatchers) {
      AppDispatchers(
        computation = Default,
        io = IO,
        main = Main,
        mainImmediate = Main.immediate
      )
    }
  }
}
