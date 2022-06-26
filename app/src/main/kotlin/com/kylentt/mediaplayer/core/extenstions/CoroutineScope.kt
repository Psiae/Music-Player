package com.kylentt.mediaplayer.core.extenstions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive

inline fun CoroutineScope.checkCancellation(ifCancelled: () -> Unit) {
	if (!isActive) {
		ifCancelled()
		ensureActive()
	}
}
