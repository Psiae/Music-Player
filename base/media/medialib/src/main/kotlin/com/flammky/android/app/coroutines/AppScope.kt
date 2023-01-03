package com.flammky.android.app.coroutines

import kotlinx.coroutines.CoroutineScope

data class AppScope(
	val computationScope: CoroutineScope,
	val ioScope: CoroutineScope,
	val mainScope: CoroutineScope,
	val immediateScope: CoroutineScope,
	val unconfinedScope: CoroutineScope
)
