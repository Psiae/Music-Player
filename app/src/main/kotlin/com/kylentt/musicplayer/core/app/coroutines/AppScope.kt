package com.kylentt.musicplayer.core.app.coroutines

import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Singleton
data class AppScope(
  val computationScope: CoroutineScope,
  val ioScope: CoroutineScope,
  val mainScope: CoroutineScope,
  val immediateScope: CoroutineScope,
  val unconfinedScope: CoroutineScope
)
