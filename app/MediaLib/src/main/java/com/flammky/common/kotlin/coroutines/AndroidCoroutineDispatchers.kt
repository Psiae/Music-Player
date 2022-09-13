package com.flammky.common.kotlin.coroutines

import kotlinx.coroutines.CoroutineDispatcher

data class AndroidCoroutineDispatchers(
	val computation: CoroutineDispatcher,
	val io: CoroutineDispatcher,
	val main: CoroutineDispatcher,
	val mainImmediate: CoroutineDispatcher,
	val unconfined: CoroutineDispatcher
) {

	companion object {
		val DEFAULT = with(kotlinx.coroutines.Dispatchers) {
			AndroidCoroutineDispatchers(
				computation = Default, io = IO,
				main = Main, mainImmediate = Main.immediate,
				unconfined = Unconfined
			)
		}
	}
}
