package com.flammky.common.kotlin.coroutines

import com.flammky.common.kotlin.coroutine.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.coroutineContext

suspend fun <T> Flow<T>.safeCollect(
	fallback: () -> Unit = {},
	collect: suspend (T) -> Unit
) {
	collect {
		coroutineContext.ensureActive(fallback)
		collect(it)
	}
}
