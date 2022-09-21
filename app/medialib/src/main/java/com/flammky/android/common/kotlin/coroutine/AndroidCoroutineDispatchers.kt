package com.flammky.android.common.kotlin.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class AndroidCoroutineDispatchers(
	val computation: CoroutineDispatcher,
	val io: CoroutineDispatcher,
	val main: CoroutineDispatcher,
	val mainImmediate: CoroutineDispatcher,
	val unconfined: CoroutineDispatcher
) {

	companion object {
		val DEFAULT = AndroidCoroutineDispatchers(
			computation = Dispatchers.Default,
			io = Dispatchers.IO,
			main = Dispatchers.Main,
			mainImmediate = Dispatchers.Main.immediate,
			unconfined = Dispatchers.Unconfined
		)
	}
}
