package dev.dexsr.klio.base

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.loop as fuLoop

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
            private val state = atomic(Any()).apply { loopOpen() }

            fun start() = state.loopStart()

            fun end() = state.loopEnd<T>()

            override fun LOOP_BREAK(result: T): Nothing {
                state.loopClose(result)
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

    fun AtomicRef<Any>.loopOpen() {
        value = LOOP_OPEN
    }

    fun AtomicRef<Any>.loopStart() {
        fuLoop { state ->
            when (state) {
                LOOP_OPEN -> if (compareAndSet(state, LOOP_LOOPING)) return
                LOOP_LOOPING -> error("Cannot start an already looping Loop")
                is LOOP_CLOSE<*> -> error("Cannot end an already closed Loop ")
                else -> error("Invalid State")
            }
        }
    }

    fun AtomicRef<Any>.loopClose(result: Any?) {
        fuLoop { state ->
            when (state) {
                LOOP_OPEN -> error("Cannot close un-started Loop")
                LOOP_LOOPING -> if (compareAndSet(state, LOOP_CLOSE(result))) return
                is LOOP_CLOSE<*> -> error("Cannot close an already closed Loop ")
                else -> error("Invalid State")
            }
        }
    }

    // reify ?
    fun <T> AtomicRef<Any>.loopEnd(): T {
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

    companion object {
        val OPEN: Any get() = LOOP_OPEN
    }
}

private object LOOP_OPEN
private object LOOP_LOOPING
private class LOOP_CLOSE<T>(val result: T)
