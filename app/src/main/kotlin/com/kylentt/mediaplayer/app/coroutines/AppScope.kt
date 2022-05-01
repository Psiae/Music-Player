package com.kylentt.mediaplayer.app.coroutines

import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Application Scoped CoroutineScope,
 * @constructor [computationScope] CoroutineScope for Computation Task
 * @constructor [ioScope] CoroutineScope for IO_Operation Task
 * @constructor [mainScope] CoroutineScope for Main_Thread Task
 * @constructor [immediateScope] CoroutineScope for Main_Thread Immediate Task
 * @constructor [unconfinedScope] CoroutineScope for other than above Task
 * @see [AppDispatchers]
 * @since 2022/04/30
 * @author Kylentt, copied from Tivi Google Sample
 */

@Singleton
data class AppScope(
  val computationScope: CoroutineScope,
  val ioScope: CoroutineScope,
  val mainScope: CoroutineScope,
  val immediateScope: CoroutineScope,
  val unconfinedScope: CoroutineScope
)


