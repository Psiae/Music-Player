package com.kylentt.disposed.musicplayer.app.util

import kotlinx.coroutines.CoroutineScope

data class AppScope(
  val defaultScope: CoroutineScope,
  val ioScope: CoroutineScope,
  val mainScope: CoroutineScope,
)
