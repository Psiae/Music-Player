package com.flammky.android.medialib.temp.media3.internal.mediacontroller

import android.os.Handler
import android.os.Looper
import com.flammky.android.medialib.concurrent.PublicThreadLocked
import com.flammky.android.medialib.temp.player.LibraryPlayer
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.locks.LockSupport
import kotlin.coroutines.EmptyCoroutineContext

interface ThreadLockedPlayer<P: LibraryPlayer> : LibraryPlayer, PublicThreadLocked<P> {
	val publicLooper: Looper
}

abstract class BaseThreadLockedPlayer<P: LibraryPlayer> : ThreadLockedPlayer<P> {
	protected val looperHandler = Handler(publicLooper)
	protected val looperDispatcher = looperHandler.asCoroutineDispatcher()

	abstract protected val basePlayer: P

	override fun post(block: P.() -> Unit) {
		looperHandler.post { basePlayer.block() }
	}

	override fun <R> postListen(block: P.() -> R, listener: (R) -> Unit) {
		looperHandler.post { listener(basePlayer.block()) }
	}

	override fun <R> joinBlocking(block: P.() -> R): R {
		return if (Looper.myLooper() == publicLooper) {
			basePlayer.block()
		} else {
			val hold = Any()
			val thread = Thread.currentThread()
			var result: Any? = hold

			postListen(block) {
				result = it
				LockSupport.unpark(thread)
			}

			while (result === hold) LockSupport.park(this)

			result as R
		}
	}

	override fun <R> joinBlockingSuspend(block: suspend P.() -> R): R {
		val context =
			if (Looper.myLooper() == publicLooper) {
				EmptyCoroutineContext
			} else {
				looperDispatcher.immediate
			}
		return runBlocking(context) { basePlayer.block() }
	}

	override suspend fun <R> joinSuspend(block: suspend P.() -> R): R {
		return withContext(looperDispatcher.immediate) { basePlayer.block() }
	}
}
