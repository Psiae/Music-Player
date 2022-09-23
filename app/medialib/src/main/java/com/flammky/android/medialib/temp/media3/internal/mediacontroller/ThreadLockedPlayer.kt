package com.flammky.android.medialib.temp.media3.internal.mediacontroller

import android.os.Handler
import android.os.Looper
import com.flammky.android.medialib.concurrent.PublicThreadLocked
import com.flammky.android.medialib.temp.player.LibraryPlayer
import java.util.concurrent.locks.LockSupport

interface ThreadLockedPlayer : LibraryPlayer, PublicThreadLocked {

	val publicLooper: Looper
	val publicHandler: Handler

	override fun post(block: () -> Unit) {
		publicHandler.post { block() }
	}

	override fun <R> postListen(block: () -> R, listener: (R) -> Unit) {
		publicHandler.post { listener(block()) }
	}

	override fun <R> joinBlocking(block: () -> R): R {
		return if (Looper.myLooper() == publicLooper) {
			block()
		} else {
			val hold = Any()
			val thread = Thread.currentThread()
			var result: Any? = hold

			postListen(block) {
				result = it
				LockSupport.unpark(thread)
			}

			while (result === hold) LockSupport.park()

			return result as R
			/*runBlocking(publicHandler.asCoroutineDispatcher()) { block() }*/
		}
	}
}
