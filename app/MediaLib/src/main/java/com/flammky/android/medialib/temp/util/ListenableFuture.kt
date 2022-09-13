package com.flammky.android.medialib.temp.util

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

fun <T, F : ListenableFuture<T>> F.addListener(
	executor: Executor,
	action: (ListenableFuture<T>) -> Unit
) {
	addListener({ action(this) }, executor)
}

fun <T, F : ListenableFuture<T>> F.addResultListener(
	executor: Executor,
	action: (T) -> Unit
) {
	addListener(executor) { action(get()) }
}
