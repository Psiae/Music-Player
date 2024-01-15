package dev.dexsr.klio.base

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

			@Volatile
			private var latestState = loopOpen()

            fun start() {
				latestState = loopStart()
			}

            fun end(): T {
				return latestState.loopEnd()
			}

            override fun LOOP_BREAK(result: T): Nothing {
				latestState = loopClose(result)
				breakLoop()
            }

            override fun LOOP_CONTINUE(): Nothing {
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
	fun <T> Any.loopEnd(): T {
		return when (this) {
			LOOP_OPEN -> error("Cannot end un-started Loop")
			LOOP_LOOPING -> error("Cannot end un-closed Loop")
			is LOOP_CLOSE<*> -> {
				@Suppress("UNCHECKED_CAST")
				result as T
			}
			else -> error("Invalid Loop State")
		}
	}

    fun shouldBreak(ex: Exception) = ex is BreakLoopException
    fun shouldContinue(ex: Exception) = ex is ContinueLoopException
}

// example use 1: { data -> this breakLoop data }
// example use 2: { data -> breakLoop(data) }
infix fun <T> ResultingLoopScope<T>.breakLoop(value: T): Nothing = LOOP_BREAK(value)

// example use 1: { data -> this continueLoop delay(pollInterval) }
// example use 2: { data -> continueLoop(delay(pollInterval)) }
infix fun <T> ResultingLoopScope<T>.continueLoop(unit: Unit): Nothing = LOOP_CONTINUE()
infix fun <T> ResultingLoopScope<T>.continueLoop(any: Any): Nothing = LOOP_CONTINUE()

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

val <T> ResultingLoopScope<T>.looper
	get() = this
