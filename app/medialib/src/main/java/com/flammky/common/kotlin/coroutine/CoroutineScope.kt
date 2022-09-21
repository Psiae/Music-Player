package com.flammky.common.kotlin.coroutine

import kotlinx.coroutines.CoroutineScope

inline fun CoroutineScope.ensureCancellation(onCancel: () -> Unit) {
	return coroutineContext.ensureCancellation(onCancel)
}