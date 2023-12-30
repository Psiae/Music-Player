package dev.dexsr.klio.base

import kotlinx.atomicfu.atomic

// TODO: improvise

// TODO: Should we constraint that the [block] must return [T] ?
inline fun <T> resultingLoop(block: ResultingLoopScope<T>.() -> Unit): T {
    return resultingLoop<T>().loop(block)
}

// block must return nothing, that includes call to `breakLoop` or `continueLoop`
inline fun <T> strictResultingLoop(block: ResultingLoopScope<T>.() -> Nothing): T {
    return resultingLoop(block)
}

fun <T> resultingLoop(): ResultingLoop = ResultingLoop()

interface ResultingLoopScope <T> {

    fun LOOP_BREAK(result: T): Nothing

    fun LOOP_CONTINUE(): Nothing
}

class ResultingLoop internal constructor() {

    inline fun <T> loop(block: ResultingLoopScope<T>.() -> Unit): T {
        return object : ResultingLoopScope<T> {

            // do we really need synchronized FU ?
            private val state = atomic(loopOpen())

            fun start() {
				state.compareAndSet(loopOpen(), loopStart())
			}

            fun end(): T {
				state.compareAndSet(loopClose(state.value), loopEnd(state.value))
				return state.value as T
			}

            override fun LOOP_BREAK(result: T): Nothing {
                check(state.compareAndSet(loopStart(), loopClose(result)))
                breakLoop()
            }

            override fun LOOP_CONTINUE(): Nothing {
				check(state.value == loopStart())
                continueLoop()
            }
        }.run {
            start()
            while (true) {
                try {
                    block()
                } catch (ex: Exception) {
                    if (shouldBreak(ex)) break
                    if (shouldContinue(ex)) continue
                    throw ex
                }
            }
            end()
        }
    }

    fun breakLoop(): Nothing = throw BreakLoopException()
    fun continueLoop(): Nothing = throw ContinueLoopException()

	fun loopOpen(): Any = LOOP_OPEN
	fun loopStart(): Any = LOOP_LOOPING
	fun loopClose(value: Any?): Any = LOOP_CLOSE(value)
	fun <T> loopEnd(value: Any): T {
		return when (val state = value) {
			LOOP_OPEN -> error("Cannot end un-started Loop")
			LOOP_LOOPING -> error("Cannot end un-closed Loop")
			is LOOP_CLOSE<*> -> {
				@Suppress("UNCHECKED_CAST")
				state.result as? T ?: error("Invalid Loop Result")
			}
			else -> error("Invalid Loop State")
		}
	}

    fun shouldBreak(ex: Exception) = ex is BreakLoopException
    fun shouldContinue(ex: Exception) = ex is ContinueLoopException
}

private object LOOP_OPEN
private object LOOP_LOOPING
private class LOOP_CLOSE<T>(val result: T) {

	override fun equals(other: Any?): Boolean {
		return other is LOOP_CLOSE<*> && other.result === result
	}

	override fun hashCode(): Int {
		var hash = 0
		hash += result.hashCode()
		return hash
	}
}
