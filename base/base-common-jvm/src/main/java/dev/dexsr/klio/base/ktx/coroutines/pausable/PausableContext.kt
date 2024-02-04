package dev.dexsr.klio.base.ktx.coroutines.pausable

import dev.dexsr.klio.base.kt.castOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.AbstractCoroutineContextKey
import kotlin.coroutines.CoroutineContext

class PausableContext(
	private val parent: PausableContext?,
	private val pausableDispatcher: PausableCoroutineDispatcher?,
) : AbstractCoroutineContextElement(PausableHandle), PausableHandle {

	override val isPaused: Boolean
		get() = pausableDispatcher?.isPaused ?: parent?.isPaused ?: false

	override fun pause(): Boolean {
		return pausableDispatcher?.pause() ?: parent?.pause() ?: false
	}

	override fun resume(): Boolean {
		return pausableDispatcher?.resume() ?: parent?.resume() ?: false
	}

	internal suspend fun checkPausable() {
		suspendCancellableCoroutine<Unit> { uCont ->
			pausableDispatcher
				?.enqueuePausableOrInvoke { uCont.resumeWith(Result.success(Unit)) }
				?: parent?.pausableDispatcher?.enqueuePausableOrInvoke { uCont.resumeWith(Result.success(Unit)) }
				?: uCont.resumeWith(Result.success(Unit))
		}
	}


	@OptIn(ExperimentalStdlibApi::class)
	internal fun derive(coroutineContext: CoroutineContext): PausableContext {
		val dispatcher = coroutineContext[CoroutineDispatcher]
		return PausableContext(this, dispatcher?.let(::PausableCoroutineDispatcher))
	}

	@OptIn(ExperimentalStdlibApi::class)
	companion object Key : AbstractCoroutineContextKey<PausableHandle, PausableContext>(
		baseKey = PausableHandle.Key,
		safeCast = { it.castOrNull() }
	)
}


