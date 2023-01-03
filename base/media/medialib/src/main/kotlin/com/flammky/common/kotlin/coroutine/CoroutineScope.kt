package com.flammky.common.kotlin.coroutine

import kotlinx.coroutines.CoroutineScope

inline fun CoroutineScope.ensureNotCancelled(onCancel: () -> Unit) {
	return coroutineContext.ensureNotCancelled(onCancel)
}
