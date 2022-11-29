package com.flammky.android.kotlin.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

data class AndroidCoroutineDispatchers(
	val computation: CoroutineDispatcher,
	val io: CoroutineDispatcher,
	val main: MainCoroutineDispatcher,
	val mainImmediate: MainCoroutineDispatcher,
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
