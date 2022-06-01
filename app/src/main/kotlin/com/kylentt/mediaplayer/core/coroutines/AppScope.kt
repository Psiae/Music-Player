package com.kylentt.mediaplayer.core.coroutines

import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Application Coroutine Scope
 *
 * ~ copied from Tivi Google Sample
 *
 * @param [computationScope] Computation Task
 * @param [ioScope] IO Task
 * @param [mainScope] Main Thread Task
 * @param [immediateScope] Main Thread Immediate Task
 * @param [unconfinedScope] Other Task
 * @see [AppDispatchers]
 * @author Kylentt
 * @since 2022/04/30
 */

@Singleton
data class AppScope(
  val computationScope: CoroutineScope,
  val ioScope: CoroutineScope,
  val mainScope: CoroutineScope,
  val immediateScope: CoroutineScope,
  val unconfinedScope: CoroutineScope
)
