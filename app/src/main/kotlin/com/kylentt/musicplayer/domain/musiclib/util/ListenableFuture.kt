package com.kylentt.musicplayer.domain.musiclib.util

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

fun <T: Any, F: ListenableFuture<T>> F.addListener(executor: Executor, runnable: (T) -> Unit) {
	addListener({ runnable(get()) }, executor)
}
