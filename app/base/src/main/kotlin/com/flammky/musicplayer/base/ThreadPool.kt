package com.flammky.musicplayer.base

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.Executor

/**
 * ThreadPool for non blocking operations
 */
object NonBlockingThreadPool {
	fun getExecutor(): Executor = Dispatchers.Default.asExecutor()
}
