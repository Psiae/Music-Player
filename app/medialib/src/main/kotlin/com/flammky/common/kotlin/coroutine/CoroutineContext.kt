package com.flammky.common.kotlin.coroutine

import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext

inline fun CoroutineContext.ensureNotCancelled(onCancel: () -> Unit) {
	get(Job)?.apply {
		if (isCancelled) {
			onCancel()
			ensureActive()
		}
	}
}

inline fun CoroutineContext.ensureActive(onCancel: () -> Unit) {
	get(Job)?.apply {
		if (!isActive) {
			onCancel()
			ensureActive()
		}
	}
}
