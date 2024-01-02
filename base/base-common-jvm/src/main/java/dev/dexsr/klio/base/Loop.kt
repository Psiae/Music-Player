package dev.dexsr.klio.base

// TODO: should we constraint that the [block] must return [Nothing] ?
@Suppress("FunctionName")
inline fun loop(
    block: LoopScope.() -> Unit
): Unit = (LoopScope() as LoopScopeImpl).loop(block)

// block must return nothing, that includes call to `breakLoop` or `continueLoop`
@Suppress("FunctionName")
inline fun strictLoop(
    block: LoopScope.() -> Nothing
): Unit = loop(block)

fun LoopScope(): LoopScope = LoopScopeImpl()

interface LoopScope {

    fun LOOP_BREAK(): Nothing

    fun LOOP_CONTINUE(): Nothing
}

// TODO: LoopBuilder

// public so we can inline the block
class LoopScopeImpl internal constructor(): LoopScope {

    inline fun loop(block: LoopScope.() -> Unit): Unit {
        while (true) {
            try {
                block()
            } catch (ex: Exception) {
                if (shouldBreak(ex)) break
                if (shouldContinue(ex)) continue
                throw ex
            }
        }
    }

    override fun LOOP_BREAK(): Nothing {
        throw BreakLoopException()
    }

    override fun LOOP_CONTINUE(): Nothing {
        throw ContinueLoopException()
    }

    fun shouldBreak(exception: Exception) = exception is BreakLoopException

    fun shouldContinue(exception: Exception) = exception is ContinueLoopException
}

// private ?

internal class BreakLoopException : Exception()

internal class ContinueLoopException : Exception()
