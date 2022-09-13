package com.flammky.common.kotlin.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

inline fun CoroutineContext.checkCancellation(ifCancelled: () -> Unit) {
	if (!isActive) {
		ifCancelled()
		ensureActive()
	}
}

inline fun CoroutineScope.checkCancellation(ifCancelled: () -> Unit) {
	return coroutineContext.checkCancellation(ifCancelled)
}
