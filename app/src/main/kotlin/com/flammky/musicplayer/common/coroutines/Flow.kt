package com.flammky.musicplayer.common.coroutines

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.coroutineContext

suspend inline fun <T> Flow<T>.safeCollect(crossinline block: suspend (T) -> Unit) {
	collect {
		coroutineContext.ensureActive()
		block(it)
	}
}
