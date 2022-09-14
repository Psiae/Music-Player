package com.flammky.musicplayer.common.coroutines

import com.flammky.android.common.kotlin.coroutines.AndroidCoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import javax.annotation.concurrent.Immutable
import javax.inject.Singleton

@Singleton
@Immutable
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
