package dev.dexsr.klio.base.ktx.coroutines.pausable

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

// less intrusive implementation ?

// TODO: delay behavior
@OptIn(InternalCoroutinesApi::class)
class PausableCoroutineDispatcher internal constructor(
	private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
	private val queue: PausableDispatchQueue
) : CoroutineDispatcher() {

	private val defaultDelay by lazy<Delay> {
		val field = Class.forName("kotlinx.coroutines.DefaultExecutorKt")
			.declaredFields
			.firstOrNull { it.name == "DefaultDelay" }
		if (field != null) {
			field.isAccessible = true
			field.get(null) as? Delay
				?: error("cannot retrieve DefaultDelay, field type was not ${"Delay"}")
		} else {
			error("cannot retrieve DefaultDelay, field does not exist")
		}
	}

	override fun dispatch(context: CoroutineContext, block: Runnable) {
		val dispatch = { doDispatch(context, block) }
		if (context.nonPausable()) {
			dispatch()
			return
		}
		if (queue.offer(dispatch)) {
			return
		}
		dispatch()
	}

	@InternalCoroutinesApi
	override fun dispatchYield(context: CoroutineContext, block: Runnable) {
		val dispatch = { doDispatchYield(context, block) }
		if (context.nonPausable()) {
			dispatch()
			return
		}
		if (queue.offer(dispatch)) {
			return
		}
		dispatch()
	}

	override fun isDispatchNeeded(context: CoroutineContext): Boolean {
		if (queue.isPaused) return true
		return dispatcher.isDispatchNeeded(context)
	}

	@ExperimentalCoroutinesApi
	override fun limitedParallelism(parallelism: Int): CoroutineDispatcher {
		return PausableCoroutineDispatcher(dispatcher.limitedParallelism(parallelism), queue)
	}

	internal fun enqueuePausableOrInvoke(runnable: Runnable) {
		if (!queue.offer(runnable)) runnable.run()
	}

	val isPaused: Boolean
		get() = queue.isPaused

	fun pause(): Boolean {
		return queue.pause()
	}

	fun resume(): Boolean {
		return queue.resume()
	}

	override fun toString(): String {
		return "PausableDispatcher(actual=$dispatcher)"
	}
	private fun CoroutineContext.nonPausable() = get(NonPausable.key) != null

	private fun doDispatch(context: CoroutineContext, block: Runnable) = dispatcher.dispatch(context, block)
	@OptIn(InternalCoroutinesApi::class)
	private fun doDispatchYield(context: CoroutineContext, block: Runnable) = dispatcher.dispatchYield(context, block)
}

fun PausableCoroutineDispatcher(actual: CoroutineDispatcher): PausableCoroutineDispatcher {
	return PausableCoroutineDispatcher(actual, PausableDispatchQueue())
}

// fixme: take Interceptor
@OptIn(ExperimentalStdlibApi::class)
internal fun CoroutineContext.pausableDispatcherWrapper(): CoroutineContext {
	val dispatcher = this[CoroutineDispatcher]
		// no dispatcher is present
		?: return this
	if (dispatcher is PausableCoroutineDispatcher) {
		// already wrapped
		return this
	}
	return this + PausableCoroutineDispatcher(dispatcher)
}

@OptIn(ExperimentalStdlibApi::class)
internal fun CoroutineContext.plusDerivedPausableContext(): CoroutineContext {
	return this[PausableContext]
		?.let { context -> plus(context.derive(this)) }
		?: plusPausableContext()
}

@OptIn(ExperimentalStdlibApi::class)
internal fun CoroutineContext.plusPausableContext(): CoroutineContext {
	return plus(
		PausableContext(
			null,
			this[CoroutineDispatcher]?.let(::PausableCoroutineDispatcher)
		)
	)
}

fun CoroutineContext.pausable(): CoroutineContext {
	return plusPausableContext()
}

fun CoroutineContext.derivedPausable(): CoroutineContext {
	return plusDerivedPausableContext()
}

suspend fun checkPausable() {
	coroutineContext[PausableContext]?.checkPausable()
}
