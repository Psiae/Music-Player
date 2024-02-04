package dev.dexsr.klio.base.ktx.coroutines.pausable

import kotlin.coroutines.CoroutineContext

interface PausableHandle : CoroutineContext.Element {

	val isPaused: Boolean

	fun pause(): Boolean

	fun resume(): Boolean

	companion object Key : CoroutineContext.Key<PausableHandle>
}
