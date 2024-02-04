package dev.dexsr.klio.base.ktx.coroutines.pausable

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object NonPausable : AbstractCoroutineContextElement(Key) {
	private object Key : CoroutineContext.Key<NonPausable>
}
